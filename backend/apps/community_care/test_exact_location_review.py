import re
from uuid import uuid4

from django.contrib.auth import get_user_model
from django.contrib.auth.models import Permission
from django.core import signing
from django.http import Http404
from django.test import override_settings
from rest_framework.exceptions import PermissionDenied, ValidationError
from rest_framework.test import APITestCase
from rest_framework_simplejwt.tokens import RefreshToken

from . import services
from .duplicate_review import duplicate_is_clear, record_duplicate_review
from .geocoding import query_details
from .location_review import approve_manual_location, review_location
from .models import Hospital, HospitalDuplicateReview, HospitalMembership, GeocodingRun, LocationCandidate, VerificationAudit
from .review_forms import map_links


@override_settings(PASSWORD_HASHERS=["django.contrib.auth.hashers.MD5PasswordHasher"])
class ExactLocationTests(APITestCase):
    def setUp(self):
        self.verifier = get_user_model().objects.create_superuser("exact-verifier", "fixture@example.test", "test-only")
        self.staff = get_user_model().objects.create_user("hospital-staff", is_staff=True)
        self.manager = get_user_model().objects.create_user("directory-manager", is_staff=True)
        self.manager.user_permissions.add(Permission.objects.get(content_type__app_label="community_care", codename="manage_directory"))
        self.hospital = Hospital.objects.create(name="Synthetic facility", address="1 Supplied Street", city="Udaipur", district="Udaipur",
            state="Rajasthan", postal_code="313001", phone="9900000011", source_namespace="fixture", source_record_id="UDR-001", import_review_status="NEEDS_REVIEW")
        data, query, digest = query_details(self.hospital)
        self.run = GeocodingRun.objects.create(hospital=self.hospital, snapshot=data, query=query, query_hash=digest, status="SUCCESS")
        self.proposal = LocationCandidate.objects.create(run=self.run, fingerprint="original", latitude="24.500000", longitude="73.700000",
            label="Broad search label from fixture", confidence=.4, attribution="Fixture attribution",
            flags=["BROAD_LOCATION_NOT_CONFIRMED_FACILITY"])
        self.url = f"/admin/community_care/hospital/{self.hospital.pk}/location-review/"

    def values(self, **changes):
        self.hospital.refresh_from_db()
        return {"expected_revision": self.hospital.revision, "latitude": "24.585400", "longitude": "73.712500",
            "evidence_url": "https://example.test/official-facility-location", "reason": "Independent entrance and facility-address checks completed for fixture.",
            "submission_id": str(uuid4()), "exact_location_confirmed": True, "confirm_save": True,
            "based_on_candidate_id": self.proposal.pk, **changes}

    def approve(self, **changes):
        return approve_manual_location(self.verifier, self.hospital.pk, **self.values(**changes))

    def flag_duplicate(self):
        self.hospital.import_provenance = {"duplicate_candidates": ["UDR-004", "UDR-005"]}
        self.hospital.save()
        return HospitalDuplicateReview.objects.create(hospital=self.hospital)

    def resolve_distinct(self, **changes):
        self.hospital.refresh_from_db()
        review = HospitalDuplicateReview.objects.get(hospital=self.hospital)
        return record_duplicate_review(self.verifier, self.hospital.pk, "DISTINCT_CONFIRMED", self.hospital.revision,
            review.revision, "Independent documents establish distinct facility identities in fixture.",
            "https://example.test/identity-record", human_confirmed=True, **changes)

    def admin_payload(self):
        data = self.values()
        data["based_on_candidate"] = data.pop("based_on_candidate_id")
        data.pop("confirm_save")
        data["review_action"] = "preview_manual"
        return data

    def make_public(self):
        services.update_record(self.verifier, Hospital, self.hospital.pk,
            {"import_review_status": "REVIEWED", "import_review_notes": "Independent source and facility identity checked in fixture."})
        self.hospital.refresh_from_db()
        services.transition(self.verifier, Hospital, self.hospital.pk, "SUBMIT", self.hospital.revision, "Source checks ready for separate review")
        self.hospital.refresh_from_db()
        services.transition(self.verifier, Hospital, self.hospital.pk, "VERIFY", self.hospital.revision, "Independent identity verification completed in fixture")

    def test_manual_approval_keeps_original_and_records_evidence_reviewer_time_reason(self):
        original = LocationCandidate.objects.values().get(pk=self.proposal.pk)
        point = self.approve()
        self.assertEqual(LocationCandidate.objects.values().get(pk=self.proposal.pk), original)
        self.assertEqual(point.provider, "Manual confirmation")
        self.assertIsNone(point.confidence)
        self.assertEqual(point.based_on_candidate_id, self.proposal.pk)
        self.assertEqual(point.reviewed_by_id, self.verifier.pk)
        self.assertIsNotNone(point.reviewed_at)
        self.assertEqual(point.evidence_url, "https://example.test/official-facility-location")
        self.hospital.refresh_from_db()
        self.assertEqual((self.hospital.verification_status, self.hospital.import_review_status, self.hospital.booking_enabled), ("DRAFT", "NEEDS_REVIEW", False))
        self.assertEqual(self.hospital.approved_location_id, point.pk)
        audit = VerificationAudit.objects.get(action="LOCATION_APPROVED")
        self.assertEqual(audit.details["evidence_url"], point.evidence_url)
        self.assertEqual(audit.actor, self.verifier)
        self.assertEqual((audit.previous_status, audit.new_status), ("DRAFT", "DRAFT"))

    def test_no_confirmation_never_creates_manual_records(self):
        for changes in ({"exact_location_confirmed": False}, {"confirm_save": False}):
            with self.subTest(changes=changes), self.assertRaises(ValidationError):
                self.approve(**changes)
        self.assertEqual((GeocodingRun.objects.count(), LocationCandidate.objects.count()), (1, 1))

    def test_invalid_coordinates_nonce_and_evidence_rejected(self):
        for changes in ({"latitude": "NaN"}, {"longitude": "Infinity"}, {"latitude": "91"}, {"longitude": "-181"},
                        {"latitude": "24.1234567"}, {"submission_id": "bad"}, {"evidence_url": ""},
                        {"evidence_url": "javascript:alert(1)"}, {"evidence_url": "ftp://example.test/evidence"},
                        {"evidence_url": "https://user:secret@example.test/evidence"}, {"evidence_url": "https://example.test/?key=do-not-store"}):
            with self.subTest(changes=changes), self.assertRaises(ValidationError):
                self.approve(**changes)
        self.assertEqual(LocationCandidate.objects.count(), 1)

    def test_unauthorized_inactive_and_assigned_verifiers_cannot_approve(self):
        for actor in (self.staff, self.manager):
            with self.assertRaises(PermissionDenied):
                approve_manual_location(actor, self.hospital.pk, **self.values())
        services.save_membership(self.verifier, {"hospital": self.hospital, "user": self.verifier, "role": "ADMIN"})
        with self.assertRaises(PermissionDenied):
            self.approve()
        self.verifier.is_active = False
        self.verifier.save()
        with self.assertRaises(PermissionDenied):
            self.approve()

    def test_stale_and_repeated_submissions_do_not_duplicate_approval(self):
        data = self.values()
        approve_manual_location(self.verifier, self.hospital.pk, **data)
        with self.assertRaises(services.Conflict):
            approve_manual_location(self.verifier, self.hospital.pk, **data)
        self.hospital.refresh_from_db()
        data["expected_revision"] = self.hospital.revision
        with self.assertRaises(services.Conflict):
            approve_manual_location(self.verifier, self.hospital.pk, **data)
        self.assertEqual(LocationCandidate.objects.filter(provider="Manual confirmation").count(), 1)

    def test_original_proposal_must_belong_to_the_same_hospital(self):
        other = Hospital.objects.create(name="Other fixture", address="2 Test Street", city="Udaipur", state="Rajasthan", postal_code="313001", phone="9900000012")
        with self.assertRaises(Http404):
            approve_manual_location(self.verifier, other.pk, **self.values(expected_revision=1))

    def test_existing_broad_proposal_requires_evidence_even_with_confirmation(self):
        with self.assertRaises(ValidationError):
            review_location(self.verifier, self.proposal.pk, "APPROVE", 1, "Independent fixture confirmation provided", True)
        review_location(self.verifier, self.proposal.pk, "APPROVE", 1, "Independent facility position verified in fixture", True,
                        evidence_url="https://example.test/source")
        self.proposal.refresh_from_db()
        self.assertTrue(self.proposal.evidence_url)

    def test_pending_duplicates_cannot_be_cleared_by_checkbox_or_manual_approval(self):
        self.flag_duplicate()
        with self.assertRaises(ValidationError):
            self.approve(duplicate_review_resolved=True)
        self.assertEqual(LocationCandidate.objects.count(), 1)
        self.assertEqual(GeocodingRun.objects.count(), 1)
        self.assertFalse(VerificationAudit.objects.filter(action="LOCATION_APPROVED").exists())

    def test_distinct_review_is_persisted_audited_and_allows_separate_location_review(self):
        self.flag_duplicate()
        review = self.resolve_distinct()
        self.assertEqual(review.status, "DISTINCT_CONFIRMED")
        self.assertEqual(review.reviewed_by, self.verifier)
        self.assertTrue(duplicate_is_clear(self.hospital))
        self.approve(duplicate_review_resolved=True)
        self.assertEqual(VerificationAudit.objects.filter(action="DUPLICATE_REVIEW").count(), 1)
        self.hospital.refresh_from_db()
        self.assertEqual(self.hospital.verification_status, "DRAFT")

    def test_confirmed_duplicate_is_retained_and_never_public_or_location_approved(self):
        self.flag_duplicate()
        other = Hospital.objects.create(name="Related fixture", address="2 Test Street", city="Udaipur", state="Rajasthan", postal_code="313001", phone="9900000012")
        self.hospital.refresh_from_db()
        record_duplicate_review(self.verifier, self.hospital.pk, "DUPLICATE_CONFIRMED", self.hospital.revision, 1,
            "Same physical facility confirmed by independent records in fixture.", "https://example.test/registry", related_hospital_id=other.pk, human_confirmed=True)
        with self.assertRaises(ValidationError):
            self.approve(duplicate_review_resolved=True)
        self.assertEqual(Hospital.objects.count(), 2)
        Hospital.objects.filter(pk=self.hospital.pk).update(verification_status="VERIFIED", import_review_status="REVIEWED")
        self.assertEqual(self.client.get("/api/care/hospitals/").data["count"], 0)

    def test_duplicate_decision_requires_role_evidence_confirmation_and_nonself_relation(self):
        self.flag_duplicate()
        self.hospital.refresh_from_db()
        args = [self.hospital.pk, "DISTINCT_CONFIRMED", self.hospital.revision, 1, "Independent facility identity evidence checked."]
        with self.assertRaises(PermissionDenied):
            record_duplicate_review(self.manager, *args, evidence_url="https://example.test/source", human_confirmed=True)
        with self.assertRaises(ValidationError):
            record_duplicate_review(self.verifier, *args, evidence_url="", human_confirmed=True)
        with self.assertRaises(ValidationError):
            record_duplicate_review(self.verifier, *args, evidence_url="https://example.test/source", human_confirmed=False)
        with self.assertRaises(ValidationError):
            record_duplicate_review(self.verifier, self.hospital.pk, "DUPLICATE_CONFIRMED", self.hospital.revision, 1,
                "Same facility identity independently checked.", "https://example.test/source", self.hospital.pk, True)

    def test_stale_duplicate_snapshot_and_revision_are_blocked(self):
        self.flag_duplicate()
        self.resolve_distinct()
        with self.assertRaises(services.Conflict):
            record_duplicate_review(self.verifier, self.hospital.pk, "NEEDS_REVIEW", self.hospital.revision, 1,
                "More independent identity checks are required.", human_confirmed=True)
        services.update_record(self.verifier, Hospital, self.hospital.pk, {"address": "3 Changed Street"})
        self.hospital.refresh_from_db()
        self.assertFalse(duplicate_is_clear(self.hospital))
        with self.assertRaises(ValidationError):
            self.approve(duplicate_review_resolved=True)

    def test_duplicate_revocation_keeps_audit_and_removes_approved_location_eligibility(self):
        self.flag_duplicate()
        review = self.resolve_distinct()
        self.approve(duplicate_review_resolved=True)
        self.hospital.refresh_from_db()
        record_duplicate_review(self.verifier, self.hospital.pk, "NEEDS_REVIEW", self.hospital.revision, review.revision,
            "New source discrepancy requires another independent review.", human_confirmed=True)
        self.hospital.refresh_from_db()
        self.assertEqual(self.hospital.location_status, "STALE")
        self.assertEqual(VerificationAudit.objects.filter(action="DUPLICATE_REVIEW").count(), 2)
        self.assertEqual(VerificationAudit.objects.filter(action="LOCATION_APPROVED").count(), 1)

    def test_manual_location_alone_never_publishes_and_separate_identity_approval_does(self):
        self.approve()
        nearby = "/api/care/hospitals/nearby/?latitude=24.5854&longitude=73.7125"
        self.assertEqual(self.client.get(nearby).data["count"], 0)
        self.assertEqual(self.client.get("/api/care/hospitals/").data["count"], 0)
        self.make_public()
        self.assertEqual(self.client.get(nearby).data["count"], 1)

    def test_admin_page_compares_facility_and_proposals_and_does_not_prefill_coordinates(self):
        self.client.force_login(self.verifier)
        result = self.client.get(self.url)
        self.assertEqual(result.status_code, 200)
        self.assertContains(result, "1 Supplied Street")
        self.assertContains(result, "Broad search label from fixture")
        self.assertContains(result, "Search relevance")
        self.assertContains(result, "Inspect in OpenStreetMap")
        self.assertContains(result, 'rel="noopener noreferrer"')
        self.assertIsNone(result.context_data["manual_form"]["latitude"].value())
        self.assertFalse(result.context_data["preview"])
        self.assertContains(self.client.get(f"/admin/community_care/locationcandidate/{self.proposal.pk}/change/"), "Supplied facility")
        self.assertEqual(self.client.get("/admin/community_care/hospital/add/").status_code, 200)

    def test_hospital_name_and_id_links_open_change_page_and_review_workspace(self):
        from django.urls import reverse, resolve
        change_url = reverse("admin:community_care_hospital_change", args=[self.hospital.pk])
        self.assertEqual(resolve(change_url).url_name, "community_care_hospital_change")
        for actor in (self.verifier, self.manager):
            with self.subTest(actor=actor.username):
                self.client.force_login(actor)
                listing = self.client.get(reverse("admin:community_care_hospital_changelist"))
                self.assertEqual(listing.status_code, 200)
                html = listing.content.decode()
                for column in ("name", "id"):
                    cell = re.search(r'<(?:th|td)\b[^>]*class="field-' + column + r'"[^>]*>(.*?)</(?:th|td)>', html, re.S)
                    self.assertIsNotNone(cell, f"Missing {column} column")
                    self.assertIn(f'<a href="{change_url}">', cell.group(1))
                detail = self.client.get(change_url)
                self.assertContains(detail, "Compare proposals and review an exact location")
                self.assertContains(detail, self.url)
                workspace = self.client.get(self.url)
                self.assertEqual(workspace.status_code, 200)
                self.assertEqual(workspace.context_data["can_verify"], actor == self.verifier)

    def test_admin_preview_has_no_writes_and_signed_confirmation_records_manual_point(self):
        self.client.force_login(self.verifier)
        data = self.admin_payload()
        preview = self.client.post(self.url, data)
        self.assertEqual(preview.status_code, 200)
        self.assertTrue(preview.context_data["preview_token"])
        self.assertEqual(LocationCandidate.objects.count(), 1)
        data.update(review_action="approve_manual", confirm_save=True, preview_token=preview.context_data["preview_token"])
        self.assertEqual(self.client.post(self.url, data).status_code, 302)
        self.assertEqual(LocationCandidate.objects.filter(provider="Manual confirmation", status="APPROVED").count(), 1)

    def test_missing_and_tampered_preview_tokens_cannot_approve(self):
        self.client.force_login(self.verifier)
        data = self.admin_payload()
        result = self.client.post(self.url, data)
        token = result.context_data["preview_token"]
        for changes in ({"latitude": "24.590000", "preview_token": token}, {"preview_token": ""}, {"confirm_save": False, "preview_token": token}):
            post = {**data, "review_action": "approve_manual", "confirm_save": True, **changes}
            self.assertEqual(self.client.post(self.url, post).status_code, 200)
            self.assertEqual(LocationCandidate.objects.count(), 1)

    def test_expired_preview_is_not_saved(self):
        from unittest.mock import patch
        self.client.force_login(self.verifier)
        data = self.admin_payload()
        preview = self.client.post(self.url, data)
        envelope = signing.loads(preview.context_data["preview_token"], salt="community-care.exact-location-preview")
        with patch("django.core.signing.TimestampSigner.timestamp", return_value="0"):
            expired = signing.dumps(envelope, salt="community-care.exact-location-preview")
        data.update(review_action="approve_manual", confirm_save=True, preview_token=expired)
        result = self.client.post(self.url, data)
        self.assertContains(result, "Preview again")
        self.assertEqual(LocationCandidate.objects.count(), 1)

    def test_preview_is_bound_to_the_authenticated_reviewer(self):
        self.client.force_login(self.verifier)
        data = self.admin_payload()
        preview = self.client.post(self.url, data)
        other = get_user_model().objects.create_superuser("another-verifier", "other@example.test", "test-only")
        self.client.force_login(other)
        data.update(review_action="approve_manual", confirm_save=True, preview_token=preview.context_data["preview_token"])
        self.assertContains(self.client.post(self.url, data), "Preview again")
        self.assertEqual(LocationCandidate.objects.count(), 1)

    def test_staff_members_cannot_open_review_page_and_manager_is_read_only(self):
        self.client.force_login(self.staff)
        self.assertEqual(self.client.get(self.url).status_code, 403)
        self.assertEqual(self.client.post(self.url, self.admin_payload()).status_code, 403)
        self.client.force_login(self.manager)
        self.assertContains(self.client.get(self.url), "Read-only review")
        self.assertEqual(self.client.post(self.url, self.admin_payload()).status_code, 403)

    def test_admin_duplicate_decision_is_recorded_without_location_approval(self):
        self.flag_duplicate()
        self.client.force_login(self.verifier)
        self.hospital.refresh_from_db()
        data = {"review_action": "save_duplicate_review", "expected_revision": self.hospital.revision,
            "expected_review_revision": 1, "status": "DISTINCT_CONFIRMED", "evidence_url": "https://example.test/identities",
            "reason": "Distinct facility identities checked against independent fixture documents.", "human_confirmed": True}
        self.assertEqual(self.client.post(self.url, data).status_code, 302)
        self.assertEqual(HospitalDuplicateReview.objects.get(hospital=self.hospital).status, "DISTINCT_CONFIRMED")
        self.hospital.refresh_from_db()
        self.assertIsNone(self.hospital.latitude)

    def test_template_and_candidate_comparison_escape_provider_and_supplied_html(self):
        self.hospital.name = '<script>alert("facility")</script>'
        self.hospital.save()
        self.proposal.label = '<script>alert("provider")</script>'
        self.proposal.save()
        self.client.force_login(self.verifier)
        result = self.client.get(self.url)
        self.assertNotContains(result, '<script>alert("provider")</script>')
        self.assertContains(result, "&lt;script&gt;")
        candidate = self.client.get(f"/admin/community_care/locationcandidate/{self.proposal.pk}/change/")
        self.assertNotContains(candidate, '<script>alert("facility")</script>')

    def test_external_map_links_include_only_validated_coordinates_and_no_keys(self):
        links = map_links("24.5854", "73.7125")
        self.assertIn("api=1", links["google"])
        self.assertIn("24.5854%2C73.7125", links["google"])
        self.assertTrue(links["osm"].startswith("https://www.openstreetmap.org/"))
        self.assertNotIn("key=", links["google"])

    def test_unknown_action_is_visible_error_and_csrf_protects_review_posts(self):
        self.client.force_login(self.verifier)
        self.assertContains(self.client.post(self.url, {"review_action": "unknown"}), "No data was saved")
        from django.test import Client
        csrf_client = Client(enforce_csrf_checks=True)
        csrf_client.force_login(self.verifier)
        self.assertEqual(csrf_client.post(self.url, self.admin_payload()).status_code, 403)

    def test_jwt_management_cannot_forge_duplicate_status_and_broad_evidence_is_returned(self):
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {RefreshToken.for_user(self.verifier).access_token}")
        self.assertEqual(self.client.patch(f"/api/care/hospitals/{self.hospital.pk}/", {"duplicate_review_status": "DISTINCT_CONFIRMED"}, format="json").status_code, 400)
        point = self.approve()
        result = self.client.get(f"/api/care/admin/location-candidates/{point.pk}/")
        self.assertEqual(result.data["evidence_url"], point.evidence_url)
