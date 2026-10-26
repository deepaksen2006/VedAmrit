# Task 3.3: Personalized Diet screen redesign

Implemented on `agentic-ai-integration`. No commit or push was made. This task
changes Android presentation and tests only; Django, PostgreSQL, scoring, AI
algorithms, JWT handling and doctor approval protections are unchanged.

## Result and data flow

The diet route now opens a white/green Compose screen with VedAmrit branding,
the requested title/subtitle, actual doctor/validation status, seven selectable
days, a saved-day meal card, food guidance and lifestyle recommendations.

`DietAssessmentScreen` still uses `AgenticPlanViewModel`, `AgenticRepository`,
the existing assessment sync controller and patient-scoped stores. The new
`PersonalizedDietContent` renders their state. `PersonalizedDietData` maps the
existing `WellnessPlanResponse`; it does not generate a plan or substitute a
static one. Days are selected by their saved day number, not their list position.

Meals and lifestyle guidance require current server lineage, the existing
`draft_view_allowed`/validation checks, matching local profile version when a
local profile exists, completed assessments and synchronized local results.
Rejected/change-requested plans are withheld. Failed/stale/restricted plans show
an explanation; validation/safety warnings remain outside the collapsed details.
The existing ViewModel continues enforcing patient and exact input identity.

Generate and Refresh retain their existing ViewModel callbacks. **Plan Details &
Sync** contains assessment sync/retry, plan and run metadata, full assessment and
profile version references, RAG status, final-report controls and **Restore saved
plan**. The previous general constitution guidance and example PDF remain in a
separate, explicitly labeled **General guidance & examples** section.

**Download Your Diet Report** calls the existing `PatientDraftPdfActions` with
presentation options. Its authenticated repository, eligibility checks, private
cache, public MediaStore export, older-device Save As flow, errors, Open PDF and
Share PDF handlers are unchanged. The endpoint remains:

```text
GET /api/agentic/plans/{id}/draft-pdf/
```

Public exports remain in **Downloads/VedAmrit**. The disclosure that these files
are accessible outside app account isolation is still visible. A header download
shortcut scrolls to these actions; it does not start an export automatically.

## Screenshot comparison

| Reference element | Implemented behavior |
| --- | --- |
| White background, green serif headings, leaf branding | Matching palette, rounded cards and a small neutral leaf mark; existing back navigation retained. |
| My Personalized Diet Plan and assessment subtitle | Exact requested wording. Text wraps naturally on phones. |
| Doctor pending banner | Actual server status; no invented PENDING during loading. Draft label says **AI-Generated Draft — Not Doctor Reviewed** and makes no promise of future review. |
| Day 1–7 pills | Selected day green; horizontal scrolling; each selection displays that saved day's records. |
| Four meal sections | Breakfast, Lunch, Evening Snack and Dinner, mapped from backend meal names. Missing sections have explicit empty states. Additional backend meal sections are retained. |
| Meal photographs | Neutral plate/cutlery symbols. No verified matching photos were available, so screenshot photos are not presented as generated meals. |
| Day theme, date and meal descriptions | Displayed only if present in saved fields. The screenshot's date and “Balancing & Energizing” title are not copied into patient data. |
| Nutrition panels | Only supplied finite, nonnegative values. Missing totals are explained; no zero defaults or estimated sums. Explicit zero is preserved. Food-level details and saved calorie targets can be expanded. |
| Foods to Eat / Avoid | Saved explicit guidance, or clearly labeled foods from the saved weekly schedule; avoidance includes saved/matching allergies, intolerances and restrictions. No generic main-screen fallback. |
| Recommendations | Actual saved recommendations with expandable reasons/sources. Safety text stays visible while sources are collapsed. Retrieval is not described as clinical verification. |
| Dark-green report action | Full-width **Download Your Diet Report**, using the existing real PDF flow. Open/Share and eligibility/error messages remain available. |
| Compact main screen | Technical cards are collapsed by default. Narrow screens stack nutrition; large text also stacks action buttons and food guidance. Interactive controls retain at least 48dp tap targets. |

