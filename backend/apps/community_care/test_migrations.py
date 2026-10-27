"""Forward compatibility exercised only inside Django's disposable test database."""
from django.db import connection
from django.db.migrations.executor import MigrationExecutor
from django.test import TransactionTestCase


class CareMigrationTests(TransactionTestCase):
    def test_exact_review_upgrade_flags_only_import_duplicates_without_approvals(self):
        executor = MigrationExecutor(connection)
        latest = executor.loader.graph.leaf_nodes()
        before = [("community_care", "0003_hospital_location_status_geocodingrun_and_more")]
        executor.migrate(before)
        try:
            old = executor.loader.project_state(before).apps
            Hospital = old.get_model("community_care", "Hospital")
            flagged = []
            for source_id in ("UDR-002", "UDR-004", "UDR-005"):
                flagged.append(Hospital.objects.create(name="Fixture " + source_id, address="Fixture address " + source_id,
                    city="Udaipur", state="Rajasthan", postal_code="313001", phone="", source_namespace="fixture", source_record_id=source_id,
                    import_review_status="NEEDS_REVIEW", import_provenance={"duplicate_candidates": ["fixture-related-record"]}))
            Hospital.objects.create(name="Unflagged fixture", address="Other fixture street", city="Udaipur", state="Rajasthan", postal_code="313001", phone="")
            run = old.get_model("community_care", "GeocodingRun").objects.create(hospital=flagged[0], query_hash="a" * 64, query="Fixture", snapshot={}, status="SUCCESS")
            candidate = old.get_model("community_care", "LocationCandidate").objects.create(run=run, fingerprint="b" * 64, latitude="24.500000", longitude="73.700000", label="Unapproved fixture", attribution="Fixture")
            executor = MigrationExecutor(connection)
            executor.migrate(latest)
            current = executor.loader.project_state(latest).apps
            reviews = current.get_model("community_care", "HospitalDuplicateReview").objects.all()
            self.assertEqual(reviews.count(), 3)
            self.assertEqual(set(reviews.values_list("hospital__source_record_id", flat=True)), {"UDR-002", "UDR-004", "UDR-005"})
            self.assertEqual(reviews.filter(status="NEEDS_REVIEW", reviewed_by__isnull=True, reviewed_at__isnull=True).count(), 3)
            restored = current.get_model("community_care", "LocationCandidate").objects.get(pk=candidate.pk)
            self.assertEqual(restored.status, "PROPOSED")
            self.assertEqual(restored.evidence_url, "")
            self.assertIsNone(restored.based_on_candidate_id)
            self.assertEqual(current.get_model("community_care", "Hospital").objects.filter(latitude__isnull=True, location_status="UNVERIFIED", verification_status="DRAFT").count(), 4)
        finally:
            MigrationExecutor(connection).migrate(latest)

    def test_import_fields_preserve_existing_hospital_identity_and_verification(self):
        executor = MigrationExecutor(connection)
        latest = executor.loader.graph.leaf_nodes()
        before = [("community_care", "0001_initial")]
        executor.migrate(before)
        try:
            old = executor.loader.project_state(before).apps
            record = old.get_model("community_care", "Hospital").objects.create(name="Historical hospital fixture", address="1 Historical Street",
                city="Pune", state="Maharashtra", postal_code="411001", phone="9900000011", verification_status="VERIFIED")
            executor = MigrationExecutor(connection)
            executor.migrate(latest)
            new = executor.loader.project_state(latest).apps.get_model("community_care", "Hospital").objects.get(pk=record.pk)
            self.assertEqual((new.name, new.phone, new.verification_status), (record.name, record.phone, "VERIFIED"))
            self.assertEqual(new.import_review_status, "NOT_IMPORTED")
            self.assertFalse(new.booking_enabled)
            self.assertEqual(new.import_provenance, {})
        finally:
            MigrationExecutor(connection).migrate(latest)

    def test_upgrade_preserves_existing_doctor_user_consultation_and_approval(self):
        executor = MigrationExecutor(connection)
        latest = executor.loader.graph.leaf_nodes()
        before = [("community_care", None), ("doctors", "0001_initial")]
        executor.migrate(before)
        try:
            old = executor.loader.project_state([("doctors", "0001_initial"), ("consultations", "0001_initial")]).apps
            User = old.get_model("auth", "User")
            Doctor = old.get_model("doctors", "DoctorProfile")
            patient = User.objects.create(username="migration-patient")
            clinician = User.objects.create(username="migration-doctor")
            pending_user = User.objects.create(username="migration-pending-doctor")
            doctor = Doctor.objects.create(user=clinician, full_name="Historical fixture", phone="9900000011",
                email="migration@example.test", bams_registration_number="HIST-001", specialization="Ayurveda",
                experience=4, clinic_name="Historical fixture clinic", consultation_fee="500.00", is_verified=True)
            pending = Doctor.objects.create(user=pending_user, full_name="Unverified historical fixture", phone="9900000012",
                email="pending-migration@example.test", bams_registration_number="HIST-002", specialization="Ayurveda",
                experience=1, clinic_name="Historical fixture clinic", consultation_fee="400.00", is_verified=False)
            consultation = old.get_model("consultations", "Consultation").objects.create(patient=patient, doctor=doctor, symptoms="Migration fixture")
            saved_id, created_at = doctor.pk, doctor.created_at
            executor = MigrationExecutor(connection)
            executor.migrate(latest)
            new = executor.loader.project_state(latest).apps
            restored = new.get_model("doctors", "DoctorProfile").objects.get(pk=saved_id)
            self.assertEqual(restored.user_id, clinician.pk)
            self.assertEqual(restored.verification_status, "VERIFIED")
            self.assertTrue(restored.is_verified)
            self.assertEqual(restored.created_at, created_at)
            self.assertEqual(restored.qualifications, [])
            self.assertEqual(new.get_model("doctors", "DoctorProfile").objects.get(pk=pending.pk).verification_status, "DRAFT")
            self.assertEqual(new.get_model("consultations", "Consultation").objects.get(pk=consultation.pk).doctor_id, saved_id)
            self.assertFalse(new.get_model("community_care", "VerificationAudit").objects.exists())
        finally:
            MigrationExecutor(connection).migrate(latest)
