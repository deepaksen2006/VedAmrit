"""Offline, create-only import. Source cells are evidence/data, never authority."""
import csv
import hashlib
import io
import json
import re
from decimal import Decimal, InvalidOperation
from difflib import SequenceMatcher
from pathlib import Path

from django.core.exceptions import ValidationError
from django.db import connection, transaction
from django.utils import timezone

from .models import Hospital, HospitalDuplicateReview
from .permissions import require_directory_admin


HEADERS = ("source_record_id", "name", "facility_type", "address", "city", "district", "state", "postal_code", "phone",
           "listed_hours_unverified", "services_unverified", "latitude", "longitude", "source_type", "source_url", "source_notes",
           "verification_status", "booking_enabled", "import_review_status")
DEFAULT_NAMESPACE = "udaipur_hospitals"


class ImportFailure(Exception):
    def __init__(self, report):
        self.report = report
        super().__init__(report.get("error", "Import rejected; no records committed."))


def normalized(value):
    return " ".join(re.findall(r"\w+", value.casefold(), flags=re.UNICODE))


def identity(row):
    return tuple(normalized(row.get(key, "")) for key in ("name", "address", "city"))


def possible_duplicate(a, b):
    if normalized(a["city"]) != normalized(b["city"]):
        return False
    phone_a, phone_b = re.sub(r"\D", "", a.get("phone", "")), re.sub(r"\D", "", b.get("phone", ""))
    shared_phone = bool(phone_a and phone_a == phone_b)
    old_city = (a.get("facility_type", "").startswith("government_") and b.get("facility_type", "").startswith("government_")
                and "old city" in normalized(a["address"]) and "old city" in normalized(b["address"])
                and a.get("postal_code") == b.get("postal_code"))
    similar_name = SequenceMatcher(None, normalized(a["name"]), normalized(b["name"])).ratio() >= .88
    return shared_phone or old_city or similar_name


def parse_coordinate(value):
    if not value.strip():
        return None
    try:
        result = Decimal(value.strip())
        if not result.is_finite():
            raise InvalidOperation()
        return result
    except InvalidOperation as error:
        raise ValidationError("Coordinates must be finite decimal numbers or blank.") from error


def row_to_hospital(row, namespace, file_hash, filename, actor):
    for field in ("name", "address", "city", "district", "state"):
        value = row[field].strip()
        if len(value) < 3 or any(ord(c) < 32 for c in value):
            raise ValidationError({field: "Provide at least three printable characters."})
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_-]{1,79}", row["source_record_id"].strip()):
        raise ValidationError({"source_record_id": "Use a stable 2–80 character source identifier."})
    if row["facility_type"].strip() not in Hospital.FacilityType.values or row["facility_type"].strip() == "unspecified":
        raise ValidationError({"facility_type": "An explicit supported facility type is required."})
    if not re.fullmatch(r"[1-9][0-9]{5}", row["postal_code"].strip()):
        raise ValidationError({"postal_code": "This Indian dataset requires a six-digit postal code; validate locality separately."})
    if row["verification_status"].strip().upper() not in {"", "DRAFT", "PENDING"}:
        raise ValidationError({"verification_status": "An import cannot claim verified, rejected or suspended authority."})
    if row["booking_enabled"].strip().casefold() not in {"", "false", "0", "no"}:
        raise ValidationError({"booking_enabled": "Booking must remain disabled."})
    # Keep source claims separate from confirmed facilities/availability.
    fields = {key: row[key].strip() for key in ("name", "address", "city", "district", "state", "postal_code", "phone",
                                              "facility_type", "listed_hours_unverified", "services_unverified")}
    row_hash = hashlib.sha256(json.dumps(row, ensure_ascii=False, sort_keys=True).encode("utf-8")).hexdigest()
    record = Hospital(**fields, latitude=parse_coordinate(row["latitude"]), longitude=parse_coordinate(row["longitude"]),
        source_namespace=namespace, source_record_id=row["source_record_id"].strip(), source_url="", facilities=[],
        verification_status="DRAFT", booking_enabled=False, import_review_status="NEEDS_REVIEW", created_by=actor,
        import_provenance={"original_row": row, "file_name": filename, "file_sha256": file_hash, "row_sha256": row_hash,
                           "imported_at": timezone.now().isoformat(), "operator": "django_user" if actor else "server_operator",
                           "source_type": row["source_type"], "review_flags": []})
    # Expression uniqueness is resolved below as a duplicate outcome, not a malformed row.
    record.full_clean(validate_unique=False, validate_constraints=False)
    return record


