import json
from pathlib import Path

from django.core.management.base import BaseCommand, CommandError

from apps.community_care.geocoding import MapTilerClient, GeocodingError, geocode_hospital, query_details
from apps.community_care.models import Hospital, GeocodingRun


class Command(BaseCommand):
    help = "Generate unapproved MapTiler location candidates for imported hospitals. Never verifies or publishes hospitals."

    def add_arguments(self, parser):
        parser.add_argument("--district")
        parser.add_argument("--limit", type=int, default=12)
        parser.add_argument("--source-ids", nargs="+")
        parser.add_argument("--dry-run", action="store_true", help="No provider calls or database writes.")
        parser.add_argument("--report", help="New JSON file; existing files are not overwritten.")

    def handle(self, *args, **options):
        if not 1 <= options["limit"] <= 100:
            raise CommandError("Limit must be between 1 and 100.")
        hospitals = Hospital.objects.exclude(source_record_id="").order_by("source_record_id", "id")
        if options["district"]:
            hospitals = hospitals.filter(district__iexact=options["district"])
        if options["source_ids"]:
            hospitals = hospitals.filter(source_record_id__in=options["source_ids"])
        hospitals = list(hospitals[:options["limit"]])
        if not hospitals:
            raise CommandError("No imported hospitals match the supplied filters.")
        output = None
        report = {"dry_run": options["dry_run"], "provider": "MapTiler Cloud", "records": [], "http_requests": 0}
        client, references, stopped = None, {}, None
        try:
            if options["report"]:
                path = Path(options["report"])
                path.parent.mkdir(parents=True, exist_ok=True)
                output = path.open("x", encoding="utf-8")
            for hospital in hospitals:
                _, query, digest = query_details(hospital)
                row = {"hospital_id": hospital.pk, "source_record_id": hospital.source_record_id, "query": query}
                report["records"].append(row)
                if options["dry_run"]:
                    row["outcome"] = "would_geocode"
                    continue
                if stopped:
                    row.update(outcome="not_requested", error_code=stopped)
                    continue
                run = GeocodingRun.objects.filter(hospital=hospital, query_hash=digest, status__in=["SUCCESS", "NO_RESULTS"]).first()
                outcome = "skipped"
                if not run:
                    try:
                        if client is None:
                            client = MapTilerClient()
                        run, outcome = geocode_hospital(hospital, client, references)
                    except GeocodingError as error:
                        row.update(outcome="failed", error_code=error.code)
                        stopped = error.code
                        continue
                row.update(outcome=outcome, run_id=run.pk, status=run.status, error_code=run.error_code,
                    candidates=[{"id": candidate.pk, "latitude": str(candidate.latitude), "longitude": str(candidate.longitude),
                                 "confidence": candidate.confidence, "provider_reference": candidate.provider_reference,
                                 "label": candidate.label, "flags": candidate.flags, "attribution": candidate.attribution,
                                 "status": candidate.status, "created_at": candidate.created_at.isoformat()}
                                for candidate in run.candidates.all()])
                if run.error_code in {"MISSING_KEY", "RATE_LIMITED", "AUTH_RESTRICTED", "REQUEST_BUDGET"}:
                    stopped = run.error_code
            report["http_requests"] = client.requests if client else 0
            report["summary"] = {"hospitals": len(hospitals), "candidate_count": sum(len(row.get("candidates", [])) for row in report["records"]),
                                 "failed": sum(row["outcome"] == "failed" or row.get("status") == "FAILED" for row in report["records"]),
                                 "not_requested": sum(row["outcome"] == "not_requested" for row in report["records"]),
                                 "skipped": sum(row["outcome"] == "skipped" for row in report["records"])}
            if output:
                json.dump(report, output, indent=2, ensure_ascii=False)
                output.write("\n")
        except OSError:
            raise CommandError("Report storage failed; completed per-hospital geocoding results may already be persisted. Reruns reuse successful results.") from None
        finally:
            if output:
                output.close()
        self.stdout.write(json.dumps({**report["summary"], "http_requests": report["http_requests"], "dry_run": report["dry_run"]}))
        if report["summary"]["failed"]:
            raise CommandError("Some provider requests failed; inspect sanitized error codes in the report. No hospital coordinates were approved.")
