"""Synthetic fixtures only. Never run against or publish live practitioner data."""
from django.contrib.auth import get_user_model
from django.contrib.auth.models import Permission
from django.test import override_settings
from rest_framework.test import APITestCase

from apps.doctors.models import DoctorProfile
from . import services, tests as fixtures
from .models import Hospital, VerificationAudit


@override_settings(PASSWORD_HASHERS=["django.contrib.auth.hashers.MD5PasswordHasher"])
class DoctorDirectoryTests(APITestCase):
    def setUp(self):
        self.verifier = get_user_model().objects.create_superuser("fixture-verifier", "verifier@example.test", "test-password")
        self.owner = get_user_model().objects.create_user("fixture-owner", password="test-password")
        self.manager = get_user_model().objects.create_user("fixture-manager", password="test-password")
        self.reader = get_user_model().objects.create_user("fixture-reader", password="test-password")
        self.hospital = services.create_hospital(self.verifier, fixtures.CareTests.hospital_data())
        self.doctor = services.create_doctor(self.verifier, fixtures.CareTests.doctor_values(), hospital_id=self.hospital.pk, user=self.owner)
        services.save_membership(self.verifier, {"hospital": self.hospital, "user": self.manager, "role": "ADMIN"})
        services.save_membership(self.verifier, {"hospital": self.hospital, "user": self.reader, "role": "STAFF"})

    def submit(self, record):
        record.refresh_from_db()
        services.transition(self.verifier, type(record), record.pk, "SUBMIT", record.revision, "Isolated fixture submission")
        record.refresh_from_db()

    def publish(self, record):
        self.submit(record)
        extra = {"credential_review": fixtures.CareTests.credential_review()} if isinstance(record, DoctorProfile) else {}
        services.transition(self.verifier, type(record), record.pk, "VERIFY", record.revision, "Isolated independent review", **extra)
        record.refresh_from_db()

    def decision(self, user, review=None, revision=None, action="verify"):
        self.client.force_authenticate(user)
        self.doctor.refresh_from_db()
        payload = {"expected_revision": revision or self.doctor.revision, "reason": "Isolated credential checks"}
        if review is not None:
            payload["credential_review"] = review
        return self.client.post(f"/api/care/admin/doctors/{self.doctor.pk}/{action}/", payload, format="json")

    def test_missing_or_partial_credential_review_cannot_verify_or_append_approval(self):
        self.submit(self.doctor)
        for review in (None, {}, {**fixtures.CareTests.credential_review(), "registration_checked": False},
                       {**fixtures.CareTests.credential_review(), "evidence_references": []},
                       {**fixtures.CareTests.credential_review(), "qualifications_checked": False},
                       {**fixtures.CareTests.credential_review(), "evidence_references": [" "]}):
            with self.subTest(review=review):
                self.assertEqual(self.decision(self.verifier, review).status_code, 400)
        self.doctor.refresh_from_db()
        self.assertEqual(self.doctor.verification_status, "PENDING")
        self.assertFalse(VerificationAudit.objects.filter(record_type="DOCTOR", action="VERIFY").exists())

    def test_permission_independence_and_stale_revision_precede_approval(self):
        self.submit(self.doctor)
        review = fixtures.CareTests.credential_review()
        for user in (self.owner, self.manager, self.reader):
            self.assertEqual(self.decision(user, review).status_code, 403)
        permission = Permission.objects.get(content_type__app_label="community_care", codename="verify_directory")
        self.owner.user_permissions.add(permission)
        self.manager.user_permissions.add(permission)
        for user in (self.owner, self.manager):
            self.assertEqual(self.decision(user, review).status_code, 403)
        self.assertEqual(self.decision(self.verifier, review, revision=1).status_code, 409)

    def test_valid_review_records_actor_references_and_current_revision_privately(self):
        self.submit(self.doctor)
        review = fixtures.CareTests.credential_review()
        response = self.decision(self.verifier, review)
        self.assertEqual(response.status_code, 200)
        audit = VerificationAudit.objects.get(record_type="DOCTOR", record_id=self.doctor.pk, action="VERIFY")
        self.assertEqual(audit.actor, self.verifier)
        self.assertEqual(audit.details["credential_review"]["evidence_references"], review["evidence_references"])
        self.assertEqual(audit.details["revision"], response.data["revision"])
        self.assertEqual(audit.details["credential_review"]["protocol"], "doctor-credentials-v1")

    def test_flags_without_recorded_review_never_publish_even_with_verified_hospital(self):
        self.publish(self.hospital)
        # Deliberate database bypass confined to isolated tests: assert public defense.
        DoctorProfile.objects.filter(pk=self.doctor.pk).update(verification_status="VERIFIED", is_verified=True)
        for endpoint in ("/api/care/doctors/", "/doctors/verified", "/api/doctors/verified"):
            self.assertEqual(self.client.get(endpoint).data["count"], 0)
        self.assertEqual(self.client.get(f"/api/care/doctors/{self.doctor.pk}/").status_code, 404)

    def test_public_search_detail_filters_and_legacy_aliases_share_safe_contract(self):
        self.publish(self.hospital)
        self.publish(self.doctor)
        for url in ("/api/care/doctors/?search=Synthetic", "/api/care/doctors/?specialization=Ayurveda",
                    f"/api/care/doctors/?hospital_id={self.hospital.pk}", "/doctors/verified", "/api/doctors/verified"):
            data = self.client.get(url).data
            self.assertEqual(data["count"], 1)
            self.assertIsNotNone(data["results"][0]["credentials_reviewed_at"])
            for private in ("user", "user_id", "email", "phone", "degree_certificate", "profile_metadata", "credential_review"):
                self.assertNotIn(private, data["results"][0])
        detail = self.client.get(f"/api/care/doctors/{self.doctor.pk}/").data
        self.assertEqual(detail["registration_council"], self.doctor.registration_council)
        self.assertEqual(detail["affiliations"][0]["verification_status"], "VERIFIED")
        for private in ("user", "user_id", "email", "phone", "degree_certificate", "profile_metadata", "credential_review"):
            self.assertNotIn(private, detail)
        self.assertEqual(self.client.get("/api/care/doctors/?search=missing").data["count"], 0)
        self.assertEqual(self.client.get("/api/care/doctors/?hospital_id=bad").status_code, 400)
        self.assertEqual(self.client.get("/api/care/doctors/?page=999999").status_code, 404)

    def test_hospital_draft_suspension_and_inactive_affiliation_remove_doctor(self):
        self.publish(self.doctor)
        self.assertEqual(self.client.get("/api/care/doctors/").data["count"], 0)
        self.publish(self.hospital)
        affiliation = self.doctor.care_affiliations.get()
        affiliation.is_active = False
        affiliation.save()
        self.assertEqual(self.client.get("/api/care/doctors/").data["count"], 0)
        affiliation.is_active = True
        affiliation.save()
        services.transition(self.verifier, Hospital, self.hospital.pk, "SUSPEND", self.hospital.revision, "Isolated suspension")
        self.assertEqual(self.client.get("/api/care/doctors/").data["count"], 0)

    def test_critical_self_edit_hides_credentials_and_cannot_self_verify(self):
        self.publish(self.hospital)
        self.publish(self.doctor)
        self.client.force_authenticate(self.owner)
        response = self.client.patch("/doctor/profile/update", {"specialization": "Changed claim"}, format="multipart")
        self.assertEqual(response.status_code, 200)
        self.assertFalse(response.data["is_verified"])
        self.client.force_authenticate(None)
        self.assertEqual(self.client.get(f"/api/care/doctors/{self.doctor.pk}/").status_code, 404)

    def test_management_roles_and_private_audit_access(self):
        self.client.force_authenticate(self.reader)
        url = f"/api/care/manage/doctors/{self.doctor.pk}/"
        self.assertEqual(self.client.get(url).status_code, 200)
        self.assertEqual(self.client.patch(url, {"bio": "Unauthorized"}, format="json").status_code, 404)
        self.assertEqual(self.client.get("/api/care/admin/audits/?record_type=DOCTOR").status_code, 403)
        self.client.force_authenticate(self.manager)
        self.assertEqual(self.client.patch(url, {"bio": "Isolated updated draft"}, format="json").status_code, 200)

    def test_revocation_cannot_be_restored_by_flags_using_old_evidence(self):
        self.publish(self.hospital)
        self.publish(self.doctor)
        services.transition(self.verifier, DoctorProfile, self.doctor.pk, "SUSPEND", self.doctor.revision, "Isolated revocation")
        DoctorProfile.objects.filter(pk=self.doctor.pk).update(verification_status="VERIFIED", is_verified=True)
        self.assertEqual(self.client.get("/api/care/doctors/").data["count"], 0)

    def test_noncritical_edit_retains_review_but_critical_edit_needs_fresh_approval(self):
        self.publish(self.hospital)
        self.publish(self.doctor)
        first_approval = VerificationAudit.objects.get(record_type="DOCTOR", record_id=self.doctor.pk, action="VERIFY")
        services.update_record(self.verifier, DoctorProfile, self.doctor.pk, {"consultation_fee": "25.00"})
        self.assertEqual(self.client.get("/api/care/doctors/").data["count"], 1)
        services.update_record(self.verifier, DoctorProfile, self.doctor.pk, {"qualifications": ["Changed isolated qualification"]})
        self.doctor.refresh_from_db()
        self.assertEqual(self.doctor.verification_status, "PENDING")
        self.assertEqual(self.client.get("/api/care/doctors/").data["count"], 0)
        invalidation = VerificationAudit.objects.filter(record_type="DOCTOR", record_id=self.doctor.pk).latest("id")
        self.assertEqual(invalidation.action, "REVERIFY_REQUIRED")
        self.assertNotIn("credential_review", invalidation.details)
        self.assertEqual(self.decision(self.verifier).status_code, 400)
        self.assertEqual(self.decision(self.verifier, fixtures.CareTests.credential_review()).status_code, 200)
        self.client.force_authenticate(None)
        self.assertEqual(self.client.get("/api/care/doctors/").data["count"], 1)
        fresh_approval = VerificationAudit.objects.filter(record_type="DOCTOR", record_id=self.doctor.pk, action="VERIFY").latest("id")
        self.assertGreater(fresh_approval.pk, first_approval.pk)
        self.assertEqual(invalidation.details.get("changed_fields"), ["qualifications"])

    def test_draft_hospital_affiliation_is_hidden_even_with_another_eligible_hospital(self):
        self.publish(self.hospital)
        self.publish(self.doctor)
        draft = services.create_hospital(self.verifier, fixtures.CareTests.hospital_data(name="Hidden isolated hospital"))
        services.save_affiliation(self.verifier, {"doctor_id": self.doctor.pk, "hospital_id": draft.pk,
                                                  "consultation_mode": "IN_PERSON", "fees": "0.00"})
        response = self.client.get(f"/api/care/doctors/{self.doctor.pk}/")
        self.assertEqual(response.status_code, 200)
        self.assertEqual([a["hospital_id"] for a in response.data["affiliations"]], [self.hospital.pk])
        self.assertEqual(self.client.get(f"/api/care/doctors/?hospital_id={draft.pk}").data["count"], 0)
