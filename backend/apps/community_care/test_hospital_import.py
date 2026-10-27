import csv
import io
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch

from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.management import call_command
from django.core.management.base import CommandError
from django.test import override_settings
from rest_framework.exceptions import PermissionDenied, ValidationError as ApiValidationError
from rest_framework.test import APITestCase
from rest_framework_simplejwt.tokens import RefreshToken

from apps.doctors.models import DoctorProfile
from apps.consultations.models import Consultation
from . import services
from .hospital_import import HEADERS, ImportFailure, import_hospitals, load_dataset
from .models import Hospital, HospitalMembership, DoctorHospitalAffiliation, VerificationAudit


@override_settings(PASSWORD_HASHERS=["django.contrib.auth.hashers.MD5PasswordHasher"])
class HospitalImportTests(APITestCase):
    def setUp(self):
        self.directory = TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.csv = Path(self.directory.name) / "input.csv"
        self.dataset = Path(settings.BASE_DIR).parent / "data" / "imports" / "udaipur_hospitals.csv"
        self.rows = [row for _, row in load_dataset(self.dataset)[0]]
        self.admin = get_user_model().objects.create_superuser("import-admin", "fixture@example.test", "test-only")
        self.manager = get_user_model().objects.create_user("import-hospital-admin")

    def write_rows(self, rows):
        with self.csv.open("w", encoding="utf-8", newline="") as output:
            writer = csv.DictWriter(output, fieldnames=HEADERS)
            writer.writeheader()
            writer.writerows(rows)
        return self.csv

    def sign_in(self, user):
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {RefreshToken.for_user(user).access_token}")

    def test_actual_supplied_dataset_imports_twelve_distinct_unverified_types(self):
        report = import_hospitals(self.dataset)
        self.assertEqual(report["summary"]["created"], 12)
        self.assertEqual(report["summary"]["invalid"], 0)
        self.assertEqual(Hospital.objects.count(), 12)
        self.assertEqual(Hospital.objects.filter(facility_type="government_hospital").count(), 3)
        self.assertEqual(Hospital.objects.filter(facility_type="government_teaching_hospital").count(), 1)
        self.assertEqual(Hospital.objects.filter(facility_type="government_dispensary").count(), 1)
        self.assertEqual(Hospital.objects.filter(facility_type="private_hospital").count(), 4)
        self.assertEqual(Hospital.objects.filter(facility_type="private_clinic").count(), 2)
        self.assertEqual(Hospital.objects.filter(facility_type="private_treatment_centre").count(), 1)
        self.assertEqual(Hospital.objects.filter(verification_status="DRAFT", import_review_status="NEEDS_REVIEW", booking_enabled=False).count(), 12)

    def test_missing_coordinates_phones_and_urls_stay_missing(self):
        import_hospitals(self.dataset)
        self.assertEqual(Hospital.objects.filter(latitude__isnull=True, longitude__isnull=True).count(), 12)
        self.assertEqual(Hospital.objects.filter(phone="").count(), 4)
        self.assertEqual(Hospital.objects.filter(source_url="").count(), 12)
        self.assertTrue(all(row.facilities == [] for row in Hospital.objects.all()))

    def test_hours_are_provenance_not_doctor_availability_or_slots(self):
        import_hospitals(self.dataset)
        row = Hospital.objects.get(source_record_id="UDR-008")
        self.assertEqual(row.listed_hours_unverified, "Listed 24 hours; OPD unconfirmed")
        self.assertIn("Panchakarma", row.services_unverified)
        self.assertEqual((DoctorProfile.objects.count(), Consultation.objects.count(), DoctorHospitalAffiliation.objects.count()), (0, 0, 0))
        self.assertFalse(Hospital.objects.filter(booking_enabled=True).exists())

    def test_original_snapshot_identifiers_and_file_hash_are_preserved(self):
        report = import_hospitals(self.dataset, actor=self.admin)
        record = Hospital.objects.get(source_record_id="UDR-001")
        self.assertEqual(record.import_provenance["original_row"], self.rows[0])
        self.assertEqual(record.import_provenance["file_sha256"], report["file_sha256"])
        self.assertEqual(record.source_namespace, "udaipur_hospitals")
        self.assertEqual(record.created_by, self.admin)
        self.assertIn("not hospital registration", record.import_provenance["original_row"]["source_notes"])
        self.assertEqual(VerificationAudit.objects.filter(action="IMPORT", actor=self.admin).count(), 12)

    def test_dry_run_does_not_write_hospitals_audits_or_memberships(self):
        report = import_hospitals(self.dataset, dry_run=True, district="udaipur")
        self.assertEqual(report["summary"]["would_create"], 12)
        self.assertEqual(report["summary"]["created"], 0)
        self.assertFalse(Hospital.objects.exists())
        self.assertFalse(VerificationAudit.objects.exists())
        self.assertFalse(HospitalMembership.objects.exists())

    def test_repeat_import_is_idempotent_and_does_not_change_revisions_or_audit(self):
        import_hospitals(self.dataset)
        before = list(Hospital.objects.order_by("id").values())
        audits = VerificationAudit.objects.count()
        report = import_hospitals(self.dataset)
        self.assertEqual(report["summary"]["skipped"], 12)
        self.assertEqual(report["summary"]["created"], 0)
        self.assertEqual(before, list(Hospital.objects.order_by("id").values()))
        self.assertEqual(audits, VerificationAudit.objects.count())

    def test_changed_snapshot_of_existing_source_is_reported_not_overwritten(self):
        import_hospitals(self.dataset)
        original = Hospital.objects.get(source_record_id="UDR-001")
        changed = [{**self.rows[0], "name": "Changed source identity"}]
        report = import_hospitals(self.write_rows(changed))
        self.assertTrue(report["records"][0]["source_snapshot_changed"])
        original.refresh_from_db()
        self.assertEqual(original.name, self.rows[0]["name"])

    def test_old_city_candidates_are_flagged_without_automatically_merging(self):
        report = import_hospitals(self.dataset)
        flagged = {row["source_record_id"] for row in report["records"] if row["duplicate_candidates"]}
        self.assertTrue({"UDR-002", "UDR-004", "UDR-005"}.issubset(flagged))
        self.assertEqual(Hospital.objects.filter(source_record_id__in=flagged).count(), len(flagged))
        self.assertEqual(report["summary"]["review_needed"], 12)

    def test_exact_duplicate_with_new_source_id_is_skipped_and_reported(self):
        first, duplicate = self.rows[0], {**self.rows[0], "source_record_id": "UDR-999"}
        report = import_hospitals(self.write_rows([first, duplicate]))
        self.assertEqual(report["summary"]["created"], 1)
        self.assertEqual(report["summary"]["duplicate"], 1)
        self.assertEqual(report["summary"]["skipped"], 1)

    def test_existing_verified_hospital_is_never_overwritten_by_identity_or_source(self):
        import_hospitals(self.write_rows([self.rows[0]]))
        record = Hospital.objects.get()
        services.update_record(self.admin, Hospital, record.pk, {"import_review_status": "REVIEWED", "import_review_notes": "Phone and address independently checked; unique identity confirmed."})
        record.refresh_from_db()
        services.transition(self.admin, Hospital, record.pk, "SUBMIT", record.revision, "Source review complete")
        record.refresh_from_db()
        services.transition(self.admin, Hospital, record.pk, "VERIFY", record.revision, "Independent verifier checked source identity")
        record.refresh_from_db()
        before = record.__dict__.copy()
        report = import_hospitals(self.write_rows([{**self.rows[0], "address": "Changed upstream source address"}]))
        record.refresh_from_db()
        self.assertEqual(record.__dict__, before)
        self.assertEqual(report["summary"]["skipped"], 1)
        report = import_hospitals(self.write_rows([{**self.rows[0], "source_record_id": "OTHER-001"}]))
        self.assertEqual(report["summary"]["duplicate"], 1)
        self.assertEqual(Hospital.objects.count(), 1)

    def test_invalid_rows_abort_the_whole_import(self):
        for changes in ({"name": ""}, {"address": "x"}, {"postal_code": "bad"}, {"phone": "abc"}, {"latitude": "91", "longitude": "73"},
                        {"latitude": "18", "longitude": ""}, {"latitude": "NaN", "longitude": "73"}, {"facility_type": "unknown"},
                        {"verification_status": "VERIFIED"}, {"booking_enabled": "True"}):
            with self.subTest(changes=changes):
                with self.assertRaises(ImportFailure) as error:
                    import_hospitals(self.write_rows([self.rows[0], {**self.rows[1], **changes}]))
                self.assertEqual(error.exception.report["summary"]["created"], 0)
                self.assertEqual(error.exception.report["summary"]["invalid"], 1)
                self.assertFalse(Hospital.objects.exists())
                self.assertFalse(VerificationAudit.objects.exists())

    def test_repeated_source_id_in_one_file_is_invalid(self):
        with self.assertRaises(ImportFailure):
            import_hospitals(self.write_rows([self.rows[0], self.rows[0]]))
        self.assertFalse(Hospital.objects.exists())

    def test_district_filter_skips_other_districts_and_rejects_no_match(self):
        rows = [self.rows[0], {**self.rows[1], "district": "Other district"}]
        report = import_hospitals(self.write_rows(rows), district="Udaipur")
        self.assertEqual(report["summary"]["created"], 1)
        self.assertEqual(report["summary"]["skipped"], 1)
        with self.assertRaises(ImportFailure):
            import_hospitals(self.dataset, district="No matching district")

    def test_database_failure_rolls_back_prior_hospital_and_audit_writes(self):
        original = Hospital.save
        calls = 0
        def fail_second(record, *args, **kwargs):
            nonlocal calls
            calls += 1
            if calls == 2:
                raise RuntimeError("Synthetic database failure")
            return original(record, *args, **kwargs)
        with patch.object(Hospital, "save", fail_second), self.assertRaises(ImportFailure) as error:
            import_hospitals(self.dataset)
        self.assertEqual(error.exception.report["summary"]["created"], 0)
        self.assertFalse(Hospital.objects.exists())
        self.assertFalse(VerificationAudit.objects.exists())

    def test_report_write_failure_rolls_back_data(self):
        def fail_report(report):
            raise OSError("Synthetic report storage failure")
        with self.assertRaises(ImportFailure):
            import_hospitals(self.dataset, write_report=fail_report)
        self.assertFalse(Hospital.objects.exists())
        self.assertFalse(VerificationAudit.objects.exists())

    def test_command_writes_real_report_and_refuses_existing_report(self):
        report_path = Path(self.directory.name) / "report.json"
        output = io.StringIO()
        call_command("import_hospitals", file=str(self.dataset), dry_run=True, district="Udaipur", report=str(report_path), stdout=output)
        self.assertEqual(json.loads(report_path.read_text(encoding="utf-8"))["summary"]["would_create"], 12)
        with self.assertRaises(CommandError):
            call_command("import_hospitals", file=str(self.dataset), report=str(report_path))
        self.assertFalse(Hospital.objects.exists())

    def test_malformed_headers_csv_and_utf8_are_rejected_without_writes(self):
        for data in (b"name,name\na,b\n", b"\xffbad", b"source_record_id,name\nUDR-001,wrong\n"):
            self.csv.write_bytes(data)
            with self.assertRaises(ImportFailure):
                import_hospitals(self.csv)
        self.assertFalse(Hospital.objects.exists())

    def test_supplied_actor_must_be_authorized_and_system_operator_is_not_impersonated(self):
        with self.assertRaises(PermissionDenied):
            import_hospitals(self.dataset, actor=self.manager)
        import_hospitals(self.write_rows([self.rows[0]]))
        self.assertIsNone(VerificationAudit.objects.get().actor_id)
        self.assertEqual(Hospital.objects.get().import_provenance["operator"], "server_operator")

    def test_imports_hidden_and_verification_blocked_until_review(self):
        import_hospitals(self.dataset)
        self.assertEqual(self.client.get("/api/care/hospitals/").data["count"], 0)
        self.sign_in(self.admin)
        record = Hospital.objects.first()
        result = self.client.post(f"/api/care/hospitals/{record.pk}/submit/", {"expected_revision": record.revision, "reason": "Attempt before source review"}, format="json")
        self.assertEqual(result.status_code, 400)

    def test_hospital_admin_cannot_clear_review_or_forge_provenance_or_enable_booking(self):
        import_hospitals(self.write_rows([self.rows[0]]))
        record = Hospital.objects.get()
        services.save_membership(self.admin, {"hospital": record, "user": self.manager, "role": "ADMIN"})
        self.sign_in(self.manager)
        self.assertEqual(self.client.patch(f"/api/care/hospitals/{record.pk}/", {"import_review_status": "REVIEWED", "import_review_notes": "Attempted self review"}, format="json").status_code, 403)
        for payload in ({"source_record_id": "forged"}, {"import_provenance": {}}, {"booking_enabled": True}):
            self.assertEqual(self.client.patch(f"/api/care/hospitals/{record.pk}/", payload, format="json").status_code, 400)

    def test_missing_phone_must_be_corrected_by_admin_before_submission(self):
        import_hospitals(self.write_rows([self.rows[1]]))
        record = Hospital.objects.get()
        services.update_record(self.admin, Hospital, record.pk, {"import_review_status": "REVIEWED", "import_review_notes": "Address and source checked; telephone not yet obtained."})
        record.refresh_from_db()
        with self.assertRaises(ApiValidationError):
            services.transition(self.admin, Hospital, record.pk, "SUBMIT", record.revision, "Still missing contact")
        services.update_record(self.admin, Hospital, record.pk, {"phone": "+919900000099"})
        record.refresh_from_db()
        services.transition(self.admin, Hospital, record.pk, "SUBMIT", record.revision, "Contact independently confirmed")
        record.refresh_from_db()
        self.assertEqual(record.verification_status, "PENDING")

    def test_unverified_source_url_stays_blank_but_original_claim_is_retained(self):
        import_hospitals(self.write_rows([{**self.rows[0], "source_url": "https://example.test/unverified-listing"}]))
        record = Hospital.objects.get()
        self.assertEqual(record.source_url, "")
        self.assertEqual(record.import_provenance["original_row"]["source_url"], "https://example.test/unverified-listing")
