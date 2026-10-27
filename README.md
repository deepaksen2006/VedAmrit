# VedAmrit

VedAmrit is an Android wellness application focused on Ayurvedic diet guidance, dosha assessment, patient onboarding, doctor discovery, community care, reminders, yoga, meditation, and herbal product browsing. The repository also includes a Django REST backend scaffold for doctor, consultation, and diet-plan APIs.

## Features

- Patient onboarding with profile validation and saved onboarding progress.
- Dosha assessment with Vata, Pitta, Kapha, dual-profile, and balanced-profile scoring.
- Dashboard flow for diet assessment, health reminders, yoga and meditation, ingredient book, shopping, and community care.
- Community care locator using MapTiler and OpenStreetMap-style location data.
- Doctor module with registration/login API contract, verified doctor listings, consultations, and diet-plan draft models.
- Django REST backend apps for doctors, consultations, diet plans, JWT authentication, media, and PostgreSQL configuration.

## Tech Stack

### Android App

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Android Gradle Plugin 8.13.2
- Gradle 8.13
- MapTiler support for map/location previews

### Backend

- Python
- Django 5.1.4
- Django REST Framework
- Simple JWT
- PostgreSQL
- python-dotenv

## Project Structure

```text
.
+-- app/                         # Android application
|   +-- src/main/java/com/example/vedaahar/
|       +-- AppNavigation.kt      # Main navigation graph
|       +-- dosha/                # Dosha assessment models, scoring, storage, UI
|       +-- doctor/               # Doctor module models, repository, API contract, UI
|       +-- *.kt                  # Patient, dashboard, reminder, diet, shopping, care screens
+-- backend/                      # Django REST backend
|   +-- apps/
|   |   +-- consultations/
|   |   +-- dietplans/
|   |   +-- doctors/
|   +-- vedamrit_backend/
+-- gradle/                       # Gradle wrapper and version catalog
+-- output/                       # Generated showcase/architecture assets
```

## Android Setup

### Requirements

- Android Studio
- JDK 17 or newer to run Gradle (app bytecode targets Java 11)
- Android SDK with compile SDK 36

### Run the App

1. Open the project root in Android Studio.
2. Patient login and registration contact Django in both debug and release builds.
   Registration keeps the existing account-created screen and requires signing in.
   Login saves Django access/refresh tokens; profile, assessment, and plan clients
   attach the access token and refresh once on HTTP 401.

3. Choose a development connection. Set `agenticApiBaseUrl` in ignored
   `local.properties`, or override it with `-PagenticApiBaseUrl=...` when building.
   A trailing `/` is required. An existing local property overrides the emulator default.

| Connection | Development API URL | Django bind address |
| --- | --- | --- |
| Android Studio emulator | `http://10.0.2.2:8000/` (default) | `127.0.0.1:8000` |
| USB phone with `adb reverse tcp:8000 tcp:8000` | `http://127.0.0.1:8000/` | `127.0.0.1:8000` |
| Phone on the same trusted Wi-Fi | `http://<PC_LAN_ADDRESS>:8000/` | `0.0.0.0:8000` |

For example, for a USB phone:

```text
agenticApiBaseUrl=http://127.0.0.1:8000/
```

4. Sync Gradle, then build and run the `app` debug configuration. The phone's
   `127.0.0.1` reaches the PC only while USB reverse is active; emulator `10.0.2.2`
   is not a physical-phone address.

You can also build from the terminal:

```powershell
.\gradlew.bat assembleDebug
```

Release builds require a separate `agenticReleaseApiBaseUrl` with a real HTTPS
endpoint and trusted certificate. There is no production fallback to the local
development address. Missing, HTTP, or malformed release URLs fail validation.
Cleartext is disabled in the main/release network configuration; the debug-only
configuration permits local development HTTP without changing production TLS.

```powershell
.\gradlew.bat assembleRelease "-PagenticReleaseApiBaseUrl=https://<YOUR_API_HOST>/"
```

## Backend Setup

### Requirements

- Python 3.11 or newer
- PostgreSQL

### Create Environment

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

### Configure Environment Variables

Create a `.env` file inside `backend/`:

```env
DJANGO_SECRET_KEY=change-this-secret
DJANGO_DEBUG=True
DJANGO_ALLOWED_HOSTS=localhost,127.0.0.1
POSTGRES_DB=vedaahar
POSTGRES_USER=vedaahar
POSTGRES_PASSWORD=vedaahar
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
```

### Run the existing backend and test a USB phone

Keep the existing PostgreSQL database and `.env` credentials. No database reset
or schema migration is required for patient authentication changes. From a
PowerShell terminal:

```powershell
cd D:\VedAmrit\backend
$env:DJANGO_ALLOWED_HOSTS = "localhost,127.0.0.1,10.0.2.2"
.\.venv\Scripts\python.exe manage.py check
.\.venv\Scripts\python.exe manage.py runserver 127.0.0.1:8000
```

In a second terminal with Android SDK platform-tools on PATH:

```powershell
cd D:\VedAmrit
adb devices
adb reverse tcp:8000 tcp:8000
.\gradlew.bat installDebug "-PagenticApiBaseUrl=http://127.0.0.1:8000/"
```

Open VedAmrit, create an account using your own email/password, then sign in.
Confirm that incorrect credentials show an error, successful login opens the
existing onboarding flow, and saving the patient profile contacts `/api/profile/`.
Close and reopen the app to check refresh-based session restoration. Disconnect
the backend to check visible connection errors; temporary failures retain tokens
for retry. Expired/rejected refresh tokens require signing in again.

For Wi-Fi instead of USB, use the PC's current LAN address in the ignored local
property, add that address **without a port** to `DJANGO_ALLOWED_HOSTS`, bind Django
to `0.0.0.0:8000`, and allow the Python service through Windows Firewall on the
private network. Do not use `ALLOWED_HOSTS=*` or disable the firewall. Rebuild
after changing the URL. Avoid real patient credentials over development Wi-Fi HTTP.

Networking references: [Android emulator host address](https://developer.android.com/studio/run/emulator-networking-address),
[Android network security configuration](https://developer.android.com/privacy-and-security/security-config).

### Authentication checks

```powershell
.\gradlew.bat --offline testDebugUnitTest
.\gradlew.bat --offline connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.vedaahar.network.AuthTokenStoreTest"
```

HTTP unit tests use MockWebServer and verify request/response contracts and refresh
behavior. Device token-store tests use isolated test preferences and never replace
the patient's actual tokens. They do not by themselves verify a real Django login.

If the offline Gradle device runner reports a missing UTP dependency, the compiled
test APK can be run directly without downloading dependencies:

```powershell
.\gradlew.bat --offline assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r -e class com.example.vedaahar.network.AuthTokenStoreTest com.vedamrit.test/androidx.test.runner.AndroidJUnitRunner
```

## Configuration Notes

- `local.properties` is ignored and should stay local to each machine.
- For Android maps, set `mapTilerApiKey` in ignored `local.properties` or the `MAPTILER_ANDROID_API_KEY` environment variable. Use a dedicated Android MapTiler key restricted to User-Agent `VedAmrit-Android`, separate from backend geocoding. Client keys remain extractable from APKs; rotate the previously hardcoded key in MapTiler Cloud.
- A missing Android map key disables only the map. Hospital directory and nearby search still use Django. See [Task 4.2C setup and verification](docs/android-hospital-map.md).
- The doctor Android repository currently includes mocked sample data, while API request/response contracts and Django backend modules are present for backend integration.

## Useful Commands

```powershell
# Build Android debug APK
.\gradlew.bat assembleDebug

# Run Android unit tests
.\gradlew.bat test

# Run Android instrumented tests
.\gradlew.bat connectedAndroidTest

# Start Django backend
cd backend
.\.venv\Scripts\Activate.ps1
python manage.py runserver
```

## Development Notes

- Keep generated folders such as `.gradle/`, `build/`, and Python `__pycache__/` out of version control.
- Use Android Studio for Compose previews, Gradle sync, and emulator/device testing.
- Use Django migrations when backend models change.
- Validate Django, PostgreSQL, and MapTiler configuration before testing authentication, maps, or location-related flows.
