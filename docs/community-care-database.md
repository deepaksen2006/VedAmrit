# Task 4.1 — Hospital and Doctor Database

Implemented and verified on `agentic-ai-integration`. No commit/push, MapTiler
integration or hospital dataset import was performed. Existing Task 3.3 work is
preserved in commit `e6adbda`; its UI files are not part of this task's diff.

## Repository audit and design

- Django 5.1.4, DRF 3.15.2, SimpleJWT, PostgreSQL and the existing `auth.User`
  model are reused. `AUTH_USER_MODEL` was not replaced.
- `apps.doctors.DoctorProfile` is already referenced by consultations, diet plans
  and agentic doctor reviews. It was extended rather than duplicated.
- Directory doctors may have no login account. The nullable one-to-one user link
  preserves existing IDs and relationships. Explicit VedAmrit-only account linking
  avoids creating dummy users or a second doctor identity.
- Existing Android `ApiClient` provides authenticated OkHttp/JWT handling.
  `CommunityCareScreen` currently uses demo places and a MapTiler/WebView search;
  the old doctor Retrofit contract is commented out. This task adds a tested public
  directory client/DTOs without replacing that screen or navigation.
- Django's default API permission remains authenticated. Only the new public care
  directory GETs allow anonymous access. Management endpoints use JWT and scoped
  querysets/services.

## Models

| Model | Storage and rules |
| --- | --- |
| `community_care.Hospital` | Name/description, address/city/state/postal code, optional paired decimal coordinates, phone/email, facilities, verification status, active flag, creator/timestamps and revision. Coordinates have range checks. Case-insensitive name/address/city uniqueness prevents duplicate identities. |
| Existing `doctors.DoctorProfile` | Keeps original fields and `is_verified`; adds council, qualifications, internal metadata, five-state verification, active flag, creator/update time/revision. Registration/email have case-insensitive uniqueness. The existing registration column is exposed as `registration_number` in the new API. |
| `HospitalMembership` | Existing user + hospital, ADMIN or read-only STAFF, active flag/timestamps. Unique hospital/user pair. Assignment never changes `is_staff`, groups or global permissions. |
| `DoctorHospitalAffiliation` | Existing doctor + hospital, IN_PERSON/ONLINE/BOTH, nonnegative decimal fees, active flag/timestamps. Unique doctor/hospital pair. |
| `VerificationAudit` | Actor, record type/id, action, old/new status, reason, revision/change-field metadata and timestamp. Read-only API/Admin; model instance updates/deletes are rejected. No document contents or token values are recorded. |

Public-status, city, membership-scope, affiliation and audit indexes are included.
Historical foreign keys keep the original doctor ID. User deletion now leaves the
doctor record intact via SET_NULL; it does not cascade-delete its clinical history.

### Permissions and transitions

- VedAmrit directory admins: active superusers, or users explicitly granted
  `community_care.manage_directory` or `community_care.verify_directory`.
- Verification decisions additionally require `verify_directory` (or superuser).
  `is_staff` alone does not grant directory-management or verification rights.
- Hospital ADMIN memberships permit editing only assigned hospitals and their
  actively affiliated doctors. STAFF memberships provide scoped read-only access.
- An existing hospital admin may propose a **new DRAFT hospital**, but creation
  never grants membership. A VedAmrit admin must assign that new hospital before
  the proposer can edit/submit it. No one can self-assign an arbitrary existing
  hospital through the membership API.
- A hospital admin's doctor creation requires an assigned hospital and creates its
  affiliation transactionally. Adding an unrelated existing doctor to a hospital
  requires a VedAmrit admin; hospital admins cannot claim another doctor's record.
- Existing user accounts can be attached at doctor creation by a VedAmrit admin,
  or later through `link-user`. An already-linked doctor cannot be reassigned and
  a user cannot receive a second doctor profile. Hospital admins cannot link users.

Transitions:

```text
DRAFT -> PENDING -> VERIFIED
              \-> REJECTED -> PENDING
VERIFIED -> SUSPENDED -> PENDING (reopen: VedAmrit verifier only)
VERIFIED --critical edit--> PENDING
```

