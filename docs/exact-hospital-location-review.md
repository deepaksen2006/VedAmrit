# Task 4.2B.1 — Exact hospital location review

Implemented on `agentic-ai-integration`, preserving earlier uncommitted work.
**No live coordinates were entered or approved.** The 12 imported facilities
remain DRAFT / NEEDS_REVIEW with NULL approved coordinates and booking disabled.
All 60 original MapTiler proposals remain unchanged and PROPOSED.

## What changed

- Each hospital has an **exact-location review workspace** linked from its
  Django Admin record. Supplied name/address/source/status appear next to a table
  of every candidate's coordinates, geocoder label, provider/reference, relevance,
  attribution, warnings and review state. Stale snapshots are identified.
- Candidate detail pages also include a side-by-side facility/point comparison
  and a link to the hospital workspace.
- Each point has external Google Maps and OpenStreetMap inspection links. These
  transmit only coordinates when the reviewer follows the link—no API key or JWT.
  There are no map embeds, SDK changes, automatic map requests or evidence scraping.
  Google Maps links follow the documented
  [Maps URL syntax](https://developers.google.com/maps/documentation/urls/get-started).
- A verifier can enter manually confirmed coordinates, independent evidence URL,
  reason and an optional original-proposal reference. Coordinate fields start
  blank; poor geocoder results are never preselected as confirmed positions.
- Manual entries require **preview, then explicit final confirmation**. The
  signed preview binds the hospital, authenticated reviewer, input/evidence and
  revision, expires after ten minutes, and becomes invalid if the form changes.
- Saving creates a **new manual candidate**, with provider `Manual confirmation`,
  no invented confidence, evidence URL, reviewer, server timestamp, reason and
  optional link to the original proposal. It never changes the original proposal.
- Location approval is separate from hospital identity verification/import review.
  It does not make a hospital VERIFIED, clear import review or enable booking.
- Duplicate review is now an explicit persisted decision, not a checkbox. Only
  an independent authorized VedAmrit verifier can make that decision.

## Review instructions

1. Sign in to Django Admin as an active VedAmrit verifier with
   `community_care.verify_directory` or superuser rights, and staff access for the
   Admin UI. A hospital membership or staff flag alone grants no review authority.
2. Open **Community care → Hospitals**, select the facility, then click
   **Compare proposals and review an exact location**. Direct workspace URLs are:

   ```text
   /admin/community_care/hospital/{hospital_id}/location-review/
   ```

   For the existing flagged records these are hospital IDs 2, 4 and 5. Other
   installations must use their actual IDs, not assume those numeric values.
3. Compare the supplied street/address and original source cautions against the
   provider labels and points. The stored proposals are broad/low-relevance;
   a city/street centroid or a displayed map pin is not proof of an exact facility.
   Inspect positions externally and independently confirm the physical facility
   and entrance. Provider/source text is escaped, not rendered as trusted HTML.
4. For UDR-002, UDR-004 and UDR-005, complete **Record duplicate review** first:
   - `DISTINCT_CONFIRMED`: independent evidence establishes a separate facility.
   - `DUPLICATE_CONFIRMED`: evidence establishes a duplicate; choose the other
     retained record. Neither record is merged, deleted or automatically published.
   - `NEEDS_REVIEW`: leave/reopen unresolved review. Do not pick DISTINCT merely
     to bypass the location gate.

   Supply a public evidence URL, meaningful identity-check reason and explicit
   human confirmation. Decisions are revision-checked and audited. An identity/
   address change makes the saved review snapshot stale; it must be reviewed again.
5. Enter the **exact manually checked latitude and longitude**, at most six decimal
   places, in the manual form. Provide a public HTTP(S) facility-location evidence
   URL and a reason describing the actual checks. An evidence URL is a recorded
   human source, not an automatically validated claim. Do not include patient data,
   credentials or API-token URLs. No server-side fetch of that URL takes place.
6. Optionally select which stored proposal is being corrected. The proposal must
   belong to this hospital. Confirm that the exact facility position was checked;
   flagged facilities also require acknowledgment of the saved duplicate decision.
7. Click **Review entered coordinates**. This writes nothing. Inspect the preview
   and its map link. If fields change, update the preview before saving.
8. Check **After preview, I authorize saving these confirmed coordinates**, then
   click **Confirm and approve these coordinates**. Missing/expired/tampered or
   other-reviewer preview tokens cannot save a location. Stale revisions and
   repeated submissions are rejected without duplicate approvals.
9. Review the **Location and duplicate decision history** below the forms. It
   includes actor, time, reason, evidence, related candidate and duplicate-state
   changes. The current duplicate state is also visible in the read-only
   **Hospital duplicate reviews** Admin section.
10. Hospital source/identity verification remains a separate Task 4.1 action.
    Approving a draft's position alone leaves both public directory and nearby
    search empty for that draft. Booking remains disabled.

An operator assigned as ADMIN to the same hospital cannot approve its location
or duplicate decision, even if separately granted verifier rights. Global
directory managers may inspect the workspace but cannot submit decisions.

## Existing proposal review and API compatibility

Existing approve/reject endpoints and Admin actions remain available. Approving
a broad/manual proposal now also requires an evidence URL. A flagged facility
must have a current `DISTINCT_CONFIRMED` duplicate review; the old
`duplicate_review_resolved` checkbox is an acknowledgment, not authority to clear
the new review record. JWT management serializers expose duplicate status as
read-only and return saved candidate evidence/origin metadata.

Ordinary coordinate/address edits still invalidate location approval. Reopening
duplicate review or confirming a duplicate makes a previously approved location
STALE and removes public eligibility without changing hospital identity status.
Pending/confirmed duplicate records are excluded from the public directory and
nearby API. Import/identity submission also checks that duplicate review is clear.

Manual correction is an Admin workflow and protected service, not a new anonymous
or patient-facing coordinate-write endpoint. The standard Admin CSRF and login
protections apply. Evidence URLs reject non-HTTP(S), credentials and common API
token query parameters. Coordinates reject nonfinite, out-of-range and excess-
precision values. All mutation steps are transactional and use consistent
hospital-first locking; audit failure rolls back the decision and manual point.

## Models and migration

`community_care.0004_locationcandidate_based_on_candidate_and_more` is applied
to PostgreSQL. No pending migrations or model drift remain.

- Adds `LocationCandidate.evidence_url` and optional `based_on_candidate`.
  Existing candidate values are preserved; the new fields default blank/NULL.
- Adds `HospitalDuplicateReview`, one per flagged hospital, with status, related
  retained hospital, identity snapshot, evidence, reviewer/time/reason and revision.
- Backfills imported possible-duplicate flags as **NEEDS_REVIEW only**. This
  initializes review bookkeeping, not human confirmation. No reviewer, evidence,
  timestamp, canonical relationship or approval is invented.
- Future imports also initialize pending review for flagged created records.
- No clinical/user/doctor schema changes, dataset replacement, reset, merge or
  deletion was performed. Original import provenance stays read-only.

## Actual PostgreSQL verification

| Item | Result |
| --- | --- |
| Hospitals | 12, all DRAFT / NEEDS_REVIEW |
| Approved coordinate count | **0** |
| NULL coordinate pairs | 12 |
| Original proposed candidates | 60, unchanged |
| New live manual approvals | **0** |
| Booking-enabled hospitals | 0 |
| Public directory / nearby | HTTP 200, count 0 for both |

| Source ID | Hospital ID | Duplicate state | Human reviewer / evidence |
| --- | ---: | --- | --- |
| UDR-002 | 2 | **NEEDS_REVIEW** | None recorded |
| UDR-004 | 4 | **NEEDS_REVIEW** | None recorded |
| UDR-005 | 5 | **NEEDS_REVIEW** | None recorded |

No duplicate was declared confirmed or distinct by this task. These results are
the unresolved live state, not inferred resolutions. The independent verifier
must perform those reviews; source descriptions alone are insufficient.

Before/after counts and serialized-data SHA-256 hashes match for existing users,
doctors, consultations, assessments, patient profiles, agent runs, wellness plans,
final reports, hospitals, geocoding runs and verification audits. The 60 original
candidate hashes also match when excluding the two added schema fields. The only
new live business records are three pending duplicate-review records.

Evidence: [verification report](import-reports/udaipur-exact-location-review-verification.json).

## Tests and checks

| Check | Exact result |
| --- | --- |
| New exact-review tests | **25 passed**, 0 failed; final focused rerun includes Admin creation-page compatibility. |
| New migration compatibility test | **1 passed**; original proposals and unresolved flagged identities preserved. |
| Community Care tests within full run | **130 passed**, 0 skipped/failed. |
| Full Django regressions | **215 discovered: 212 passed, 3 skipped, 0 failed/errors**. |
| Agent-engine regressions | **54 passed**, 0 failed, plus 3 passed subtests; 1 dependency warning. |
| System check | Passed, 0 issues. |
| Migration consistency | Passed, no changes detected; migration 0004 applied. |

Coverage includes coordinate/URL validation; role, inactive-user and self-review
denials; evidence/reviewer/time/audit preservation; transaction rollback; original
proposal preservation; manual preview/no-write and signed confirmation; nonce,
revision, expiry and cross-reviewer protection; CSRF and escaped source/provider
HTML; duplicate review/state/revocation; and directory/nearby visibility.

One initial test patched Django's shared signing loader, which also invalidated
the test login session. It was corrected to create a genuinely expired signed
preview, exercising the real expiry path; the final suite passes.

The three skipped existing tests are opt-in real graph/RAG/PDF smoke tests. The
PostgreSQL role still lacks CREATEDB; automated write/concurrency fixtures ran in
isolated SQLite. PostgreSQL migration, pending-review backfill, public reads and
preservation checks ran against the configured database. No live manual approval,
external-map browser acceptance, council lookup or real-world location validation
was performed. No Android changes or build were required for this backend task.

Windows commands (isolated test terminal):

```powershell
Set-Location D:\VedAmrit\backend
$env:DJANGO_DATABASE_ENGINE='sqlite'
$env:SQLITE_DB_PATH=':memory:'
$env:HF_HUB_OFFLINE='1'
$env:TRANSFORMERS_OFFLINE='1'
.\.venv\Scripts\python.exe manage.py test apps.community_care.test_exact_location_review apps.community_care.test_geocoding apps.community_care.test_hospital_import apps.community_care.tests apps.community_care.test_migrations apps.authentication.tests apps.agentic_reports.tests apps.agentic_reports.test_assessment_sync apps.agentic_reports.test_draft_generation apps.agentic_reports.test_draft_pdf --noinput -v 1
.\.venv\Scripts\python.exe -m pytest agent_engine/tests -q --disable-warnings
```

Backend checks/start in a separate terminal with existing PostgreSQL credentials:

```powershell
Set-Location D:\VedAmrit\backend
$env:DJANGO_DATABASE_ENGINE='postgresql'
.\.venv\Scripts\python.exe manage.py check
.\.venv\Scripts\python.exe manage.py makemigrations --check --dry-run
.\.venv\Scripts\python.exe manage.py showmigrations community_care
.\.venv\Scripts\python.exe manage.py migrate --plan
.\.venv\Scripts\python.exe manage.py runserver 127.0.0.1:8000 --noreload
```

Migration 0004 is already applied here; do not blindly rerun or reverse migrations
to operate the review UI. Do not reset PostgreSQL or use production records as
test fixtures. Refer to Task 4.1 for initial authorized Admin setup.

## Task-specific changed/new files

- `backend/apps/community_care/models.py`
- `backend/apps/community_care/location_review.py`
- `backend/apps/community_care/duplicate_review.py`
- `backend/apps/community_care/review_forms.py`
- `backend/apps/community_care/review_admin.py`
- `backend/apps/community_care/admin.py`
- `backend/apps/community_care/templates/admin/community_care/hospital/location_review.html`
- `backend/apps/community_care/serializers.py`
- `backend/apps/community_care/views.py`
- `backend/apps/community_care/hospital_import.py`
- `backend/apps/community_care/test_geocoding.py`
- `backend/apps/community_care/test_migrations.py`
- `backend/apps/community_care/test_exact_location_review.py`
- `backend/apps/community_care/migrations/0004_locationcandidate_based_on_candidate_and_more.py`
- `docs/exact-hospital-location-review.md`
- `docs/hospital-geocoding-nearby.md`
- `docs/import-reports/udaipur-exact-location-review-verification.json`

This is the Task 4.2B.1 delta within the existing uncommitted working tree. No
MapTiler API requests, Android SDK integration, clinical changes, commit or push
were performed. Remaining work is human review and independent evidence, not
automatic approval of the broad geocoding results.
