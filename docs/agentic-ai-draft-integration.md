# Task 3 — Agentic diet and lifestyle integration

Reviewed and implemented on local branch **agentic-ai-integration**, starting from
173a883. The working tree was clean before this task. No commits, pushes, package
installations, database resets, migrations, assessment scoring changes, model/provider
changes, or navigation/design replacements were performed.

## Outcome and verification boundary

The actual JWT-authenticated Django endpoint executed the real graph, existing CSV
planner, semantic Chroma retrieval, validation and ORM persistence successfully in
an isolated in-memory database. A second actual execution passed with vegan,
peanut-allergy, milk-intolerance and brinjal-avoidance constraints. Android builds,
JVM tests and isolated storage tests on the connected physical phone pass.

**Full phone → running Django → PostgreSQL → AI → phone acceptance remains NOT
VERIFIED.** PostgreSQL was inspected read-only; no synthetic patients/plans were
written there. Task 1 phone login was previously verified by the user. These facts
must not be combined into a claim that the complete live patient workflow passed.

## Initial audit and resulting connections

| Connection | Initial evidence / gap | Implemented behavior and verification |
| --- | --- | --- |
| Android assessments → API | Task 2 already pins three canonical results to an owned assessment ID/version through AgenticPlanRequestMapper and AssessmentSyncCoordinator. | Preserved questions, scoring and sync. Generation also pins the saved profile version and requests seven days. JVM and physical storage tests verify references/account partitioning. |
| API → persisted inputs | services._persisted_engine_payload used DB assessments/profile but coerced unknown lists to empty arrays. No profile freshness check; REST allowed generation without synchronized references. | Require assessment references and profile_version. Preserve unknown versus explicit empty. Ownership/stale-input tests reject before graph invocation. |
| Django → graph | create_wellness_plan already called generate_personalized_plan → graph.invoke; production execution was real. | Retained architecture. Real JWT endpoint tests persist actual generated JSON and leave doctor PENDING. |
| Food planning → safety | Serving bounds were lost; safety used a different candidate subset. Initial minimum portions could make pairs infeasible. | Preserve serving/ingredient/allergen metadata; validate the actual filtered/ranked pool. Redistribute residual calories within bounds and select feasible ranked singletons/pairs. Existing scoring, calorie formula and 10% tolerance remain. |
| Safety → response | Final review metadata could mask diet failures; unusable output could appear usable. | Merge safety/nutrition/review failures. Require seven distinct days, four meals/day, finite nutrients, serving bounds and restrictions. Failed output remains in DB for doctor review but patient meal/lifestyle content is redacted. |
| RAG → lifestyle | Active agents/lifestyle.py already used get_lifestyle_rag_evidence. Index availability was confused with successful retrieval. | Expose actual retrieval/fallback/evidence count. Display real provenance, never missing-metadata placeholders as citations. |
| Plan → Android | Generated meal/lifestyle UI existed, but no auto-restore or durable attempt recovery. | Recover unknown outcomes by UUID; otherwise load latest matching server plan. Check owner and inputs after responses; server flags changes from other devices too. |
| Doctor boundary | Pending content lacked a development-only gate. Doctor APIs and report protection existed. | Valid/current drafts require server DEBUG + flag and debug client opt-in. Release flag is false. No auto-approval; pending final-report access still returns 409. |
| Downloads | Buttons downloaded bundled examples under personalized filenames. | Preserve example downloads, clearly label buttons/files/toasts as examples; no claim they are generated patient reports. |

Primary evidence: Android AgenticPlanViewModel, AgenticPlanRequestMapper,
AgenticPlanStore, AgenticRepository, AgenticPlanPresentation and DietAssessmentScreen;
Django agentic_reports services/generation/serializers/views; engine
integration_service, diet_planning, food_filter, safety, lifestyle and draft_validation.
Unchanged graph.py defines actual node wiring; unchanged models.py defines DB relationships.

## Actual architecture