Decisions/submission require a reason and the exact `expected_revision`; stale
decisions return 409. A verifier cannot approve their assigned hospital, a doctor
at their administered hospital, or their own doctor profile. Submission/verification
requires doctor qualifications and an issuing council. Identifier syntax checks
do **not** establish professional credential validity; human external checks are
still required.

Critical public hospital fields and doctor identity/credentials/profile changes
remove VERIFIED visibility until re-approved. This also covers the existing
doctor self-update endpoint. Doctor `is_verified` and the new status are kept in
sync, including a database check constraint, so credential changes/suspension
also revoke legacy verified-doctor authorization. Fee/availability changes alone
do not change credentials. Editing a suspended record does not clear suspension.

Transactions and row locks protect mutations; database constraints back up input
validation. Use the services for management writes. Raw SQL and ORM bulk updates
bypass model hooks and must not be used to edit identities/status. The audit is an
application-level append-only history, not a database-superuser tamper-proof ledger.

## API contract

Prefix: `/api/care/`. Paths have trailing slashes. JSON IDs are integers, decimal
coordinates/fees are strings, timestamps are ISO 8601, and missing coordinates are
null. Unknown/read-only write fields return 400 instead of being silently accepted.

| Endpoint | Access/behavior |
| --- | --- |
| GET `hospitals/`, `hospitals/{id}/` | Anonymous; verified active hospitals only. |
| GET `doctors/`, `doctors/{id}/` | Anonymous; verified active doctors with at least one active affiliation to a verified active hospital. Only those public affiliations appear in JSON. Private user/contact/certificate/internal metadata fields are omitted. |
| POST `hospitals/`, `doctors/` | JWT; create DRAFT records according to the scope rules above. |
| PATCH `hospitals/{id}/`, `doctors/{id}/` | JWT; assigned hospital ADMIN or VedAmrit admin. No verification/creator/account privilege fields. |
| POST `hospitals/{id}/submit/`, `doctors/{id}/submit/` | JWT; scoped submit with reason + expected revision. |
| GET `manage/hospitals/`, `manage/doctors/` and `{id}/` | JWT; private management records including nonpublic statuses, scoped to memberships or VedAmrit admin. POST/PATCH/submit are also supported under these prefixes. |
| GET/POST `manage/affiliations/`, PATCH `manage/affiliations/{id}/` | JWT; scoped management. Doctor/hospital IDs are immutable after creation; deactivate instead of deleting. |
| POST `admin/hospitals/{id}/verify/`, `reject/`, `suspend/`, `reopen/` | JWT + independent authorized VedAmrit verifier; reason + expected revision. |
| POST `admin/doctors/{id}/verify/`, `reject/`, `suspend/`, `reopen/` | Same verification protections for doctors. |
| POST `admin/doctors/{id}/link-user/` | VedAmrit admin; existing active `user_id`, reason + expected revision. Linking a verified record requires re-verification. |
| GET/POST `admin/memberships/`, PATCH `admin/memberships/{id}/` | VedAmrit admin only. Hospital/user identity is fixed after creation; role/active status may change. |
| GET `admin/audits/` | VedAmrit admin only; optional `record_type`/`record_id` filters. No create/update/delete API. |

Lists use `{count, next, previous, results}` with 20 rows by default and a maximum
`page_size` of 100. `page`, case-insensitive `city`/`state`, and `search` are
supported. Search matches hospital name/city or doctor name/specialization.
Doctors additionally accept `specialization` and `hospital_id`. Management lists
also accept `status`. Combined doctor location filters match one visible/assigned
affiliation; a hidden affiliation cannot make a public search match.

403 means insufficient role, 404 includes records outside the caller's object
scope/public directory, 400 indicates invalid/duplicate data, 401 indicates
missing/invalid/expired JWT, and 409 indicates stale revision/invalid transition.
No DELETE endpoints are exposed. Public GETs stay public-filtered even for admins;
use `manage/` to inspect drafts.

### Illustrative JSON (documentation only; no production seed records created)

Hospital POST:

```json
{
  "name": "Documentation-only hospital",
  "description": "Replace with independently checked real details",
  "address": "1 Example Street",
  "city": "Pune",
  "state": "Maharashtra",
  "postal_code": "411001",
  "latitude": "18.520400",
  "longitude": "73.856700",
  "phone": "+919000000000",
  "email": "example@example.test",
  "facilities": ["Outpatient consultation"]
}
```

