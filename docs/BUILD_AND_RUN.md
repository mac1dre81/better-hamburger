# Build and Run

## Requirements

- JDK 17 or newer
- Android SDK with API level 36
- `adb` installed and available in PATH
- Optional: Android Studio for editing and debugging

## Build the APK

From the repository root:

```powershell
cd C:\better-hamburger
./gradlew app:assembleDebug
```

## Install to a Device

1. Confirm device connection:
   ```powershell
adb devices
```
2. Install the debug APK:
   ```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Launch

Open the app from your device launcher after installation.

## App Flow

- The app starts at `SplashActivity`.
- Then it navigates to `MainActivity` with the home screen.
- The home screen shows remaining scans, quick actions, and premium pricing.
- Settings are available via the settings dialog.

## Notes

- The launcher activity is configured in `app/src/main/AndroidManifest.xml`.
- The splash screen uses `Theme.TextraOCR.Splash` and shows the app icon/title.
