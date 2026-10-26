# Task 3.2 — Public personalized PDF downloads

Branch: **agentic-ai-integration**.

Task 3.1 changes were already present and uncommitted when this task started.
They were preserved. The files listed below describe the Task 3.2 delta, not the
entire pre-existing working tree. No commit/push, backend/database change, AI
algorithm change, assessment scoring change or package installation was performed.

## Root cause and inspection

PatientDraftPdfActions called the authenticated DraftPdfRepository, then
PatientPdfStore.save. That saved the validated PDF in:

~~~text
noBackupFilesDir/patient_draft_reports/<patient-and-backend-hash>/draft-<references>.pdf
~~~

Opening it made a temporary copy in cacheDir/patient_draft_reports and granted
read access through PatientPdfProvider. Neither private location is public
Downloads. The FileProvider XML grants URI access; it does not publish a file to
MediaStore or make the private source visible in Files.

The repository already validates application/pdf, the PDF signature and the size
limit. Task 3.1 PDF tests verify complete generated PDF bytes; this task preserves
that validation and copies the exact successful authenticated response. Actual
Android PDF parsing, byte equality and public export were also tested with isolated
synthetic PDFs. No sample PDF is used as a production fallback.

## Implemented flow

The existing **Download Personalized Diet PDF** button now:

1. Requests the persisted plan's draft-PDF endpoint with the existing JWT client.
2. Keeps the validated private, patient-scoped copy from Task 3.1.
3. On Android 10+ creates a new MediaStore.Downloads entry on the primary volume.
4. Writes the actual response bytes while IS_PENDING=1.
5. Closes the output, checks the current patient/input references, then clears
   IS_PENDING. Only after successful publication does it show:
   **Saved to Downloads/VedAmrit**.
6. Stores the new content URI in patient-scoped in-app metadata. Opening/sharing
   uses that URI and temporary read permission.

The relative path is exactly:

~~~kotlin
Environment.DIRECTORY_DOWNLOADS + "/VedAmrit"
~~~

Files apps usually label this Downloads/VedAmrit. The actual shared-storage path
on the tested phone was **/storage/emulated/0/Download/VedAmrit**.

Example filename pattern:

~~~text
VedAmrit_Diet_Plan_<planId>_v<version>_<timestamp>_<uniqueSuffix>.pdf
~~~

Names contain no patient name, JWT or private API URL. Each download creates a new
uniquely named copy; it does not overwrite an older export or an unrelated file.
The latest URI is remembered for in-app Open PDF and Share PDF.

A failed write, close, publication, cancellation or detected account change removes
the newly created entry. Cleanup never scans/deletes unrelated files. The private
copy remains available. Storage failures are visible rather than reported as success.
An unexpected process kill cannot execute application cleanup; any interrupted
MediaStore entry remains pending rather than being published as a complete PDF.

This follows Android's [shared-storage/MediaStore contract](https://developer.android.com/training/data-storage/shared/media)
for app-owned Downloads and pending publication.

## Older Android and permissions

API 24–28 uses ACTION_CREATE_DOCUMENT through the system Save As picker, with
application/pdf and the generated filename. The user should select
Downloads/VedAmrit. The app writes the saved authenticated bytes to the returned
document URI, attempts to retain read access, and reports the selected location
rather than falsely claiming Downloads if another folder was chosen.

Pending picker state contains a patient hash and report references, not tokens or
PDF bytes. A late picker result for another account is rejected; no previous
patient's cached report is automatically exported.

Android's [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files)
provides this user-selected document flow without broad storage permission.
The new feature does not add/request MANAGE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE
or WRITE_EXTERNAL_STORAGE. AndroidManifest.xml and file_paths.xml are unchanged
relative to the start of Task 3.2. A pre-existing WRITE_EXTERNAL_STORAGE declaration
limited to API 28 remains for the older bundled-example helper; this new flow does
not request or rely on it.

## Open, share and account boundaries

- Open PDF uses ACTION_VIEW/application/pdf with a content URI, ClipData and
  temporary read permission. No-viewer devices keep the file and receive an
  actionable message.
- Share PDF uses ACTION_SEND/EXTRA_STREAM through the chooser with read-only URI
  permission. It does not automatically send to anyone.
