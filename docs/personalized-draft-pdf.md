# Task 3.1 — Personalized draft PDF generation, download and viewing

Branch: **agentic-ai-integration**. The working tree was clean at the start.
No commits/pushes, database reset, migration, assessment scoring changes, AI algorithm
changes, package installations or fabricated approvals were performed.

## Root causes reported before workflow changes

1. DietAssessmentScreen.downloadAssessmentReport calls downloadBundledPdf with
   app/src/main/assets/AI_Assessment_Report_1.pdf. Its output filename is explicitly
   Example-Ayurvedic-Assessment.pdf. This is a bundled example, not a network download.
2. downloadDietPlan similarly copies the bundled
   Final_Personalized_Wellness_Report_.pdf as Example-Diet-Plan.pdf. Neither handler
   reaches Django, AuthInterceptor or a patient-specific report resource.
3. AgenticPlanViewModel.loadFinalReport → AgenticRepository.finalReport calls
   GET /api/agentic/plans/<id>/report/. AgenticPlanReportView enforces genuine doctor
   approval and returns FinalReport metadata/reference JSON, not application/pdf
   bytes. Pending plans correctly receive 409. This protection was not the defect.
4. The graph already creates preliminary assessment PDFs through
   agent_engine/services/two_report_pdf.py. They are not the saved seven-day
   diet/lifestyle export and had no authenticated Android download connection.
