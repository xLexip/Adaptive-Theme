# Copilot Coding Agent Instructions for Hecate (Adaptive Theme)

## Project Summary

Hecate is an Android app ("Adaptive Theme") that automatically switches between Light and Dark
system themes based on ambient light sensor readings. It targets Android 12+ (API 31-36), uses
Jetpack Compose for UI, and requires the `WRITE_SECURE_SETTINGS` permission granted via ADB,
Shizuku, or root.

## Tech Stack & Build Info

- **Language:** Kotlin (JVM target 17)
- **UI:** Jetpack Compose with Material 3/Material You
- **Build:** Gradle, AGP
- **Target SDK:** 35 | **Min SDK:** 34
- **Architecture:** Single-module Android app with MVVM pattern
- **Dependencies:** Firebase (Analytics, Crashlytics), Shizuku API, AndroidX DataStore, Compose BOM

## Build Commands

**Always use the Gradle wrapper** (`./gradlew`). JDK 17+ is required (JDK 23 used in CI).

| Task              | Command                                                                      |
|-------------------|------------------------------------------------------------------------------|
| Clean build       | `./gradlew clean build`                                                      |
| Debug build       | `./gradlew assembleDebug`                                                    |
| Play Release      | `./gradlew assemblePlayRelease`                                              |
| FOSS Release      | `./gradlew assembleFossRelease`                                              |
| Beta build        | `./gradlew assembleBeta`                                                     |
| Lint only         | `./gradlew lint`                                                             |
| Build + Lint (CI) | `./gradlew lint --build-cache && ./gradlew build sonar --info --build-cache` |
| List tasks        | `./gradlew tasks`                                                            |

**Note:** First build may take several minutes to download Gradle and dependencies.

## Required Files for Build

- `google-services.json` - Required at `app/src/play/` for the Play flavor. In CI, this is created
  from a GitHub secret.
- `app/src/main/kotlin/dev/lexip/hecate/util/DarkThemeHandler.kt` - Core theme handler. CI creates a
  mock version from a secret; the real file must exist locally.

## Project Structure

```
hecate/
├── app/
│   ├── build.gradle.kts          # App-level Gradle config (flavors: play, foss)
│   ├── proguard-rules.pro        # ProGuard/R8 rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # Core permissions, services
│       │   └── kotlin/dev/lexip/hecate/
│       │       ├── HecateApplication.kt
│       │       ├── broadcasts/
│       │       ├── data/
│       │       ├── services/
│       │       ├── ui/
│       │       │   ├── MainActivity.kt
│       │       │   ├── navigation/         # Type-safe navigation (Routes.kt)
│       │       │   ├── setup/              # Setup wizard (SetupViewModel.kt)
│       │       │   └── ...
│       │       └── util/
│       │           └── DarkThemeHandler.kt
│       ├── play/                 # Play Store flavor (Proprietary)
│       │   ├── AndroidManifest.xml
│       │   ├── google-services.json
│       │   └── java/dev/lexip/hecate/
│       │       ├── logging/      # Firebase implementation
│       │       └── util/         # InAppUpdateManager implementation
│       └── foss/                 # FOSS flavor (Open Source)
│           └── java/dev/lexip/hecate/
│               ├── logging/      # No-op implementation
│               └── util/         # No-op implementation
├── build.gradle.kts              # Root Gradle
└── ...
```

## Key Architecture Details

- **Flavors:**
    - `play`: Includes Firebase Analytics/Crashlytics and In-App Updates.
    - `foss`: Completely open source, no proprietary dependencies, no-op logging.
- **State Management:** MVVM with `AdaptiveThemeViewModel`, `SetupViewModel`, and Kotlin Flow.
- **Navigation:** Jetpack Compose Navigation with type-safe routes (`Routes.kt`).
- **Persistence:** AndroidX DataStore for user preferences.
- **Background Service:** `BroadcastReceiverService` (foreground service).
- **Theme Switching:** `DarkThemeHandler` writes to `Settings.Secure`.
- **Shizuku Support:** Optional permission grant via `ShizukuManager`.

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
2. Run `./gradlew assemblePlayDebug` - must succeed
3. Run `./gradlew assembleFossDebug` - must succeed
4. Verify no new lint warnings in changed files
5. Ensure license headers present on new Kotlin files

## Trust These Instructions

Use this document as the primary reference. Only search the codebase if information here is
incomplete or produces errors.

