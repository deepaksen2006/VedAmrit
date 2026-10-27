# Task 4.2D — first hospital verification review

Reviewed on **27 October 2026 (Asia/Kolkata)**. Evidence-only follow-up on the same
date: retrieved an original August 2026 college publication and the original
2021–22 state annual report; investigated the profile digest using read-only data.

**UDR-001 is still DRAFT and is not publicly visible.** Research corroborates the
institution and its current published college office address/telephone, but does
not identify the intended patient hospital/entrance or independently confirm its
coordinates. No live import-review, identity, location or booking
decision was made by this task. The approved coordinates and existing audit
history are preserved. The first genuine public marker remains blocked.

## Live record and approval boundary

| Field | Audited live value |
| --- | --- |
| Database ID / source | 1 / `udaipur_hospitals:UDR-001` |
| Name | Madan Mohan Malviya Government Ayurvedic College And Hospital |
| Type | `government_teaching_hospital` |
| Address | Ambamata Scheme - A Rd, Radaji circle |
| Locality | Udaipur district/city, Rajasthan |
| Postal code | **313004** |
| Telephone | 0294-2431900 |
| Email / source URL / description | Blank |
| Public facilities | Empty list |
| Imported service note | Ayurveda teaching hospital; explicitly unverified |
| Imported hours | Mon–Sat 08:00–17:00; Sun 08:00–11:00; explicitly unverified |
| Identity / import review / active | DRAFT / NEEDS_REVIEW / true |
| Revision at audit | **3**; always reload before a decision |
| Position | **24.588336, 73.679142** |
| Location / approved candidate | APPROVED / **61**, Manual confirmation |
| Booking | **False** |

Candidate 61 records a manual location decision on 26 October 2026. Audit 13 is
`LOCATION_APPROVED`, with DRAFT → DRAFT. Its evidence is a Google Maps link.
That is location-review history, **not independent identity verification**.
UDR-001 has no imported duplicate candidates and `duplicate_is_clear` currently
returns true. No duplicate decision was invented or required just to publish it.

`services.submission_errors()` currently rejects submission because import review
is NEEDS_REVIEW. An authenticated, independent human verifier must resolve the
evidence discrepancies and make the actual decision. Existing eligible account
capabilities were inspected read-only; no account was impersonated, logged in as,
created, assigned, or granted permissions by this task.

## Independent evidence and its limits

All follow-up sources were accessed on **27 October 2026**. Publication dates and
retrieval dates are distinct. Original PDFs below were downloaded over validated
HTTPS and relevant pages rendered and read; search snippets were used only as
leads. No calls, messages or institutional enquiries were sent by this task.

### Retrieved originals and current government portal data

