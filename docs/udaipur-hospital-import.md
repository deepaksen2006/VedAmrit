# Task 4.2A — Udaipur hospital dataset integration

The input is the actual user-supplied
`C:\Users\Care\Downloads\VedAmrit_Udaipur_12_Hospital_Import.csv`, copied byte for
byte to [`data/imports/udaipur_hospitals.csv`](../data/imports/udaipur_hospitals.csv).
SHA-256: `85417a77df8bb50fe7c28d129d86bdd721d07126ba9abb91940bc8251632c261`.
No records, phone numbers, GPS coordinates or source URLs were supplemented from
memory, search, maps or a third-party dataset. Source cells are treated as data,
not instructions or authorization to verify records.

## Mapping and safeguards

The existing Hospital model is reused; DoctorProfile, authentication, assessments,
AI algorithms, Kotlin navigation and Task 3.3 UI are unchanged.

| Supplied information | Storage/interpretation |
| --- | --- |
| `source_record_id` | Immutable namespaced identity, e.g. `udaipur_hospitals:UDR-001`; unique database constraint. Keep the namespace unchanged on repeat imports. |
| Name, address, city/state, postal code, phone | Existing fields; validate printable identity/address, lengths, six-digit Indian PIN syntax, phone syntax and model constraints. Missing phones remain blank on drafts; submission/verification requires a valid confirmed phone. |
| `district`, `facility_type` | Typed fields distinguish government teaching hospital, government hospital, government dispensary, private hospital, private clinic and private treatment centre. |
| Latitude/longitude | Existing decimal fields; paired/range/finite validation. All twelve supplied coordinate pairs are blank and remain SQL NULL. No geocoding runs. |
| Listed hours/services | Separate `listed_hours_unverified` and `services_unverified` text. They do not populate verified facilities, doctor availability or appointment slots. “24 hours” does not establish OPD/doctor availability. |
| Source type/notes/original status/review flags | Immutable `import_provenance`, including the entire original row, file/row hashes, file name, timestamp and duplicate candidates. The college code in UDR-001 stays a note, not professional registration. |
| Source URL | `source_url` stays blank on import. Any supplied unverified URL is retained only in the original private snapshot. All twelve actual source URLs are blank. |
| CSV PENDING / review-required values | Persist as **DRAFT + NEEDS_REVIEW**; the original values remain in provenance. Importing is not Task 4.1 submission or approval. |
| Booking | `booking_enabled=False`, read-only through APIs/Admin and protected by a database constraint. No doctors, memberships, affiliations, consultations or slots are created. |

One new migration,
`community_care.0002_hospital_booking_enabled_hospital_district_and_more`, adds the
import fields, source-key/review/booking constraints and a district index. The phone
column permits blank draft data, while the existing management-create API still
requires a phone. Existing hospitals default to NOT_IMPORTED without changing
their verification. Both forward doctor/consultation and hospital-preservation
migration tests run in an isolated test database.

## Command

```powershell
Set-Location D:\VedAmrit\backend
$env:DJANGO_DATABASE_ENGINE='postgresql'
.\.venv\Scripts\python.exe manage.py import_hospitals --file ..\data\imports\udaipur_hospitals.csv --district Udaipur --dry-run --report ..\docs\import-reports\udaipur-hospitals-dry-run.json
.\.venv\Scripts\python.exe manage.py import_hospitals --file ..\data\imports\udaipur_hospitals.csv --district Udaipur --report ..\docs\import-reports\udaipur-hospitals-import.json
.\.venv\Scripts\python.exe manage.py import_hospitals --file ..\data\imports\udaipur_hospitals.csv --district Udaipur --report ..\docs\import-reports\udaipur-hospitals-repeat.json
```

These report names are reserved for this task's evidence. For a later run, choose
a new report filename: the command refuses to overwrite an existing report or
input file. Supported options are `--file`, `--dry-run`, `--district`, `--report`,
optional `--source` (default `udaipur_hospitals`) and optional `--actor <user_id>`.

This is an offline management command requiring trusted server/DB access, not a
new import HTTP endpoint. If `--actor` is supplied, that existing user must be an
active authorized VedAmrit directory admin. Without it, audit entries explicitly
record a **server operation with actor=NULL**, rather than attributing the import
to an unchosen human account. It never grants permissions or verifies a record.

The importer validates every selected row before writing. A malformed selected
row rejects the whole batch. A district filter is exact/case-insensitive; unmatched
rows are reported, and no matching rows is an error. CSV headers, row structure,
encoding, duplicate source IDs and unsafe approval/booking claims are validated.

