# Assessment synchronization (Task 2)

Questions, answer IDs, scoring formulas, and routes are unchanged. A small sync
status/retry section is added to the existing assessment and diet screens.

## Local ownership and durability

- Prakriti, Vikriti, Agni, questionnaire drafts, supporting dosha/symptom history,
  the planner's profile cache, and latest plan ID are partitioned by backend URL
  and JWT patient ID. JWT decoding determines a local namespace only; Django
  remains responsible for authentication and authorization.
- Old preferences without a provable owner are retained but never adopted by the
  next person who logs in. Restore the authenticated patient's server assessment
  or retake the assessments. No local files or PostgreSQL records are deleted.
- Unfinished answers and a complete canonical outbox snapshot are persisted.
  Completing all three mandatory assessments makes the snapshot ready to upload.
  Starting an edit blocks planning and prevents restoration over that edit.
- Keep the backend URL stable while unsynced data exists. Changing the URL selects
  another namespace; pending data remains under the original URL/patient.

## States and retries

`PENDING -> SYNCING -> SYNCED` or `FAILED` is visible on screen. Failed requests
retain results and their submission ID. Retry uses the same ID; an interrupted
`SYNCING` record is retried safely when the screen is reopened. There is one
bounded automatic attempt when opening the screen or completing an assessment,
plus the explicit retry control. There is no background scheduler or infinite
retry loop.

Uploads require a complete Prakriti/Vikriti/Agni payload and an authenticated
patient. Account-bound HTTP clients reject a request if another patient's JWT
replaces the original session before dispatch. A late upload/restore response
cannot acknowledge or overwrite newer local edits or populate another account.
JWT rejection, timeout, offline, invalid payload, and backend failures remain
visible. Sign back into the original account before retrying expired sessions.

## Additive API fields; no migration

- `POST /api/agentic/assessments/` retains the existing canonical payload and
  accepts an optional UUID `submission_id`. New Android submissions always send
  it. First creation returns 201; an identical retry returns the same row with
  200. Reusing that ID for different content returns 409.
- A PostgreSQL transaction locks the authenticated user's row to serialize retry
  lookup/creation. The submission ID is stored in existing `canonical_payload`
  JSON. It is scoped to that patient, never a submitted `user_id`.
- Assessment responses add `submission_id` and `assessment_version`. The latter
  is a SHA-256 content version of all three results, independent of JSON object
  key order, integral float formatting, and omitted optional null fields.
- `GET /api/agentic/assessments/latest/` returns the authenticated patient's latest
  persisted result. Clean local caches may restore it. Dirty or partially answered
  local assessments always take precedence. Older incomplete server payloads that
  cannot be decoded as native Android results produce a visible error and require
  a retake; no scores are recalculated during restoration.

## Exact planner inputs

New Android plan requests require `SYNCED` local results and send both
`assessment_id` and `assessment_version`. Django checks ownership, latest-row
identity, content version, and any submitted assessment body. Stale or conflicting
inputs return 409 before invoking AI. Legacy callers retain their payload format,
but supplied results can no longer be silently replaced with different server
results. The existing direct-payload fallback remains available for legacy callers
without persisted data; Android uses the explicit synced-reference path.

The assessment reference is recorded in `AgentRun.assessment`, its request runtime
metadata, the wellness-plan foreign key/assessment summary, and response fields.
Django rechecks freshness while committing the completed plan, without holding a
row lock during AI execution. An intervening assessment change marks the run failed
and returns 409 instead of publishing an old plan. Android verifies the returned
reference and displays its ID/version. Agent-engine algorithms are unchanged.

## Verification on a physical phone

Use the existing working Task 1 URL, PostgreSQL `.env`, and authenticated account.
Restart Django with the updated code; no migration/reset is required. Rebuild and
install the debug APK, retaining application data. For USB reverse, the commands
and URL setup are in the repository README.

1. Complete Prakriti, Vikriti, and Agni. Verify individual results are unchanged,
   the status reaches `SYNCED`, and an assessment ID/version is shown.
2. Stop Django or disconnect networking. Retake an assessment. Verify `FAILED`
   with an actionable error and retained answers. Restart/reconnect and use retry.
   Retry again: PostgreSQL must still contain one row for that submission ID.
3. Close the app during an unfinished questionnaire and reopen it. Verify the
   patient's selected answers resume. New local edits must survive a restore action.
4. Log out and sign in as a second patient. Verify the first patient's results,
   drafts, profile cache, and latest plan ID are not shown or submitted. Return to
   the first account and verify its pending results remain available.