Doctor POST (reuse an existing doctor instead of duplicating its registration):

```json
{
  "full_name": "Documentation-only doctor",
  "phone": "+919000000001",
  "email": "doctor@example.test",
  "registration_number": "EXAMPLE-001",
  "registration_council": "Issuing council to be independently checked",
  "qualifications": ["BAMS"],
  "specialization": "Ayurveda",
  "experience_years": 5,
  "clinic_name": "Documentation-only clinic",
  "consultation_fee": "500.00",
  "hospital_id": 101,
  "consultation_mode": "BOTH",
  "fees": "500.00"
}
```

Submit/decision body (replace revision with the latest management response):

```json
{"expected_revision": 2, "reason": "Record the actual evidence checked and decision rationale"}
```

Membership body: `{"hospital":101,"user":27,"role":"ADMIN","is_active":true}`.
Account-link body: `{"user_id":27,"expected_revision":3,"reason":"Account ownership independently checked"}`.

Illustrative public response:

```json
{
  "count": 1,
  "next": null,
  "previous": null,
  "results": [{
    "id": 201,
    "full_name": "Documentation-only doctor",
    "qualifications": ["BAMS"],
    "specialization": "Ayurveda",
    "registration_number": "EXAMPLE-001",
    "registration_council": "Example issuing council",
    "experience_years": 5,
    "bio": "",
    "verification_status": "VERIFIED",
    "updated_at": "2026-10-26T12:00:00+05:30",
    "affiliations": [{"id":301,"hospital_id":101,"hospital_name":"Documentation-only hospital","city":"Pune","consultation_mode":"BOTH","fees":"500.00"}]
  }]
}
```

## PostgreSQL migration and data verification

Both migrations were successfully applied, then independently checked through
`showmigrations`, the migration recorder, and `migrate --plan`. On resume, no
migrations were rerun. The final plan has **no pending operations**.

1. `doctors.0002_doctorprofile_created_by_doctorprofile_is_active_and_more` adds
   directory fields/indexes/constraints and makes the existing user link optional.
   It preserves legacy approved flags as VERIFIED and leaves unverified records
   DRAFT. Case-insensitive duplicates cause an explicit migration failure rather
   than a merge/delete. No historical verification decisions are fabricated.
2. `community_care.0001_initial` creates Hospital, Membership, Affiliation and
   VerificationAudit with their constraints/indexes. It depends on the doctor
   extension and existing user model. No auth-user schema migration was needed.

| Existing table | Before migration | After migration and resume |
| --- | ---: | ---: |
| `auth.User` | 6 | 6 |
| `AssessmentResult` | 3 | 3 |
| `DoctorProfile` | 0 | 0 |
| `Consultation` | 0 | 0 |
| `WellnessPlan` | 3 | 3 |

New care tables contain zero records. No patient/assessment/wellness-plan data
migration or seed/import command ran. The check establishes unchanged row counts
and the reviewed migration write scope; a pre-migration field-by-field data hash
was not captured. The isolated migration test separately verifies preservation of
an existing doctor ID/user link/approval/date and consultation foreign key.
Both public directory GETs also returned HTTP 200 and count 0 against PostgreSQL.

Do not reverse these migrations blindly: unlinked directory doctors cannot fit
the old required-user schema. Plan any rollback with an operator and data backup.

## Django Admin and initial operators

Hospital, the existing DoctorProfile, memberships, affiliations and audit history
are registered. Search/list filters are available. Verification/creator/revision
fields are read-only. There is no delete action. Decision actions require exactly
one record, its reviewed revision, a reason and the verifier permission. Audit
history has no add/change/delete UI. A hospital membership alone grants no global
Django Admin access, even if someone separately sets the user's staff flag.

Create the initial operator interactively (no embedded password):

```powershell
Set-Location D:\VedAmrit\backend
$env:DJANGO_DATABASE_ENGINE='postgresql'
.\.venv\Scripts\python.exe manage.py createsuperuser
```

Then log in at `/admin/`. For less-privileged VedAmrit operators, create a group
and explicitly grant the Hospital content type's **Manage the VedAmrit care
directory** permission and, only for verifiers, **Verify the VedAmrit care
directory**. Add `is_staff` only if that VedAmrit operator needs the Django Admin
UI. Hospital admins need ordinary existing user accounts plus ADMIN memberships,
not staff/superuser flags or global groups. Use a separate independent verifier
for records linked to an operator's own hospital/doctor identity.