| Source / document date | Inspected finding | Confidence and limit |
| --- | --- | --- |
| [College recruitment advertisement 01/2026–27, original PDF](https://jankalyanfile.rajasthan.gov.in/Files//Content/UploadFolder/OrderEntry/Department/2026/Recruitment_Related/O_130826_c4ce97da-8f89-4f33-92d6-409499e58e2a.pdf#page=1), dated **12 August 2026**, uploaded **13 August 2026** | Page 1 identifies Madan Mohan Malviya Government Ayurved College, Udaipur. Its letterhead gives Radaji Circle, near Fatehsagar, Udaipur **313001**, and **0294-2431900**. Two-page recruitment notice, not a hospital directory. Page 2 points back to department portal 226. | **High** for the college's published identity, correspondence address, postcode and office telephone as of August 2026. Does not identify the hospital reception/entrance, prove telephone connectivity, or supply coordinates. A visible signature block was inspected; cryptographic signature validity was not assessed. |
| [Official college portal, department 226](https://department.rajasthan.gov.in/home/dptHome/226), current public API retrieval | The portal's public department-detail API identifies the same college. Its public order list and order detail **240845** link to the August PDF and supply publication/upload dates. | **High** for provenance of that publication. The department-detail response has null address fields and flags `IsLivePortal=false`, `IsUpdationComplete=false`; fetching it today does not prove all portal data is current. No hospital coordinates were established. |
| [Rajasthan Directorate of Ayurved annual progress report **2021–22**](https://rmsc.health.rajasthan.gov.in/content/dam/doitassets/Medical-and-Health-Portal/Directorate-Of-Ayurved/pdf/Year2022/April2022/Prativedan%20Ayurveda%20Book%202021-22.pdf#page=89), PDF physical pages **89–90**, printed **85–86** | College cover repeats Ambamata Scheme/Radaji crossroads near Fatehsagar, **313001**, **0294-2431900**. The next page lists **three hospitals under the college**: Rajvaidya Premshankar Sharma Government Ayurved Hospital; Government Ayurved College Hospital, Motichohatta; and Government Ayurved Research Centre. | **High for historical institutional association** and college contact publication. This is not a current hospital roster, proof of a common campus, or a mapping of UDR-001 to any one of the three facilities. |
| [Government counselling seat matrix, 2025 publication URL](https://cdnbbsr.s3waas.gov.in/s3653ac11ca60b3e021a8c609c7198acfc/uploads/2025/09/202509131971354362.pdf#page=8), PDF pages **8–9**, retrieved in the initial review | Institution **700174**, Udaipur, Rada Ji Circle/Ambamata, postcode **313001**, government counselling quota. | **High for this historical academic listing**. Admissions do not establish a patient entrance, current hospital operations, services or hours. |

The current-publication discovery closes the earlier gap of having only indexed
office-contact evidence. It **does not** settle which associated hospital the
generic imported “College And Hospital” record represents.

### Inaccessible or incomplete sources — not accepted as original proof

| Source | Follow-up status on 27 October 2026 | Evidentiary treatment |
| --- | --- | --- |
| [NCISM permitted-college list, AY 2025–26, dated 12 September 2025](https://www.ncismindia.org/assets/pdf/List%20of%20Permitted%20Ayurveda%20Colleges%20for%20the%20A.Y.%202025-26%20as%20on%2012.09.2025.pdf) | Direct retrieval again **406**; regulator home retrieval also failed (**409** through the web tool). No inspected 2026–27 target profile was obtained. | Earlier indexed **AYU0226** entry is a research lead only. It is not independently verified original evidence in this report and is not proof of current permission. |
| [Rajasthan AYUSH counselling profile 125.PDF](https://rajayushcounselling.com/Images/Notifications/125.PDF) | Direct **404** again. | Indexed address/office-phone extracts remain leads; no new original profile was inspected. |
| [AIIA government AYUSH hospital list, August 2025 URL](https://aiia.gov.in/wp-content/uploads/2025/08/a.-Government-AYUSH-Hospitals-in-India.pdf) | Direct TLS validation failure; prior web timeout / separate 404 retained in the initial retrieval report. | Indexed hospital association is not original proof. A suspect sequential mobile in that extract must not be treated as a validated contact. TLS verification was not disabled. |
| [AACCC participating institutes, 2026 counselling](https://aaccc.gov.in/participating-institutes-ug/) | Main page **200**. Its linked NIC institutional-profile form now loads, improving on the earlier NIC error, but the retrieved form has no populated institute/program options or target profile. | Accessible form is not evidence of this facility's address or current clinical operation. No UDR-001-specific 2026 original profile was inspected. |
| [MPUAT/CTAE telephone directory, **2022**](https://ctae.ac.in/images/staticContent/Telephone-Directory-MPUAT-(2022).pdf) | Direct **406**; indexed material suggests an Ayurveda hospital at Motichohatta with the same number. | Historical lead only; target original row was not inspected. It cannot establish that the office landline is an exclusive hospital contact. |

The initial [source retrieval report](import-reports/first-hospital-source-retrieval.json)
is retained as history. The [evidence follow-up report](import-reports/first-hospital-evidence-followup.json)
records new retrievals, original-document hashes and page references. Original PDFs
and page images are local research intermediates in `build/evidence-gap-review/`;
the earlier counselling PDF remains in `build/first-hospital-evidence/`. Commercial
directories, news reports and similarly named hospitals elsewhere were not accepted
as proof. No access controls, TLS validation or authentication were bypassed.

### Field-by-field conclusion and physical-location distinction

| Field | Independently established | Still unresolved / human evidence needed |
| --- | --- | --- |
| College identity | **High confidence:** named government college, current August 2026 original notice plus historical government originals. | Exact current legal/patient-facing name of the hospital represented by UDR-001, and its relationship to the college. |
| College correspondence address | **High confidence:** Radaji Circle, near Fatehsagar, Udaipur **313001** in the 2026 letterhead; historical Ambamata Scheme detail aligns. | A college office address does not identify a hospital's patient entrance or prove campus/address equivalence. |
| Hospital address / postcode | The stored **313004 conflicts with the college's published 313001**. No inspected original supports 313004 for the intended hospital. | Hospital-specific current institutional letter/address document must resolve the postcode. Do not automatically replace it with the college postcode. |
| Telephone **0294-2431900** | **High confidence as the college's published office telephone**, including August 2026. | Call to confirm current institutional ownership/connectivity and whether patients should use it, or obtain a hospital-specific official contact statement. No call has been made. |
| Approved **24.588336, 73.679142** | Existing location decision and exact saved pair are preserved; the college locality broadly aligns. | **Not independently confirmed as the intended patient facility.** None of the inspected originals ties this pair to a named hospital, entrance or campus plan. A matching locality is insufficient; retain existing APPROVED history without claiming this evidence validates it. |
| Hospital association / services | **High confidence historically** that the college had three associated hospitals in 2021–22. | Current facility affiliation, hospital name and patient services/hours require current institutional evidence. Do not infer services from teaching subjects or recruitment posts. |

**Exact next human action:** call the published college office **0294-2431900**
and ask to be connected to the principal's office or hospital superintendent for
directory verification. Do not present the call as already completed. Request a
dated institutional letter or current official hospital publication that:

1. Names the specific hospital UDR-001 is intended to represent, states its college
   affiliation, and distinguishes it from the Motichohatta hospital and research centre.
2. Gives the **patient-facing address and postcode**, explicitly resolving 313004
   versus 313001, and identifies whether the college correspondence address differs.
3. Confirms or corrects **24.588336, 73.679142** against a marked campus plan or
   hospital entrance. If the institution cannot document the point, obtain an
   on-site check with dated entrance/signage photographs and a recorded GPS point;
   a repeat Google Maps pin alone is insufficient.
4. Confirms the public patient telephone and only those services/hours intended
   for publication. Record the call date, official role and findings, retaining the
   supporting institutional document for the authorized human reviewer.

The immediate next step is **institutional evidence collection**.
No postal correction, coordinate change, location re-approval or identity decision
was made in this evidence-only follow-up.

## Exact existing workflow requirements

These are enforced by the current code, not new approval requirements introduced
by this report:

1. `Hospital.validate_ready_for_review()` requires a nonblank syntactically valid
   telephone; import review must not be NEEDS_REVIEW; duplicate review must be clear.
   `full_clean()` also checks model fields and constraints. An imported record
   cannot evade review by switching to NOT_IMPORTED.
2. Setting `import_review_status=REVIEWED` requires notes of at least **10
   characters**. Only a directory administrator can edit import-review fields.
   Short syntax thresholds are not evidence of a completed real-world review.
3. `services.transition()` allows **DRAFT/REJECTED → PENDING** through SUBMIT,
   followed by **PENDING → VERIFIED** through VERIFY. Direct DRAFT → VERIFIED is
   rejected. The current revision and a reason of at least **3 trimmed characters**
   are required; the API reason is limited to 2,000 characters.
4. A verifier must be active and either superuser or explicitly hold
   `community_care.verify_directory`. The verifier cannot be an active ADMIN
   member of the target hospital. Ordinary staff or manage-directory permission
   alone does not authorize verification. Django Admin additionally needs an
   active staff login. No permissions should be changed merely to finish this task.
5. The code does not enforce different submitter/verifier accounts when the
   verifier is otherwise independent of the hospital; do not claim an enforced
   two-person rule. Human independent review is still necessary for this record.
6. Admin identity status, revision, location status, approved candidate and booking
   are read-only. Decisions use exactly one selected record, its reviewed revision,
   and the decision-reason field. Application-level append-only VerificationAudit
   records the authenticated actor, action, before/after status, reason and revision.
7. Identity verification currently has no separate mandatory evidence-upload field
   or automatic external regulator lookup. Record evidence URLs/findings in import
   notes and decision reason. A persisted VERIFIED flag alone is not independent
   proof that the checks were actually performed.

Relevant implementation: `base.py`, `models.py`, `services.py`, `permissions.py`,
`admin.py`, `duplicate_review.py`, `location_review.py`, `serializers.py`, and
`views.py` in `backend/apps/community_care/`.

## Human checklist and Django Admin instructions

**Do not submit or verify until the unresolved items below are closed.** This task
stops before those live human decisions.

1. Obtain a current original government/college/hospital record or institutional
   confirmation connecting this hospital, its official name and the college at
   the recorded locality. Retain URL/document date, access date, issuer and reviewer.
2. Resolve **313004 versus 313001**, and document whether the hospital address and
   college correspondence address are identical. Confirm the approved point still
   identifies the intended patient facility; do not substitute a city centroid.
3. The August 2026 original confirms publication of 0294-2431900 as the college
   office number. Confirm current connectivity and patient-facing suitability by
   institutional contact; obtain a different hospital contact only with evidence.
4. Retain only service/hours claims independently supported for patients. Empty
   public facilities and blank optional fields are preferable to inferred claims.
   Keep booking disabled and do not create doctors to populate the directory.
5. An authorized reviewer must record completion of source/duplicate checks. This
   record has no duplicate flag requiring a manufactured DISTINCT_CONFIRMED row.

Then use the existing Admin workflow:

1. Sign in at `http://127.0.0.1:8000/admin/` using an **existing authorized staff
   account**. Open **Community Care → Hospitals**, search `UDR-001`, or visit
   `/admin/community_care/hospital/1/change/`.
2. Inspect the imported provenance, current revision, original audit, candidate 61,
   location APPROVED, exact coordinates and booking False. The location workspace
   is `/admin/community_care/hospital/1/location-review/`.
3. Resolve the evidence first. If no geographic correction is required, record a
   checked public source URL, set Import review status to **Reviewed**, and write
   substantive Import review notes. Save once through Admin; this creates an
   audited edit and increments revision. Do not touch the read-only status or
   coordinate/identity fields merely to force the record onto the map.
4. Return to `/admin/community_care/hospital/`. Select **only UDR-001**. Choose
   **Submit selected record for review**. Fill **Reviewed revision (one record
   only)** with the newly reloaded revision and **Decision reason** with the
   evidence summary and source references. Click **Go**. Confirm PENDING and a
   SUBMIT audit with the real reviewer identity.
5. The independently authorized verifier must re-read the record/evidence. Reload
   its new revision; select only UDR-001; choose **Verify selected record after
   independent credential checks**; enter that revision and a meaningful final
   reason; click **Go**. Confirm VERIFIED plus a VERIFY audit. Never reuse the
   pre-edit revision or treat an Admin success banner without checking the record
   as sufficient proof.
6. Inspect **Verification audits** at `/admin/community_care/verificationaudit/`,
   filter record type HOSPITAL / record ID 1. Confirm the pre-existing location
   history remains and new edits/SUBMIT/VERIFY identify the actual authenticated
   decision-makers. Stale revisions, missing evidence or permission denials should
   be resolved by review, not SQL, role changes, `QuerySet.update()` or audit edits.

Suggested notes structure, **to be completed by the human rather than pasted as
an unearned confirmation**:

```text
Reviewer/date and current original evidence URLs/document references:
Facility/college/hospital relationship confirmed by:
Address and postal discrepancy resolution, with evidence:
Telephone evidence and whether it is office or patient contact:
Public service/hours claims retained or deliberately not claimed:
Duplicate check findings:
Location candidate/coordinate checks and any separate re-review reference:
Remaining issues: [must be resolved before final verification]
```

### If a geographic field must be corrected

Editing name/address/city/district/state/postcode/coordinates intentionally marks
an approved location **STALE**, even if the numbers themselves stay the same.
That safeguard must not be suppressed. Candidate 61 and its audit remain history.
Do not change 313004 to 313001 while pretending APPROVED remains current.

After a human-supported correction, use the exact-location workspace's existing
manual **preview then final confirmation** flow with the same coordinates only
if independently reconfirmed. An independent verifier supplies current evidence,
reason, revision and explicit attestations. It creates a new candidate tied to the
current snapshot; link the prior candidate as appropriate. Preserve candidate 61
and its original audit, rather than editing them or manually resetting flags.
Complete source review and identity decisions only after the resulting location
and evidence are consistent. This report does **not** authorize or perform those
conditional edits on the live record.

## Public API and Android verification — initial implementation checks

The following are retained results of the initial Task 4.2D implementation checks,
not new Android/API tests in this evidence-only follow-up:

| Request | Result |
| --- | --- |
| `GET /api/care/hospitals/` | 200, count 0 |
| `GET /api/care/hospitals/?search=Madan` | 200, count 0 |
| `GET /api/care/hospitals/1/` | 404 |
| Nearby at 24.588336, 73.679142, radius 0.01 km | 200, count 0 |
| Nearby at the same approved point, radius 20 km | 200, count 0 |
| Anonymous managed directory / location-candidate list | 401 / 401 |
| Non-finite nearby latitude | 400 |

Public identity eligibility requires VERIFIED + active + acceptable import review
and no blocked duplicate status. **The directory deliberately can list an eligible
verified identity without a map position**, but its public coordinates are null
unless the position is approved, belongs to that hospital and matches exactly.
Nearby additionally requires all those position gates before distance filtering.
Android independently requires VERIFIED + APPROVED and finite in-range coordinates
for a pin. Neither a map position nor an Admin login exposes a DRAFT publicly.

After an actual human decision, rerun the above requests. Expect UDR-001 in search
and in-range nearby, correct name/address, the approved decimal coordinate pair,
distance near zero at that point and booking false. Other DRAFT hospitals must
remain absent. Then verify the real marker/details on Android. Those **real-record
positive-path checks remain blocked**, rather than being substituted with test data.

## Read-only PatientProfile digest investigation — 27 October 2026 follow-up

**The change is real at the aggregate-hash level, but the writer and changed
business fields cannot be established from the available evidence.** No patient
names, IDs, health values, request bodies or per-field hashes were exported. The
investigation compared saved whole-table digests, count and timestamp metadata;
canonical row serialization was used only in memory to reproduce the existing
aggregate hash. All live queries used PostgreSQL **REPEATABLE READ, READ ONLY**.
No profile HTTP endpoint was called.

| Evidence | Finding / limitation |
| --- | --- |
| Saved snapshots | Profile count is **2** throughout. The initial task's before/after hashes differ. Earlier care snapshots have yet another hash, so the evidence does not establish a single change across the entire session. Old snapshots contain aggregate hashes, not field values or row history. |
| Current read-only comparison | Current profile digest **matches `first-hospital-db-after.json`**. This confirms no further aggregate difference at the time of this follow-up comparison; it does not explain the earlier change. |
| Last-save metadata | One existing profile has `updated_at=2026-10-27 05:01:25.212098 UTC` (**10:31:25.212098 IST**). The other profile's last-save timestamp predates this date. A timestamp records a save, not which business values changed or who saved it. |
| Original file timing | Before snapshot file last-written **05:00:03 UTC**; after snapshot **05:08:52 UTC**. The recorded last-save falls between them. Django test log creation **05:03:59 UTC**, Android build log **05:05:09 UTC**, connected-test log **05:05:57 UTC**. These filesystem times support that this save preceded those particular recorded runs; they are not an immutable actor/request audit or a way to exclude every earlier process. |
| Existing model / API | `PatientProfile.updated_at` uses `auto_now=True`; PUT/PATCH call `serializer.save()`. Even a save with unchanged submitted values can change the aggregate digest through the timestamp. GET of an existing profile does not save it; `get_or_create` could create a missing profile, but the snapshots show no net count increase. No claim is made that only the timestamp changed. |
| Existing Android path | Repository save calls the profile PUT endpoint from the profile save flow; profile loading caches locally. This identifies a possible write path, **not evidence that it ran at the relevant time**. |
| PostgreSQL / Admin logs | `log_statement=none`, `log_min_duration_statement=-1`; statement auditing was unavailable. The accessible active PostgreSQL log had **0 profile-table mentions**. No PatientProfile Django Admin log entry was found in **05:00–05:09 UTC**. No persistent Django HTTP access log was located in the workspace. Absence of these logs cannot identify or exclude a writer. |
| Triggers | No custom non-FK trigger was found on the profile table. No logging settings, permissions or schemas were changed to investigate. |

Code inspected: `backend/apps/agentic_reports/models.py:76,105`,
`backend/apps/agentic_reports/views.py:72–97`,
`app/src/main/java/com/example/vedaahar/profile/PatientProfileRepository.kt`, and
the profile screen's save action. The sanitized
[follow-up evidence report](import-reports/first-hospital-evidence-followup.json)
preserves the comparison conclusions and timing without private patient content.
Local detailed metadata remains in `build/profile-metadata-investigation.json`;
raw patient values were not written there.

**Next human action for attribution:** ask the operator whether the profile Save
action was used around **10:31 IST on 27 October 2026**, and preserve any already
retained backend/proxy access logs for roughly **10:23–10:39 IST** (covering the
earlier digest difference too). Look for route/method/time, request ID and an
authorized actor reference, without collecting health payloads, tokens or passwords.
If those logs do not exist, historical actor/field attribution remains unresolved;
do not invent it. No additional logging or application change is part of this task.

The final read-only comparison also found all **7 Community Care model digests**
equal to the initial task's after snapshot, including hospitals, membership,
affiliation, verification audits, geocoding runs, location candidates and duplicate
reviews. UDR-001 is still **DRAFT / NEEDS_REVIEW**, revision **3**, postcode
**313004**, approved candidate **61**, exact approved coordinates unchanged and
booking **False**. No live records were changed by this follow-up.

## Tests, build and data preservation — retained initial-task results

This follow-up changed research documentation only. No Django/Android test, APK
build, install, migration or application workflow was run again. The results below
are the earlier Task 4.2D results, retained for context.

| Check | Result |
| --- | --- |
| Targeted Django tests | **118 passed**, including 8 new first-hospital workflow tests |
| Android unit tests | **168 passed**, 0 failures |
| Connected OnePlus CPH2619 / API 35 | **2 tests passed**: isolated synthetic native-marker flow and live public-empty map flow |
| Debug APK / test APK | Built successfully and installed with `adb install -r`; app data retained |
| Lint | Existing **3 errors, 115 warnings, 1 hint**; no new care findings |
| Hospital, approved candidates, geocoding and verification audits | Before/after model digests match; approved target coordinates unchanged |
| Whole-database comparison | Initial task: **22 of 23** model digests matched; PatientProfile count stayed 2 but its digest changed. The evidence-only follow-up above narrows timing and confirms the current digest matches that after snapshot, but cannot attribute the writer or fields. Do not claim the entire live database was unchanged. |

The existing lint errors are `PublicPdfDownloads.kt:55` (API 29), `themes.xml:8`
(API 29 attribute), and `PatientProfileScreen.kt:955` (ViewModel construction in a
composable). They were not changed or suppressed for this task.

The synthetic fixture is named **Isolated verified hospital fixture** and exists
only in a local MockWebServer response inside `androidTest`. It is not UDR-001
and was never inserted into PostgreSQL. The device test checks one actual native
marker at the fixture coordinates, taps it, checks selection/name/address/details,
rejects a DRAFT/APPROVED fixture, clears the marker on empty search and exercises
loading/error/empty states. Production Kotlin contains no new marker or hospital
fixture. The separate live test asserts zero public hospitals and pins.

The installed debug build retains Task 4.2C's USB backend arrangement:
`http://127.0.0.1:8000/` with `adb reverse tcp:8000 tcp:8000`. The saved LAN URL
was not edited. No GPS permission was granted; coordinate-nearby is the exercised
path. The MapTiler key stayed in ignored configuration and was not printed or
copied into source/reports. The previously documented shared Android/backend-key
configuration remains owner configuration work. No booking, doctor, authentication,
diet, PDF or agentic-AI production change was made. No PostgreSQL reset, migration,
commit or push was performed.

Reproduction (isolated Django test shell, from `backend`):

```powershell
$env:DJANGO_DATABASE_ENGINE='sqlite'
$env:SQLITE_DB_PATH=':memory:'
.\.venv313\Scripts\python.exe manage.py test apps.community_care.test_first_hospital_workflow apps.community_care.test_geocoding apps.community_care.test_exact_location_review apps.community_care.tests --noinput
```

Android build and targeted phone tests (project root, phone unlocked):

```powershell
.\gradlew.bat --offline '-PagenticApiBaseUrl=http://127.0.0.1:8000/' :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest
adb -s 6dea27cd reverse tcp:8000 tcp:8000
adb -s 6dea27cd install -r app/build/outputs/apk/debug/app-debug.apk
adb -s 6dea27cd install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s 6dea27cd shell am instrument -w -r -e verifyFixtureCareMap true -e verifyLiveCareMap true -e class com.example.vedaahar.care.VerifiedHospitalFixtureMapTest,com.example.vedaahar.care.LiveCareMapTest com.vedamrit.test/androidx.test.runner.AndroidJUnitRunner
```

Evidence artifacts:

- [Read-only target/API audit](import-reports/first-hospital-api-audit.json)
- [Source retrieval metadata](import-reports/first-hospital-source-retrieval.json)
- [Evidence-only follow-up, original hashes and sanitized profile investigation](import-reports/first-hospital-evidence-followup.json)
- [Test/build results and APK hash](import-reports/first-hospital-test-results.json)
- APK: `app/build/outputs/apk/debug/app-debug.apk`
- Local logs: `build/first-hospital-django-tests.txt`,
  `build/first-hospital-device-tests.txt`, `build/first-hospital-android-build.txt`,
  `build/first-hospital-lint.txt`

**Actual publication outcome: blocked at evidence resolution and authorized human
approval. UDR-001 remains DRAFT; no real UDR-001 marker was displayed or claimed.**