Dry-run performs no database saves, audit writes or sequence allocation. A real
import uses one transaction; PostgreSQL also takes a source-scoped advisory
transaction lock. Unique constraints defend against other writers. Database or
report-write failure rolls back hospital and audit writes together. Failed reports
show zero committed creations and rolled-back outcomes. A successful command
returns only after its transaction exits successfully.

Existing source IDs are skipped without changing any field, revision, audit or
verification status. Changed upstream snapshots are flagged for human comparison,
not applied. Exact normalized name/address/city duplicates under a different
source ID are skipped without attaching provenance to or modifying an existing
record, including verified hospitals.

## Duplicate and review workflow

The importer does **not** merge the Old City government listings. UDR-002,
UDR-004 and UDR-005 have distinct supplied identities/addresses and are retained
as separate drafts with cross-references for possible duplicate/related-facility
review. Similar names or a shared phone can also create conservative candidates;
these are review hints, not proof of identity.

Reports separate exact `duplicate` rows (skipped) from `possible_duplicate` rows
(retained for review). `review_needed` and possible-duplicate counts overlap
created/skipped counts; they are not additional facilities. Original warnings
about postal codes, address numbering, college code and accreditation remain
available to the reviewer.

1. Use an authorized VedAmrit operator at `/admin/community_care/hospital/`.
   Filter **Import review status = NEEDS_REVIEW**, district Udaipur, and inspect
   `source_record_id`, the read-only provenance snapshot and duplicate candidates.
2. Independently confirm facility identity/type, address, postal code and contact.
   Resolve UDR-002/004/005 before publishing. Leave unconfirmed GPS/URLs blank;
   coordinates may stay NULL for directory-only use. Do not copy a map guess into
   confirmed coordinates. Confirm operating information separately from availability.
3. Correct editable fields. Record actual checks and duplicate resolution in
   `import_review_notes`, then set `import_review_status=REVIEWED`. The model requires
   meaningful review notes; hospital admins cannot clear this gate. Source identity
   and the original snapshot remain read-only.
4. Through the existing Task 4.1 action/API, submit with the current revision and
   reason. Missing phone or unresolved import review blocks submission/verification.
5. An independent VedAmrit verifier uses the existing verify action/endpoint with
   the reviewed revision and reason. Hospital admins still cannot verify themselves,
   their hospital or its doctors. Critical verified edits trigger re-verification.
6. If a draft is a duplicate or cannot be confirmed, retain its provenance and
   deactivate it, or submit/reject it after resolving required submission fields.
   Never verify it solely to make it appear in the public list.

Management APIs expose the new review fields to appropriately scoped callers.
Public APIs still require active VERIFIED status and now additionally exclude
NEEDS_REVIEW imports. Public responses add district, facility type and disabled
booking; raw provenance and unverified hours/services are not published. No
existing approval bypass was added.

## Execution evidence and tests

The actual PostgreSQL command and repeat import both exited successfully.
Reports live in [`docs/import-reports`](import-reports):

| Run | Created | Would create | Skipped | Exact duplicates skipped | Possible duplicates | Invalid | Review needed |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| PostgreSQL dry-run | 0 | 12 | 0 | 0 | 3 | 0 | 12 |
| Actual PostgreSQL import | 12 | 0 | 0 | 0 | 3 | 0 | 12 |
| Actual repeat import | 0 | 0 | 12 | 0 | 3 | 0 | 12 |

Persisted types: 1 government teaching hospital, 3 government hospitals,
1 government dispensary, 4 private hospitals, 2 private clinics and 1 private
treatment centre. All 12 are DRAFT / NEEDS_REVIEW, booking disabled, coordinates
NULL and source URLs blank. Four phone numbers remain missing. All 12 revisions
remain 1, update timestamps precede the repeat run and only 12 IMPORT audits
exist. No records changed on repeat. Both public hospital and doctor list GETs
returned HTTP 200 with **count 0**.

The new migration is applied and the final migration plan has no pending
operations. PostgreSQL introspection confirms the source identity and disabled
booking/review constraints exist.

Before/after row counts **and serialized-data SHA-256 hashes** match for existing
users, doctor profiles, consultations, assessments, patient profiles, agent runs,
wellness plans, final reports, legacy diet plans, memberships and affiliations.
The only new business records are 12 hospitals and their 12 import audit entries.
The preservation results are in
[`udaipur-hospitals-verification.json`](import-reports/udaipur-hospitals-verification.json).

| PostgreSQL model | Before | After |
| --- | ---: | ---: |
| Users | 6 | 6 |
| Assessments | 3 | 3 |
| Patient profiles | 2 | 2 |
| Agent runs | 3 | 3 |
| Wellness plans | 3 | 3 |
| Doctors / consultations / final reports / legacy diet plans | 0 each | 0 each |
| Hospital memberships / affiliations | 0 each | 0 each |
| Hospitals | 0 | 12 |

