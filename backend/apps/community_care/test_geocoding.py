import io
import json
from decimal import Decimal
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import Mock, MagicMock, patch
from urllib.error import HTTPError, URLError

from django.contrib.auth import get_user_model
from django.core.management import call_command
from django.core.management.base import CommandError
from django.test import SimpleTestCase, override_settings
from rest_framework.exceptions import PermissionDenied, ValidationError
from rest_framework.test import APITestCase
from rest_framework_simplejwt.tokens import RefreshToken

from . import services
from .geocoding import MapTilerClient, GeocodingError, features, geocode_hospital, query_details, snapshot
from .location_review import review_location
from .models import Hospital, HospitalMembership, GeocodingRun, LocationCandidate, VerificationAudit


def response(lat=24.5854, lon=73.7125, kind="poi", label="Synthetic hospital, Udaipur, Rajasthan, India", relevance=.95):
    return {"features": [{"id": "fixture.123", "geometry": {"type": "Point", "coordinates": [lon, lat]},
        "place_type": [kind], "place_name": label, "relevance": relevance}], "attribution": "Synthetic fixture attribution"}


class FakeClient:
    def __init__(self, payload=None):
        self.requests = 0
        self.payload = payload if payload is not None else response()

    def search(self, query):
        self.requests += 1
        if query == "Udaipur, Rajasthan, India":
            return response(kind="municipality", label="Udaipur, Rajasthan, India")
        return self.payload


@override_settings(MAPTILER_API_KEY="unit-test-key", MAPTILER_GEOCODING_MIN_INTERVAL=.2,
                   MAPTILER_GEOCODING_RETRIES=2, MAPTILER_GEOCODING_REQUEST_BUDGET=24)
class ProviderClientTests(SimpleTestCase):
    def make_client(self, payload=None):
        client = MapTilerClient()
        client.opener = MagicMock()
        client.opener.open.return_value.__enter__.return_value.read.return_value = json.dumps(payload or response()).encode()
        return client

    @override_settings(MAPTILER_API_KEY="")
    def test_missing_key_is_actionable_without_network(self):
        with self.assertRaises(GeocodingError) as error:
            MapTilerClient()
        self.assertEqual(error.exception.code, "MISSING_KEY")

    def test_documented_https_endpoint_and_country_limits(self):
        client = self.make_client()
        client.search("A hospital, Udaipur")
        request = client.opener.open.call_args.args[0]
        self.assertTrue(request.full_url.startswith("https://api.maptiler.com/geocoding/"))
        self.assertIn("country=in", request.full_url)
        self.assertIn("limit=5", request.full_url)
        self.assertEqual(client.requests, 1)

    @patch("apps.community_care.geocoding.time.sleep")
    def test_rate_limit_retries_with_retry_after_and_budget(self, sleep):
        client = self.make_client()
        success = client.opener.open.return_value
        client.opener.open.side_effect = [HTTPError("private", 429, "limited", {"Retry-After": "2"}, None), success]
        client.search("fixture")
        self.assertEqual(client.requests, 2)
        sleep.assert_any_call(2)

    @patch("apps.community_care.geocoding.time.sleep")
    def test_long_retry_after_defers_without_retrying_early(self, sleep):
        client = self.make_client()
        client.opener.open.side_effect = HTTPError("private", 429, "limited", {"Retry-After": "120"}, None)
        with self.assertRaises(GeocodingError) as error:
            client.search("fixture")
        self.assertEqual(error.exception.code, "RATE_LIMITED")
        self.assertEqual(client.requests, 1)

    @patch("apps.community_care.geocoding.time.sleep")
    def test_network_failures_have_bounded_retries(self, sleep):
        client = self.make_client()
        client.opener.open.side_effect = URLError("https://private/?key=unit-test-key")
        with self.assertRaises(GeocodingError) as error:
            client.search("fixture")
        self.assertEqual(client.requests, 3)
        self.assertNotIn("unit-test-key", str(error.exception))
        self.assertEqual(error.exception.code, "NETWORK_TIMEOUT")

    def test_invalid_key_error_is_sanitized_and_not_retried(self):
        client = self.make_client()
        client.opener.open.side_effect = HTTPError("https://private/?key=unit-test-key", 403, "unit-test-key", {}, None)
        with self.assertRaises(GeocodingError) as error:
            client.search("fixture")
        self.assertEqual(client.requests, 1)
        self.assertNotIn("unit-test-key", str(error.exception))

    @override_settings(MAPTILER_GEOCODING_REQUEST_BUDGET=1)
    def test_request_budget_includes_subsequent_searches(self):
        client = self.make_client()
        client.search("one")
        with self.assertRaises(GeocodingError) as error:
            client.search("two")
        self.assertEqual(error.exception.code, "REQUEST_BUDGET")

    def test_malformed_response_is_rejected(self):
        client = self.make_client({"not_features": []})
        with self.assertRaises(GeocodingError):
            client.search("fixture")

    def test_coordinates_are_lon_lat_and_invalid_points_are_discarded(self):
        row = features(response())[0]
        self.assertEqual(row["latitude"], Decimal("24.585400"))
        self.assertEqual(row["longitude"], Decimal("73.712500"))
        self.assertEqual(features(response(lat=91)), [])
        self.assertEqual(features(response(lat="NaN")), [])

    def test_provider_echo_cannot_persist_key(self):
        row = features(response(label="unit-test-key"))[0]
        self.assertNotIn("unit-test-key", row["label"])


