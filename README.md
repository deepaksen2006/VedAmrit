# VedAahar

VedAahar is an Android wellness application focused on Ayurvedic diet guidance, dosha assessment, patient onboarding, doctor discovery, community care, reminders, yoga, meditation, and herbal product browsing. The repository also includes a Django REST backend scaffold for doctor, consultation, and diet-plan APIs.

## Features

- Patient onboarding with profile validation and saved onboarding progress.
- Dosha assessment with Vata, Pitta, Kapha, dual-profile, and balanced-profile scoring.
- Dashboard flow for diet assessment, health reminders, yoga and meditation, ingredient book, shopping, and community care.
- Community care locator using MapTiler and OpenStreetMap-style location data.
- Doctor module with registration/login API contract, verified doctor listings, consultations, and diet-plan draft models.
- Firebase integration for analytics, authentication, Firestore, and storage.
- Django REST backend apps for doctors, consultations, diet plans, JWT authentication, media, and PostgreSQL configuration.

## Tech Stack

### Android App

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Android Gradle Plugin 8.13.2
- Gradle 8.13
- Firebase BoM, Analytics, Auth, Firestore, and Storage
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
|   +-- vedaahar_backend/
+-- gradle/                       # Gradle wrapper and version catalog
+-- output/                       # Generated showcase/architecture assets
```

## Android Setup

### Requirements

- Android Studio
- JDK 11 or newer
- Android SDK with compile SDK 36
- Firebase project configuration

### Run the App

1. Open the project root in Android Studio.
2. Add or confirm Firebase configuration at:

```text
app/google-services.json
```

3. Sync Gradle.
4. Build and run the `app` configuration on an emulator or Android device.

You can also build from the terminal:

```powershell
.\gradlew.bat assembleDebug
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

### Run Backend

```powershell
python manage.py migrate
python manage.py runserver
```

The backend will run at:

```text
http://127.0.0.1:8000/
```

## Configuration Notes

- `local.properties` is ignored and should stay local to each machine.
- Firebase credentials are expected through `app/google-services.json`.
- The Android app defines a `MAPTILER_API_KEY` build config value in `app/build.gradle.kts`.
- For production, move secrets and API keys out of committed source and into secure build-time configuration.
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
- Validate Firebase and MapTiler configuration before testing authentication, storage, maps, or location-related flows.