~~~mermaid
flowchart TD
    A[Android JWT patient session] --> B[Three synchronized canonical assessments]
    B --> C[Mapper: assessment ID/version + profile version]
    C --> D[Patient-scoped durable generation UUID]
    D --> E[Retrofit + AuthInterceptor + JWT refresh]
    E --> F[POST /api/agentic/plans/]
    F --> G[Owned current inputs + claim AgentRun under patient lock]
    G --> H[Persisted profile + AssessmentResult]
    H --> I[Actual graph.invoke]
    I --> J[Canonical adapter + preliminary assessment PDF]
    J --> K[Nutrition + condition context]
    K --> L[CSV filtering/ranking + seven-day planner]
    L --> M[Diet presentation + safety validation]
    M --> N[Lifestyle + existing Chroma semantic retrieval]
    N --> O[Pending review + merged validation]
    O --> P[Recheck input versions under patient lock]
    P --> Q[AgentRun + WellnessPlan JSON; doctor PENDING]
    Q --> R[Development draft serializer gate]
    R --> S[Android days/meals/nutrition/lifestyle]
    E --> T[GET generation status / list / detail]
    T --> Q
    Q --> U[Existing assigned verified-doctor APIs]
    U --> V[Existing approved final-report boundary]
~~~

Failed safety output takes the existing review-response route rather than lifestyle;
the patient gate blocks usable content. AgentRun COMPLETED means execution/persistence
completed, **not** validation passed or doctor approved. Check validation_status separately.

## Retry, freshness and development contract

- Profile reads/saves return stable profile_version. Reload/save the health profile
  online once after installing this update.
- POST plans requires synchronized assessment_id, assessment_version and current
  profile_version. Android sends plan_days=7 and durable UUID generation_id.
  Older clients lacking versions must update.
- generation_id stays optional for legacy callers; duplicate/recovery guarantees
  require reusing it. Updated Android always supplies it. Direct internal legacy
  service calls remain compatible; Android REST cannot silently generate from
  unsynchronized assessment JSON.
- Claiming a UUID under the existing patient row lock reuses a completed plan,
  returns 202 for a running run, or reports a failed run. Changed inputs with the
  same UUID return 409. No extra schema or job queue was introduced.
- GET /api/agentic/generations/<uuid>/ is authenticated/patient-scoped and returns
  run state, plan ID and exact input versions, without raw health data.
- Android retains the UUID after offline/timeouts/unknown outcomes. A confirmed
  failed run is cleared for a later explicit attempt. Repeated taps cannot start
  parallel requests. Unchanged acknowledged attempts remain idempotent.
- Reopening lists the latest matching plan after an acknowledged completion;
  unacknowledged attempts recover first. The Task 2 patient/backend key partitions
  storage. Late responses cannot be saved/displayed across accounts or input changes.
- Profile/assessment writes share the patient lock used by plan publication.
  Recheck versions before commit. Responses expose inputs_current and exact
  versions/run/generation IDs. Superseded pending drafts are hidden even when a
  phone cache is old.
- Only agentic reads increase to 180 seconds; profile reads stay 60 seconds and
  login keeps its existing client. Timeout never proves server generation failed.
- Usable pending drafts require DEBUG=True, AGENTIC_DRAFT_PREVIEW_ENABLED=True,
  and X-VedAmrit-Draft-Preview: 1 from the debug client. Release BuildConfig preview
  is false. DEBUG=False blocks preview even if the header/flag is supplied.
- Label: **AI-Generated Draft — Not Doctor Reviewed**. Doctor status remains
  PENDING. Invalid output is blocked. Review APIs/assignments/report gates remain.
- Client runtime dataset/output-path overrides are ignored; these are server configuration.

## Real execution, RAG and disconnected implementations

Active execution uses deterministic CSV filtering, Ayurvedic ranking, energy
calculation, constrained portions and lifestyle templates. **It does not invoke a
generative LLM.** The finite demo_foods.csv is the actual planning dataset, not a
fallback JSON success response. Clinical accuracy, validated disease treatment and
complete medication/doctor restriction reconciliation are not established.

Condition-specific dietary rules remain incomplete and retain review warnings.
Older agents/lifestyle_agent.py:LifestyleAgent and richer unused RAG protocol
helpers are not the active lifestyle node. Manual approval/final PDF workflow is
separate from patient draft viewing.

Three PDFs exist in backend/agent_engine/rag/books. The configured ayurveda_sources
Chroma collection contains **1,532 embeddings**; legacy langchain is not the active
source. Availability now counts the configured collection via read-only SQLite.

