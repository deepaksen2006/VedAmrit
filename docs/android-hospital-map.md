# Task 4.2C — Android hospital map and nearby search

## Implemented behavior

Community Care keeps its existing route, `onBack` callback and Consult navigation.
It now reads hospitals exclusively from Django `/api/care/hospitals/` and
`/api/care/hospitals/nearby/`. The old demo places, invented ratings/opening hours,
emergency categories and client-side provider POI search have been removed.
No booking action or write API was added.

- Search the public directory by hospital name or city without GPS/login.
- Use foreground device location, or explicitly enter latitude/longitude, for
  distance-sorted nearby results. Radius accepts 0.01–100 km, matching Django.
- Search text also filters nearby results on the server, before pagination.
- Refresh, retry and load more preserve the current query. New searches cancel
  previous requests and clear old results; late responses cannot replace them.
- Directory/network loading, HTTP errors, successful empty results, invalid
  coordinates/radius, denied/unavailable location, missing map configuration,
  map authorization/rate-limit/network failures and map timeout have distinct UI.
- Device location is requested only after the user taps the location control.
  Coarse permission is accepted. A new fix times out after 15 seconds; no
  last-known fallback, background permission or ongoing tracking is used. The
  request is cancelled when the screen stops or another search is submitted.
- Hospital cards show backend fields, approved-position status and optional
  distance. Selecting a marker selects its hospital; selecting a mapped card
  focuses its map pin. Verified hospitals without approved positions can remain
  in the directory, with no pin. Pins represent the loaded page(s), not all pages.

Map rendering uses native MapLibre Android 13.7.1 and MapTiler `streets-v4`.
MapTiler/OpenStreetMap attribution remains visible. Provider attribution on an
approved hospital is displayed as plain text. Lifecycle start/resume/pause/stop,
disposal and low-memory callbacks are forwarded to MapView. No WebView or
JavaScript bridge remains in this feature.

## Independent verification gates

The backend still requires verified identity, active status, completed import
review where applicable, and no unresolved/confirmed duplicate record for public
hospital visibility. Nearby adds all the pre-existing position gates:

1. Hospital location is APPROVED.
2. Its approved candidate is still APPROVED and belongs to that same hospital.
3. Both hospital coordinates match the approved candidate exactly.

The public serializer now includes `location_status` and `location_attribution`.
It returns null coordinates and UNVERIFIED position metadata unless those
position gates hold. This closes the older directory contract that returned
supplied, unapproved coordinates. Nearby additionally returns `distance_km`.

Android independently requires `verification_status == VERIFIED`,
`location_status == APPROVED`, and finite, in-range coordinates for every pin.
Older servers lacking the new location metadata fail closed for pins. The
Android client cannot infer verification from coordinates or MapTiler results.

Read-only PostgreSQL verification during this task:

| Records | Identity | Location | Public visibility |
| --- | --- | --- | --- |
| UDR-001 | DRAFT | APPROVED | Hidden; detail 404 |
| UDR-002 through UDR-012 | DRAFT | UNVERIFIED | Hidden; details 404 |

All 12 still have NEEDS_REVIEW import status and booking disabled. Directory,
Udaipur name/city search and nearby at 24.5854, 73.7125 with radius 20 km return
HTTP 200/count 0. This is the correct empty state, not a reason to approve data.
Synthetic hospitals exist only in isolated test fixtures; none were inserted
into the configured PostgreSQL database.

## Dedicated Android MapTiler key

Set up a **separate Android client key** in MapTiler Cloud. Keep backend
`MAPTILER_API_KEY` for backend geocoding only. The previously committed Android
key was removed from source; its owner should revoke/rotate it in MapTiler Cloud.
This task does not have account access to create keys, alter restrictions or
revoke the old key.

Configure the new key's **Allowed User-Agent** substring as `VedAmrit-Android`.
The SDK's dedicated HTTP client sends `VedAmrit-Android/1.0`. Do not apply web
origin restrictions to this native key or spoof an origin to bypass a restriction.
The client accepts only HTTPS requests to `api.maptiler.com`, disables redirects
and SDK request logging, and never shares the authentication HTTP client.
Application errors never display provider URLs, credentials or response bodies.

In ignored `D:\VedAmrit\local.properties`, preserve existing SDK/backend settings
and add:

