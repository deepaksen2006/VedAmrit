# Task 4.3 — verified Ayurvedic doctor directory

## Audit and implementation plan (before implementation)

The existing `doctors.DoctorProfile` is the canonical account-linked directory
record. Reuse its shared `VerifiedRecord` lifecycle, `DoctorHospitalAffiliation`,
scoped management services, revision checks, independent-verifier permission,
append-only `VerificationAudit`, Django Admin, public `/api/care/doctors/` list and
detail routes, filters and bounded pagination. No additional doctor model or
schema migration is needed.

Missing pieces identified:

1. Doctor VERIFY accepts a reason but has no mandatory credential evidence or
   explicit human attestations. Require both and preserve them in the existing
   private audit; public eligibility must require that recorded review.
2. Legacy `/doctors/verified` aliases use the private profile serializer and only
   the legacy boolean. Route their responses through the safe public query and
   serializer with pagination.
3. Android has Retrofit doctor DTOs/routes but no real directory repository,
   list/profile state or Community Care doctor screen. Add these and remove the
   placeholder public doctor list. Keep existing hospital map/navigation intact.
4. Add isolated permission/publication/workflow tests, Android state/API/UI tests,
   connected-device verification where available, and build/install the debug APK.
   Compare live database digests before and after without calling profile APIs.

Publication retains the existing conservative rule: verified active doctor,
recorded credential review, and an active affiliation with a publicly eligible
verified active hospital. A doctor profile, registration string, certificate upload
or association alone never authorizes publication. Booking remains unavailable.

## Completion status — 27 October 2026

**Task 4.3 implementation and validation are complete. Actual practitioner
publication remains blocked by human credential verification and eligible hospital
affiliation.** Live PostgreSQL contains **0 DoctorProfile records** and **0 publicly
eligible hospitals**. No real doctor or hospital was created, approved or published.
UDR-001 remains **DRAFT**, revision **3**, location **APPROVED**, candidate **61**,
coordinates **24.588336, 73.679142**, booking **False**. The other 11 imported
hospitals remain DRAFT. See [the first-hospital review](first-verified-hospital.md).

No schema migration, PostgreSQL reset, permission change, booking enablement,
commit or push was performed. Existing uncommitted work was retained.

## Reused and completed components

Reused: canonical `DoctorProfile` and account linkage; shared verification model;
hospital affiliations and membership scopes; independent-verifier permissions;
revision-based services; append-only audit; Django Admin; public and management
viewsets; bounded pagination; Retrofit DTOs/client; Community Care navigation/theme.

Completed:

- Required credential evidence references and four human attestations for doctor
  VERIFY, persisted with actor, reason and revision in the existing private audit.
- Public eligibility checks the latest relevant credential decision, rather than
  accepting the legacy boolean alone. Critical edits and revocations invalidate it.
- Legacy verified-list aliases use the safe public query and serializer.
- Android `DoctorDirectoryRepository`, `DoctorDirectoryViewModel` and
  `DoctorDirectoryScreen` implement real public API list/detail flows.
- Removed the hardcoded public doctor list from `DoctorRepository` and the patient
  Consult tab. The old doctor account prototype no longer defaults to a fabricated
  verified identity. This task does not turn that prototype into a production
  account-editing client; authenticated editing remains available through the
  existing backend routes below.

JWT login, patient profiles, diet generation, PDF export and agentic AI production
flows were not changed by this implementation. The regression suite includes
authentication and agentic report/PDF tests.

## API endpoints and access rules

| Endpoint | Access / behavior |
| --- | --- |
| `GET /api/care/doctors/` | Anonymous public list. `search` matches name/specialization; `specialization` filters specialty; `hospital_id`, `city`, `state` filter eligible affiliations. All affiliation filters must match the same public affiliation. |
| `GET /api/care/doctors/{id}/` | Anonymous public detail; **404** for absent or ineligible records. Same eligibility and safe serializer as listing. |
| `GET /doctors/verified`, `GET /api/doctors/verified` | Legacy aliases retained, now use the same public eligibility and safe serializer. Responses are paginated objects; clients expecting the former unpaginated array must use `results`. |
| `GET/POST /api/care/manage/doctors/`, `GET/PATCH /api/care/manage/doctors/{id}/` | JWT required. Directory administrators manage globally; assigned hospital ADMIN members create/edit within scope; assigned STAFF can read within scope. Account linking is a separate authorized action. |
| `POST /api/care/manage/doctors/{id}/submit/` | Authorized directory/hospital administrator, current `expected_revision`, meaningful `reason`. DRAFT/REJECTED → PENDING; registration council and qualifications must be present and model validation must pass. |
| `POST /api/care/admin/doctors/{id}/{verify,reject,suspend,reopen}/` | JWT plus active explicit `community_care.verify_directory` permission or superuser. Revision and reason required. VERIFY additionally requires independent reviewer and credential review below. |
| `POST /api/care/admin/doctors/{id}/link-user/` | Directory administrator only; current revision and reason; no replacement of an already linked account and no duplicate doctor profile. |
| `GET/POST /api/care/manage/affiliations/`, `PATCH /api/care/manage/affiliations/{id}/` | Scoped authenticated management; hospital/doctor identity of an existing affiliation is immutable. Does not verify either record or enable booking. |
| `GET /api/care/admin/audits/?record_type=DOCTOR&record_id={id}` | Directory administrator only. Read-only, paginated private evidence/audit history. |
| `GET /doctor/profile`, `PUT/PATCH /doctor/profile/update` | Existing authenticated doctor accesses/edits their own private profile. Multipart/form input retained. Identity, status and revision are read-only; critical changes require re-verification. |

