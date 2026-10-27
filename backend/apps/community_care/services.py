from django.core.exceptions import ValidationError as ModelValidationError
from django.db import IntegrityError, transaction
from django.shortcuts import get_object_or_404
from rest_framework.exceptions import APIException, PermissionDenied, ValidationError

from apps.doctors.models import DoctorProfile
from .base import VerificationStatus as S
from .models import Hospital, HospitalMembership, DoctorHospitalAffiliation, VerificationAudit
from .permissions import can_verify, doctor_scope, hospital_scope, is_directory_admin, require_directory_admin


class Conflict(APIException):
    status_code = 409
    default_detail = "The record changed. Reload it before retrying."


def checked_save(record, actor, *, action=None, reason=""):
    record._care_actor = actor
    if action:
        record._care_action = action
    record._care_reason = reason
    if isinstance(record, DoctorProfile):
        record.is_verified = record.verification_status == S.VERIFIED
    try:
        # Savepoint keeps a duplicate/constraint failure from breaking the caller's transaction.
        with transaction.atomic():
            record.full_clean()
            record.save()
    except ModelValidationError as error:
        raise ValidationError(getattr(error, "message_dict", error.messages)) from error
    except IntegrityError as error:
        raise ValidationError({"detail": "A duplicate record or conflicting database constraint was detected."}) from error
    return record


def check_revision(record, expected_revision):
    if expected_revision != record.revision:
        raise Conflict()


@transaction.atomic
def create_hospital(actor, values):
    if not is_directory_admin(actor) and not hospital_scope(actor, write=True).exists():
        raise PermissionDenied("Only VedAmrit administrators or assigned hospital administrators may propose hospitals.")
    # A proposal NEVER grants membership. A VedAmrit admin must explicitly assign it.
    if set(values).intersection({"import_review_status", "import_review_notes"}) and not is_directory_admin(actor):
        raise PermissionDenied("Only a VedAmrit administrator may record import review decisions.")
    return checked_save(Hospital(created_by=actor, **values), actor)


@transaction.atomic
def create_doctor(actor, values, *, hospital_id=None, user=None, consultation_mode="IN_PERSON", fees=None):
    if user is not None:
        require_directory_admin(actor)
        if hasattr(user, "doctor_profile"):
            raise ValidationError({"user_id": "This user already has a doctor profile. Manage that existing record."})
    hospital = None
    if hospital_id is not None:
        hospital = get_object_or_404(Hospital.objects.select_for_update(), pk__in=hospital_scope(actor, write=True).values("pk"), pk=hospital_id)
    elif not is_directory_admin(actor):
        raise ValidationError({"hospital_id": "An assigned hospital is required."})
    record = checked_save(DoctorProfile(created_by=actor, user=user, **values), actor)
    if hospital:
        affiliation = DoctorHospitalAffiliation(doctor=record, hospital=hospital, consultation_mode=consultation_mode,
                                               fees=fees if fees is not None else record.consultation_fee)
        checked_save(affiliation, actor)
    return record


@transaction.atomic
def update_record(actor, model, pk, values, *, expected_revision=None):
    scope = hospital_scope(actor, write=True) if model is Hospital else doctor_scope(actor, write=True)
    record = get_object_or_404(model.objects.select_for_update(), pk__in=scope.values("pk"), pk=pk)
    if expected_revision is not None:
        check_revision(record, expected_revision)
    if model is Hospital and set(values).intersection({"import_review_status", "import_review_notes"}):
        require_directory_admin(actor)
    allowed = set(record.critical_fields) | {"is_active"}
    if model is DoctorProfile:
        allowed |= {"consultation_fee", "available_timings"}
    if set(values) - allowed:
        raise ValidationError("Verification fields, account linkage and ownership are not editable here.")
    for field, value in values.items():
        if field == "user":
            raise ValidationError("Account linkage is immutable through this endpoint.")
        setattr(record, field, value)
    return checked_save(record, actor)


@transaction.atomic
def link_doctor_user(actor, pk, user, expected_revision, reason):
    require_directory_admin(actor)
    record = get_object_or_404(DoctorProfile.objects.select_for_update(), pk=pk)
    check_revision(record, expected_revision)
    if record.user_id is not None:
        raise Conflict("This doctor already has a login account. Account reassignment is not supported.")
    if DoctorProfile.objects.filter(user=user).exists():
        raise ValidationError({"user_id": "This user already has a doctor profile. Use that existing identity."})
    record.user = user
    return checked_save(record, actor, action="LINK_USER", reason=reason)


def submission_errors(record):
    if isinstance(record, Hospital):
        try:
            record.validate_ready_for_review()
        except ModelValidationError as error:
            raise ValidationError(getattr(error, "message_dict", error.messages)) from error
    if isinstance(record, DoctorProfile):
        errors = {}
        if not record.registration_council.strip():
            errors["registration_council"] = "Identify the issuing council for credential review."
        if not record.qualifications:
            errors["qualifications"] = "Provide qualifications for credential review."
        if errors:
            raise ValidationError(errors)
    # Includes syntax validation; this is not external credential verification.
    try:
        record.full_clean()
    except ModelValidationError as error:
        raise ValidationError(getattr(error, "message_dict", error.messages)) from error


