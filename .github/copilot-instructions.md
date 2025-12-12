# Copilot Coding Agent Instructions for Hecate (Adaptive Theme)

## Project Summary

Hecate is an Android app ("Adaptive Theme") that automatically switches between Light and Dark
system themes based on ambient light sensor readings. It targets Android 12+ (API 31-36), uses
Jetpack Compose for UI, and requires the `WRITE_SECURE_SETTINGS` permission granted via ADB,
Shizuku, or root.

## Tech Stack & Build Info

- **Language:** Kotlin (JVM target 17)
- **UI:** Jetpack Compose with Material 3/Material You
- **Build:** Gradle 9.2.1, AGP 8.13.2
- **Target SDK:** 36 | **Min SDK:** 31
- **Architecture:** Single-module Android app with MVVM pattern
- **Dependencies:** Firebase (Analytics, Crashlytics), Shizuku API, AndroidX DataStore, Compose BOM

## Build Commands

**Always use the Gradle wrapper** (`./gradlew`). JDK 17+ is required (JDK 23 used in CI).

| Task              | Command                                                                      |
|-------------------|------------------------------------------------------------------------------|
| Clean build       | `./gradlew clean build`                                                      |
| Debug build       | `./gradlew assembleDebug`                                                    |
| Release build     | `./gradlew assembleRelease`                                                  |
| Beta build        | `./gradlew assembleBeta`                                                     |
| Lint only         | `./gradlew lint`                                                             |
| Build + Lint (CI) | `./gradlew lint --build-cache && ./gradlew build sonar --info --build-cache` |
| List tasks        | `./gradlew tasks`                                                            |

**Note:** First build may take several minutes to download Gradle and dependencies.

## Required Files for Build

- `google-services.json` - Required at both repo root AND `app/` directory for Firebase. In CI, this
  is created from a GitHub secret.
- `app/src/main/java/dev/lexip/hecate/util/DarkThemeHandler.kt` - Core theme handler. CI creates a
  mock version from a secret; the real file must exist locally.

## Project Structure

```
hecate/
├── app/
│   ├── build.gradle.kts          # App-level Gradle config
│   ├── proguard-rules.pro        # ProGuard/R8 rules (Shizuku-specific keeps)
│   └── src/main/
│       ├── AndroidManifest.xml   # Permissions, services, receivers
│       └── java/dev/lexip/hecate/
│           ├── HecateApplication.kt    # App entry, DataStore singleton
│           ├── analytics/              # Firebase Analytics logging
│           ├── broadcasts/             # Boot/screen-on receivers
│           ├── data/                   # DataStore preferences, models
│           ├── services/               # Foreground service, QS tile
│           ├── ui/                     # Compose screens, ViewModel
│           │   ├── MainActivity.kt     # Main entry Activity
│           │   ├── AdaptiveThemeScreen.kt
│           │   ├── AdaptiveThemeViewModel.kt
│           │   ├── components/         # Reusable UI components
│           │   ├── setup/              # Permission setup wizard
│           │   └── theme/              # Material theme
│           └── util/                   # Sensor managers, Shizuku
│               ├── DarkThemeHandler.kt # System theme control
│               ├── LightSensorManager.kt
│               ├── ProximitySensorManager.kt
│               └── shizuku/            # Shizuku integration
├── build.gradle.kts              # Root Gradle, SonarCloud config
├── settings.gradle.kts           # Project settings
├── gradle/
│   ├── libs.versions.toml        # Version catalog (dependencies)
│   └── wrapper/                  # Gradle wrapper
├── gradle.properties             # JVM args, Android settings
└── .github/workflows/build.yml   # CI: lint, build, SonarCloud analysis
```

## Key Architecture Details

- **State Management:** MVVM with `AdaptiveThemeViewModel` and Kotlin Flow
- **Persistence:** AndroidX DataStore for user preferences (not SharedPreferences)
- **Background Service:** `BroadcastReceiverService` runs as foreground service with `specialUse`
  type
- **Theme Switching:** `DarkThemeHandler` writes to `Settings.Secure` (requires
  `WRITE_SECURE_SETTINGS`)
- **Shizuku Support:** Optional permission grant method via `ShizukuManager` and `GrantService`

## CI/CD Pipeline (GitHub Actions)

The `build.yml` workflow runs on push and PR:

1. Creates `google-services.json` from secret
2. Creates mock `DarkThemeHandler.kt` from secret
3. Runs lint: `./gradlew lint --build-cache`
4. Builds and analyzes: `./gradlew build sonar --info --build-cache`

## Code Style & Conventions

- **License Header:** All Kotlin files must include GPL-3.0 license header (see existing files)
- **Package:** `dev.lexip.hecate`
- **Compose:** Use Material 3 composables, follow existing component patterns
- **Logging:** Use `android.util.Log` with class-specific TAG constants
- **Error Handling:** Catch specific exceptions, use `Log.e()` for errors
- **Coroutines:** Use `serviceScope` in services, ViewModelScope in ViewModels
- **Commits:** Use the Conventional Commits specification for commit messages. See
  `.github/instructions/conventional-commits.md` for the full guidance and examples.

## Important Notes

- **No tests directory exists** - unit/instrumented tests are defined in build.gradle.kts but no
  test files exist
- **Localization:** 50+ locales supported (translated externally in the Play Console); strings in
  `res/values/strings.xml`, locale config in
  `res/xml/locales_config.xml`
- **Build Variants:** `debug`, `release`, `beta` - beta is release with `-beta` suffix
- **Renovate:** Automated dependency updates target `develop` branch

## Validation Checklist

Before submitting changes:

1. Run `./gradlew lint` - fix all errors
2. Run `./gradlew assembleDebug` - must succeed
3. Verify no new lint warnings in changed files
4. Ensure license headers present on new Kotlin files
5. Check `DarkThemeHandler.kt` is not accidentally modified (contains proprietary logic)

## Trust These Instructions

Use this document as the primary reference. Only search the codebase if information here is
incomplete or produces errors.