Existing `/doctor/register`, `/doctor/login` and `/doctor/status` routes remain.
Registration does not verify credentials. Self-profile and management routes may
return private fields only to their authorized caller; the public lists/details
exclude account IDs, email, phone, certificate URLs, internal profile metadata and
credential evidence references.

Public lists return `{count, next, previous, results}`. Default page size is **20**,
capped at **100**, with stable name/ID ordering. Empty results are **200** with an
empty `results`; malformed hospital ID is **400**; invalid/out-of-range pages are
**404**. Android requests numbered pages at its configured API host and never
follows a server-supplied pagination URL to another host.

### Public eligibility and regression review

A public doctor must satisfy **all** of these:

1. `verification_status=VERIFIED`, synchronized `is_verified=True`, `is_active=True`.
2. The latest relevant audit action among VERIFY, REVERIFY_REQUIRED, SUSPEND,
   REOPEN and REJECT must be VERIFY with the service-generated
   `doctor-credentials-v1` review protocol.
3. At least one active affiliation points to a hospital satisfying the existing
   public hospital identity/import/duplicate-review gates. Only eligible
   affiliations are serialized. Approved map coordinates are a separate hospital
   map requirement; affiliation output supplies no unapproved coordinates.

The legacy endpoint review confirmed it now uses `PublicDoctorSerializer`, not
`DoctorProfileSerializer`, and the same `public_doctors()` query. Tests explicitly
assert private-field exclusion on both aliases and the canonical API.

Critical identity/credential/profile changes move a VERIFIED doctor to PENDING,
clear `is_verified`, and append REVERIFY_REQUIRED. Old credential evidence remains
historical and cannot authorize republication. A fresh independent VERIFY with new
attestations/references is required. Suspension/rejection/reopening likewise cannot
be bypassed with the old approval. Noncritical consultation-fee edits do not erase
an otherwise valid review. Review tests cover both behaviors and ensure temporary
audit context is not copied into the later invalidation audit. No production
regression was found in this review.

## Required human verification and approval boundary

The software checks authorization, state, revision, evidence-reference structure
and attestations. It does **not** authenticate a qualification, query a council
automatically, or establish that a submitted URL/document actually proves a claim.
The authorized human verifier must perform those checks before confirming them.

Before approval, independently confirm the practitioner's identity, issuing council,
registration/current standing, each published qualification, specialty/experience/
biography claims and actual hospital affiliation. Retain dated council/institutional
evidence and controlled document references. Do not put patient material, private
certificate contents, passwords or tokens into review references. Unsupported
claims must be removed or resolved before approval; an uploaded certificate alone
is insufficient.

Doctor VERIFY requires a `credential_review` object containing:

| Field | Requirement |
| --- | --- |
| `evidence_references` | 1–10 nonblank checked council URLs or controlled institutional document references; 3–1,000 characters each. |
| `registration_checked` | `true`, after checking issuing council/current registration standing. |
| `qualifications_checked` | `true`, after checking every published qualification. |
| `profile_claims_checked` | `true`, after checking specialty, experience and biography claims. |
| `affiliations_checked` | `true`, after checking hospital affiliations. |

Reasons must contain 3–2,000 trimmed characters. `expected_revision` must match the
freshly loaded record. Missing/false attestations or missing references fail with
**400**, stale revision with **409**, unauthorized/self-interested verification
with **403**. A doctor cannot verify their own account, nor can an active hospital
ADMIN verify a doctor actively affiliated with their assigned hospital. Ordinary
staff or manage-directory permission alone does not grant verification permission.

### Existing Django Admin workflow

These are instructions for a future legitimate human review; this task performed
none of these live actions.

