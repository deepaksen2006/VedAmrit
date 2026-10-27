"""Task 4.2D: inspect UDR-001 and public gates without writing live records."""
import json
import os
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "backend"))
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "vedaahar_backend.settings")
import django
django.setup()

from django.contrib.auth import get_user_model
from django.db import connection, transaction
from django.test import override_settings
from rest_framework.exceptions import ValidationError
from rest_framework.test import APIClient
from apps.community_care import services
from apps.community_care.duplicate_review import duplicate_is_clear
from apps.community_care.models import Hospital, HospitalMembership, VerificationAudit
from apps.community_care.permissions import can_verify

with transaction.atomic():
    if connection.vendor == "postgresql":
        with connection.cursor() as cursor:
            cursor.execute("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ, READ ONLY")
    h = Hospital.objects.select_related("approved_location__run").get(source_record_id="UDR-001")
    fields = ("id", "name", "facility_type", "address", "city", "district", "state", "postal_code", "phone", "email",
              "description", "facilities", "listed_hours_unverified", "services_unverified", "source_namespace",
              "source_record_id", "source_url", "import_review_status", "import_review_notes", "verification_status",
              "is_active", "revision", "latitude", "longitude", "location_status", "approved_location_id", "booking_enabled")
    hospital = {field: getattr(h, field) for field in fields}
    hospital["import_flags"] = h.import_provenance.get("duplicate_candidates", [])
    try:
        services.submission_errors(h)
        readiness = {}
    except ValidationError as error:
        readiness = error.detail
    c = h.approved_location
    candidate = None if c is None else {field: getattr(c, field) for field in
        ("id", "status", "latitude", "longitude", "provider", "evidence_url", "review_reason", "reviewed_at", "reviewed_by_id")}
    eligible = []
    for user in get_user_model().objects.filter(is_active=True):
        if can_verify(user):
            assigned = HospitalMembership.objects.filter(hospital=h, user=user, is_active=True, role="ADMIN").exists()
            eligible.append({"user_id": user.pk, "admin_login_allowed": user.is_staff,
                             "assigned_hospital_admin": assigned, "independent_for_target": not assigned})
    endpoints = {}
    with override_settings(ALLOWED_HOSTS=["testserver"]):
        client = APIClient()
        for url in ("/api/care/hospitals/", "/api/care/hospitals/?search=Madan", f"/api/care/hospitals/{h.pk}/",
                    "/api/care/hospitals/nearby/?latitude=24.588336&longitude=73.679142&radius_km=0.01",
                    "/api/care/hospitals/nearby/?latitude=24.588336&longitude=73.679142&radius_km=20",
                    "/api/care/manage/hospitals/", "/api/care/admin/location-candidates/",
                    "/api/care/hospitals/nearby/?latitude=NaN&longitude=73"):
            response = client.get(url)
            endpoints[url] = {"status": response.status_code}
            if isinstance(getattr(response, "data", None), dict) and "count" in response.data:
                endpoints[url].update(count=response.data["count"], results=response.data["results"])
    audits = list(VerificationAudit.objects.filter(record_type="HOSPITAL", record_id=h.pk).order_by("id").values(
        "id", "action", "previous_status", "new_status", "actor_id", "reason", "timestamp", "details"))
    result = {"hospital": hospital, "approved_location": candidate, "duplicate_clear": duplicate_is_clear(h),
              "submission_blockers": readiness, "authorized_account_capabilities": eligible,
              "authenticated_verifier_session_used": False, "audit_history": audits, "public_api_checks": endpoints}
    print(json.dumps(result, default=str, indent=2))