Standalone real semantic search used the existing cached
sentence-transformers/all-MiniLM-L6-v2 and returned three nonempty passages in
18.37 seconds: IJAAR review page label 3 and Charaka Samhita page labels 448/303.
The real graph received five lifestyle evidence matches. Labels are index
metadata, not newly verified printed pagination. Citations retain verified=false;
retrieval alone does not validate recommendations. Missing/failed retrieval shows
conservative fallback without fabricated citations.

Bundled PDFs and constitution-based static guidance remain clearly marked
examples/standard guidance, never successful AI generation.

## Tests and execution results

| Check | Result | Boundary |
| --- | --- | --- |
| Android JVM | 137 passed; zero failures/errors/skips | Includes Task 1/2 regression, gating, 202/errors, retries/timeouts, duplicate taps, account/profile changes and latest restoration. Gateways/HTTP fixtures are test doubles. |
| Django/engine | 126 passed, including two opt-in real graph tests | Isolated SQLite; actual JWT validation/ORM; most API tests mock the engine, opt-in graph tests do not. |
| Real JWT → graph → RAG → ORM | Passed: 7 days, 28 meals, five evidence matches; doctor PENDING | Synthetic fixture, real model/index/CSV, SQLite. Cold runs about 19–27 seconds; cached run about 7 seconds. No production PostgreSQL write or phone interaction. |
| Real restricted-profile graph | Passed | Vegan + peanut allergy + milk intolerance + brinjal avoidance; ingredients checked. Synthetic SQLite fixture. |
| Standalone semantic retrieval | Passed | Existing local model/Chroma, real embeddings. Ingestion/fallback regression tests separately use fixtures. |
| Debug/instrumentation APKs | Passed offline | app/build/outputs/apk/debug/app-debug.apk uses http://127.0.0.1:8000/. |
| Physical phone storage | 8 passed | Current APK installed; test tokens/preferences isolated from patient files. Not full UI/network acceptance. |
| Release guard | Passed | Generated preview=false release / true debug. Missing release HTTPS URL is rejected; positive configuration check used example.invalid as an undeployed test argument. |
| Django system/migrations | Passed; no changes detected | check and makemigrations --check --dry-run; no migrations applied. |
| PostgreSQL | Read-only checks passed | Two profiles, two assessments, two runs, two plans; zero ownership/assessment relationship mismatches. No production writes/deletes. |
| Complete live phone pipeline | NOT VERIFIED | Acceptance procedure below; credentials were not extracted. |

Reproduce Android checks from D:\VedAmrit:

~~~powershell
.\gradlew.bat --offline testDebugUnitTest assembleDebug assembleDebugAndroidTest '-PagenticApiBaseUrl=http://127.0.0.1:8000/'
~~~

Reproduce the Django/engine suite in a separate terminal. SQLite is explicitly
isolated from PostgreSQL, and the fast hasher is an in-process test setting only:

~~~powershell
Set-Location D:\VedAmrit\backend
$env:DJANGO_DATABASE_ENGINE='sqlite'
$env:SQLITE_DB_PATH=':memory:'
$env:HF_HUB_OFFLINE='1'
$env:TRANSFORMERS_OFFLINE='1'
$env:ANONYMIZED_TELEMETRY='False'
$env:AGENT_ENGINE_REPORT_DIR='tmp_agent_engine_reports/task3_tests'
$env:RUN_AGENTIC_REAL_SMOKE='1'
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
          'apps.agentic_reports.test_draft_generation', 'apps.authentication.tests']
labels += ['agent_engine.tests.' + name for name in (
    'test_draft_validation', 'test_integration_service', 'test_assessment_adapter',
    'test_lifestyle_personalization', 'test_report_security', 'test_report_validation',
    'test_rag_ingestion', 'test_two_report_workflow')]
raise SystemExit(bool(DiscoverRunner(verbosity=1, interactive=False).run_tests(labels)))
'@ | .\.venv\Scripts\python.exe -B -
~~~

Real-smoke PDFs are preliminary synthetic assessment artifacts in ignored test
output, not approved final reports. Original print-only engine demos were inspected
but not all run as a test suite. No PostgreSQL multiworker concurrency/load, release
deployment, live doctor/signature/PDF delivery, or complete phone UI acceptance ran.

## Physical-phone acceptance: exact PowerShell commands

Terminal 1, retaining existing PostgreSQL .env credentials:

~~~powershell
Set-Location D:\VedAmrit\backend
$env:DJANGO_DATABASE_ENGINE='postgresql'
$env:DJANGO_DEBUG='True'
$env:DJANGO_ALLOWED_HOSTS='localhost,127.0.0.1,10.0.2.2'
$env:AGENTIC_DRAFT_PREVIEW_ENABLED='True'
$env:HF_HUB_OFFLINE='1'
$env:TRANSFORMERS_OFFLINE='1'
$env:ANONYMIZED_TELEMETRY='False'
.\.venv\Scripts\python.exe manage.py check
.\.venv\Scripts\python.exe manage.py runserver 127.0.0.1:8000 --noreload
~~~

Offline flags use the existing model cache verified here; no new model/provider
or index rebuild is needed. Terminal 2, with USB debugging authorized:

~~~powershell
Set-Location D:\VedAmrit
$taskAdb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
& $taskAdb devices
& $taskAdb reverse tcp:8000 tcp:8000
& $taskAdb reverse --list
.\gradlew.bat --offline installDebug '-PagenticApiBaseUrl=http://127.0.0.1:8000/'
& $taskAdb shell am start -n com.vedamrit/com.example.vedaahar.MainActivity
~~~

If multiple devices appear, set $env:ANDROID_SERIAL to the intended listed serial.
USB reverse maps phone loopback to the PC; no machine-specific LAN IP is hardcoded.
Emulator default without the Gradle property is http://10.0.2.2:8000/. Production
remains HTTPS-only with preview disabled.

1. Sign in. Reload/save the health profile online and explicitly answer safety
   fields: empty arrays mean none known; null means unanswered. Do not invent
   answers to bypass validation.
2. Complete/restore all three assessments; confirm SYNCED and server ID/version.
3. Generate once. Expect loading, the exact draft label, validation and PENDING.
4. Select all seven days. Verify four meals, portions/daily nutrition, actual
   lifestyle and source/fallback status. Failed validation must hide usable output.
5. Refresh/reopen: latest matching plan restores. Repeated Generate taps must not
   create parallel/duplicate runs for the saved attempt.
6. Disconnect/reconnect during a request, restore ADB reverse, Refresh/reopen to
   recover running/completed work. If never received, retry Generate. Check counts.
7. Change/sync an assessment or save profile changes: older versions must not
   masquerade as current. Switch accounts and confirm local/server plan isolation.
8. Final-report access stays unavailable pending approval. Example downloads must
   be clearly identified and never mistaken for approved patient reports.

Django terminal logs show POST plans, GET generation/list/detail and relevant
400/409/401/502 responses. Do not add JWT/profile logging. Terminal 3 safely inspects
database identifiers, versions and statuses without printing health answers:

~~~powershell
Set-Location D:\VedAmrit\backend
$env:DJANGO_DATABASE_ENGINE='postgresql'
@'
import os
os.environ['DJANGO_SETTINGS_MODULE'] = 'vedaahar_backend.settings'
import django
django.setup()
from django.db import connection, transaction
from apps.agentic_reports.models import PatientProfile, AssessmentResult, AgentRun, WellnessPlan
assert connection.vendor == 'postgresql'
with transaction.atomic():
    with connection.cursor() as cursor:
        cursor.execute('SET TRANSACTION READ ONLY')
    print({m.__name__: m.objects.count() for m in (PatientProfile, AssessmentResult, AgentRun, WellnessPlan)})
    for plan in WellnessPlan.objects.select_related('assessment', 'agent_run').order_by('-pk')[:5]:
        runtime = (plan.agent_run.request_payload.get('runtime') or {}) if plan.agent_run_id else {}
        print({'plan_id': plan.pk, 'assessment_id': plan.assessment_id, 'agent_run_id': plan.agent_run_id,
               'validation': plan.validation_status, 'doctor': plan.doctor_status,
               'assessment_version': runtime.get('assessment_version'), 'profile_version': runtime.get('profile_version'),
               'generation_id': runtime.get('generation_id')})
'@ | .\.venv\Scripts\python.exe -B -
~~~

Do not reset/migrate the database for acceptance.

## Remaining blockers and risks

- Full live phone/PostgreSQL acceptance remains outstanding. Deployment must also
  verify multiworker locking and infrastructure timeouts against the client allowance.