1. An existing authorized staff user opens `/admin/doctors/doctorprofile/`, finds
   the canonical record, and checks credentials and account linkage. Do not create
   a duplicate profile to get around a registration/account conflict.
2. A directory or assigned hospital administrator completes supported profile
   fields and affiliations and selects exactly one record. Choose **Submit selected
   record for review**, enter its freshly reloaded revision and substantive reason.
   Confirm PENDING and the SUBMIT audit.
3. An independent authorized verifier reads the evidence. Reload the new revision,
   select exactly one pending doctor and choose **Verify selected record after
   independent credential checks**. Enter revision, meaningful reason and evidence
   references **one per line**; check all four attestations only after performing
   the checks. Confirm VERIFIED and its actual actor/references in
   `/admin/community_care/verificationaudit/`.
4. Verify publication via public list/detail/search and eligible hospital filter.
   A verified doctor associated only with DRAFT/unreviewed/inactive hospitals stays
   hidden. Hospital identity approval must follow its own legitimate workflow.

Historical VERIFIED doctors lacking the new review stay hidden; an authorized
human must use the existing SUSPEND → REOPEN → independently VERIFY workflow for
re-review. Do not fabricate an audit, change flags with SQL or grant permissions
merely to make a record appear. There are no live doctor records requiring that
transition at completion.

## Android navigation and states

Patient dashboard **Consult → Browse verified Ayurvedic doctors** opens Community
Care. Select **Browse verified doctors** to open the doctor directory; the hospital
map remains available. Search by doctor name/specialty, optionally set the specialty
filter, tap a card to fetch the current public detail endpoint, and use **Doctors at
this hospital** to filter by that eligible hospital. **All hospitals** or **Clear
filters** removes that filter. Load more uses bounded API pages.

Back from a profile refreshes the directory; back from the directory returns to
the hospital map; the existing Community Care back action still returns to Consult.
System Back is supported. A resumed doctor screen refreshes after device/app stop.
Headings, labelled fields, standard touch controls and status announcements support
accessibility and long content scrolls.

Loading list/profile, empty search, network/offline failure, backend error and
unavailable/revoked profile are distinct states with retry. Opening/refreshing a
profile clears old details; a denied or malformed response does not fall back to
cached credentials. Responses missing credential-review metadata, required
credential fields or a verified affiliation fail closed. Request cancellation and
generation checks prevent late responses restoring a previous search/profile.

Production uses real Django responses. Positive doctor data is synthetic only in
unit tests and instrumentation MockWebServer responses; no fixture doctor or
hospital was inserted into live PostgreSQL. No appointment or booking action was
added. The existing MapTiler configuration and ignored `local.properties` remain
in place; no API key was printed or copied into documentation.

## Validation and installed APK

| Check | Result |
| --- | --- |
| Broad Django regression run | **237 run, 234 passed, 3 skipped**, no failures. Covers Community Care, authentication, agentic reports, assessment sync, generation and draft PDF. |
| Final doctor review regression run | **11 passed**, including alias privacy, credential invalidation/fresh review, permissions and hidden draft-hospital affiliations. Overlaps the broad run; do not add these counts together. |
| Android unit tests | **176 passed**, 0 failures/errors/skips. |
| OnePlus CPH2619 / API 35 doctor UI tests | **5 passed**: synthetic profile/credentials/filter/draft exclusion; loading/search/error/retry/revoked profile; offline; hospital-to-doctor/back navigation; real public empty directory. |
| OnePlus hospital-map regressions | **4 passed**: existing Community Care search/error tests, native synthetic verified-marker flow, real MapTiler map and nearby/empty flow. |
| Debug APK and instrumentation APK | Built successfully; installed with `adb install -r`, app data retained. Final installed production APK hash matches the built debug APK. |
| Schema dry run | `makemigrations --check --dry-run`: **No changes detected**. No migration applied. |
| Lint | Existing **3 errors, 115 warnings, 1 hint**; no new doctor-directory findings. |

Django tests used an isolated, disposable **SQLite in-memory test database**;
positive PostgreSQL publication was not exercised against live records. Three
skips are existing opt-in real agent-graph/RAG/PDF integration smokes. Live
PostgreSQL checks below were read-only. No real clinician was approved for testing.

Lint remains blocked by `agentic/PublicPdfDownloads.kt:55` (API 29 call),
`res/values/themes.xml:8` (API 29 attribute) and `PatientProfileScreen.kt:955`
(ViewModel construction inside a composable). They were not changed or suppressed.

APK: `app/build/outputs/apk/debug/app-debug.apk` (79,727,657 bytes).
SHA-256: `32898526e6ddb6143000c9fc82fe91fa54d562d9163b1a58f2a3c04aec02179f`.
Installed app: `com.vedamrit`; OnePlus package metadata and on-device APK SHA-256
were checked during finalization. No Android production code changed after the
passing device runs; the final review added backend tests and documentation only.