def load_dataset(path):
    raw = Path(path).read_bytes()
    if len(raw) > 5_000_000:
        raise ValueError("CSV exceeds the 5 MB import limit.")
    reader = csv.DictReader(io.StringIO(raw.decode("utf-8-sig"), newline=""), strict=True)
    if reader.fieldnames is None or len(reader.fieldnames) != len(set(reader.fieldnames)) or set(reader.fieldnames) != set(HEADERS):
        raise ValueError("CSV must have the documented, unique column headers.")
    rows = []
    for line, row in enumerate(reader, start=2):
        if None in row or any(value is None for value in row.values()):
            raise ValueError(f"Row {line} has extra or missing columns.")
        rows.append((line, row))
        if len(rows) > 10000:
            raise ValueError("CSV exceeds the 10000-row import limit.")
    if not rows:
        raise ValueError("CSV contains no facility records.")
    return rows, hashlib.sha256(raw).hexdigest()


def summarize(report):
    rows = report["records"]
    report["summary"] = {
        "total": len(rows), "created": sum(r["outcome"] == "created" for r in rows),
        "would_create": sum(r["outcome"] == "would_create" for r in rows),
        "skipped": sum(r["outcome"] in {"skipped", "duplicate", "district_skipped"} for r in rows),
        "duplicate": sum(r["outcome"] == "duplicate" for r in rows),
        "possible_duplicate": sum(bool(r.get("duplicate_candidates")) and r["outcome"] != "duplicate" for r in rows),
        "invalid": sum(r["outcome"] == "invalid" for r in rows),
        "review_needed": sum(bool(r.get("review_flags")) for r in rows),
    }
    return report