JWTs use the existing `/api/auth/login/` endpoint and refresh contract; management
requests send `Authorization: Bearer <access token>`. No new credential system or
hardcoded token was added. No real operator account/permission was created here.

## Windows commands and verification results

Start the existing local backend using its existing `.env` PostgreSQL credentials:

```powershell
Set-Location D:\VedAmrit\backend
$env:DJANGO_DATABASE_ENGINE='postgresql'
.\.venv\Scripts\python.exe manage.py check
.\.venv\Scripts\python.exe manage.py makemigrations --check --dry-run
.\.venv\Scripts\python.exe manage.py showmigrations doctors community_care
.\.venv\Scripts\python.exe manage.py migrate --plan
.\.venv\Scripts\python.exe manage.py runserver 127.0.0.1:8000 --noreload
```

The current database is already migrated. A fresh deployment must review its
migration plan and backup policy before applying `manage.py migrate --noinput`.
Do not run reset/flush/import commands for this task.

Use a **separate test terminal** for isolated SQLite tests; these commands never
use the existing PostgreSQL records or the checked-in SQLite file:

```powershell
Set-Location D:\VedAmrit\backend
$env:DJANGO_DATABASE_ENGINE='sqlite'
$env:SQLITE_DB_PATH=':memory:'
$env:HF_HUB_OFFLINE='1'
$env:TRANSFORMERS_OFFLINE='1'
.\.venv\Scripts\python.exe manage.py test apps.community_care.tests apps.community_care.test_migrations apps.authentication.tests apps.agentic_reports.tests apps.agentic_reports.test_assessment_sync apps.agentic_reports.test_draft_generation apps.agentic_reports.test_draft_pdf --noinput -v 1
.\.venv\Scripts\python.exe -m pytest agent_engine/tests -q --disable-warnings
```

Explicit test-module labels avoid the repository's existing namespace-package
discovery limitation with `manage.py test apps.community_care`.

Android checks:

```powershell
Set-Location D:\VedAmrit
.\gradlew.bat --offline testDebugUnitTest assembleDebug '-PagenticApiBaseUrl=http://127.0.0.1:8000/'
.\gradlew.bat --offline testDebugUnitTest --tests com.example.vedaahar.network.CareDirectoryApiTest assembleDebug '-PagenticApiBaseUrl=http://127.0.0.1:8000/'
```

| Verification | Final result |
| --- | --- |
| Django system check | Passed, 0 issues. |
| `makemigrations --check --dry-run` | Passed, no changes detected. |
| PostgreSQL migration state | Both new migrations applied; no pending operations. |
| PostgreSQL public GET smoke | 2/2 HTTP 200, empty directory; no fabricated records inserted. |
| Community Care + migration tests | **47 passed**, 0 failed/skipped in the final combined suite: the original 39 plus account linking, Admin rendering/actions, migration preservation, legacy duplicate handling and affiliation-filter regressions. |
| Django combined regressions | **132 discovered: 129 passed, 0 failed/errors, 3 skipped**. Includes all 47 care/migration tests and 85 existing auth/assessment/generation/doctor-review/report tests (82 passed, 3 opt-in real graph smoke tests skipped). |
| Agent engine pytest regressions | **54 passed**, 0 failed, 3 additional subtests passed; 1 dependency warning. No algorithms changed. |
| Android JVM suite | **159 passed**, 0 failed/errors/skipped, including 3 CareDirectoryApi tests. |
| Focused `CareDirectoryApiTest` rerun | **3 passed**, 0 failures/errors/skipped. |
| Debug APK | Offline build passed. No Community Care UI/device flow was introduced or claimed. |

Review reproduced and corrected two relevant failures: a legacy case-insensitive
duplicate email edit returned 500 instead of 400, and combined affiliation
location filters could match different hospitals. Both have focused regressions.
The filter fix also prevents management searches from using an unassigned
hospital's affiliation to match a shared doctor.

## Kotlin integration and Task 4.2 limits

