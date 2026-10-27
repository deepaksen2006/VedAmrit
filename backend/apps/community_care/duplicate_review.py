from django.db import transaction
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework.exceptions import ValidationError

from .geocoding import snapshot
from .models import Hospital, HospitalDuplicateReview, VerificationAudit
from .services import check_revision, Conflict


def duplicate_is_clear(hospital):
    review = HospitalDuplicateReview.objects.filter(hospital=hospital).first()
    if review is None:
        return not bool(hospital.import_provenance.get("duplicate_candidates"))
    return review.status == "DISTINCT_CONFIRMED" and review.snapshot == snapshot(hospital)


@transaction.atomic
def record_duplicate_review(actor, hospital_id, status, expected_revision, expected_review_revision, reason,
                            evidence_url="", related_hospital_id=None, human_confirmed=False):
    from .location_review import require_independent_verifier, validate_evidence_url
    hospital = get_object_or_404(Hospital.objects.select_for_update(), pk=hospital_id)
    require_independent_verifier(actor, hospital)
    check_revision(hospital, expected_revision)
    if not human_confirmed:
        raise ValidationError("Explicit human duplicate review confirmation is required.")
    if status not in HospitalDuplicateReview.Status.values:
        raise ValidationError({"status": "Unknown duplicate review status."})
    if len(reason.strip()) < 20:
        raise ValidationError({"reason": "Describe the independent identity checks (at least 20 characters)."})
    review, _ = HospitalDuplicateReview.objects.get_or_create(hospital=hospital)
    review = HospitalDuplicateReview.objects.select_for_update().get(pk=review.pk)
    if review.revision != expected_review_revision:
        raise Conflict("The duplicate review changed. Reload before reviewing again.")
    related = None
    if status != "NEEDS_REVIEW":
        evidence_url = validate_evidence_url(evidence_url, required=True)
    elif evidence_url:
        evidence_url = validate_evidence_url(evidence_url)
    if status == "DUPLICATE_CONFIRMED":
        if not related_hospital_id or related_hospital_id == hospital.pk:
            raise ValidationError({"related_hospital": "Select a different retained facility record."})
        related = get_object_or_404(Hospital, pk=related_hospital_id)
    previous = review.status
    review.status, review.related_hospital = status, related
    review.snapshot, review.evidence_url = snapshot(hospital), evidence_url
    review.reviewed_by, review.reviewed_at, review.reason = actor, timezone.now(), reason.strip()
    review.revision += 1
    review.full_clean()
    review.save()
    # A pending/confirmed-duplicate decision revokes location eligibility, not hospital identity.
    if status != "DISTINCT_CONFIRMED" and hospital.location_status == "APPROVED":
        Hospital.objects.filter(pk=hospital.pk).update(location_status="STALE", revision=hospital.revision + 1, updated_at=timezone.now())
    VerificationAudit.objects.create(actor=actor, record_type="HOSPITAL", record_id=hospital.pk, action="DUPLICATE_REVIEW",
        previous_status=hospital.verification_status, new_status=hospital.verification_status, reason=reason.strip(),
        details={"previous_duplicate_status": previous, "new_duplicate_status": status, "review_revision": review.revision,
                 "evidence_url": evidence_url, "related_hospital_id": related.pk if related else None})
    return review