@override_settings(PASSWORD_HASHERS=["django.contrib.auth.hashers.MD5PasswordHasher"], MAPTILER_API_KEY="unit-test-key")
class GeocodingWorkflowTests(APITestCase):
    def setUp(self):
        self.admin = get_user_model().objects.create_superuser("geo-admin", "admin@example.test", "test-only")
        self.manager = get_user_model().objects.create_user("geo-manager")
        self.hospital = Hospital.objects.create(name="Synthetic hospital", address="1 Test Street", city="Udaipur", district="Udaipur",
            state="Rajasthan", postal_code="313001", phone="9900000011", source_namespace="fixture", source_record_id="UDR-001",
            import_review_status="NEEDS_REVIEW")

    def proposal(self, payload=None):
        run, _ = geocode_hospital(self.hospital, FakeClient(payload), {})
        return run.candidates.first()

    def sign_in(self, user):
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {RefreshToken.for_user(user).access_token}")

    def approve(self, candidate, **kwargs):
        self.hospital.refresh_from_db()
        return review_location(self.admin, candidate.pk, "APPROVE", self.hospital.revision,
            "Exact facility entrance independently checked with evidence.", exact_location_confirmed=True, **kwargs)

    def test_generation_does_not_change_approved_coordinates_or_hospital_state(self):
        before = Hospital.objects.values().get(pk=self.hospital.pk)
        candidate = self.proposal()
        self.hospital.refresh_from_db()
        self.assertEqual(Hospital.objects.values().get(pk=self.hospital.pk), before)
        self.assertEqual(candidate.status, "PROPOSED")
        self.assertEqual(candidate.confidence, .95)
        self.assertTrue(candidate.attribution)

    def test_query_uses_full_identity_and_repeat_has_no_api_calls(self):
        client = FakeClient()
        run, _ = geocode_hospital(self.hospital, client, {})
        self.assertIn("Synthetic hospital, 1 Test Street, Udaipur, Rajasthan", run.query)
        self.assertEqual(client.requests, 2)
        second, outcome = geocode_hospital(self.hospital, client, {})
        self.assertEqual(run.pk, second.pk)
        self.assertEqual(outcome, "skipped")
        self.assertEqual(client.requests, 2)
        self.assertEqual(LocationCandidate.objects.count(), 1)

    def test_long_queries_are_bounded_with_name_street_and_locality_preserved(self):
        import re
        self.hospital.name = "Government Ayurvedic Hospital and Research Centre Gulab Bagh Udaipur"
        self.hospital.address = "Palace Road Old City Kalaji Goraji near Gulab Bagh additional source landmarks"
        data, query, digest = query_details(self.hospital)
        self.assertLessEqual(len(re.findall(r"\w+", query)), 20)
        self.assertIn("Government", query)
        self.assertIn("Palace", query)
        self.assertIn("Udaipur", query)
        self.assertEqual(data["address"], self.hospital.address)
        candidate = self.proposal()
        self.assertIn("QUERY_SHORTENED_FOR_PROVIDER", candidate.flags)

    def test_ambiguous_results_preserve_multiple_proposals(self):
        payload = response()
        payload["features"].append(response(lon=73.72)["features"][0])
        self.proposal(payload)
        self.assertEqual(LocationCandidate.objects.count(), 2)
        self.assertTrue(all("AMBIGUOUS_RESULTS" in candidate.flags for candidate in LocationCandidate.objects.all()))

    def test_distant_broad_and_low_relevance_matches_flagged(self):
        candidate = self.proposal(response(lat=28, lon=77, kind="municipality", relevance=.4))
        self.assertIn("DISTANT_FROM_LOCALITY_REFERENCE", candidate.flags)
        self.assertIn("BROAD_LOCATION_NOT_CONFIRMED_FACILITY", candidate.flags)
        self.assertIn("LOW_OR_MISSING_RELEVANCE", candidate.flags)

    def test_duplicate_flags_never_merge_and_require_explicit_resolution(self):
        self.hospital.import_provenance = {"duplicate_candidates": ["UDR-004", "UDR-005"]}
        self.hospital.save()
        candidate = self.proposal()
        self.assertIn("DUPLICATE_REVIEW_REQUIRED", candidate.flags)
        with self.assertRaises(ValidationError):
            self.approve(candidate)
        from .duplicate_review import record_duplicate_review
        self.hospital.refresh_from_db()
        record_duplicate_review(self.admin, self.hospital.pk, "DISTINCT_CONFIRMED", self.hospital.revision, 1,
            "Separate facility identities independently confirmed in fixture.", "https://example.test/facility-evidence", human_confirmed=True)
        self.approve(candidate, duplicate_review_resolved=True)
        self.assertEqual(Hospital.objects.count(), 1)

    def test_empty_results_are_cached_without_inventing_coordinates(self):
        client = FakeClient({"features": []})
        run, _ = geocode_hospital(self.hospital, client, {})
        self.assertEqual(run.status, "NO_RESULTS")
        self.assertEqual(LocationCandidate.objects.count(), 0)
        geocode_hospital(self.hospital, client, {})
        self.assertEqual(client.requests, 2)

    def test_provider_failure_is_recorded_and_cooldown_avoids_calls(self):
        client = Mock()
        client.search.side_effect = GeocodingError("RATE_LIMITED")
        run, _ = geocode_hospital(self.hospital, client, {})
        self.assertEqual(run.error_code, "RATE_LIMITED")
        self.assertEqual(geocode_hospital(self.hospital, client, {})[1], "cooldown")
        self.assertEqual(client.search.call_count, 1)

    def test_location_approval_preserves_draft_identity_and_records_audit(self):
        candidate = self.proposal()
        self.approve(candidate)
        self.hospital.refresh_from_db()
        self.assertEqual(self.hospital.verification_status, "DRAFT")
        self.assertEqual(self.hospital.import_review_status, "NEEDS_REVIEW")
        self.assertEqual(self.hospital.location_status, "APPROVED")
        self.assertEqual(self.hospital.latitude, candidate.latitude)
        audit = VerificationAudit.objects.get(action="LOCATION_APPROVED")
        self.assertEqual(audit.actor, self.admin)
        self.assertEqual((audit.previous_status, audit.new_status), ("DRAFT", "DRAFT"))
        self.assertEqual(self.client.get("/api/care/hospitals/nearby/?latitude=24.5854&longitude=73.7125").data["count"], 0)

    def test_exact_location_attestation_and_verifier_role_are_required(self):
        candidate = self.proposal()
        with self.assertRaises(PermissionDenied):
            review_location(self.manager, candidate.pk, "APPROVE", 1, "Attempted self approval", True)
        with self.assertRaises(ValidationError):
            review_location(self.admin, candidate.pk, "APPROVE", 1, "No independent confirmation yet")

    def test_verifier_cannot_approve_assigned_hospital(self):
        services.save_membership(self.admin, {"hospital": self.hospital, "user": self.admin, "role": "ADMIN"})
        candidate = self.proposal()
        with self.assertRaises(PermissionDenied):
            self.approve(candidate)

    def test_rejection_is_audited_without_touching_hospital(self):
        candidate = self.proposal()
        review_location(self.admin, candidate.pk, "REJECT", 1, "Result is a different facility, independently checked.")
        self.hospital.refresh_from_db()
        self.assertIsNone(self.hospital.latitude)
        self.assertEqual(VerificationAudit.objects.filter(action="LOCATION_REJECTED").count(), 1)

    def test_changed_address_blocks_stale_proposal_and_requires_new_geocoding(self):
        candidate = self.proposal()
        services.update_record(self.admin, Hospital, self.hospital.pk, {"address": "2 Different Street"})
        with self.assertRaises(services.Conflict):
            self.approve(candidate)

    def test_coordinate_or_address_edit_invalidates_location_approval(self):
        self.approve(self.proposal())
        services.update_record(self.admin, Hospital, self.hospital.pk, {"address": "3 New Street"})
        self.hospital.refresh_from_db()
        self.assertEqual(self.hospital.location_status, "STALE")

    def test_location_api_requires_auth_and_blocks_non_verifiers(self):
        candidate = self.proposal()
        self.assertEqual(self.client.get("/api/care/admin/location-candidates/").status_code, 401)
        self.sign_in(self.manager)
        payload = {"expected_revision": 1, "reason": "Confirmed exact position independently", "exact_location_confirmed": True}
        self.assertEqual(self.client.post(f"/api/care/admin/location-candidates/{candidate.pk}/approve/", payload, format="json").status_code, 403)
        self.sign_in(self.admin)
        self.assertEqual(self.client.get("/api/care/admin/location-candidates/").data["count"], 1)
        self.assertEqual(self.client.post(f"/api/care/admin/location-candidates/{candidate.pk}/approve/", payload, format="json").status_code, 200)

    def test_command_dry_run_does_not_require_key_or_make_requests(self):
        with TemporaryDirectory() as directory, patch("apps.community_care.management.commands.geocode_hospitals.MapTilerClient") as client:
            path = Path(directory) / "report.json"
            call_command("geocode_hospitals", district="Udaipur", limit=1, dry_run=True, report=str(path), stdout=io.StringIO())
            client.assert_not_called()
            self.assertEqual(json.loads(path.read_text())["records"][0]["outcome"], "would_geocode")
            self.assertEqual(GeocodingRun.objects.count(), 0)

    def test_command_uses_mocked_provider_report_and_repeat_cache(self):
        with TemporaryDirectory() as directory, patch("apps.community_care.management.commands.geocode_hospitals.MapTilerClient", return_value=FakeClient()):
            path = Path(directory) / "report.json"
            call_command("geocode_hospitals", source_ids=["UDR-001"], report=str(path), stdout=io.StringIO())
            report = json.loads(path.read_text())
            self.assertEqual(report["summary"]["candidate_count"], 1)
            self.assertNotIn("unit-test-key", path.read_text())
            call_command("geocode_hospitals", stdout=io.StringIO())
            self.assertEqual(GeocodingRun.objects.count(), 1)

    def test_admin_proposal_view_and_action_use_review_permissions(self):
        candidate = self.proposal()
        self.client.force_login(self.admin)
        self.assertEqual(self.client.get(f"/admin/community_care/locationcandidate/{candidate.pk}/change/").status_code, 200)
        result = self.client.post("/admin/community_care/locationcandidate/", {"action": "approve_location", "_selected_action": [candidate.pk],
            "index": "0", "expected_revision": "1", "reason": "Exact location independently checked against entrance evidence.", "exact_location_confirmed": "on"})
        self.assertEqual(result.status_code, 302)
        candidate.refresh_from_db()
        self.assertEqual(candidate.status, "APPROVED")

    def test_stale_revision_and_repeated_decision_are_rejected(self):
        candidate = self.proposal()
        with self.assertRaises(services.Conflict):
            review_location(self.admin, candidate.pk, "APPROVE", 99, "Exact location independently checked", True)
        self.approve(candidate)
        with self.assertRaises(services.Conflict):
            self.approve(candidate)