- `CareDirectoryClient.create()` uses the existing configured backend base URL
  with an anonymous OkHttp client; directory reads do not need JWT. No location
  permission, Maps key or patient-token persistence is required for these reads.
- `CareDirectoryApi` provides list/detail GETs for hospitals/doctors, pagination
  and filters. `CarePage`, `CareHospitalDto`, `CareDoctorDto` and
  `CareAffiliationDto` match the JSON contract; decimal strings retain precision.
- It is not yet wired to `CommunityCareScreen`. That existing screen/demo/map
  behavior is unchanged and must not be represented as this verified database's
  output. Task 4.2 can replace demo results with paged directory data and add
  appropriate loading/error/empty states without changing navigation.
- For future authenticated management clients reuse `ApiClient.createAuthenticatedApi`
  and existing token storage/refresh; do not embed tokens or blindly follow an
  arbitrary pagination URL. Public clients already accept explicit page numbers.
- The PostgreSQL role has **no CREATEDB permission**; there was no existing test
  database. Automated write/concurrency tests ran in disposable SQLite instead.
  PostgreSQL DDL/indexes/constraints and public reads were checked on the actual
  database, but row-lock races/concurrent uniqueness need a separately provisioned
  PostgreSQL test database. No existing database was repurposed/reset for tests.
- Three existing real graph/RAG/PDF smoke tests are opt-in and were skipped;
  mocked Django integration and the agent-engine regression suite ran. No live
  phone-to-PostgreSQL management acceptance or credential-registry lookup ran.
- The installed backend interpreter reports Python 3.13; the project's requirements
  document Python 3.12 as its supported runtime. Existing environment/dependencies
  were reused; no packages/interpreters were installed.
- Verification is manual, not council API validation. No hospital dataset, geocoder,
  radius/GPS search, map wiring, booking/payment system or hospital-admin Android UI
  was added. No production directory records were seeded. These remain separate
  tasks after Task 4.1 approval.

## Actual changed/new files

Existing files changed:

- `backend/apps/doctors/models.py`
- `backend/apps/doctors/serializers.py`
- `backend/apps/doctors/views.py`
- `backend/vedaahar_backend/settings.py`
- `backend/vedaahar_backend/urls.py`

New files:

- `backend/apps/doctors/migrations/0002_doctorprofile_created_by_doctorprofile_is_active_and_more.py`
- `backend/apps/community_care/__init__.py`
- `backend/apps/community_care/apps.py`
- `backend/apps/community_care/base.py`
- `backend/apps/community_care/models.py`
- `backend/apps/community_care/validators.py`
- `backend/apps/community_care/permissions.py`
- `backend/apps/community_care/services.py`
- `backend/apps/community_care/serializers.py`
- `backend/apps/community_care/views.py`
- `backend/apps/community_care/urls.py`
- `backend/apps/community_care/admin.py`
- `backend/apps/community_care/tests.py`
- `backend/apps/community_care/test_migrations.py`
- `backend/apps/community_care/migrations/__init__.py`
- `backend/apps/community_care/migrations/0001_initial.py`
- `app/src/main/java/com/example/vedaahar/network/CareDirectoryApi.kt`
- `app/src/main/java/com/example/vedaahar/network/dto/CareDirectoryDtos.kt`
- `app/src/test/java/com/example/vedaahar/network/CareDirectoryApiTest.kt`
- `docs/community-care-database.md`

The actual final inventory is 25 files (including this handoff), rather than the
earlier 22-file estimate. No Task 3.3 file, assessment formula, AI algorithm or
doctor wellness-plan approval endpoint was edited.

Suggested commit title: **Add verified hospital and doctor Community Care database**

Suggested description:

```text
Add scoped hospital administration, affiliations, verification transitions and
audited decisions under /api/care/. Extend the existing DoctorProfile identity
without replacing users or clinical relationships. Invalidate verified identity
and credential edits, including legacy doctor self-updates.

Publish only active verified directory records and valid affiliations, with
pagination/filtering and private management endpoints. Add restricted Django
Admin actions and an Android-compatible public Retrofit contract/DTOs.

Apply two additive PostgreSQL migrations while preserving existing clinical
records. Document tests, operator setup and PostgreSQL test-role limitations.
Keep map integration, dataset import and Android directory UI wiring separate.
```

Task 4.1 ends here; Task 4.2 has not begun.
