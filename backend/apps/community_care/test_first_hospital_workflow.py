"""Isolated first-hospital workflow fixtures. Never executed against live data."""
from decimal import Decimal

from django.contrib.auth import get_user_model
from django.contrib.auth.models import Permission
from django.test import override_settings
from rest_framework.exceptions import PermissionDenied, ValidationError
from rest_framework.test import APITestCase

from . import services
from .geocoding import query_details
from .location_review import review_location
from .models import Hospital, HospitalMembership, GeocodingRun, LocationCandidate, VerificationAudit


@override_settings(PASSWORD_HASHERS=["django.contrib.auth.hashers.MD5PasswordHasher"])
class FirstHospitalWorkflowTests(APITestCase):
    def setUp(self):
        users = get_user_model()
        self.verifier = users.objects.create_superuser("independent-fixture-verifier", "verifier@example.test", "fixture-only")
        self.manager = users.objects.create_user("fixture-manager", is_staff=True)
        self.manager.user_permissions.add(Permission.objects.get(content_type__app_label="community_care", codename="manage_directory"))
        self.hospital = services.create_hospital(self.manager, {
            "name": "Isolated first hospital fixture", "address": "1 Isolated Fixture Street",
            "city": "Udaipur", "district": "Udaipur", "state": "Rajasthan", "postal_code": "313004",
            "phone": "9900000011", "facilities": [],
        })
        # Imported state is test setup only, never a live import or identity approval.
        Hospital.objects.filter(pk=self.hospital.pk).update(source_namespace="isolated_first_hospital",
            source_record_id="FIXTURE-001", import_review_status="NEEDS_REVIEW")
        self.hospital.refresh_from_db()
        snapshot, query, digest = query_details(self.hospital)
        run = GeocodingRun.objects.create(hospital=self.hospital, snapshot=snapshot, query=query, query_hash=digest, status="SUCCESS")
        self.position = LocationCandidate.objects.create(run=run, fingerprint="isolated-exact-position",
            latitude="24.588336", longitude="73.679142", label="Isolated test position", attribution="Synthetic fixture")
        review_location(self.verifier, self.position.pk, "APPROVE", self.hospital.revision,
            "Exact test-fixture position independently reviewed; not evidence for any real facility.", True)
        self.hospital.refresh_from_db()

    def act(self, action, actor=None, revision=None):
        self.hospital.refresh_from_db()
        return services.transition(actor or self.verifier, Hospital, self.hospital.pk, action,
            self.hospital.revision if revision is None else revision,
            "Independent isolated-fixture review; no real-world verification claim.")

    def review_import(self):
        services.update_record(self.manager, Hospital, self.hospital.pk, {
            "import_review_status": "REVIEWED",
            "import_review_notes": "Isolated fixture source, contact, address and duplicate review completed for test only.",
        }, expected_revision=self.hospital.revision)
        self.hospital.refresh_from_db()

    def publish(self):
        self.review_import()
        self.act("SUBMIT", self.manager)
        self.act("VERIFY")

    def nearby(self, latitude="24.588336", radius="0.01"):
        return self.client.get(f"/api/care/hospitals/nearby/?latitude={latitude}&longitude=73.679142&radius_km={radius}")

    def assert_private(self):
        self.assertEqual(self.client.get("/api/care/hospitals/?search=Isolated").data["count"], 0)
        self.assertEqual(self.nearby().data["count"], 0)
        self.assertEqual(self.client.get(f"/api/care/hospitals/{self.hospital.pk}/").status_code, 404)

    def test_legal_workflow_keeps_location_and_audit_history_and_publishes_only_after_verify(self):
        original_position = LocationCandidate.objects.values().get(pk=self.position.pk)
        original_audit = list(VerificationAudit.objects.order_by("id").values())
        self.assert_private()
        with self.assertRaises(ValidationError):
            self.act("SUBMIT", self.manager)
        self.review_import()
        self.assert_private()
        self.act("SUBMIT", self.manager)
        self.assert_private()
        self.act("VERIFY")
        self.hospital.refresh_from_db()
        self.assertEqual((self.hospital.verification_status, self.hospital.location_status), ("VERIFIED", "APPROVED"))
        self.assertEqual((self.hospital.latitude, self.hospital.longitude), (Decimal("24.588336"), Decimal("73.679142")))
        self.assertEqual(LocationCandidate.objects.values().get(pk=self.position.pk), original_position)
        for audit in original_audit:
            self.assertEqual(VerificationAudit.objects.values().get(pk=audit["id"]), audit)
        for action, previous, current, actor in (("SUBMIT", "DRAFT", "PENDING", self.manager), ("VERIFY", "PENDING", "VERIFIED", self.verifier)):
            row = VerificationAudit.objects.get(action=action)
            self.assertEqual((row.previous_status, row.new_status, row.actor_id), (previous, current, actor.pk))
            self.assertTrue(row.reason)
        result = self.nearby()
        self.assertEqual(result.data["count"], 1)
        self.assertEqual(result.data["results"][0]["id"], self.hospital.pk)
        self.assertFalse(result.data["results"][0]["booking_enabled"])

    def test_direct_draft_verification_is_rejected_without_writing_an_audit(self):
        count = VerificationAudit.objects.count()
        with self.assertRaises(services.Conflict):
            self.act("VERIFY")
        self.assertEqual(VerificationAudit.objects.count(), count)
        self.assert_private()

    def test_manager_and_assigned_verifier_cannot_self_verify(self):
        self.review_import()
        self.act("SUBMIT", self.manager)
        with self.assertRaises(PermissionDenied):
            self.act("VERIFY", self.manager)
        self.manager.user_permissions.add(Permission.objects.get(content_type__app_label="community_care", codename="verify_directory"))
        self.manager = get_user_model().objects.get(pk=self.manager.pk)
        services.save_membership(self.verifier, {"hospital": self.hospital, "user": self.manager, "role": "ADMIN"})
        with self.assertRaises(PermissionDenied):
            self.act("VERIFY", self.manager)
        self.assert_private()

    def test_stale_revision_cannot_approve_and_does_not_append_decision(self):
        self.review_import()
        self.act("SUBMIT", self.manager)
        count = VerificationAudit.objects.count()
        with self.assertRaises(services.Conflict):
            self.act("VERIFY", revision=1)
        self.assertEqual(count, VerificationAudit.objects.count())
        self.assert_private()

    def test_postal_correction_invalidates_location_and_is_never_silently_reapproved(self):
        self.review_import()
        services.update_record(self.manager, Hospital, self.hospital.pk, {"postal_code": "313001"})
        self.act("SUBMIT", self.manager)
        self.act("VERIFY")
        self.hospital.refresh_from_db()
        self.assertEqual(self.hospital.location_status, "STALE")
        self.assertEqual(self.hospital.approved_location_id, self.position.pk)
        self.assertEqual(self.hospital.latitude, Decimal("24.588336"))
        self.assertEqual(self.nearby().data["count"], 0)
        row = self.client.get(f"/api/care/hospitals/{self.hospital.pk}/").data
        self.assertIsNone(row["latitude"])
        self.assertIsNone(row["longitude"])

    def test_verified_response_distance_search_bounds_and_private_fields(self):
        self.publish()
        row = self.nearby().data["results"][0]
        self.assertEqual(row["name"], self.hospital.name)
        self.assertEqual(row["address"], self.hospital.address)
        self.assertEqual((row["latitude"], row["longitude"]), ("24.588336", "73.679142"))
        self.assertAlmostEqual(row["distance_km"], 0, places=6)
        self.assertEqual(row["location_status"], "APPROVED")
        for key in ("created_by", "revision", "import_provenance", "approved_location", "reviewed_by"):
            self.assertNotIn(key, row)
        self.assertEqual(self.nearby(latitude="24.688336", radius="1").data["count"], 0)
        farther = self.nearby(latitude="24.688336", radius="20").data["results"][0]
        self.assertAlmostEqual(farther["distance_km"], 11.1195, places=3)
        self.assertEqual(self.client.get("/api/care/hospitals/?search=Isolated").data["count"], 1)
        self.assertEqual(self.client.get("/api/care/hospitals/?search=NoSuchFixture").data["count"], 0)

    def test_admin_actions_use_recorded_revision_reason_and_authenticated_actor(self):
        self.review_import()
        self.client.force_login(self.verifier)
        for action, expected in (("submit_review", "PENDING"), ("verify_records", "VERIFIED")):
            self.hospital.refresh_from_db()
            response = self.client.post("/admin/community_care/hospital/", {
                "action": action, "_selected_action": [self.hospital.pk], "index": "0",
                "expected_revision": self.hospital.revision, "reason": "Independent fixture documents reviewed in test only.",
            })
            self.assertEqual(response.status_code, 302)
            self.hospital.refresh_from_db()
            self.assertEqual(self.hospital.verification_status, expected)
        self.assertEqual(VerificationAudit.objects.get(action="VERIFY").actor_id, self.verifier.pk)

    def test_anonymous_management_and_decision_routes_require_authentication(self):
        self.assertEqual(self.client.get("/api/care/manage/hospitals/").status_code, 401)
        self.assertEqual(self.client.post(f"/api/care/admin/hospitals/{self.hospital.pk}/verify/", {
            "expected_revision": self.hospital.revision, "reason": "Attempted anonymous approval",
        }, format="json").status_code, 401)
        self.assert_private()