The reference screenshot was available in the conversation. The referenced
current-UI video was not supplied; it was requested but is still unavailable.
Implementation was compared with the screenshot, existing code and device renders.

## Exact modified/new files

| File | Change |
| --- | --- |
| `app/src/main/java/com/example/vedaahar/DietAssessmentScreen.kt` | Wire the new screen to existing state/actions; move technical and general/example content into expandable sections. |
| `app/src/main/java/com/example/vedaahar/diet/PersonalizedDietContent.kt` | New reusable Compose layout, status, day tabs, meal/nutrient cards, food guidance, recommendations and empty/error states. |
| `app/src/main/java/com/example/vedaahar/diet/PersonalizedDietData.kt` | Saved-output projection, additional current-profile/status guards, food guidance and optional date/nutrient handling. |
| `app/src/main/java/com/example/vedaahar/agentic/AgenticPlanPresentation.kt` | Optional saved descriptions/day metadata and finite numeric nutrient parsing; no scoring changes. |
| `app/src/main/java/com/example/vedaahar/agentic/PatientDraftPdfActions.kt` | Optional label/compact presentation and button styling; download/storage/URI handlers retained. |
| `app/src/test/java/com/example/vedaahar/diet/PersonalizedDietDataTest.kt` | Nine focused mapping, missing-data, lineage/status and safety tests. |
| `app/src/androidTest/java/com/example/vedaahar/diet/PersonalizedDietContentTest.kt` | Nine Compose tests with isolated synthetic saved-plan DTOs and phone screenshot capture. |
| `docs/personalized-diet-redesign.md` | This handoff. |

## Verification

| Check | Result and scope |
| --- | --- |
| Android JVM suite | **156 passed, 0 failed/errors/skipped**, including nine new data tests and existing auth, repository, generation/retry, exact-version restoration and stale/account-switch tests. |
| Debug APK and instrumentation APK | Offline builds passed; both installed on the connected physical API 35 phone without clearing app data. |
| Physical-device instrumentation | **26 passed**: nine new Compose tests plus 17 existing public/private PDF and assessment patient-store tests. |
| Compose interactions | Day switching, selected-day data, Generate/Refresh/PDF callback wiring, details expansion, failure/loading/missing-assessment states, recommendation safety/source expansion and 320dp width at 1.6 font scale passed. |
| PDF regressions on the phone | Existing production store/export methods tested with synthetic PDF bytes: public path/MIME/bytes, repeat export, persistence after store recreation, URI opening/sharing intent flags, missing viewers and patient isolation. |
| Visual review | Captured and inspected actual phone renders of synthetic DTOs at normal and enlarged font sizes. Fixed header/action wrapping found during review. These are layout fixtures, not patient meal or clinical-result evidence. |
| Whitespace | `git diff --check` passed. |

The first day-switch assertion incorrectly matched a Day 1 entry in the separate
weekly Foods to Eat list. It was narrowed to the selected-day card; the final
suite passes. No production behavior was changed to satisfy that assertion.

Commands used:

```powershell
Set-Location D:\VedAmrit
.\gradlew.bat --offline testDebugUnitTest assembleDebug assembleDebugAndroidTest '-PagenticApiBaseUrl=http://127.0.0.1:8000/'
$taskAdb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
& $taskAdb install -r app/build/outputs/apk/debug/app-debug.apk
& $taskAdb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $taskAdb shell am instrument -w -r -e class com.example.vedaahar.diet.PersonalizedDietContentTest,com.example.vedaahar.agentic.PublicPdfDownloadsTest,com.example.vedaahar.agentic.PatientPdfStoreTest,com.example.vedaahar.assessment.AssessmentPatientStoreTest com.vedamrit.test/androidx.test.runner.AndroidJUnitRunner
```

The nine Compose tests were also rerun after stabilizing screenshot capture.
Screenshots are local QA artifacts under ignored `backend/tmp_agent_engine_reports/`
(`task3_3_phone_header.png`, `task3_3_phone_meals.png`,
`task3_3_phone_large_font.png`); no patient information is in these fixtures.

## Physical-phone acceptance with a real patient plan

