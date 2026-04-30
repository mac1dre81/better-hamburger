# Contributing to TextraOCR

Thank you for your interest in contributing to TextraOCR!

## How to contribute

1. Fork the repository.
2. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Make your changes.
4. Test locally by building the app:
   ```bash
   ./gradlew app:assembleDebug
   ```
5. Commit with a clear message.
6. Open a pull request against `main`.

## Coding guidelines

- Keep Kotlin code idiomatic and concise.
- Use Jetpack Compose patterns for UI and state.
- Add new strings to `app/src/main/res/values/strings.xml`.
- Keep AndroidManifest changes minimal and explicit.
- Prefer `MaterialTheme` and `Material3` components.

## Testing

- Validate feature behavior by running the debug APK on a device or emulator.
- Confirm settings persist and UI flows work after changes.
- If you add new resources, ensure they are referenced correctly.

## Notes

- This project uses Android Gradle Plugin `9.2.0` and Kotlin `2.3.20`.
- The launcher activity is `SplashActivity`.
- Billing and AdMob features should be tested with appropriate sandbox/test credentials.