@override_settings(PASSWORD_HASHERS=["django.contrib.auth.hashers.MD5PasswordHasher"])
class NearbyTests(APITestCase):
    def setUp(self):
        self.admin = get_user_model().objects.create_superuser("nearby-verifier", "nearby@example.test", "test-only")

    def hospital(self, name, lat, lon, verified=True):
        hospital = Hospital.objects.create(name=name, address=name + " Street", city="Udaipur", state="Rajasthan", postal_code="313001",
            phone="9900000011", verification_status="VERIFIED" if verified else "DRAFT")
        data, query, digest = query_details(hospital)
        run = GeocodingRun.objects.create(hospital=hospital, snapshot=data, query=query, query_hash=digest, status="SUCCESS")
        candidate = LocationCandidate.objects.create(run=run, fingerprint="fixture", latitude=lat, longitude=lon, label=name, attribution="Fixture attribution")
        review_location(self.admin, candidate.pk, "APPROVE", 1, "Exact position independently confirmed for isolated fixture.", True)
        hospital.refresh_from_db()
        return hospital

    def get(self, extra=""):
        return self.client.get("/api/care/hospitals/nearby/?latitude=24.5854&longitude=73.7125" + extra)

    def test_radius_sorting_and_page_contract(self):
        near = self.hospital("Near fixture", "24.585400", "73.712500")
        far = self.hospital("Far fixture", "24.685400", "73.712500")
        self.hospital("Outside fixture", "25.585400", "73.712500")
        result = self.get("&radius_km=20&page_size=1")
        self.assertEqual(result.status_code, 200, result.data)
        self.assertEqual(result.data["count"], 2)
        self.assertEqual(result.data["results"][0]["id"], near.pk)
        self.assertAlmostEqual(result.data["results"][0]["distance_km"], 0, places=4)
        self.assertIsNotNone(result.data["next"])
        result = self.get("&radius_km=20&page=2&page_size=1")
        self.assertEqual(result.data["results"][0]["id"], far.pk)
        self.assertAlmostEqual(result.data["results"][0]["distance_km"], 11.1195, places=3)
        self.assertFalse(result.data["results"][0]["booking_enabled"])

    def test_draft_inactive_unapproved_and_stale_do_not_leak(self):
        self.hospital("Draft fixture", "24.5854", "73.7125", verified=False)
        inactive = self.hospital("Inactive fixture", "24.5854", "73.7125")
        Hospital.objects.filter(pk=inactive.pk).update(is_active=False)
        stale = self.hospital("Stale fixture", "24.5854", "73.7125")
        Hospital.objects.filter(pk=stale.pk).update(location_status="STALE")
        Hospital.objects.create(name="Unapproved fixture", address="Unknown", city="Udaipur", state="Rajasthan", postal_code="313001", phone="9900000011", verification_status="VERIFIED", latitude="24.5854", longitude="73.7125")
        self.assertEqual(self.get().data["count"], 0)

    def test_invalid_ranges_missing_inputs_and_nonfinite_radius(self):
        for url in ("?latitude=91&longitude=73", "?latitude=24&longitude=-181", "?latitude=NaN&longitude=73",
                    "?latitude=24&longitude=73&radius_km=101", "?latitude=24&longitude=73&radius_km=0", "?longitude=73", "?latitude=24&longitude=73&radius_km=Infinity"):
            self.assertEqual(self.client.get("/api/care/hospitals/nearby/" + url).status_code, 400)

    def test_dateline_distance_is_geographic_not_plain_coordinate_difference(self):
        self.hospital("Dateline fixture", "0", "-179.95")
        result = self.client.get("/api/care/hospitals/nearby/?latitude=0&longitude=179.95&radius_km=20")
        self.assertEqual(result.data["count"], 1)
        self.assertAlmostEqual(result.data["results"][0]["distance_km"], 11.1195, places=3)

    def test_existing_directory_does_not_require_location_approval(self):
        Hospital.objects.create(name="Directory-only fixture", address="Unknown", city="Udaipur", state="Rajasthan", postal_code="313001", phone="9900000011", verification_status="VERIFIED")
        self.assertEqual(self.client.get("/api/care/hospitals/").data["count"], 1)
        self.assertEqual(self.get().data["count"], 0)

    def test_udr001_approved_location_draft_identity_and_eleven_drafts_stay_private(self):
        # Isolated fixtures model the live UDR situation; never approve live records.
        first = self.hospital("UDR-001 isolated identity fixture", "24.5854", "73.7125", verified=False)
        Hospital.objects.filter(pk=first.pk).update(source_namespace="isolated_udr_test", source_record_id="UDR-001",
                                                    import_review_status="NEEDS_REVIEW")
        for index in range(2, 13):
            Hospital.objects.create(name=f"UDR-{index:03} isolated draft", address=f"{index} Fixture Street",
                city="Udaipur", state="Rajasthan", postal_code="313001", verification_status="DRAFT",
                source_namespace="isolated_udr_test", source_record_id=f"UDR-{index:03}", import_review_status="NEEDS_REVIEW")
        self.assertEqual(Hospital.objects.count(), 12)
        for suffix in ("", "?search=UDR", "?city=Udaipur"):
            self.assertEqual(self.client.get("/api/care/hospitals/" + suffix).data["count"], 0)
        self.assertEqual(self.get().data["count"], 0)
        self.assertEqual(self.client.get(f"/api/care/hospitals/{first.pk}/").status_code, 404)
        first.refresh_from_db()
        self.assertEqual((first.verification_status, first.location_status), ("DRAFT", "APPROVED"))
        self.assertFalse(first.booking_enabled)
        # Identity remains an independent gate even after source/import review is complete.
        Hospital.objects.filter(pk=first.pk).update(import_review_status="REVIEWED",
            import_review_notes="Independent fixture source checks completed; identity still pending.")
        self.assertEqual(self.client.get("/api/care/hospitals/").data["count"], 0)
        self.assertEqual(self.get().data["count"], 0)
        self.assertEqual(self.client.get(f"/api/care/hospitals/{first.pk}/").status_code, 404)

    def test_directory_hides_unapproved_stale_mismatched_and_revoked_coordinates(self):
        unapproved = Hospital.objects.create(name="Unapproved coordinate fixture", address="Test Street",
            city="Udaipur", state="Rajasthan", postal_code="313001", verification_status="VERIFIED",
            latitude="24.5854", longitude="73.7125")
        stale = self.hospital("Stale coordinate fixture", "24.5854", "73.7125")
        Hospital.objects.filter(pk=stale.pk).update(location_status="STALE")
        mismatch = self.hospital("Mismatched coordinate fixture", "24.5854", "73.7125")
        Hospital.objects.filter(pk=mismatch.pk).update(latitude="24.6")
        revoked = self.hospital("Revoked coordinate fixture", "24.5854", "73.7125")
        LocationCandidate.objects.filter(pk=revoked.approved_location_id).update(status="REJECTED")
        for hospital in (unapproved, stale, mismatch, revoked):
            data = self.client.get(f"/api/care/hospitals/{hospital.pk}/").data
            self.assertEqual(data["verification_status"], "VERIFIED")
            self.assertEqual(data["location_status"], "UNVERIFIED")
            self.assertIsNone(data["latitude"])
            self.assertIsNone(data["longitude"])
            self.assertEqual(data["location_attribution"], "")
        self.assertEqual(self.get().data["count"], 0)

    def test_directory_and_nearby_return_same_approved_marker_metadata(self):
        hospital = self.hospital("Approved map fixture", "24.5854", "73.7125")
        directory = self.client.get(f"/api/care/hospitals/{hospital.pk}/").data
        nearby = self.get().data["results"][0]
        for field in ("verification_status", "location_status", "latitude", "longitude", "location_attribution"):
            self.assertEqual(directory[field], nearby[field])
        self.assertEqual(directory["location_status"], "APPROVED")
        self.assertEqual(directory["location_attribution"], "Fixture attribution")
        self.assertFalse(directory["booking_enabled"])

    def test_nearby_name_city_search_is_filtered_before_pagination(self):
        first = self.hospital("Alpha map fixture", "24.5854", "73.7125")
        self.hospital("Beta map fixture", "24.5855", "73.7125")
        result = self.get("&search=alpha&page_size=1")
        self.assertEqual(result.data["count"], 1)
        self.assertEqual(result.data["results"][0]["id"], first.pk)
        self.assertIsNone(result.data["next"])
        self.assertEqual(self.get("&search=UDAIPUR").data["count"], 2)
        self.assertEqual(self.get("&search=missing").data["count"], 0)