- Missing/moved/changed public files cause a visible error and can be downloaded again.
- Private source storage and in-app public references remain patient/backend scoped.
  Current ownership and local assessment/profile versions are checked before export,
  publication and in-app opening/sharing. Nothing auto-exports on app startup.
- **Public PDFs are accessible outside VedAmrit, including after the app closes or
  accounts change. They are no longer protected by app-level account isolation.**
  This is stated in the UI before export. Other apps/Files can retain copies.
- JWT handling, backend ownership/lineage/clinical gates, draft disclaimers,
  doctor PENDING and approved final-report protections are unchanged.
- Existing private-cache opening is retained for earlier downloads that have not
  yet been exported publicly.

## Tests and results

| Check | Result |
| --- | --- |
| Android JVM | **147 passed**, zero failures/errors/skips. Includes six new export tests and the existing auth/PDF regression suite. |
| Export transaction faults | Injected write, close and publication failures remove the fresh entry; invalid bytes/missing patient cannot create entries; account changes during write/publication remove the export. No destructive disk-full test was run. |
| Debug APK and instrumentation APK | Offline builds passed. Latest debug APK installed on the connected phone with http://127.0.0.1:8000/. |
| Phone regression suite | **17 tests passed** on Android 15/API 35: real MediaStore path/MIME/pending state/bytes, Android PdfRenderer, repeat exports, restored references, open/share intents, account switching, no-viewer handling, plus Task 3.1 private storage and Task 2 assessment storage. |
| After app closes | Synthetic export survived force-stopping VedAmrit. ADB confirmed it in /sdcard/Download/VedAmrit; a fresh instrumentation process reopened it. |
| Files app + viewer | Opt-in UI test found the exported file in Google Files/DocumentsUI, opened it in WPS PDF Reader, and verified the viewer canvas. A captured screenshot visibly rendered the synthetic PDF text. |
| Lifecycle tests | Create, Files/viewer and verify/cleanup checks each passed: **3 additional physical checks**, making **20 passing device checks** in total. |
| Cleanup | Only newly created synthetic test URIs/files and test preferences were removed. The test Downloads/VedAmrit folder was empty afterward. No patient reports were deleted. |
| Older Android | SDK 24/28 route and Save As intent were tested; no Android 9-or-older device/emulator was available for real picker/write verification. |
| Permissions/config | Manifest and FileProvider paths have identical hashes to the Task 3.2 baseline. No storage permission request was introduced. |
| Backend/clinical | No backend code changed; Django/AI tests were not rerun for this Android-only delta. The existing endpoint/gates remain intact. |
| Git | diff --check passed. No commit or push. |

The UI test initially encountered the locked phone; it now explicitly requires an
unlocked device. Viewer verification accommodates the tested WPS canvas because
that viewer does not expose the full filename as an accessibility title. The final
unlocked Files/viewer test passed; no default PDF viewer preference was changed.

Physical tests used clearly synthetic documents and isolated preferences. They did
not extract patient JWTs or copy real patient files into public storage. A live
patient's authenticated download-to-export interaction still needs the normal
acceptance tap below; the actual production export method was exercised on-device.

## Physical-phone acceptance

The latest debug APK is installed. If rebuilding/reinstalling:

~~~powershell
Set-Location D:\VedAmrit
$taskAdb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
& $taskAdb devices
& $taskAdb reverse tcp:8000 tcp:8000
.\gradlew.bat --offline installDebug '-PagenticApiBaseUrl=http://127.0.0.1:8000/'
& $taskAdb shell am start -n com.vedamrit/com.example.vedaahar.MainActivity
~~~

Keep the existing Task 3.1 Django server running; no backend restart/migration is
needed for this task. If necessary, use the server-start commands in
personalized-draft-pdf.md.

1. Sign in to the intended patient, open the diet screen, and refresh the existing
   eligible persisted plan. No new AI plan is needed just to download.
2. Tap Download Personalized Diet PDF. On Android 10+ expect
   Saved to Downloads/VedAmrit only after publication.
3. Open the phone's Files/Downloads app → Downloads (or Download) → VedAmrit.
   Check the new VedAmrit_Diet_Plan filename and application/pdf type.
4. Open it from Files. Confirm the personalized seven days, lifestyle, source
   references and every-page unreviewed-draft disclaimer.