```properties
mapTilerApiKey=YOUR_DEDICATED_ANDROID_KEY
```

Alternatively set `MAPTILER_ANDROID_API_KEY` in the build environment. Environment
configuration takes precedence. `local.properties.example` contains placeholders
only. Empty configuration intentionally builds a working directory/search UI with
the map disabled; rebuild after adding the key. Build-time validation rejects
characters that could escape a generated Kotlin/Java string.

Keys included in a mobile APK remain extractable. Ignored/build-environment
configuration prevents committing the credential; provider restrictions,
separate application keys, quotas and rotation are still needed. User-Agent
restrictions are an abuse control, not authentication or a way to make an APK
secret. [MapTiler key guidance](https://docs.maptiler.com/guides/developer/credentials/api-key/)
documents native User-Agent restrictions and separate platform keys.

The existing backend URL configuration is reused. Emulator default is
`http://10.0.2.2:8000/`; keep the configured LAN/USB endpoint if using a device.
Release builds retain the existing HTTPS-only backend URL check. No JWT storage,
refresh logic, diet workflow, PDF provider, agentic endpoint or navigation graph
was changed.

## Validation

| Check | Result |
| --- | --- |
| Android JVM tests | 168 passed, 0 failures/errors; includes 8 new care state/marker tests and nearby HTTP contract coverage |
| Android instrumentation | 36 passed on isolated API 36 emulator; 1 opt-in live-map check skipped; includes directory/search/coordinate-nearby/empty/error/retry and existing token, diet, assessment and PDF tests |
| OnePlus live verification | Passed: native tiles rendered, Udaipur directory search, 20 km coordinate-nearby search and both visible empty states; 0 hospital pins |
| Django explicit full suite | 220 run: 217 passed, 3 opt-in graph/RAG/PDF smoke tests skipped |
| Agent engine pytest | 54 passed, 3 subtests passed, 1 dependency warning |
| Django system check | Passed |
| Migration consistency | No changes detected; no migration applied by this task |
| Live PostgreSQL directory/nearby/details | Expected empty/hidden state verified read-only |
| Debug APK and instrumentation APK | Built successfully |
| Android lint | Completed analysis: 3 existing errors outside Community Care, 115 warnings, 1 hint; no care-file findings |

The Android app's application ID is `com.vedamrit`; the stale scaffold
instrumentation assertion was changed to use `BuildConfig.APPLICATION_ID`.
The old backend test expecting unapproved public coordinates was updated to
assert null coordinates and UNVERIFIED position metadata.

Django's namespace-package discovery can load apps with incorrect module names.
Use the explicit suite command below rather than `manage.py test apps`.

```powershell
# From D:\VedAmrit
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
.\gradlew.bat :app:lintDebug

# From D:\VedAmrit\backend; separate shell for isolated tests
$env:DJANGO_DATABASE_ENGINE='sqlite'
$env:SQLITE_DB_PATH=':memory:'
$env:HF_HUB_OFFLINE='1'
$env:TRANSFORMERS_OFFLINE='1'
.\.venv313\Scripts\python.exe manage.py test apps.community_care.test_exact_location_review apps.community_care.test_geocoding apps.community_care.test_hospital_import apps.community_care.tests apps.community_care.test_migrations apps.authentication.tests apps.agentic_reports.tests apps.agentic_reports.test_assessment_sync apps.agentic_reports.test_draft_generation apps.agentic_reports.test_draft_pdf --noinput
.\.venv313\Scripts\python.exe -m pytest agent_engine/tests -q --disable-warnings
```

Artifacts are under `app/build/outputs/apk/debug/app-debug.apk` and
`app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`. Local check
logs and read-only before/after snapshots are under root `build/care-*`.
`scripts/audit_care_readonly.py` snapshots model counts/digests and public API
visibility without printing credentials or private record contents. It uses a
read-only, repeatable-read transaction on PostgreSQL. Write tests used a separate
in-memory SQLite database; no live data was reset or modified, and no commit ran.
All 23 model counts and serialized-data digests matched before/after. The
sanitized comparison is recorded in
`docs/import-reports/android-hospital-map-verification.json`.

## Remaining verification

- The locally configured Android key now works on the OnePlus. A value-only
  comparison found it **matches the backend geocoding key**. No key value was
  printed or added to reports. A separate Android key and provider-side
  restriction/rotation remain owner configuration work; separate credentials
  must not be claimed for the current configuration.
  The final credential scan also found the value in `local.properties.example`;
  only that credential value was removed from the versionable template. The
  working ignored configuration was retained. A repeat scan found no occurrences
  in tracked or unignored text files.
- Real hospital markers remain blocked by independent identity/import/duplicate
  and exact-position reviews. UDR-001's approved location does not resolve its
  DRAFT identity. No booking is enabled.
- Permission prompt denial, disabled GPS, rotation/backgrounding with live map
  tiles, and physical-device GPS accuracy still need device acceptance testing.
- Lint's initial Google dependency download stalled. A completed official
  download was checked against Google's SHA-1; lint then completed analysis and
  found three existing errors in unchanged files: `PublicPdfDownloads.kt:55`
  (API 29 detection), `themes.xml:8` (API 29 theme attribute), and
  `PatientProfileScreen.kt:955` (ViewModel construction inside a composable).
  These remain outside this task's care feature; lint is not reported as passed.
  Full details are in `app/build/reports/lint-results-debug.txt`.

The owner authorized the targeted live map check on connected
Android device `6dea27cd` (OnePlus CPH2619, API 35). It passed after rebuilding
and installing with `adb install -r`; app data was retained. All 23 live database
model counts and hashes matched before/after, and UDR-001 remains DRAFT/APPROVED
with booking disabled. The other 11 remain DRAFT/UNVERIFIED.

The phone could not reach the existing saved LAN backend address. The installed
debug APK therefore uses **`http://127.0.0.1:8000/` over USB reverse forwarding**,
selected using a temporary Gradle property. The saved `agenticApiBaseUrl` in
`local.properties` was not changed. The local backend requires the USB connection
and forwarding shown below; map tiles use the phone's normal internet connection.

The opt-in `LiveCareMapTest` uses actual public directory and map requests and
does not read/modify JWTs, patient settings or saved plans. Run only that test on
the phone; the other instrumentation tests intentionally manipulate isolated
session/profile test state and were run on the disposable emulator instead.

```powershell
# Reproduce the verified USB-device build; keep the phone unlocked for the UI test.
adb -s 6dea27cd reverse tcp:8000 tcp:8000
.\gradlew.bat '-PagenticApiBaseUrl=http://127.0.0.1:8000/' :app:assembleDebug :app:assembleDebugAndroidTest
adb -s 6dea27cd install -r app/build/outputs/apk/debug/app-debug.apk
adb -s 6dea27cd install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s 6dea27cd shell am instrument -w -r -e verifyLiveCareMap true -e class com.example.vedaahar.care.LiveCareMapTest com.vedamrit.test/androidx.test.runner.AndroidJUnitRunner
```

The live check requires a configured key, a fully rendered native map frame,
and a successful public Django request. It prints counts only. A successful test
with zero hospitals is expected while the 12 identity reviews remain incomplete.

Live device testing found and fixed SDK initialization ordering, false network
errors from cancelled tile calls, keyboard coverage of search results, and
premature map readiness. MapLibre is initialized before HTTP setup; cancellations
do not show connectivity errors; search clears input focus/hides the keyboard
and the list respects IME insets; map readiness waits for native idle after
camera/tile rendering. The test keeps its own activity's screen on, waits for
IME dismissal and saves temporary screenshots in a dedicated app cache folder.
No device screen-timeout setting or location permission was changed.

The phone has neither coarse nor fine location permission granted. Nearby search
was verified using explicitly entered Udaipur coordinates (24.5854, 73.7125), not
the phone's GPS. GPS permission/accuracy testing and real verified-marker
interaction remain unperformed; no hospital was approved to enable such testing.

Evidence:

- [OnePlus verification report](import-reports/android-hospital-map-oneplus-verification.json)
- [Rendered Udaipur map and nearby empty state](verification/community-care-oneplus/care-live-nearby-map.png)
- [Directory empty state](verification/community-care-oneplus/care-live-directory-empty.png)
- Local final build/test logs: `build/care-phone-build.txt`,
  `build/care-phone-live-test.txt`; 168 Android unit tests still pass.

Reference: [MapLibre Android lifecycle quickstart](https://github.com/maplibre/maplibre-native/blob/main/platform/android/docs/getting-started.md).
