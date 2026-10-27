import hashlib
import json
from decimal import Decimal, InvalidOperation
from urllib.parse import parse_qs, urlsplit
from uuid import UUID

from django.core.exceptions import ValidationError as DjangoValidationError
from django.core.validators import URLValidator
from django.db import transaction
from django.db.models import F
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework.exceptions import PermissionDenied, ValidationError

from .geocoding import snapshot
from .models import Hospital, HospitalMembership, GeocodingRun, LocationCandidate, VerificationAudit
from .permissions import can_verify
from .services import Conflict, check_revision


def require_independent_verifier(actor, hospital):
    if not can_verify(actor):
        raise PermissionDenied("An authorized VedAmrit verifier is required for location decisions.")
    if HospitalMembership.objects.filter(hospital=hospital, user=actor, role="ADMIN", is_active=True).exists():
        raise PermissionDenied("Use an independent VedAmrit verifier for an assigned hospital.")


def validate_evidence_url(value, required=False):
    value = (value or "").strip()
    if not value and not required:
        return ""
    try:
        URLValidator(schemes=["http", "https"])(value)
        parts = urlsplit(value)
        if len(value) > 1000 or parts.username or parts.password or any(k.casefold() in {"key", "api_key", "access_token", "token"} for k in parse_qs(parts.query)):
            raise ValueError()
    except (DjangoValidationError, ValueError):
        raise ValidationError({"evidence_url": "Provide a public HTTP(S) facility evidence URL without credentials or API tokens."}) from None
    return value


@transaction.atomic
def review_location(actor, pk, decision, expected_revision, reason, exact_location_confirmed=False, duplicate_review_resolved=False, evidence_url=""):
    if not can_verify(actor):
        raise PermissionDenied("An authorized VedAmrit verifier is required for location decisions.")
    candidate = get_object_or_404(LocationCandidate.objects.select_related("run"), pk=pk)
    hospital = Hospital.objects.select_for_update().get(pk=candidate.run.hospital_id)
    candidate = LocationCandidate.objects.select_for_update().get(pk=pk)
    require_independent_verifier(actor, hospital)
    check_revision(hospital, expected_revision)
    if candidate.status != "PROPOSED":
        raise Conflict("This proposal has already been reviewed.")
    reason = reason.strip()
    if len(reason) < 10:
        raise ValidationError({"reason": "Record the independent location checks and evidence (at least 10 characters)."})
    if decision == "APPROVE":
        if snapshot(hospital) != candidate.run.snapshot:
            raise Conflict("Hospital identity/address changed. Generate and review a current proposal.")
        if not exact_location_confirmed:
            raise ValidationError({"exact_location_confirmed": "Independently confirm the exact facility position, not a city/street centroid."})
        from .duplicate_review import duplicate_is_clear
        if not duplicate_is_clear(hospital):
            raise ValidationError({"duplicate_review_resolved": "Record a current DISTINCT_CONFIRMED duplicate review first; a checkbox alone is insufficient."})
        if (hospital.import_provenance.get("duplicate_candidates") or "DUPLICATE_REVIEW_REQUIRED" in candidate.flags) and not duplicate_review_resolved:
            raise ValidationError({"duplicate_review_resolved": "Confirm the recorded duplicate review before proceeding."})
        evidence_url = validate_evidence_url(evidence_url or candidate.evidence_url,
            required=candidate.provider == "Manual confirmation" or "BROAD_LOCATION_NOT_CONFIRMED_FACILITY" in candidate.flags)
        # This guarded, audited update deliberately preserves identity-verification status.
        # General coordinate/address edits still go through Hospital.save() and invalidate location approval.
        Hospital.objects.filter(pk=hospital.pk).update(latitude=candidate.latitude, longitude=candidate.longitude,
            location_status="APPROVED", approved_location=candidate, revision=F("revision") + 1, updated_at=timezone.now())
        candidate.status = "APPROVED"
    elif decision == "REJECT":
        candidate.status = "REJECTED"
    else:
        raise ValidationError("Unknown location decision.")
    candidate.reviewed_by, candidate.review_reason, candidate.reviewed_at = actor, reason, timezone.now()
    if evidence_url:
        candidate.evidence_url = validate_evidence_url(evidence_url)
    candidate.save(update_fields=["status", "reviewed_by", "review_reason", "reviewed_at", "evidence_url"])
    VerificationAudit.objects.create(actor=actor, record_type="HOSPITAL", record_id=hospital.pk,
        action="LOCATION_APPROVED" if decision == "APPROVE" else "LOCATION_REJECTED", previous_status=hospital.verification_status,
        new_status=hospital.verification_status, reason=reason, details={"candidate_id": candidate.pk,
            "exact_location_confirmed": exact_location_confirmed, "duplicate_review_resolved": duplicate_review_resolved,
            "previous_location_status": hospital.location_status, "reviewed_hospital_revision": expected_revision,
            "new_hospital_revision": expected_revision + (1 if decision == "APPROVE" else 0), "evidence_url": candidate.evidence_url,
            "previous_approved_candidate_id": hospital.approved_location_id, "latitude": str(candidate.latitude), "longitude": str(candidate.longitude)})
    return candidate