5. On an account with persisted results and no new local edits, choose restore.
   Verify all three results reappear without a new assessment POST.
6. Generate a plan. Compare the displayed assessment ID/version with assessment
   detail/latest response, `WellnessPlan.assessment_id`, and the associated
   `AgentRun.request_payload.runtime` fields. They must match exactly.
7. Save a newer assessment from another session before generating with an older
   reference. Expect HTTP 409, no stale plan, and an instruction to restore/sync.

## Automated checks

JVM tests cover canonical POST/JWT contracts, retry IDs, timeout/offline/validation
and auth failures, process interruption, account-switch races, late responses,
restore precedence, and returned-plan reference checks. Django tests run against
an in-memory database and cover actual JWT authentication, idempotency, ownership,
canonical persistence, stale references/content, exact agent input, and assessment
changes during generation. Live PostgreSQL concurrent-worker load and the complete
phone-to-AI flow still require deployment acceptance checks.

Device preference tests isolate *all* preferences, including fixture JWTs, away
from the patient's real files:

```powershell
.\gradlew.bat --offline testDebugUnitTest assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r -e class com.example.vedaahar.assessment.AssessmentPatientStoreTest com.vedamrit.test/androidx.test.runner.AndroidJUnitRunner
```

Do not run a test runner against the development database directly. Configure an
in-memory SQLite test database before calling Django's `DiscoverRunner`, as used
for this task, or use a separately provisioned PostgreSQL test database.

## Modified files

- `app/src/androidTest/java/com/example/vedaahar/assessment/AssessmentPatientStoreTest.kt`
- `app/src/main/java/com/example/vedaahar/DietAssessmentScreen.kt`
- `app/src/main/java/com/example/vedaahar/agentic/AgenticPlanRequestMapper.kt`
- `app/src/main/java/com/example/vedaahar/agentic/AgenticPlanStore.kt`
- `app/src/main/java/com/example/vedaahar/agentic/AgenticPlanViewModel.kt`
- `app/src/main/java/com/example/vedaahar/agentic/AgenticRepository.kt`
- `app/src/main/java/com/example/vedaahar/agni/AgniResultStore.kt`
- `app/src/main/java/com/example/vedaahar/assessment/AssessmentDraftStore.kt`
- `app/src/main/java/com/example/vedaahar/assessment/AssessmentProfileStore.kt`
- `app/src/main/java/com/example/vedaahar/assessment/AssessmentRepository.kt`
- `app/src/main/java/com/example/vedaahar/assessment/AssessmentSyncCoordinator.kt`
- `app/src/main/java/com/example/vedaahar/assessment/AssessmentSyncSection.kt`
- `app/src/main/java/com/example/vedaahar/assessment/AssessmentSyncStore.kt`
- `app/src/main/java/com/example/vedaahar/assessment/CanonicalAssessment.kt`
- `app/src/main/java/com/example/vedaahar/dosha/DoshaAssessmentViewModel.kt`
- `app/src/main/java/com/example/vedaahar/dosha/DoshaResultStore.kt`
- `app/src/main/java/com/example/vedaahar/network/ApiClient.kt`
- `app/src/main/java/com/example/vedaahar/network/AuthInterceptor.kt`
- `app/src/main/java/com/example/vedaahar/network/PatientSession.kt`
- `app/src/main/java/com/example/vedaahar/network/dto/AgenticDtos.kt`
- `app/src/main/java/com/example/vedaahar/prakriti/PrakritiResultStore.kt`
- `app/src/main/java/com/example/vedaahar/profile/PatientProfileRepository.kt`
- `app/src/main/java/com/example/vedaahar/profile/PatientProfileStore.kt`
- `app/src/main/java/com/example/vedaahar/symptoms/SymptomsAnalysisStore.kt`
- `app/src/main/java/com/example/vedaahar/vikriti/VikritiResultStore.kt`
- `app/src/test/java/com/example/vedaahar/agentic/AgenticRepositoryTest.kt`
- `app/src/test/java/com/example/vedaahar/assessment/AssessmentHttpTest.kt`
- `app/src/test/java/com/example/vedaahar/assessment/AssessmentSyncCoordinatorTest.kt`
- `app/src/test/java/com/example/vedaahar/network/PatientSessionTest.kt`
- `backend/apps/agentic_reports/assessment_sync.py`
- `backend/apps/agentic_reports/serializers.py`
- `backend/apps/agentic_reports/services.py`
- `backend/apps/agentic_reports/test_assessment_sync.py`
- `backend/apps/agentic_reports/tests.py`
- `backend/apps/agentic_reports/views.py`
- `docs/assessment-synchronization.md`