| Check | Result |
| --- | --- |
| Importer tests | **22 passed**. Actual CSV parsing/mapping, no-write dry-run, repeat/source stability, duplicates, missing values, validation, transaction/report rollback and permissions. |
| Community Care + migration tests | **70 passed** (the prior 47 plus 22 importer tests and 1 hospital-upgrade compatibility test). |
| Combined Django regressions | **155 discovered: 152 passed, 0 failed/errors, 3 skipped**. Existing auth, assessment, doctor-review, generation and report tests included. |
| Agent-engine regressions | **54 passed**, 0 failed, plus 3 subtests; 1 dependency warning. |
| Django system check | Passed, 0 issues. |
| Migration consistency | Passed, no changes detected; new migration applied. |
| Source-file preservation | Canonical CSV is byte-identical to the supplied Downloads file. |
| Existing clinical data | All captured counts and hashes unchanged. |
| Task 3.3 / Android navigation | No changes; no Android UI integration/device claim for this task. |

The first targeted test run found an ambiguous duplicate summary: exact skipped
duplicates and retained possible duplicates were counted together. The report
now exposes them separately, and the complete final suite passes.

Tests use isolated SQLite because the current PostgreSQL role lacks CREATEDB;
actual PostgreSQL migration, dry-run, import, repeat import and read-only
verification are separately exercised. No existing database is reset or reused
as a test database.

The three skipped existing tests are opt-in real graph/RAG/PDF smoke tests. No
package installation or PostgreSQL concurrent-import stress test was performed.
Successful import/repeat execution exercised the actual PostgreSQL transaction
and advisory-lock path; failure rollback is covered with isolated test fixtures.

```powershell
# Separate test terminal; does not use the live PostgreSQL records.
Set-Location D:\VedAmrit\backend
$env:DJANGO_DATABASE_ENGINE='sqlite'
$env:SQLITE_DB_PATH=':memory:'
$env:HF_HUB_OFFLINE='1'
$env:TRANSFORMERS_OFFLINE='1'
.\.venv\Scripts\python.exe manage.py test apps.community_care.test_hospital_import apps.community_care.tests apps.community_care.test_migrations apps.authentication.tests apps.agentic_reports.tests apps.agentic_reports.test_assessment_sync apps.agentic_reports.test_draft_generation apps.agentic_reports.test_draft_pdf --noinput -v 1
.\.venv\Scripts\python.exe -m pytest agent_engine/tests -q --disable-warnings
```

Checks against the configured PostgreSQL database:

```powershell
Set-Location D:\VedAmrit\backend
$env:DJANGO_DATABASE_ENGINE='postgresql'
.\.venv\Scripts\python.exe manage.py check
.\.venv\Scripts\python.exe manage.py makemigrations --check --dry-run
.\.venv\Scripts\python.exe manage.py showmigrations community_care
.\.venv\Scripts\python.exe manage.py migrate --plan
```

## Task-specific changed/new files

Task 4.1 was already uncommitted in this workspace. The following is the Task
4.2A delta, not a claim that its entire previous working tree was recreated:

- `data/imports/udaipur_hospitals.csv`
- `backend/apps/community_care/models.py`
- `backend/apps/community_care/services.py`
- `backend/apps/community_care/serializers.py`
- `backend/apps/community_care/views.py`
- `backend/apps/community_care/admin.py`
- `backend/apps/community_care/hospital_import.py`
- `backend/apps/community_care/management/__init__.py`
- `backend/apps/community_care/management/commands/__init__.py`
- `backend/apps/community_care/management/commands/import_hospitals.py`
- `backend/apps/community_care/migrations/0002_hospital_booking_enabled_hospital_district_and_more.py`
- `backend/apps/community_care/test_hospital_import.py`
- `backend/apps/community_care/test_migrations.py`
- `docs/udaipur-hospital-import.md`
- `docs/import-reports/udaipur-hospitals-dry-run.json`
- `docs/import-reports/udaipur-hospitals-import.json`
- `docs/import-reports/udaipur-hospitals-repeat.json`
- `docs/import-reports/udaipur-hospitals-verification.json`

## MapTiler readiness

The backend supports reviewed directory records and district/facility-type filters.
This supplied dataset is **not ready for public map markers**: all locations and
sources require independent review, all supplied coordinates are missing, and
possible Old City duplicates are unresolved. There are no doctors or bookable
slots in this dataset. After review, confirmed coordinates can be added through
the existing scoped management workflow and checked before a separate map task.

No MapTiler integration, geocoding, doctor/appointment import, Kotlin navigation
change, commit or push is part of Task 4.2A.