@transaction.atomic
def approve_manual_location(actor, hospital_id, expected_revision, latitude, longitude, evidence_url, reason,
                            submission_id, exact_location_confirmed=False, confirm_save=False, duplicate_review_resolved=False,
                            based_on_candidate_id=None):
    if not can_verify(actor):
        raise PermissionDenied("An authorized VedAmrit verifier is required.")
    hospital = get_object_or_404(Hospital.objects.select_for_update(), pk=hospital_id)
    require_independent_verifier(actor, hospital)
    check_revision(hospital, expected_revision)
    if not exact_location_confirmed or not confirm_save:
        raise ValidationError("Preview and explicitly confirm the manually verified coordinates before saving.")
    evidence_url = validate_evidence_url(evidence_url, required=True)
    if len(reason.strip()) < 20:
        raise ValidationError({"reason": "Record independent location evidence and checks (at least 20 characters)."})
    try:
        lat, lon = Decimal(str(latitude)), Decimal(str(longitude))
        if not lat.is_finite() or not lon.is_finite() or not -90 <= lat <= 90 or not -180 <= lon <= 180:
            raise ValueError()
        if lat.as_tuple().exponent < -6 or lon.as_tuple().exponent < -6:
            raise ValueError()
        nonce = str(UUID(str(submission_id)))
    except (InvalidOperation, ValueError, TypeError):
        raise ValidationError("Supply valid coordinates with at most six decimal places and a valid submission ID.") from None
    original = get_object_or_404(LocationCandidate, pk=based_on_candidate_id, run__hospital=hospital) if based_on_candidate_id else None
    digest = hashlib.sha256(("manual-confirmation:" + nonce).encode()).hexdigest()
    if GeocodingRun.objects.filter(hospital=hospital, query_hash=digest).exists():
        raise Conflict("This manual submission was already processed. Reload before making another decision.")
    run = GeocodingRun.objects.create(hospital=hospital, query_hash=digest, query="Manual exact-location review; no provider request.",
                                      snapshot=snapshot(hospital), status="SUCCESS")
    point = LocationCandidate(run=run, fingerprint=hashlib.sha256(json.dumps([str(lat), str(lon), evidence_url]).encode()).hexdigest(),
        latitude=lat, longitude=lon, confidence=None, provider="Manual confirmation", provider_reference="manual:" + nonce,
        label="Manually confirmed position for " + hospital.name, feature_types=["manual_confirmation"],
        attribution="Independent facility evidence recorded by a VedAmrit verifier.", flags=["MANUALLY_CONFIRMED_COORDINATES"],
        evidence_url=evidence_url, based_on_candidate=original)
    point.full_clean()
    point.save()
    return review_location(actor, point.pk, "APPROVE", expected_revision, reason, exact_location_confirmed,
                           duplicate_review_resolved, evidence_url=evidence_url)