The debug build uses `http://127.0.0.1:8000/` via
`adb reverse tcp:8000 tcp:8000`. The saved local LAN configuration was not changed.
The connected phone and running backend/USB reverse are needed for that debug URL.

Live public PostgreSQL/API smoke results:

| Request | Result |
| --- | --- |
| Doctor list; name search; specialty + hospital 1 filter | **200, count 0** for each. |
| Both legacy verified-list aliases | **200, count 0**, safe paginated contract. |
| Doctor detail `/api/care/doctors/1/` | **404**. |
| Invalid `hospital_id` / out-of-range page | **400 / 404**. |
| Anonymous managed doctors / private doctor audit | **401 / 401**. |
| Public hospital directory | **200, count 0**. |

## Read-only investigation of four changed table digests

**The cause is unknown, and no writer is identified or inferred.** The saved
before snapshot was last written at **05:42:07 UTC (11:12:07 IST)** on 27 October
2026; the validation snapshot was read at **05:54:29 UTC (11:24:29 IST)**. Of 23
model comparisons, **19 matched** and exactly these four differed:

| Physical PostgreSQL table / Django model | Before → validation count | Metadata-only follow-up at 05:58:39 UTC |
| --- | --- | --- |
| `agentic_reports_patientprofile` / `PatientProfile` | **2 → 2** | Count 2; maximum `updated_at` **05:54:14.364830 UTC** (11:24:14 IST). |
| `agentic_reports_assessmentresult` / `AssessmentResult` | **3 → 4** | Count 4; maximum `created_at` **05:45:21.745314 UTC** (11:15:21 IST). |
| `agentic_reports_agentrun` / `AgentRun` | **3 → 5** | Count 5; maximum `started_at` **05:45:30.991003 UTC**; `completed_at` **05:45:44.734367 UTC** (11:15 IST). |
| `agentic_reports_wellnessplan` / `WellnessPlan` | **3 → 4** | Count 4; maximum `created_at` **05:45:44.741609 UTC** (11:15:44 IST). |

Finalization used only existing digest artifacts, aggregate counts, timestamp
maxima, trigger counts, logging configuration and Admin-event counts. Live queries
were **REPEATABLE READ, READ ONLY**. No patient identifiers, health values, payloads,
error contents, individual row data or actor identities were inspected/exported for
this follow-up. No profile/assessment/plan endpoint was invoked by the directory
checks. These records were not modified by this task's investigation.

There are **0 custom non-FK triggers** on each of the four tables and **0 matching
Django Admin log events** since the baseline. PostgreSQL has `log_statement=none`
and duration statement logging disabled (`-1`); no persistent Django HTTP access
log was located in the workspace/build inventory. These observations cannot
identify a request or explain the changed business fields. A count increase shows
net growth, not a complete event history, and a timestamp does not establish the
writer. The investigation cannot determine whether the profile difference was
limited to its timestamp. Do not attribute these changes to a user, test or process.

Doctor, hospital, affiliation, membership, candidate/geocoding, verification audit,
permissions, accounts, consultation and diet-plan digests matched their baselines.
The later metadata follow-up still found 0 doctors, 12 hospitals, 13 verification
audits and the unchanged UDR-001 status/coordinates/candidate. This is **not** a
claim that the entire live database was unchanged during the work window.

## Outstanding human and operational blockers

- No genuine doctor identity/registration/qualification review has occurred, and
  there is no eligible verified hospital. Actual public doctor/profile positive
  paths remain blocked until both legitimate workflows are complete.
- UDR-001's independent hospital identity/address/entrance evidence remains pending;
  Task 4.3 does not approve it or use it as a public affiliation.
- Existing lint errors remain; debug APK build and device tests succeed despite
  that separate failing lint gate.
- The four patient/agentic digest changes remain unattributed. Existing snapshots
  and metadata do not establish their cause. No live records were reverted and no
  logging/permissions/schema changes were introduced to investigate.

Evidence: [sanitized validation and metadata report](import-reports/doctor-directory-validation.json).
Local logs: `build/doctor-directory-django-tests.txt`,
`build/doctor-directory-review-tests.txt`, `build/doctor-directory-android-build.txt`,
`build/doctor-directory-device-tests.txt`, `build/doctor-directory-hospital-regression.txt`,
`build/doctor-directory-lint.txt`, `build/doctor-directory-schema-check.txt`.
Synthetic profile/empty and live empty screenshots were inspected under
`build/doctor-directory-device-evidence/`.