5. The latest persisted plan inspected read-only (plan #3) had REVIEW_REQUIRED,
   doctor PENDING, no failed checks, valid diet validation and PASS for food data,
   nutrition, meal structure, Ayurvedic evidence and patient constraints. RAG was retrieved.

Its exact single warning was:

> REVIEW REQUIRED: significant dietary restrictions is unknown; record an explicit none or not applicable when confirmed.

agent_engine/services/report_validation.py asks for significant_dietary_restrictions,
but PatientProfile, its API serializer and the agent payload mapper do not define
that field. Saved food_intolerances, foods_to_avoid and doctor_restrictions already
carry the corresponding modeled restriction context.

For this plan, the warning is an export-schema mismatch rather than a failed
nutrition check. This is **not clinical clearance**. The export reconciles only
that exact legacy warning, only with explicitly answered saved context and passing
safety checks. It preserves the warning/status in the PDF and database. It does
not change the validator, assume other unknown fields mean none, or blanket-allow
REVIEW_REQUIRED. Clinical, missing-evidence or unrecognized warnings block export.

## Implemented API

**GET /api/agentic/plans/<plan_id>/draft-pdf/**

Required: valid patient JWT, ownership, server DEBUG=True,
AGENTIC_DRAFT_PREVIEW_ENABLED=True and X-VedAmrit-Draft-Preview: 1.
The existing debug client supplies the header; release preview remains disabled.

The endpoint renders from the persisted WellnessPlan diet/lifestyle JSON,
AssessmentResult and frozen AgentRun patient-profile/input versions. It never
invokes the graph, regenerates a plan, changes doctor status or creates FinalReport.

Success returns real PDF bytes with:

~~~text
Content-Type: application/pdf
Content-Disposition: attachment; filename="VedAmrit-Draft-Plan-<id>-v<version>.pdf"
Cache-Control: private, no-store
X-Content-Type-Options: nosniff
~~~

Response boundaries:

| Result | HTTP |
| --- | --- |
| Missing/expired JWT | 401 |
| Development preview disabled or missing opt-in | 403 |
| Another patient's/missing plan | 404 |
| Stale lineage, failed safety, unresolved review, rejected/changed plan | 409 plus actionable reasons |
| Unsupported font characters | 422 plus a configuration message |
| Eligible pending draft | 200 application/pdf |

Eligibility is exposed on plan detail/generation responses as draft_pdf_available
and draft_pdf_block_reasons. The PDF gate requires a completed owned run, matching
assessment/profile snapshots and current server inputs, doctor PENDING, structural
validation, actual diet-validation PASS checks and explicit safety-context fields.
Clinical/medicine restrictions and lifestyle safety flags require review.
REVIEW_REQUIRED with no reason or an unrecognized reason is blocked. Eligibility is
rechecked after rendering to catch concurrent profile/assessment/review changes.

Repeated GETs render deterministic bytes from saved data without an AI run.
No filesystem path or public media URL is exposed. Existing doctor endpoints and
GET /report/ approval protection remain unchanged.

## PDF content and rendering

The report contains VedAmrit branding, patient/assessment summary, plan/assessment/
run references, exact input versions, plan generation date, validation/warnings,
recorded restrictions, seven days with breakfast/lunch/snack/dinner, portions,
available meal/daily nutrition, saved lifestyle recommendations and actual retrieved
source/page metadata. Missing nutrition is labeled rather than invented.

Every page says **AI-GENERATED DRAFT — NOT DOCTOR REVIEWED**.
Lifestyle advice stays with its safety note/source across page breaks. References
are marked as retrieved evidence for review, not verification of recommendations.

ReportLab and its bundled TrueType font are already installed. No provider, model
or dependency was added. A synthetic real-graph report produced 10 pages and 28
meal headings. Pages were rendered with Windows' built-in PDF renderer and visually
inspected; text/disclaimer/meal counts were checked with pypdf. QA PDFs/images live
only in ignored backend/tmp_agent_engine_reports; they are not shipped as patient
reports or substituted for API output.

## Android flow

DietAssessmentScreen now includes a separate Personalized AI Draft PDF section:

1. Download Personalized Diet PDF calls DraftPdfRepository through the existing
   authenticated OkHttp factory, including JWT refresh and frozen account identity.
2. It checks MIME type/PDF signature, bounds the download to 20 MiB, and displays
   loading, success and actionable auth/network/validation/storage errors.
3. PatientPdfStore atomically saves bytes in app-private no-backup storage under a
   patient/backend hash. Metadata is patient-scoped. Filenames come from plan/version
   references, never a server-supplied path.
4. Open Downloaded PDF copies to a narrow share-cache root and launches ACTION_VIEW
   with application/pdf, a content URI and read-only permission. No external storage
   permission or file:// URI is used.
5. No-viewer devices retain the download and receive an install-viewer/reopen message.
6. Recreating the app/store reloads the latest saved reference. Local assessment/profile
   changes block stale opening; switching patients blocks saving/opening and provider
   access. Known server ineligibility also disables opening that plan.
7. PatientPdfProvider checks account ownership on query, MIME lookup and file open;
   draft roots reject writes and path traversal. Existing camera_documents URI behavior
   delegates to FileProvider unchanged.

The source file is in noBackupFilesDir, not public Downloads. Uninstall/clear-data
removes private reports. A viewer may retain already-read content in its own cache;
the app cannot recall that copy. Offline cached reports are dated snapshots and
cannot discover a remote review change until the app refreshes online.

Example Assessment/Diet PDF buttons and assets remain explicitly examples.
Assessment result screens, navigation and scoring are preserved. Use the new
Personalized AI Draft PDF section for the saved diet/lifestyle document.

## Tests and verification

| Check | Result / boundary |
| --- | --- |
| Android JVM | **141 passed**, zero failures/errors/skips. Existing regression plus authenticated PDF requests, visible review/JWT errors, non-PDF rejection, timeout/network handling. HTTP fixtures are mocked. |
| Django/engine | **136 passed**, including 10 PDF tests. Actual JWT/ORM in isolated SQLite; most fixtures mock engine generation. |
| Real graph PDF smoke | **Passed** actual graph → semantic RAG → persisted plan → PDF, synthetic profile in SQLite, 10 pages/28 meals/PENDING, no FinalReport. |
| Safety/ownership | Tests block clinical/unknown warnings, FAIL, missing review reasons, broken meal structure, stale inputs, concurrent profile change, wrong patient, expired JWT and non-development requests. |
| Repeat download | Identical PDF bytes, unchanged AgentRun/WellnessPlan counts, no graph call, pending final-report access still 409. |
| Physical phone | **13 tests passed on the earlier build**, including private save/reopen, Android PdfRenderer, content URI/read grant, cross-account blocking, stale-local blocking, no-viewer handling and Task 2 storage regression. Fixture files/preferences were isolated. Viewer launch intent was captured rather than opening a real patient document. |
| Final physical rerun | Latest **14-test suite built**, but final install/rerun was blocked after USB disconnected and adb listed no devices. The extra traversal/write/foreign-metadata regression and final hardening have not been rerun on hardware. |
| APK | Latest debug and instrumentation APK builds passed offline; base URL http://127.0.0.1:8000/. Final APK installation after the disconnect is outstanding. |
| PDF layout | Final 10-page real-graph fixture rendered/inspected; every page disclaimer and all 28 headings verified. No production patient PDF was used as the QA artifact. |
| Django/DB checks | check passed; makemigrations --check --dry-run found no changes. PostgreSQL inspected read-only: 3 plans, 3 runs, 0 FinalReports; latest plan passed new PDF eligibility. |
| Full live patient tap/download/view | **Not verified** with the final build. Follow steps below after reconnecting. |
| Git | diff --check passed; nothing committed/pushed. |

Reproduce Android checks:

~~~powershell
Set-Location D:\VedAmrit
.\gradlew.bat --offline testDebugUnitTest assembleDebug assembleDebugAndroidTest '-PagenticApiBaseUrl=http://127.0.0.1:8000/'
~~~

Run the full isolated Django suite (never point tests at the patient database):

~~~powershell
Set-Location D:\VedAmrit\backend
$env:DJANGO_DATABASE_ENGINE='sqlite'
$env:SQLITE_DB_PATH=':memory:'
$env:RUN_AGENTIC_REAL_SMOKE='1'
$env:HF_HUB_OFFLINE='1'
$env:TRANSFORMERS_OFFLINE='1'
$env:ANONYMIZED_TELEMETRY='False'
$env:AGENT_ENGINE_REPORT_DIR='tmp_agent_engine_reports/task3_1_tests'
@'
import os, sys
os.environ['DJANGO_SETTINGS_MODULE'] = 'vedaahar_backend.settings'
import django
django.setup()
from django.conf import settings
settings.PASSWORD_HASHERS = ['django.contrib.auth.hashers.MD5PasswordHasher']
sys.path.insert(0, 'agent_engine/tests')
from django.test.runner import DiscoverRunner
labels = ['apps.agentic_reports.tests', 'apps.agentic_reports.test_assessment_sync',
          'apps.agentic_reports.test_draft_generation', 'apps.agentic_reports.test_draft_pdf',
          'apps.authentication.tests']
labels += ['agent_engine.tests.' + name for name in (
    'test_draft_validation', 'test_integration_service', 'test_assessment_adapter',
    'test_lifestyle_personalization', 'test_report_security', 'test_report_validation',
    'test_rag_ingestion', 'test_two_report_workflow')]
raise SystemExit(bool(DiscoverRunner(verbosity=1, interactive=False).run_tests(labels)))
'@ | .\.venv\Scripts\python.exe -B -
~~~

The test-only fast hasher and SQLite settings apply only to that process. They are
not application configuration changes.

## Physical-device acceptance

**Restart Django** to load the new endpoint/serializer. In Terminal 1, preserve
existing PostgreSQL credentials and do not reset/migrate the database:

~~~powershell
Set-Location D:\VedAmrit\backend
$env:DJANGO_DATABASE_ENGINE='postgresql'
$env:DJANGO_DEBUG='True'
$env:DJANGO_ALLOWED_HOSTS='localhost,127.0.0.1,10.0.2.2'
$env:AGENTIC_DRAFT_PREVIEW_ENABLED='True'
.\.venv\Scripts\python.exe manage.py check
.\.venv\Scripts\python.exe manage.py runserver 127.0.0.1:8000 --noreload
~~~

Terminal 2, after reconnecting and authorizing USB debugging:

~~~powershell
Set-Location D:\VedAmrit
$taskAdb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
& $taskAdb devices
& $taskAdb reverse tcp:8000 tcp:8000
.\gradlew.bat --offline installDebug '-PagenticApiBaseUrl=http://127.0.0.1:8000/'
& $taskAdb shell am start -n com.vedamrit/com.example.vedaahar.MainActivity
~~~

If multiple devices are listed, set ANDROID_SERIAL to the intended listed serial.
Emulators may use the existing default http://10.0.2.2:8000/ instead. Release HTTPS
settings are unchanged.

1. Sign in with the same patient. Open the diet screen and Refresh the persisted
   plan; **do not generate a new AI plan just to obtain a PDF**.
2. In Personalized AI Draft PDF, tap Download Personalized Diet PDF. The reported
   single legacy warning may remain REVIEW_REQUIRED; it must be printed and explained.
   Any other safety/quality review reason must produce a visible blocked message.
3. Tap Open Downloaded PDF. Verify the patient/plan/assessment references, generation
   date, seven days/four meals, nutrition, lifestyle, RAG references and draft header
   on every page. Doctor status stays PENDING; final report stays restricted.
4. Download again and confirm the same plan ID and unchanged run/plan counts.
   Reopen the app and use Open Downloaded PDF. Missing viewers should leave the
   file saved and show the actionable message.
5. Switch accounts: the prior patient's report must not appear/open. Change local
   profile/assessment data: the stale PDF must be refused until a current plan is saved.
6. Example downloads must remain visibly separate from the personalized action.
   Inspect Django for GET /api/agentic/plans/<id>/draft-pdf/ with 200 application/pdf,
   or the expected 401/403/404/409. Do not log JWTs or raw patient answers.

To run the final phone regression suite:

~~~powershell
.\gradlew.bat --offline assembleDebugAndroidTest '-PagenticApiBaseUrl=http://127.0.0.1:8000/'
& $taskAdb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $taskAdb shell am instrument -w -r -e class com.example.vedaahar.agentic.PatientPdfStoreTest,com.example.vedaahar.assessment.AssessmentPatientStoreTest com.vedamrit.test/androidx.test.runner.AndroidJUnitRunner
~~~

## Remaining issues

- Final hardware installation/14-test rerun and actual patient/viewer acceptance
  remain blocked by the disconnected phone. No full final-build device claim is made.
- Installed third-party viewer behavior is not exhaustively tested. Intent/URI and
  Android rendering are tested with isolated fixtures; no-viewer behavior is simulated.
- Unsupported characters in the bundled PDF font fail explicitly with 422; adding a
  broader language font is separate work, not silent transliteration or sample fallback.
- The existing approved final-report route remains metadata/approval gated. This task
  adds a pending development draft, not a signed/approved final medical PDF service.
- Offline cached snapshots cannot know subsequent remote review decisions. Refresh
  online for current eligibility; local input changes still prevent stale opening.

## Suggested GitHub commit

Title: **Add patient-scoped personalized draft PDF download and viewing**

~~~text
Render development draft PDFs directly from persisted wellness plans, assessments,
profile versions and retrieved source metadata. Return authenticated private
application/pdf responses without regenerating AI or creating FinalReport.

Preserve every-page unreviewed labeling and pending doctor status. Reconcile only
the proven legacy restriction-schema warning with explicitly answered context;
block clinical/unrecognized warnings, invalid safety results and stale lineage.
Recheck eligibility after rendering and expose actionable PDF eligibility metadata.

Connect Android personalized download/open actions to the JWT client. Persist PDFs
privately by patient/backend, restore the latest saved reference, use guarded
read-only content URIs, and handle storage/network/auth/no-viewer failures.
Keep example PDFs and approved report protection separate.

Validation: 141 JVM tests; 136 Django/engine tests including real graph/RAG/PDF
execution in isolated SQLite; rendered PDF layout checks; offline APK builds;
read-only PostgreSQL/system/migration checks. Thirteen earlier phone tests passed;
the final fourteen-test hardware rerun and final-build patient acceptance remain
outstanding after USB disconnected. No schema, scoring or AI algorithm changes.
~~~

## Exact modified/new files


- app/src/androidTest/java/com/example/vedaahar/agentic/PatientPdfStoreTest.kt
- app/src/main/AndroidManifest.xml
- app/src/main/java/com/example/vedaahar/DietAssessmentScreen.kt
- app/src/main/java/com/example/vedaahar/agentic/DraftPdfRepository.kt
- app/src/main/java/com/example/vedaahar/agentic/PatientDraftPdfActions.kt
- app/src/main/java/com/example/vedaahar/agentic/PatientPdfStore.kt
- app/src/main/java/com/example/vedaahar/network/DraftPdfApi.kt
- app/src/main/java/com/example/vedaahar/network/dto/AgenticDtos.kt
- app/src/main/res/xml/file_paths.xml
- app/src/test/java/com/example/vedaahar/agentic/DraftPdfRepositoryTest.kt
- backend/apps/agentic_reports/draft_pdf.py
- backend/apps/agentic_reports/serializers.py
- backend/apps/agentic_reports/test_draft_pdf.py
- backend/apps/agentic_reports/urls.py
- backend/apps/agentic_reports/views.py
- docs/personalized-draft-pdf.md