5. In VedAmrit test Open PDF and Share PDF. Share opens the chooser; select a
   recipient only if you actually intend to share the public health document.
6. Close VedAmrit and reopen the file from Files. Download again; both copies
   should remain with distinct names.
7. Switch accounts. VedAmrit must not list/export the prior patient's private
   cache or in-app references. Public Files copies intentionally remain outside
   that account boundary.
8. On API 24–28, select Downloads/VedAmrit in the Save As picker. Cancellation
   must leave the private copy intact and not claim a public save.

## Reproduce automated checks

~~~powershell
Set-Location D:\VedAmrit
.\gradlew.bat --offline testDebugUnitTest assembleDebug assembleDebugAndroidTest '-PagenticApiBaseUrl=http://127.0.0.1:8000/'
$taskAdb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
& $taskAdb install -r app/build/outputs/apk/debug/app-debug.apk
& $taskAdb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $taskAdb shell am instrument -w -r -e class com.example.vedaahar.agentic.PublicPdfDownloadsTest,com.example.vedaahar.agentic.PatientPdfStoreTest,com.example.vedaahar.assessment.AssessmentPatientStoreTest com.vedamrit.test/androidx.test.runner.AndroidJUnitRunner
~~~

Optional synthetic lifecycle/Files check on an unlocked API 29+ phone:

~~~powershell
& $taskAdb shell am instrument -w -r -e publicPdfLifecycle create -e class com.example.vedaahar.agentic.PublicPdfLifecycleCreateTest com.vedamrit.test/androidx.test.runner.AndroidJUnitRunner
& $taskAdb shell am force-stop com.vedamrit
& $taskAdb shell ls -l /sdcard/Download/VedAmrit
& $taskAdb shell am instrument -w -r -e publicPdfLifecycle files -e class com.example.vedaahar.agentic.PublicPdfFilesUiTest com.vedamrit.test/androidx.test.runner.AndroidJUnitRunner
& $taskAdb shell am instrument -w -r -e publicPdfLifecycle verify -e class com.example.vedaahar.agentic.PublicPdfLifecycleVerifyTest com.vedamrit.test/androidx.test.runner.AndroidJUnitRunner
~~~

The last command verifies readability after restart and removes only the generated
fixture whose exact URI/name was saved by the create phase. Run it even if the
Files UI phase cannot complete. Those special tests are opt-in and skip in ordinary
runs. The UI test currently handles a filename title or the observed WPS canvas;
other OEM viewer choosers may require manual verification.

## Exact Task 3.2 files

Updated:

- app/src/main/java/com/example/vedaahar/agentic/PatientDraftPdfActions.kt
- app/src/main/java/com/example/vedaahar/agentic/PatientPdfStore.kt

Added:

- app/src/main/java/com/example/vedaahar/agentic/PdfExportTransaction.kt
- app/src/main/java/com/example/vedaahar/agentic/PublicPdfDownloads.kt
- app/src/test/java/com/example/vedaahar/agentic/PdfExportTransactionTest.kt
- app/src/androidTest/java/com/example/vedaahar/agentic/PublicPdfDownloadsTest.kt
- docs/public-pdf-downloads.md

## Suggested GitHub commit

Title: **Export personalized diet PDFs to public Downloads with open and share actions**

~~~text
Publish authenticated, validated draft PDF bytes to MediaStore Downloads/VedAmrit
on Android 10+, using pending entries and cleanup on incomplete writes. Keep
private patient-scoped copies and use the system Save As picker on older Android
without introducing broad storage permission requests.

Preserve the download button and add public content-URI open/share actions,
clear success/error states, unique repeat-download names and an explicit notice
that public exports are accessible outside app account isolation. Reject late
exports after account/input changes and retain existing clinical/JWT/report gates.

Validation: 147 JVM tests; offline debug and instrumentation APK builds; 17 phone
regression tests plus three lifecycle/Files/viewer checks on Android 15. Confirmed
public MIME/path/bytes, pending state, repeat exports, app-close persistence and
actual Files-to-WPS viewing. Older-Android hardware and a real patient export tap
remain manual acceptance checks. No backend, schema, AI or scoring changes.
~~~

Stop after Task 3.2.

