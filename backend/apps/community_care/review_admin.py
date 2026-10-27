from django.contrib import messages
from django.core import signing
from django.core.exceptions import PermissionDenied
from django.http import HttpResponseRedirect
from django.shortcuts import get_object_or_404
from django.template.response import TemplateResponse
from django.urls import reverse
from rest_framework.exceptions import APIException

from .duplicate_review import duplicate_is_clear, record_duplicate_review
from .geocoding import snapshot
from .location_review import approve_manual_location
from .models import Hospital, HospitalDuplicateReview, LocationCandidate, VerificationAudit
from .permissions import can_verify, is_directory_admin
from .review_forms import DuplicateReviewForm, ManualLocationForm, map_links

SALT = "community-care.exact-location-preview"


def manual_payload(form):
    data = form.cleaned_data
    return {"expected_revision": data["expected_revision"], "submission_id": str(data["submission_id"]),
            "latitude": str(data["latitude"]), "longitude": str(data["longitude"]), "evidence_url": data["evidence_url"],
            "reason": data["reason"], "based_on_candidate_id": data["based_on_candidate"].pk if data["based_on_candidate"] else None,
            "exact_location_confirmed": data["exact_location_confirmed"], "duplicate_review_resolved": data["duplicate_review_resolved"]}


def hospital_location_review(model_admin, request, object_id):
    if not is_directory_admin(request.user):
        raise PermissionDenied
    hospital = get_object_or_404(Hospital, pk=object_id)
    verifier = can_verify(request.user)
    duplicate = HospitalDuplicateReview.objects.filter(hospital=hospital).first()
    flagged = bool(duplicate or hospital.import_provenance.get("duplicate_candidates"))
    action = request.POST.get("review_action") if request.method == "POST" else None
    manual = ManualLocationForm(request.POST if request.method == "POST" and action != "save_duplicate_review" else None,
        hospital=hospital, initial={"expected_revision": hospital.revision})
    duplicates = DuplicateReviewForm(request.POST if action == "save_duplicate_review" else None, hospital=hospital,
        initial={"expected_revision": hospital.revision, "expected_review_revision": duplicate.revision if duplicate else 1,
                 "status": duplicate.status if duplicate else "NEEDS_REVIEW"})
    preview, token = None, None
    if request.method == "POST":
        if not verifier:
            raise PermissionDenied
        if action == "save_duplicate_review" and duplicates.is_valid():
            data = duplicates.cleaned_data
            try:
                record_duplicate_review(request.user, hospital.pk, data["status"], data["expected_revision"], data["expected_review_revision"],
                    data["reason"], data["evidence_url"], data["related_hospital"].pk if data["related_hospital"] else None, data["human_confirmed"])
                messages.success(request, "Duplicate review audited. No hospital was merged, deleted or verified.")
                return HttpResponseRedirect(request.path)
            except APIException as error:
                duplicates.add_error(None, str(error))
        elif action in {"preview_manual", "approve_manual"} and manual.is_valid():
            payload = manual_payload(manual)
            envelope = {"hospital_id": hospital.pk, "actor_id": request.user.pk, "payload": payload}
            if action == "preview_manual":
                preview = {**payload, "maps": map_links(payload["latitude"], payload["longitude"])}
                token = signing.dumps(envelope, salt=SALT)
            else:
                try:
                    saved = signing.loads(request.POST.get("preview_token", ""), salt=SALT, max_age=600)
                    if saved != envelope:
                        raise signing.BadSignature()
                    if not manual.cleaned_data["confirm_save"]:
                        manual.add_error("confirm_save", "Explicitly authorize saving after reviewing the preview.")
                    else:
                        approve_manual_location(request.user, hospital.pk, **payload, confirm_save=True)
                        messages.success(request, "Manual coordinates approved and audited. Hospital identity status and booking are unchanged.")
                        return HttpResponseRedirect(request.path)
                except signing.BadSignature:
                    manual.add_error(None, "Preview again: the confirmation expired or the coordinates/evidence/reviewer changed.")
                except APIException as error:
                    manual.add_error(None, str(error))
        elif action not in {"save_duplicate_review", "preview_manual", "approve_manual"}:
            manual.add_error(None, "Choose a review action. No data was saved.")
    candidates = []
    for candidate in LocationCandidate.objects.filter(run__hospital=hospital).select_related("run", "reviewed_by").order_by("run_id", "id"):
        candidates.append({"point": candidate, "maps": map_links(candidate.latitude, candidate.longitude),
                           "stale": candidate.run.snapshot != snapshot(hospital),
                           "original_address": candidate.run.snapshot.get("address", "")})
    history = VerificationAudit.objects.filter(record_type="HOSPITAL", record_id=hospital.pk,
        action__in=["LOCATION_APPROVED", "LOCATION_REJECTED", "DUPLICATE_REVIEW"]).select_related("actor")
    context = {**model_admin.admin_site.each_context(request), "opts": Hospital._meta, "original": hospital,
        "title": "Exact location review — " + hospital.name, "hospital": hospital, "candidates": candidates,
        "manual_form": manual, "duplicate_form": duplicates, "can_verify": verifier, "flagged": flagged,
        "duplicate_review": duplicate, "duplicate_clear": duplicate_is_clear(hospital), "preview": preview, "preview_token": token,
        "history": history, "back_url": reverse("admin:community_care_hospital_change", args=[hospital.pk])}
    return TemplateResponse(request, "admin/community_care/hospital/location_review.html", context)