- Synchronous generation remains. A killed worker or database outage during failure
  recording can leave an orphaned RUNNING run. Recovery reports pending rather
  than launching duplicates. An operator must establish the original worker stopped
  before marking it failed; job queues/leases/schema changes require separate approval.
- Clinical adequacy, dataset coverage, food labels and condition/medication/doctor
  restrictions still need practitioner review. Impossible constraints fail closed.
- Generated JSON restores online. Offline errors preserve references/attempts but
  cannot fetch server content. Old plans without version/validation eligibility are
  conservatively hidden; generate from current data.
- Existing approved-report endpoint exposes metadata/reference. Task 3 does not
  implement a new signed PDF delivery service or replace doctor approval checks.

Task 3 is ready for acceptance. Stop here; Task 4 requires approval.

## Suggested GitHub commit

Title: **Integrate validated agentic diet drafts with versioned inputs and retry recovery**

~~~text
Connect patient draft viewing to the existing graph while preserving pending
doctor review and protected final reports.

Pin generation to synchronized assessment and persisted profile versions.
Preserve unknown safety values, reject stale/foreign inputs, recheck before
publication and hide superseded drafts. Persist patient-scoped generation UUIDs,
recover running/completed attempts after timeouts and restore the latest matching
server plan without duplicate runs.

Preserve scoring and validation thresholds while carrying serving/restriction
metadata, fitting feasible portions, validating the actual candidate pool and
propagating safety failures. Expose real RAG retrieval/fallback and restrict
usable content to validated development preview. Label bundled PDFs as examples.

Validation: 137 JVM tests; 126 Django/engine tests including actual JWT/graph/RAG
and restricted-profile SQLite runs; 8 phone storage tests; offline debug APK;
release preview guard; Django checks; read-only PostgreSQL lineage checks.
Full phone-to-PostgreSQL AI acceptance and multiworker load remain outstanding.
No schema migration or data reset.
~~~

## Exact modified/new files


- app/build.gradle.kts
- app/src/androidTest/java/com/example/vedaahar/assessment/AssessmentPatientStoreTest.kt
- app/src/main/java/com/example/vedaahar/DietAssessmentScreen.kt
- app/src/main/java/com/example/vedaahar/agentic/AgenticPlanPresentation.kt
- app/src/main/java/com/example/vedaahar/agentic/AgenticPlanRequestMapper.kt
- app/src/main/java/com/example/vedaahar/agentic/AgenticPlanStore.kt
- app/src/main/java/com/example/vedaahar/agentic/AgenticPlanViewModel.kt
- app/src/main/java/com/example/vedaahar/agentic/AgenticRepository.kt
- app/src/main/java/com/example/vedaahar/network/AgenticApi.kt
- app/src/main/java/com/example/vedaahar/network/ApiClient.kt
- app/src/main/java/com/example/vedaahar/network/dto/AgenticDtos.kt
- app/src/main/java/com/example/vedaahar/profile/PatientProfileRepository.kt
- app/src/main/java/com/example/vedaahar/profile/PatientProfileStore.kt
- app/src/test/java/com/example/vedaahar/agentic/AgenticPlanPresentationTest.kt
- app/src/test/java/com/example/vedaahar/agentic/AgenticPlanViewModelTest.kt
- app/src/test/java/com/example/vedaahar/agentic/AgenticRepositoryTest.kt
- app/src/test/java/com/example/vedaahar/agentic/ViewModelFactoryTest.kt
- app/src/test/java/com/example/vedaahar/assessment/AssessmentRepositoryTest.kt
- backend/agent_engine/agents/diet_planning.py
- backend/agent_engine/agents/food_filter.py
- backend/agent_engine/agents/lifestyle.py
- backend/agent_engine/agents/safety.py
- backend/agent_engine/integration_service.py
- backend/agent_engine/services/draft_validation.py
- backend/agent_engine/services/report_validation.py
- backend/agent_engine/tests/test_draft_validation.py
- backend/apps/agentic_reports/generation.py
- backend/apps/agentic_reports/serializers.py
- backend/apps/agentic_reports/services.py
- backend/apps/agentic_reports/test_assessment_sync.py
- backend/apps/agentic_reports/test_draft_generation.py
- backend/apps/agentic_reports/tests.py
- backend/apps/agentic_reports/urls.py
- backend/apps/agentic_reports/views.py
- backend/vedaahar_backend/settings.py
- docs/agentic-ai-draft-integration.md
