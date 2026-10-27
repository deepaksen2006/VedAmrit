from datetime import timedelta
from decimal import Decimal

from django.contrib import admin
from django.contrib.auth import get_user_model
from django.contrib.auth.models import Permission
from django.core.exceptions import ValidationError as ModelValidationError
from django.db import IntegrityError, transaction
from django.test import RequestFactory, override_settings
from rest_framework.test import APITestCase
from rest_framework_simplejwt.tokens import AccessToken, RefreshToken

from apps.doctors.models import DoctorProfile
from . import services
from .models import Hospital, HospitalMembership, DoctorHospitalAffiliation, VerificationAudit
from .permissions import is_directory_admin


@override_settings(PASSWORD_HASHERS=["django.contrib.auth.hashers.MD5PasswordHasher"])
class CareTests(APITestCase):
    @classmethod
    def setUpTestData(cls):
        User = get_user_model()
        cls.admin = User.objects.create_superuser("care-verifier", "verifier@example.test", "test-only-password")
        cls.manager = User.objects.create_user("hospital-manager", password="test-only-password")
        cls.other = User.objects.create_user("other-manager", password="test-only-password")
        cls.patient = User.objects.create_user("care-patient", password="test-only-password")
        cls.staff = User.objects.create_user("hospital-reader", password="test-only-password")
        cls.hospital = services.create_hospital(cls.admin, cls.hospital_data())
        cls.hospital_b = services.create_hospital(cls.admin, cls.hospital_data(name="Second test hospital", city="Mumbai"))
        services.save_membership(cls.admin, {"hospital": cls.hospital, "user": cls.manager, "role": "ADMIN"})
        services.save_membership(cls.admin, {"hospital": cls.hospital_b, "user": cls.other, "role": "ADMIN"})
        services.save_membership(cls.admin, {"hospital": cls.hospital, "user": cls.staff, "role": "STAFF"})
        cls.doctor = services.create_doctor(cls.admin, cls.doctor_values(), hospital_id=cls.hospital.pk)
        cls.doctor_b = services.create_doctor(cls.admin, cls.doctor_values(full_name="Other test doctor", phone="+919800000002",
            email="other-doctor@example.test", bams_registration_number="TEST-002"), hospital_id=cls.hospital_b.pk)

    @staticmethod
    def hospital_data(**changes):
        return {"name": "Synthetic test hospital", "description": "Isolated fixture only", "address": "1 Test Street",
                "city": "Pune", "state": "Maharashtra", "postal_code": "411001", "phone": "+919900000001",
                "email": "hospital@example.test", "facilities": ["Outpatient consultation"],
                "latitude": "18.520400", "longitude": "73.856700", **changes}

    @staticmethod
    def doctor_values(**changes):
        return {"full_name": "Synthetic test doctor", "phone": "+919800000001", "email": "doctor@example.test",
                "bams_registration_number": "TEST-001", "registration_council": "Test issuing council",
                "qualifications": ["Test qualification"], "specialization": "Ayurveda", "experience": 5,
                "clinic_name": "Test clinic", "consultation_fee": Decimal("500.00"), "bio": "Test fixture only", **changes}

    def sign_in(self, user):
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {RefreshToken.for_user(user).access_token}" if user else None)

    @staticmethod
    def credential_review():
        return {"evidence_references": ["https://council.example.test/isolated-fixture-review"],
                "registration_checked": True, "qualifications_checked": True,
                "profile_claims_checked": True, "affiliations_checked": True}

    def publish(self, record):
        record.refresh_from_db()
        services.transition(self.admin, type(record), record.pk, "SUBMIT", record.revision, "Submitted test credentials")
        record.refresh_from_db()
        extra = {"credential_review": self.credential_review()} if isinstance(record, DoctorProfile) else {}
        services.transition(self.admin, type(record), record.pk, "VERIFY", record.revision, "Independent test review complete", **extra)
        record.refresh_from_db()

    def decision(self, record, action, user=None, revision=None):
        self.sign_in(user or self.admin)
        record.refresh_from_db()
        kind = "hospitals" if isinstance(record, Hospital) else "doctors"
        extra = {"credential_review": self.credential_review()} if kind == "doctors" and action == "verify" else {}
        return self.client.post(f"/api/care/admin/{kind}/{record.pk}/{action}/",
            {"expected_revision": revision if revision is not None else record.revision, "reason": "Recorded independent test review", **extra}, format="json")

    def test_model_defaults_and_decimal_coordinates(self):
        self.assertEqual(self.hospital.verification_status, "DRAFT")
        self.hospital.refresh_from_db()
        self.assertEqual(self.hospital.latitude, Decimal("18.520400"))
        self.assertIsNone(self.doctor.user_id)
        self.assertEqual(get_user_model().objects.count(), 5)

    def test_model_validation_rejects_coordinate_pair_range_and_facilities(self):
        for values in ({"latitude": None}, {"latitude": Decimal("91")}, {"longitude": Decimal("181")}, {"facilities": "not a list"}):
            record = Hospital(**self.hospital_data(name="Invalid fixture", **values))
            with self.subTest(values=values), self.assertRaises(ModelValidationError):
                record.full_clean()

    def test_database_coordinate_and_duplicate_affiliation_constraints(self):
        with self.assertRaises(IntegrityError), transaction.atomic():
            Hospital.objects.filter(pk=self.hospital.pk).update(latitude=100)
        with self.assertRaises(IntegrityError), transaction.atomic():
            DoctorHospitalAffiliation.objects.create(doctor=self.doctor, hospital=self.hospital, fees=0)

    def test_anonymous_directory_is_read_only_and_empty_before_verification(self):
        self.assertEqual(self.client.get("/api/care/hospitals/").data["count"], 0)
        self.assertEqual(self.client.get("/api/care/doctors/").data["count"], 0)
        self.assertEqual(self.client.post("/api/care/hospitals/", self.hospital_data(), format="json").status_code, 401)
        self.assertEqual(self.client.patch(f"/api/care/hospitals/{self.hospital.pk}/", {"name": "No"}, format="json").status_code, 401)

    def test_public_doctor_requires_verified_active_hospital_and_affiliation(self):
        self.publish(self.doctor)
        self.assertEqual(self.client.get("/api/care/doctors/").data["count"], 0)
        self.publish(self.hospital)
        response = self.client.get("/api/care/doctors/")
        self.assertEqual(response.data["count"], 1)
        self.assertEqual(response.data["results"][0]["affiliations"][0]["fees"], "500.00")
        affiliation = self.doctor.care_affiliations.get()
        affiliation.is_active = False
        affiliation.save()
        self.assertEqual(self.client.get("/api/care/doctors/").data["count"], 0)

    def test_public_contract_does_not_expose_users_certificates_or_internal_metadata(self):
        self.publish(self.hospital)
        self.publish(self.doctor)
        doctor = self.client.get(f"/api/care/doctors/{self.doctor.pk}/").data
        for field in ("user", "user_id", "degree_certificate", "profile_metadata", "created_by", "email", "phone"):
            self.assertNotIn(field, doctor)
        hospital = self.client.get(f"/api/care/hospitals/{self.hospital.pk}/").data
        self.assertNotIn("created_by", hospital)
        # Identity verification does not publish a supplied, unreviewed map position.
        self.assertIsNone(hospital["latitude"])
        self.assertIsNone(hospital["longitude"])
        self.assertEqual(hospital["location_status"], "UNVERIFIED")

    def test_pending_rejected_suspended_and_inactive_hospitals_are_invisible(self):
        self.sign_in(self.admin)
        services.transition(self.admin, Hospital, self.hospital.pk, "SUBMIT", self.hospital.revision, "Initial submission")
        for action in (None, "reject"):
            if action:
                self.assertEqual(self.decision(self.hospital, action).status_code, 200)
            self.assertEqual(self.client.get(f"/api/care/hospitals/{self.hospital.pk}/").status_code, 404)
        self.publish(self.hospital)
        self.assertEqual(self.decision(self.hospital, "suspend").status_code, 200)
        self.assertEqual(self.client.get("/api/care/hospitals/").data["count"], 0)
        self.publish(self.hospital_b)
        services.update_record(self.admin, Hospital, self.hospital_b.pk, {"is_active": False})
        self.assertEqual(self.client.get("/api/care/hospitals/").data["count"], 0)

    def test_rejected_suspended_and_inactive_doctors_are_invisible(self):
        self.publish(self.hospital)
        self.doctor.refresh_from_db()
        services.transition(self.admin, DoctorProfile, self.doctor.pk, "SUBMIT", self.doctor.revision, "Initial submission")
        self.assertEqual(self.decision(self.doctor, "reject").status_code, 200)
        self.assertEqual(self.client.get("/api/care/doctors/").data["count"], 0)
        self.publish(self.doctor)
        self.assertEqual(self.decision(self.doctor, "suspend").status_code, 200)
        self.doctor.refresh_from_db()
        self.assertFalse(self.doctor.is_verified)
        self.assertEqual(self.client.get("/api/care/doctors/").data["count"], 0)
        self.assertEqual(self.decision(self.doctor, "reopen").status_code, 200)
        self.assertEqual(self.decision(self.doctor, "verify").status_code, 200)
        services.update_record(self.admin, DoctorProfile, self.doctor.pk, {"is_active": False})
        self.assertEqual(self.client.get("/api/care/doctors/").data["count"], 0)

    def test_authorized_creation_records_creator_without_granting_membership(self):
        self.sign_in(self.manager)
        response = self.client.post("/api/care/hospitals/", self.hospital_data(name="New proposed fixture"), format="json")
        self.assertEqual(response.status_code, 201, response.data)
        self.assertEqual(response.data["created_by"], self.manager.pk)
        self.assertEqual(response.data["verification_status"], "DRAFT")
        self.assertFalse(HospitalMembership.objects.filter(hospital_id=response.data["id"]).exists())
        self.assertEqual(self.client.patch(f"/api/care/hospitals/{response.data['id']}/", {"name": "Unauthorized assignment"}, format="json").status_code, 404)

    def test_patient_cannot_create_hospital_or_doctor(self):
        self.sign_in(self.patient)
        self.assertEqual(self.client.post("/api/care/hospitals/", self.hospital_data(), format="json").status_code, 403)
        self.assertEqual(self.client.post("/api/care/doctors/", self.doctor_request(), format="json").status_code, 404)

    def test_hospital_admin_cross_hospital_management_denied(self):
        self.sign_in(self.manager)
        result = self.client.get("/api/care/manage/hospitals/")
        self.assertEqual([row["id"] for row in result.data["results"]], [self.hospital.pk])
        self.assertEqual(self.client.get(f"/api/care/manage/hospitals/{self.hospital_b.pk}/").status_code, 404)
        self.assertEqual(self.client.patch(f"/api/care/hospitals/{self.hospital_b.pk}/", {"name": "Attack"}, format="json").status_code, 404)
        self.assertEqual(self.client.patch(f"/api/care/doctors/{self.doctor_b.pk}/", {"full_name": "Attack"}, format="json").status_code, 404)

    def test_staff_members_are_read_only_and_inactive_memberships_lose_access(self):
        self.sign_in(self.staff)
        self.assertEqual(self.client.get("/api/care/manage/hospitals/").data["count"], 1)
        self.assertEqual(self.client.patch(f"/api/care/hospitals/{self.hospital.pk}/", {"description": "Unauthorized"}, format="json").status_code, 404)
        HospitalMembership.objects.filter(user=self.manager).update(is_active=False)
        self.sign_in(self.manager)
        self.assertEqual(self.client.get("/api/care/manage/hospitals/").data["count"], 0)

    def test_hospital_admin_cannot_self_verify_or_mutate_verification_fields(self):
        self.assertEqual(self.decision(self.hospital, "verify", self.manager).status_code, 403)
        self.sign_in(self.manager)
        for values in ({"verification_status": "VERIFIED"}, {"created_by": self.manager.pk}, {"revision": 90}, {"is_staff": True}):
            self.assertEqual(self.client.patch(f"/api/care/hospitals/{self.hospital.pk}/", values, format="json").status_code, 400)
        self.assertEqual(self.client.patch(f"/api/care/doctors/{self.doctor.pk}/", {"is_verified": True}, format="json").status_code, 400)

    def test_membership_assignment_is_vedamrit_only_and_does_not_promote_user(self):
        self.sign_in(self.manager)
        payload = {"hospital": self.hospital_b.pk, "user": self.manager.pk, "role": "ADMIN"}
        self.assertEqual(self.client.post("/api/care/admin/memberships/", payload, format="json").status_code, 403)
        self.sign_in(self.admin)
        response = self.client.post("/api/care/admin/memberships/", payload, format="json")
        self.assertEqual(response.status_code, 201, response.data)
        self.manager.refresh_from_db()
        self.assertFalse(self.manager.is_staff)
        self.assertFalse(self.manager.is_superuser)
        self.assertFalse(self.manager.user_permissions.exists())
        self.assertEqual(self.client.patch(f"/api/care/admin/memberships/{response.data['id']}/", {"hospital": self.hospital.pk}, format="json").status_code, 400)

    def test_duplicate_membership_is_validation_error(self):
        self.sign_in(self.admin)
        response = self.client.post("/api/care/admin/memberships/", {"hospital": self.hospital.pk, "user": self.manager.pk, "role": "ADMIN"}, format="json")
        self.assertEqual(response.status_code, 400)

    def doctor_request(self, **changes):
        payload = self.doctor_values(phone="+919800000009", email="new-doctor@example.test", bams_registration_number="TEST-009")
        payload["registration_number"] = payload.pop("bams_registration_number")
        payload["experience_years"] = payload.pop("experience")
        payload["hospital_id"] = self.hospital.pk
        payload["consultation_fee"] = "500.00"
        return {**payload, **changes}

    def test_hospital_admin_creates_doctor_and_affiliation_without_creating_user(self):
        self.sign_in(self.manager)
        users_before = get_user_model().objects.count()
        response = self.client.post("/api/care/doctors/", self.doctor_request(), format="json")
        self.assertEqual(response.status_code, 201, response.data)
        record = DoctorProfile.objects.get(pk=response.data["id"])
        self.assertIsNone(record.user_id)
        self.assertEqual(record.care_affiliations.get().hospital_id, self.hospital.pk)
        self.assertEqual(get_user_model().objects.count(), users_before)

    def test_hospital_admin_cannot_link_existing_user_or_create_at_other_hospital(self):
        self.sign_in(self.manager)
        self.assertEqual(self.client.post("/api/care/doctors/", self.doctor_request(user_id=self.patient.pk), format="json").status_code, 403)
        self.assertEqual(self.client.post("/api/care/doctors/", self.doctor_request(hospital_id=self.hospital_b.pk), format="json").status_code, 404)

    def test_existing_doctor_identity_cannot_be_duplicated_by_user_or_registration(self):
        self.sign_in(self.admin)
        response = self.client.post("/api/care/doctors/", self.doctor_request(user_id=self.patient.pk), format="json")
        self.assertEqual(response.status_code, 201, response.data)
        self.assertEqual(self.client.post("/api/care/doctors/", self.doctor_request(user_id=self.patient.pk, phone="+919800000010", email="next@example.test", registration_number="TEST-010"), format="json").status_code, 400)
        self.assertEqual(self.client.post("/api/care/doctors/", self.doctor_request(registration_number="test-001"), format="json").status_code, 400)

    def test_affiliation_cannot_claim_other_doctor_or_hospital_or_reassign_row(self):
        self.sign_in(self.manager)
        for payload in ({"doctor_id": self.doctor_b.pk, "hospital_id": self.hospital.pk, "fees": "100.00"},
                        {"doctor_id": self.doctor.pk, "hospital_id": self.hospital_b.pk, "fees": "100.00"}):
            self.assertEqual(self.client.post("/api/care/manage/affiliations/", payload, format="json").status_code, 404)
        other = self.doctor_b.care_affiliations.get()
        self.assertEqual(self.client.patch(f"/api/care/manage/affiliations/{other.pk}/", {"fees": "1.00"}, format="json").status_code, 404)
        own = self.doctor.care_affiliations.get()
        self.assertEqual(self.client.patch(f"/api/care/manage/affiliations/{own.pk}/", {"doctor_id": self.doctor_b.pk}, format="json").status_code, 400)

    def test_affiliation_fee_edit_and_vedamrit_linking(self):
        self.sign_in(self.manager)
        own = self.doctor.care_affiliations.get()
        self.assertEqual(self.client.patch(f"/api/care/manage/affiliations/{own.pk}/", {"fees": "750.00", "consultation_mode": "BOTH"}, format="json").status_code, 200)
        self.sign_in(self.admin)
        response = self.client.post("/api/care/manage/affiliations/", {"hospital_id": self.hospital.pk, "doctor_id": self.doctor_b.pk, "fees": "200.00"}, format="json")
        self.assertEqual(response.status_code, 201, response.data)

    def test_successful_submission_verification_has_actor_reason_and_audit(self):
        self.sign_in(self.manager)
        response = self.client.post(f"/api/care/hospitals/{self.hospital.pk}/submit/", {"expected_revision": 1, "reason": "Please review submitted identity"}, format="json")
        self.assertEqual(response.status_code, 200, response.data)
        self.assertEqual(self.decision(self.hospital, "verify").status_code, 200)
        audit = VerificationAudit.objects.filter(record_type="HOSPITAL", record_id=self.hospital.pk, action="VERIFY").get()
        self.assertEqual((audit.actor_id, audit.previous_status, audit.new_status), (self.admin.pk, "PENDING", "VERIFIED"))
        self.assertTrue(audit.reason)

    def test_verification_requires_pending_status_reason_and_exact_revision(self):
        self.assertEqual(self.decision(self.hospital, "verify").status_code, 409)
        self.sign_in(self.admin)
        response = self.client.post(f"/api/care/hospitals/{self.hospital.pk}/submit/", {"expected_revision": 1, "reason": "Submitted for review"}, format="json")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(self.decision(self.hospital, "verify", revision=1).status_code, 409)
        self.assertEqual(self.client.post(f"/api/care/admin/hospitals/{self.hospital.pk}/verify/", {"expected_revision": 2, "reason": " "}, format="json").status_code, 400)

    def test_doctor_submission_requires_council_and_qualifications(self):
        services.update_record(self.admin, DoctorProfile, self.doctor.pk, {"registration_council": "", "qualifications": []})
        self.doctor.refresh_from_db()
        self.sign_in(self.manager)
        response = self.client.post(f"/api/care/doctors/{self.doctor.pk}/submit/", {"expected_revision": self.doctor.revision, "reason": "Submission"}, format="json")
        self.assertEqual(response.status_code, 400)
        self.assertIn("registration_council", response.data)

    def test_critical_hospital_edit_removes_public_visibility_and_increments_revision(self):
        self.publish(self.hospital)
        self.sign_in(self.manager)
        old_revision = self.hospital.revision
        response = self.client.patch(f"/api/care/hospitals/{self.hospital.pk}/", {"name": "Changed test identity"}, format="json")
        self.assertEqual(response.status_code, 200, response.data)
        self.assertEqual(response.data["verification_status"], "PENDING")
        self.assertGreater(response.data["revision"], old_revision)
        self.assertEqual(self.client.get(f"/api/care/hospitals/{self.hospital.pk}/").status_code, 404)
        audit = VerificationAudit.objects.filter(action="REVERIFY_REQUIRED", record_type="HOSPITAL", record_id=self.hospital.pk).get()
        self.assertEqual(audit.actor_id, self.manager.pk)
        self.assertTrue(audit.reason)

    def test_critical_doctor_edit_revokes_legacy_verification(self):
        self.publish(self.hospital)
        self.publish(self.doctor)
        self.sign_in(self.manager)
        response = self.client.patch(f"/api/care/doctors/{self.doctor.pk}/", {"registration_number": "TEST-CHANGED-101"}, format="json")
        self.assertEqual(response.status_code, 200, response.data)
        self.doctor.refresh_from_db()
        self.assertFalse(self.doctor.is_verified)
        self.assertEqual(self.doctor.verification_status, "PENDING")
        self.assertEqual(self.client.get("/api/care/doctors/").data["count"], 0)

    def test_legacy_doctor_self_update_also_revokes_verification(self):
        self.doctor.user = self.patient
        self.doctor.save()
        self.publish(self.doctor)
        self.sign_in(self.patient)
        result = self.client.patch("/doctor/profile/update", {"full_name": "Changed through legacy API"}, format="multipart")
        self.assertEqual(result.status_code, 200, result.data)
        self.doctor.refresh_from_db()
        self.assertFalse(self.doctor.is_verified)
        self.assertEqual(self.doctor.verification_status, "PENDING")

    def test_suspended_record_cannot_be_reactivated_by_hospital_admin(self):
        self.publish(self.hospital)
        self.assertEqual(self.decision(self.hospital, "suspend").status_code, 200)
        self.sign_in(self.manager)
        self.assertEqual(self.client.patch(f"/api/care/hospitals/{self.hospital.pk}/", {"description": "A revision"}, format="json").data["verification_status"], "SUSPENDED")
        self.hospital.refresh_from_db()
        self.assertEqual(self.client.post(f"/api/care/hospitals/{self.hospital.pk}/submit/", {"expected_revision": self.hospital.revision, "reason": "Attempt to reactivate"}, format="json").status_code, 409)
        self.assertEqual(self.decision(self.hospital, "reopen", self.manager).status_code, 403)

    def test_jwt_expiry_bad_signature_and_management_authentication(self):
        token = AccessToken.for_user(self.manager)
        token.set_exp(lifetime=timedelta(seconds=-1))
        for value in (str(token), "invalid-token"):
            self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {value}")
            self.assertEqual(self.client.get("/api/care/manage/hospitals/").status_code, 401)
        self.client.credentials()
        self.assertEqual(self.client.get("/api/care/manage/hospitals/").status_code, 401)

    def test_staff_flag_alone_does_not_grant_vedamrit_verifier(self):
        self.manager.is_staff = True
        self.manager.save()
        self.assertFalse(is_directory_admin(self.manager))
        self.assertEqual(self.decision(self.hospital, "verify", self.manager).status_code, 403)

    def test_explicit_manage_permission_does_not_grant_verification(self):
        self.patient.user_permissions.add(Permission.objects.get(content_type__app_label="community_care", codename="manage_directory"))
        self.sign_in(self.patient)
        self.assertEqual(self.client.get("/api/care/manage/hospitals/").data["count"], 2)
        self.assertEqual(self.decision(self.hospital, "verify", self.patient).status_code, 403)

    def test_independent_verifier_cannot_approve_assigned_hospital_or_own_doctor(self):
        services.save_membership(self.admin, {"hospital": self.hospital, "user": self.admin, "role": "ADMIN"})
        self.sign_in(self.admin)
        self.assertEqual(self.client.post(f"/api/care/hospitals/{self.hospital.pk}/submit/", {"expected_revision": 1, "reason": "Submitted"}, format="json").status_code, 200)
        self.assertEqual(self.decision(self.hospital, "verify").status_code, 403)

    def test_filtering_pagination_and_page_size_cap(self):
        self.publish(self.hospital)
        self.publish(self.hospital_b)
        self.publish(self.doctor)
        response = self.client.get("/api/care/hospitals/?page_size=1")
        self.assertEqual(response.data["count"], 2)
        self.assertEqual(len(response.data["results"]), 1)
        self.assertIsNotNone(response.data["next"])
        self.assertEqual(self.client.get("/api/care/hospitals/?city=pune&search=synthetic").data["count"], 1)
        self.assertEqual(self.client.get("/api/care/doctors/?city=pune&specialization=ayur&hospital_id=" + str(self.hospital.pk)).data["count"], 1)
        self.assertEqual(self.client.get("/api/care/doctors/?hospital_id=bad").status_code, 400)
        self.assertEqual(self.client.get("/api/care/hospitals/?page=999").status_code, 404)

    def test_public_filters_do_not_reveal_hidden_affiliation_or_nonverified_records_to_admin(self):
        self.publish(self.hospital)
        self.publish(self.doctor)
        services.save_affiliation(self.admin, {"doctor_id": self.doctor.pk, "hospital_id": self.hospital_b.pk, "fees": 100})
        self.sign_in(self.admin)
        self.assertEqual(self.client.get("/api/care/hospitals/?status=DRAFT").data["count"], 1)
        self.assertEqual(self.client.get("/api/care/doctors/?city=Mumbai").data["count"], 0)
        self.assertEqual(self.client.get(f"/api/care/doctors/?hospital_id={self.hospital_b.pk}").data["count"], 0)
        public = self.client.get(f"/api/care/doctors/{self.doctor.pk}/").data
        self.assertEqual(len(public["affiliations"]), 1)

    def test_affiliation_filters_match_one_visible_assigned_hospital(self):
        services.update_record(self.admin, Hospital, self.hospital_b.pk, {"state": "Goa"})
        self.publish(self.hospital)
        self.publish(self.hospital_b)
        self.publish(self.doctor)
        services.save_affiliation(self.admin, {"doctor_id": self.doctor.pk, "hospital_id": self.hospital_b.pk, "fees": 100})
        self.assertEqual(self.client.get("/api/care/doctors/?city=Pune&state=Goa").data["count"], 0)
        self.assertEqual(self.client.get("/api/care/doctors/?city=Mumbai&state=Goa").data["count"], 1)
        self.sign_in(self.manager)
        self.assertEqual(self.client.get("/api/care/manage/doctors/?city=Mumbai").data["count"], 0)
        self.assertEqual(self.client.get("/api/care/manage/doctors/?city=Pune").data["count"], 1)

    def test_duplicate_hospital_and_case_insensitive_doctor_email_are_rejected(self):
        self.sign_in(self.admin)
        response = self.client.post("/api/care/hospitals/", self.hospital_data(name=self.hospital.name.upper()), format="json")
        self.assertEqual(response.status_code, 400, response.data)
        self.assertEqual(self.client.post("/api/care/doctors/", self.doctor_request(email="DOCTOR@EXAMPLE.TEST"), format="json").status_code, 400)

    def test_invalid_identifiers_fees_metadata_and_experience_are_rejected(self):
        self.sign_in(self.admin)
        for changes in ({"registration_number": "!!!"}, {"registration_number": "letters only"}, {"phone": "abc"}, {"experience_years": -1},
                        {"consultation_fee": "-1.00"}, {"fees": "-2.00"}, {"qualifications": [3]}, {"profile_metadata": []}):
            with self.subTest(changes=changes):
                self.assertEqual(self.client.post("/api/care/doctors/", self.doctor_request(**changes), format="json").status_code, 400)

    def test_audit_is_read_only_and_hospital_admin_cannot_read_global_history(self):
        self.sign_in(self.manager)
        self.assertEqual(self.client.get("/api/care/admin/audits/").status_code, 403)
        self.sign_in(self.admin)
        result = self.client.get(f"/api/care/admin/audits/?record_type=HOSPITAL&record_id={self.hospital.pk}")
        self.assertGreater(result.data["count"], 0)
        self.assertEqual(self.client.post("/api/care/admin/audits/", {}, format="json").status_code, 405)
        audit = VerificationAudit.objects.first()
        audit.reason = "Tampered"
        with self.assertRaises(ModelValidationError):
            audit.save()
        with self.assertRaises(ModelValidationError):
            audit.delete()

    def test_django_admin_care_access_is_explicit_and_audits_read_only(self):
        request = RequestFactory().get("/admin/")
        request.user = self.manager
        hospital_admin = admin.site._registry[Hospital]
        self.assertFalse(hospital_admin.has_module_permission(request))
        self.assertFalse(hospital_admin.has_change_permission(request, self.hospital))
        request.user = self.admin
        self.assertTrue(hospital_admin.has_change_permission(request, self.hospital))
        self.assertIn("verification_status", hospital_admin.get_readonly_fields(request, self.hospital))
        audit_admin = admin.site._registry[VerificationAudit]
        self.assertFalse(audit_admin.has_add_permission(request))
        self.assertFalse(audit_admin.has_change_permission(request))
        self.assertFalse(audit_admin.has_delete_permission(request))

    def test_no_delete_or_put_endpoints(self):
        self.sign_in(self.admin)
        self.assertEqual(self.client.delete(f"/api/care/hospitals/{self.hospital.pk}/").status_code, 405)
        self.assertEqual(self.client.put(f"/api/care/doctors/{self.doctor.pk}/", {}, format="json").status_code, 405)

    def test_consistent_verification_database_constraint(self):
        with self.assertRaises(IntegrityError), transaction.atomic():
            DoctorProfile.objects.filter(pk=self.doctor.pk).update(is_verified=True)

    def test_link_existing_user_to_directory_doctor_requires_admin_and_reverification(self):
        self.publish(self.doctor)
        self.sign_in(self.manager)
        url = f"/api/care/admin/doctors/{self.doctor.pk}/link-user/"
        payload = {"user_id": self.patient.pk, "expected_revision": self.doctor.revision, "reason": "Account ownership independently checked"}
        self.assertEqual(self.client.post(url, payload, format="json").status_code, 403)
        self.sign_in(self.admin)
        result = self.client.post(url, payload, format="json")
        self.assertEqual(result.status_code, 200, result.data)
        self.doctor.refresh_from_db()
        self.assertEqual(self.doctor.user_id, self.patient.pk)
        self.assertFalse(self.doctor.is_verified)
        self.assertEqual(self.doctor.verification_status, "PENDING")
        payload["expected_revision"] = self.doctor.revision
        payload["user_id"] = self.staff.pk
        self.assertEqual(self.client.post(url, payload, format="json").status_code, 409)

    def test_linking_user_cannot_create_duplicate_doctor_identity(self):
        self.sign_in(self.admin)
        payload = {"user_id": self.patient.pk, "expected_revision": 1, "reason": "Account ownership checked"}
        self.assertEqual(self.client.post(f"/api/care/admin/doctors/{self.doctor.pk}/link-user/", payload, format="json").status_code, 200)
        response = self.client.post(f"/api/care/admin/doctors/{self.doctor_b.pk}/link-user/", payload, format="json")
        self.assertEqual(response.status_code, 400, response.data)

    def test_legacy_registration_duplicate_directory_identifier_does_not_create_orphan_account(self):
        values = self.doctor_values(email="new-login@example.test", phone="+919800000030")
        values["password"] = "test-only-long-password"
        count = get_user_model().objects.count()
        response = self.client.post("/doctor/register", values, format="multipart")
        self.assertEqual(response.status_code, 400, response.data)
        self.assertEqual(get_user_model().objects.count(), count)

    def test_legacy_doctor_edit_reports_case_insensitive_duplicate_as_validation_error(self):
        self.doctor.user = self.patient
        self.doctor.save()
        self.sign_in(self.patient)
        result = self.client.patch("/doctor/profile/update", {"email": self.doctor_b.email.upper()}, format="multipart")
        self.assertEqual(result.status_code, 400, result.data)
        self.doctor.refresh_from_db()
        self.assertNotEqual(self.doctor.email.lower(), self.doctor_b.email.lower())

    def test_admin_pages_render_with_restricted_fields(self):
        self.client.force_login(self.admin)
        for url in ("/admin/community_care/hospital/", f"/admin/community_care/hospital/{self.hospital.pk}/change/",
                    f"/admin/doctors/doctorprofile/{self.doctor.pk}/change/", "/admin/community_care/verificationaudit/"):
            with self.subTest(url=url):
                self.assertEqual(self.client.get(url).status_code, 200)

    def test_admin_verification_action_requires_reviewed_revision_and_logs_decision(self):
        services.transition(self.admin, Hospital, self.hospital.pk, "SUBMIT", 1, "Ready for independent review")
        self.client.force_login(self.admin)
        response = self.client.post("/admin/community_care/hospital/", {"action": "verify_records", "_selected_action": [self.hospital.pk],
            "index": "0", "expected_revision": "2", "reason": "Identity independently reviewed"})
        self.assertEqual(response.status_code, 302)
        self.hospital.refresh_from_db()
        self.assertEqual(self.hospital.verification_status, "VERIFIED")
        self.assertTrue(VerificationAudit.objects.filter(record_type="HOSPITAL", record_id=self.hospital.pk, action="VERIFY", actor=self.admin).exists())