The installed debug APK uses `http://127.0.0.1:8000/`. Keep the existing Django
development server and PostgreSQL configuration running. Server-start commands
are documented in [agentic-ai-draft-integration.md](agentic-ai-draft-integration.md#physical-phone-acceptance-exact-powershell-commands).
No migrations, database reset or new AI plan are required for this UI change.

```powershell
Set-Location D:\VedAmrit
$taskAdb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
& $taskAdb reverse tcp:8000 tcp:8000
& $taskAdb shell am start -n com.vedamrit/com.example.vedaahar.MainActivity
```

1. Sign in to the intended patient and open Personalized Diet. Let existing
   assessment synchronization/restoration complete, then Refresh the saved plan.
2. Confirm actual doctor status and warnings against the prior saved plan. Open
   **Plan Details & Sync** to compare exact plan/input references and RAG state.
3. Select all seven days. Check four saved meal sections, actual portions and
   supplied nutrients. Missing values must not be replaced with screenshot values.
4. Expand a lifestyle recommendation. Check its saved reason/source and the
   visible safety caution. Check the food guidance's source captions/restrictions.
5. Tap the header download icon or scroll to **Download Your Diet Report**.
   For an eligible development draft, download, then Open PDF and Share PDF.
   Stop at the share chooser unless sharing is actually intended.
6. Check Files → Downloads → VedAmrit and reopen the PDF after closing the app.
   Verify seven days and draft disclaimers. Repeat download must retain distinct
   copies. Public exports remain accessible outside app-level account isolation.
7. Reopen the diet route and use **Restore saved plan**. Check that the same
   eligible saved plan returns without generating a duplicate.
8. Check offline/error behavior, changed-input/stale status and account switching.
   No other account's plan/private cached PDF should appear. Previously exported
   public files intentionally remain in the phone's Downloads folder.
9. Increase system font size and check day scrolling, meal text, buttons and PDF
   access. Return the font setting to your preference afterward.

## Remaining verification and data limits

- A fresh live patient login → Django/PostgreSQL → existing saved plan → report
  download/Open/Share through the redesigned route was **not performed** in this
  task. Phone layout tests used isolated DTOs; PDF regressions exercised actual
  export/storage methods with synthetic PDFs. No new clinical data was generated.
- External Files/viewer UI and actual recipient sharing were not rerun here;
  Task 3.2 evidence and manual acceptance instructions remain in
  [public-pdf-downloads.md](public-pdf-downloads.md). Open/Share intent regressions
  passed. No claim of exhaustive third-party viewer support is made.
- Android 10+ export was exercised on API 35. Older-device Save As intent routing
  is tested, but no API 24–28 hardware was available. No tablet/landscape hardware
  visual check was performed.
- Saved backend output may omit dates, descriptions, meal totals or separate food
  guidance. The UI leaves optional fields absent or explains missing information.
  No matching meal photos were introduced. Long weekly guidance is expandable.
- Clinical validation restrictions and release preview restrictions still apply;
  a disabled PDF button is not overridden by this redesign.
- Django tests were not rerun because no backend code changed. No packages were
  installed and no database/schema or production transport configuration changed.

## Suggested GitHub commit

Title: **Redesign personalized diet screen around saved AI plans**

```text
Replace the diet route's long default content with a responsive Compose layout
for the patient's saved seven-day plan, actual meal nutrition, food restrictions
and expandable lifestyle recommendations with RAG sources.

Keep doctor and validation status accurate, clinical warnings visible, and stale
or restricted guidance withheld. Move assessment versions, sync/retry, restoration
and general examples into Plan Details & Sync without changing existing workflows.

Reuse the authenticated draft PDF download and Downloads/VedAmrit export with a
prominent Download Your Diet Report action and existing Open/Share handling.

Validation: 156 JVM tests, 26 physical-device instrumentation tests, offline debug
and instrumentation APK builds, and phone screenshot review including 320dp at
1.6 font scale. Live patient acceptance through the redesigned route remains to
be checked. No backend, database, scoring, AI or doctor approval rule changes.
```

Task 3.3 stops here.