def import_hospitals(path, *, dry_run=False, district=None, namespace=DEFAULT_NAMESPACE, actor=None, write_report=None):
    """Trusted server-operator entry point, not an HTTP endpoint.

    An optional named actor must be an authorized VedAmrit admin. Without one,
    CLI execution is honestly audited as a server operation, not impersonation.
    """
    if actor is not None:
        require_directory_admin(actor)
    if not re.fullmatch(r"[a-z0-9][a-z0-9_-]{1,79}", namespace):
        raise ValueError("Use a stable 2–80 character lowercase source namespace.")
    report = {"status": "dry_run" if dry_run else "imported", "dry_run": dry_run,
              "source_namespace": namespace, "file_name": Path(path).name, "district": district,
              "records": [], "started_at": timezone.now().isoformat()}
    try:
        rows, file_hash = load_dataset(path)
        report["file_sha256"] = file_hash
        with transaction.atomic():
            if connection.vendor == "postgresql" and not dry_run:
                # Serialize imports for a source; uniqueness also protects other writers.
                key = int.from_bytes(hashlib.sha256(namespace.encode()).digest()[:8], "big", signed=True)
                with connection.cursor() as cursor:
                    cursor.execute("SELECT pg_advisory_xact_lock(%s)", [key])
            selected = [(line, row) for line, row in rows if not district or row["district"].strip().casefold() == district.strip().casefold()]
            selected_rows = [row for _, row in selected]
            existing = list(Hospital.objects.all().values("pk", "name", "address", "city", "phone", "postal_code", "facility_type"))
            planned_identities = {}
            seen_source_ids = set()
            planned = []
            for line, row in rows:
                result = {"row": line, "source_record_id": row["source_record_id"], "name": row["name"], "outcome": "planned"}
                report["records"].append(result)
                if district and row["district"].strip().casefold() != district.strip().casefold():
                    result["outcome"] = "district_skipped"
                    continue
                try:
                    source_id = row["source_record_id"].strip()
                    if source_id in seen_source_ids:
                        raise ValidationError({"source_record_id": "Repeated source ID within one input file; resolve before importing."})
                    seen_source_ids.add(source_id)
                    record = row_to_hospital(row, namespace, file_hash, Path(path).name, actor)
                    flags = ["User-supplied source requires independent verification."]
                    if record.latitude is None:
                        flags.append("Coordinates missing; no map location confirmed.")
                    if not record.phone:
                        flags.append("Phone missing; confirm before submission/verification.")
                    if row["listed_hours_unverified"].strip():
                        flags.append("Listed hours are unverified and are not doctor availability.")
                    if row["source_notes"].strip():
                        flags.append("Review original source notes, including identity/address/postal cautions.")
                    candidates = [other["source_record_id"] for other in selected_rows if other is not row and possible_duplicate(row, other)]
                    candidates += [f"hospital:{other['pk']}" for other in existing if possible_duplicate(row, other)]
                    if candidates:
                        flags.append("Possible duplicate/related facility; resolve manually, do not auto-merge.")
                    result.update(review_flags=flags, duplicate_candidates=sorted(set(candidates)))
                    record.import_provenance["review_flags"] = flags
                    record.import_provenance["duplicate_candidates"] = result["duplicate_candidates"]
                    prior = Hospital.objects.filter(source_namespace=namespace, source_record_id=source_id).first()
                    if prior:
                        result.update(outcome="skipped", hospital_id=prior.pk, reason="Stable source ID already exists; no fields or approval changed.",
                                      source_snapshot_changed=prior.import_provenance.get("row_sha256") != record.import_provenance["row_sha256"])
                        # Do not flag the already-imported row itself as a possible duplicate on repeat.
                        result["duplicate_candidates"] = [c for c in result["duplicate_candidates"] if c != f"hospital:{prior.pk}"]
                        continue
                    matches = [f"hospital:{other['pk']}" for other in existing if identity(row) == identity(other)]
                    if identity(row) in planned_identities:
                        matches.append(planned_identities[identity(row)])
                    if matches:
                        result.update(outcome="duplicate", duplicate_candidates=matches, reason="Exact normalized identity exists; skipped without modifying/linking existing records.")
                        continue
                    planned_identities[identity(row)] = source_id
                    record.full_clean()
                    planned.append((record, result))
                except ValidationError as error:
                    result.update(outcome="invalid", errors=getattr(error, "message_dict", error.messages))
            if not selected:
                raise ValueError("No rows match the requested district; no records imported.")
            if any(result["outcome"] == "invalid" for result in report["records"]):
                raise ValueError("One or more invalid rows; the entire import was rejected.")
            for record, result in planned:
                if dry_run:
                    result["outcome"] = "would_create"
                else:
                    record._care_actor = actor
                    record._care_action = "IMPORT"
                    record._care_reason = f"Unverified CSV import {namespace}:{record.source_record_id}; source {file_hash}."
                    record.save()
                    if record.import_provenance.get("duplicate_candidates"):
                        HospitalDuplicateReview.objects.get_or_create(hospital=record)
                    result.update(outcome="created", hospital_id=record.pk)
            summarize(report)
            if write_report:
                # A report/storage failure also rolls back all hospital/audit writes.
                write_report(report)
        return report
    except Exception as error:
        report.update(status="failed", error=str(error))
        for result in report["records"]:
            if result["outcome"] in {"created", "planned", "would_create"}:
                result["outcome"] = "rolled_back"
                result.pop("hospital_id", None)
        summarize(report)
        if write_report:
            try:
                write_report(report)
            except OSError:
                pass
        raise ImportFailure(report) from error
