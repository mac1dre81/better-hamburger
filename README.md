# TextraOCR

**TextraOCR** is an Android OCR scanner app built with Jetpack Compose. It supports document scanning, OCR text extraction, saved document browsing, premium subscription handling, and user preferences for scan behavior.

## Features

- Camera-based document scanning
- OCR text extraction using ML Kit
- Saved document listing and file opening
- Premium subscription flow via Google Play Billing
- Free daily scan limit for non-premium users
- Settings dialog with:
  - Dark theme toggle
  - Auto OCR open toggle
  - Auto save toggle
  - Default output format selection
- App splash screen with app icon and title

## Tech Stack

- Kotlin
- Jetpack Compose
- AndroidX Activity Compose
- Material3 UI
- Google Play Billing
- ML Kit Document Scanner & Text Recognition
- AdMob test app ID integration
- Android 24+ support

## Project Structure

- `app/src/main/java/com/dredio/textraocr/`
  - `MainActivity.kt` - Home screen, settings dialog, UI state handling
  - `SplashActivity.kt` - Startup splash screen
  - `PremiumBillingManager.kt` - Billing integration and subscription state
  - `AppSettings.kt` - Persistent app preferences and scan limits
  - `DocumentScannerActivity.kt` - Document capture flow
  - `OcrResultActivity.kt` - OCR result review and save UI
  - `DocumentViewerActivity.kt` - Saved document viewer
- `app/src/main/res/` - app resources and theme definitions
- `gradle/` - shared Gradle configuration

## Getting Started

### Prerequisites

- JDK 17 or newer
- Android SDK with API level 36
- Android Studio or command line Gradle support
- `adb` available on your PATH

### Build Debug APK

From the repository root:

```powershell
cd C:\better-hamburger
./gradlew app:assembleDebug
```

### Install on Device

```powershell
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Run

Launch the app from your connected device or emulator.

## Notes

- The launcher activity is `SplashActivity`, which shows the app icon and title before starting `MainActivity`.
- `MainActivity` contains the core home screen and settings dialog.
- The app uses a test AdMob application ID in `AndroidManifest.xml`.

## Documentation

- `docs/BUILD_AND_RUN.md` - build and run instructions
- `CONTRIBUTING.md` - contribution guidelines