@transaction.atomic
def transition(actor, model, pk, action, expected_revision, reason, *, credential_review=None):
    if action == "SUBMIT":
        scope = hospital_scope(actor, write=True) if model is Hospital else doctor_scope(actor, write=True)
    else:
        if not can_verify(actor):
            raise PermissionDenied("Only an explicitly authorized VedAmrit verifier may make verification decisions.")
        scope = model.objects.all()
    record = get_object_or_404(model.objects.select_for_update(), pk__in=scope.values("pk"), pk=pk)
    check_revision(record, expected_revision)
    reason = reason.strip()
    if len(reason) < 3:
        raise ValidationError({"reason": "Record a meaningful reason (at least three characters)."})
    transitions = {
        "SUBMIT": ({S.DRAFT, S.REJECTED}, S.PENDING),
        "VERIFY": ({S.PENDING}, S.VERIFIED),
        "REJECT": ({S.PENDING}, S.REJECTED),
        "SUSPEND": ({S.VERIFIED}, S.SUSPENDED),
        # A suspension cannot be cleared by a hospital admin's edit/resubmission.
        "REOPEN": ({S.SUSPENDED}, S.PENDING),
    }
    allowed, new_status = transitions[action]
    if record.verification_status not in allowed:
        raise Conflict(f"Cannot {action.lower()} a {record.verification_status} record.")
    if action == "VERIFY":
        owns_hospital = HospitalMembership.objects.filter(user=actor, is_active=True, role="ADMIN")
        if (model is Hospital and owns_hospital.filter(hospital=record).exists()) or (
            model is DoctorProfile and (record.user_id == actor.pk or owns_hospital.filter(
                hospital__doctor_affiliations__doctor=record, hospital__doctor_affiliations__is_active=True).exists())):
            raise PermissionDenied("Use an independent VedAmrit verifier; administrators cannot verify their own credentials or assigned hospital records.")
    if action in ("SUBMIT", "VERIFY", "REOPEN"):
        submission_errors(record)
    if model is DoctorProfile and action == "VERIFY":
        from .serializers import CredentialReviewSerializer
        review = CredentialReviewSerializer(data=credential_review if credential_review is not None else {})
        review.is_valid(raise_exception=True)
        record._care_audit_details = {"credential_review": {
            **review.validated_data, "protocol": "doctor-credentials-v1",
        }}
    elif credential_review is not None:
        raise ValidationError({"credential_review": "Credential review is only accepted for a doctor VERIFY decision."})
    record.verification_status = new_status
    return checked_save(record, actor, action=action, reason=reason)


@transaction.atomic
def save_membership(actor, values, pk=None):
    require_directory_admin(actor)
    record = get_object_or_404(HospitalMembership.objects.select_for_update(), pk=pk) if pk else HospitalMembership()
    # Membership identities are fixed; deactivate rather than reassigning a row.
    if pk and set(values) - {"role", "is_active"}:
        raise ValidationError("Only role and active status may be changed. Create a separate membership for a different hospital/user.")
    for key, value in values.items():
        setattr(record, key, value)
    checked_save(record, actor)
    VerificationAudit.objects.create(actor=actor, record_type="HOSPITAL", record_id=record.hospital_id,
        action="MEMBERSHIP_UPDATED" if pk else "MEMBERSHIP_CREATED", previous_status=record.hospital.verification_status,
        new_status=record.hospital.verification_status, reason="VedAmrit administrator assigned hospital access.",
        details={"membership_id": record.pk, "user_id": record.user_id, "role": record.role, "is_active": record.is_active})
    # No is_staff, is_superuser, group or permission mutation.
    return record


@transaction.atomic
def save_affiliation(actor, values, pk=None):
    if pk:
        scope = hospital_scope(actor, write=True)
        record = get_object_or_404(DoctorHospitalAffiliation.objects.select_for_update(), pk=pk, hospital__in=scope)
        if set(values) - {"consultation_mode", "fees", "is_active"}:
            raise ValidationError("Doctor and hospital identities are immutable. Create another affiliation instead.")
    else:
        hospital = get_object_or_404(hospital_scope(actor, write=True), pk=values.pop("hospital_id"))
        doctor = get_object_or_404(doctor_scope(actor, write=True), pk=values.pop("doctor_id"))
        record = DoctorHospitalAffiliation(hospital=hospital, doctor=doctor)
    for key, value in values.items():
        setattr(record, key, value)
    checked_save(record, actor)
    VerificationAudit.objects.create(actor=actor, record_type="DOCTOR", record_id=record.doctor_id,
        action="AFFILIATION_UPDATED" if pk else "AFFILIATION_CREATED", previous_status=record.doctor.verification_status,
        new_status=record.doctor.verification_status, reason="Hospital affiliation managed.",
        details={"affiliation_id": record.pk, "hospital_id": record.hospital_id})
    return record
