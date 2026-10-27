# Task 4.2B — Hospital geocoding, location review and nearby search

Task 4.2B.1 adds a guarded manual-coordinate review workspace and persistent
duplicate decisions. See [Exact hospital location review](exact-hospital-location-review.md)
for current instructions. A checkbox alone no longer resolves duplicate identity;
broad/manual proposal approval also requires a recorded evidence URL.

The backend implementation, automated tests and live candidate generation are
complete. **60 unapproved proposals were stored for all 12 imported hospitals**.
The repeat run reused all 12 current results with **zero provider requests**.
Every match is broad/low-relevance and requires independent exact-location review;
none is a verified hospital position. **Zero locations or hospitals were approved.**

Initial configuration attempts stopped at MISSING_KEY without a request. The
saved key was then found in `backend/.env1`; only that setting was transferred to
ignored `backend/.env`, with `.env1` locally excluded from Git. One subsequent
AUTH_RESTRICTED request stopped the batch safely. After the user corrected the
provider authorization, generation proceeded. Keys were never printed or copied
into source, command arguments or reports. Historical failure reports are retained.

## Audit and preservation

Reviewed `community-care-database.md`, `udaipur-hospital-import.md`, existing
Hospital/DoctorProfile identities, scoped management/verification services,
import provenance, serializers/routes, Admin, PostgreSQL settings and tests.
Earlier uncommitted Tasks 4.1/4.2A and the committed Task 3.3 UI are preserved.
No Kotlin navigation, clinical algorithm, doctor identity or appointment workflow
was changed. Existing data counts and serialized-data hashes match before/after;
the hospital comparison excludes the two new schema fields, whose defaults are
separately checked. See the verification JSON linked below.

## Provider configuration and terms

Settings explicitly load `backend/.env` rather than relying on the working
directory. Existing process environment variables retain normal dotenv precedence.
The example configuration has an empty key placeholder only:

```dotenv
MAPTILER_API_KEY=
MAPTILER_GEOCODING_TIMEOUT=10
MAPTILER_GEOCODING_RETRIES=2
MAPTILER_GEOCODING_REQUEST_BUDGET=24
MAPTILER_GEOCODING_MIN_INTERVAL=1.1
MAPTILER_GEOCODING_DISTANCE_WARNING_KM=30
```

Put the real key only in the ignored backend `.env`, save the file, and start a
new management-command process. A shell variable already set to an empty key can
override dotenv and should be corrected in that shell. Do not paste keys into
reports, Git, screenshots or command-line arguments.

Reviewed primary sources on 2026-10-26:

- [MapTiler Cloud terms](https://www.maptiler.com/terms/cloud/), sections 6.3/6.4:
  geocoding bulk-result databases need attribution, and search-result export is
  permitted. This does not authorize map-tile caching or a substitute service.
- [MapTiler geocoding product FAQ](https://www.maptiler.com/search/): permanent
  geocoding-result storage is described for most use cases, subject to the terms.
- [Documented Cloud Geocoding API](https://docs.maptiler.com/cloud/api/geocoding/):
  backend use, forward-search parameters, relevance and response attribution.
  Returned feature IDs can change after reindexing; they are not hospital IDs.

The implementation uses HTTPS `api.maptiler.com/geocoding/{query}.json`, country
`in`, language `en`, limit 5 and autocomplete disabled. It does not proxy end-user
map traffic, cache tiles or access the user's key-settings webpage. An attribution
file accompanies the database and every candidate retains returned attribution.
Account-plan quotas/agreements remain the operator's deployment responsibility;
no billing/account entitlement is inferred merely from possession of a key.

## Generation and idempotency

`geocode_hospitals` supports `--district`, `--limit` (default 12, maximum 100),
`--source-ids`, `--dry-run` and `--report`. Reports must use a new filename.
Dry-run makes no network calls and no database writes.

Queries include the saved name, street/address, city, district, state and postal
code. Long requests are bounded to a practical 20-token budget, retaining name,
street and locality components; the full original snapshot is preserved and
shortened queries are flagged. Three initial long requests were rejected; the
bounded policy resolved them without re-requesting the nine already successful
queries. A hash binds the exact snapshot and search policy. A unique GeocodingRun
stores each hospital/query identity; successful and empty results are reused on
repeat runs, without paid duplicate requests. Failed runs have a five-minute
cooldown. API failures can still consume provider requests; no exactly-once
billing guarantee is claimed after network/process/commit failures.

A per-command request budget counts retries and locality lookups. Minimum request
spacing, bounded timeouts and retry counts, exponential waits and Retry-After
handling constrain usage. Long/date-valued retry deadlines defer further work
instead of retrying early. Authentication restriction, rate-limit and exhausted
budget errors stop the remaining batch. Errors use sanitized codes; provider
error bodies and request URLs containing keys are never logged/persisted.
Redirects are disabled so credentials cannot be forwarded to another host.

Geocoding candidates are separate from Hospital latitude/longitude. Each stores
the proposed point, provider relevance (not positional accuracy), reference ID,
label/type, timestamp, attribution and review flags. No phone/URL/availability
is inferred. Invalid or nonfinite coordinates are discarded. Empty results stay
empty; they are not filled with a city centre.

When the provider returns an unambiguous locality reference, its centroid is
stored solely as a distance-warning heuristic. It is never copied into a hospital
coordinate. Missing/ambiguous reference data produces an explicit warning.
Multiple matches, low relevance, broad locality/street results, locality mismatch,
distance over the configured warning threshold and original duplicate flags all
remain visible for review. UDR-002/004/005 retain their import duplicate concerns.
No duplicate hospital or coordinate proposal is merged automatically.

Each run is committed per hospital; the run row is locked while its bounded
request is processed. A report failure may occur after completed per-hospital
results were saved; rerunning reuses those results. This is intentionally not a
single all-or-nothing remote-service batch transaction.

## Location review

Visit `/admin/community_care/locationcandidate/` to inspect read-only proposals
and their saved hospital snapshots, labels, coordinates, attribution and flags.
Only a VedAmrit user with `verify_directory` (or an active superuser) can approve
or reject. Hospital membership alone never grants location approval; even a
verifier must use an independent verifier for their administered hospital.

Select one proposal and provide the **current hospital revision**, a meaningful
evidence/reason, **Exact facility location independently confirmed**, and—where
flagged—**Possible duplicate identities resolved**. A search relevance score or
street/city centroid alone is not confirmation of an exact facility entrance.
If no result matches, reject it and obtain independent information rather than
approving a plausible-looking point. The original import concerns remain intact.

Approval copies the selected coordinates to Hospital, records the approved
candidate and location state, increments the hospital revision and appends a
LOCATION_APPROVED audit. Rejection records actor/reason/time and an audit without
changing hospital coordinates. Already-reviewed and stale-input/revision proposals
cannot be approved again. Address/coordinate edits invalidate prior location
approval, and future geocoding uses a new snapshot.

Location approval deliberately **does not change hospital identity verification,
active/import review state, or booking**. The dedicated guarded/audited service
performs that separate update; ordinary coordinate edits still follow the existing
critical-edit re-verification policy. A draft remains a draft after approving its
location. No approval, publication or deletion ran on the live records here.

JWT endpoints, all under `/api/care/`:

```text
GET  admin/location-candidates/
GET  admin/location-candidates/{id}/
POST admin/location-candidates/{id}/approve/
POST admin/location-candidates/{id}/reject/
```

Decision JSON:

```json
{
  "expected_revision": 1,
  "reason": "Describe the actual independent location and entrance checks",
  "exact_location_confirmed": true,
  "duplicate_review_resolved": false
}
```

## Nearby API

```text
GET /api/care/hospitals/nearby/?latitude=24.5854&longitude=73.7125&radius_km=20
```

Anonymous GET is allowed. Coordinates must be finite and within geographic
ranges; radius is 0.01–100 km (default 20). Haversine distance is calculated in SQL
using PostgreSQL-compatible trigonometric expressions, clamped for floating-point
edge cases. There is no PostGIS installation requirement. Results are filtered
by radius, sorted by distance then ID, and use existing pagination (default 20,
maximum page_size 100).

Eligibility requires active VERIFIED hospital identity, cleared import review,
APPROVED location, an APPROVED candidate belonging to that same hospital, and
coordinates matching the approved candidate. A draft, pending, suspended,
inactive, stale-location or unapproved coordinate record cannot appear.

Results contain existing public hospital fields (including ID/name/address,
coordinates, facility type and disabled booking), numeric `distance_km` and
`location_attribution`. The ordinary directory keeps its existing eligibility
behavior; it does not newly require approved coordinates for a directory-only
entry. Future clients must retain applicable attribution and treat provider
strings as untrusted text, not executable HTML.

Actual PostgreSQL response on the current data:

```json
{"count":0,"next":null,"previous":null,"results":[]}
```

Invalid input example: HTTP 400 with a field-specific error such as
`{"radius_km":"Use a finite number between 0.01 and 100."}`. No patient/private
management metadata is returned. Distances are great-circle estimates, not road
travel distance or verified availability.

## Execution, migration and tests

Migration `community_care.0003_hospital_location_status_geocodingrun_and_more` is
applied to PostgreSQL. It adds the separate run/candidate tables, proposal identity
and range constraints, and Hospital location status/approved-candidate reference.
Existing coordinates are **not** automatically grandfathered as approved.
System checks pass, no migration drift is detected, and the final plan is empty.

| Evidence | Result |
| --- | --- |
| New provider/review/nearby tests | **34 passed**, mocked provider only; includes long-query handling. |
| Full Django regressions | **189 discovered: 186 passed, 0 failed/errors, 3 skipped**. Includes import, migration compatibility, auth, assessments, generation, doctor review and PDF/report protection. |
| Agent-engine regressions | **54 passed**, plus 3 subtests; 1 dependency warning. |
| PostgreSQL nearby endpoint | HTTP 200, zero results; SQL expression executed successfully. |
| PostgreSQL known-distance check | A 0.1° meridian separation returned **11.119508 km**. No fixture hospital was inserted into PostgreSQL. |
| PostgreSQL dry-run | 12 hospitals selected, zero requests/writes. |
| Initial generation attempts | Both blocked at MISSING_KEY, 0 HTTP requests. |
| Initial restricted request | 1 HTTP request, AUTH_RESTRICTED; batch stopped. User subsequently fixed provider authorization. |
| First authorized batch | 13 requests; 45 candidates from 9 facilities; three long-query failures. |
| Bounded-query completion | 4 requests; only the three corrected queries regenerated, nine results reused; **60 candidates across all 12 facilities**, 0 current failures. |
| Actual repeat | **0 HTTP requests**, 12 results reused, no new candidates. |
| Current PostgreSQL location state | 12 DRAFT/NEEDS_REVIEW hospitals; 12 NULL coordinate pairs; **0 approved locations**; 60 PROPOSED candidates; booking disabled for all. 15 query runs include 12 successful current runs and 3 historical long-query failures. |
| Preservation | Existing users, doctors, consultations, assessments, profiles, agent runs, plans, reports and hospital fields have matching before/after hashes. |

Three existing real graph/RAG/PDF smoke tests remain opt-in and were skipped.
Write/concurrency tests use isolated SQLite because the PostgreSQL role lacks
CREATEDB and no separate test database is provisioned. PostgreSQL DDL, the actual
nearby SQL, read-only smoke and data preservation were checked directly. Actual
provider outage/rate-limit retries, concurrent paid jobs and human location
acceptance are not claimed. Live provider coverage was observed to be insufficient
for exact facility positions: all 60 results are broad/low-relevance; 38 also have
locality mismatch flags, and all lack an unambiguous locality-distance reference.
UDR-002/004/005 account for 15 duplicate-review flags. These results must not be
presented as accurate facility entrances. No Android SDK/UI/device work was performed.

Reports (initial configuration failures are retained as execution history):

- `docs/import-reports/udaipur-geocoding-dry-run.json`
- `docs/import-reports/udaipur-geocoding-candidates.json`
- `docs/import-reports/udaipur-geocoding-candidates-retry.json`
- `docs/import-reports/udaipur-geocoding-live.json`
- `docs/import-reports/udaipur-geocoding-authorized.json`
- `docs/import-reports/udaipur-geocoding-bounded.json` — final complete candidate report
- `docs/import-reports/udaipur-geocoding-repeat.json` — zero-request repeat evidence
- `docs/import-reports/udaipur-geocoding-verification.json`

For a future run, use a new report name. Successful/empty results are cached;
changed input generates a new query identity, while failed runs honor a
five-minute cooldown. No Referer/origin spoofing or other restriction bypass is used:

```powershell
Set-Location D:\VedAmrit\backend
$env:DJANGO_DATABASE_ENGINE='postgresql'
.\.venv\Scripts\python.exe manage.py geocode_hospitals --district Udaipur --limit 12 --report ..\docs\import-reports\udaipur-geocoding-next.json
# Optional targeted run: --source-ids UDR-002 UDR-004 UDR-005
```

Tests, in a separate terminal:

```powershell
Set-Location D:\VedAmrit\backend
$env:DJANGO_DATABASE_ENGINE='sqlite'
$env:SQLITE_DB_PATH=':memory:'
.\.venv\Scripts\python.exe manage.py test apps.community_care.test_geocoding --noinput
# Full run: add the test modules listed in Task 4.2A's handoff.
```

## Task-specific changed/new files

- `backend/.env.example`
- `backend/vedaahar_backend/settings.py`
- `backend/apps/community_care/models.py`
- `backend/apps/community_care/serializers.py`
- `backend/apps/community_care/views.py`
- `backend/apps/community_care/urls.py`
- `backend/apps/community_care/admin.py`
- `backend/apps/community_care/geocoding.py`
- `backend/apps/community_care/location_review.py`
- `backend/apps/community_care/nearby.py`
- `backend/apps/community_care/management/commands/geocode_hospitals.py`
- `backend/apps/community_care/migrations/0003_hospital_location_status_geocodingrun_and_more.py`
- `backend/apps/community_care/test_geocoding.py`
- `data/imports/GEOCODING_ATTRIBUTION.md`
- `docs/hospital-geocoding-nearby.md`
- The eight geocoding JSON reports listed above.

Local configuration changes, intentionally not versioned: only MAPTILER_API_KEY
was copied from the user's `.env1` into ignored `backend/.env`; `.git/info/exclude`
now also excludes `/backend/.env1`. The source `.env1` was retained, not deleted.

This is the Task 4.2B delta within the earlier uncommitted working tree, which was
not reset or replaced. No credentials were committed, and no commit/push ran.

## Readiness for Task 4.2C

The backend contracts, review gates and nearby calculation are implemented and
tested and live generation/repeat verified. Real map markers remain blocked by
low-quality broad proposals and independent exact-location/identity/source/duplicate
review. No hospitals
were approved just to populate a map. Task 4.2C/Android MapTiler SDK integration
has not started.
