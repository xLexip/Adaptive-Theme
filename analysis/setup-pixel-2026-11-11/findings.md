# Pixel setup detection investigation — 2026-11-11

## Finding

The connected Pixel's Android implementation applies settings redaction inconsistently between the first tracked read and subsequent fetches using an existing generation tracker. This explains the reported difference between Developer options already being enabled and being enabled while Adaptive Theme remains open.

Adaptive Theme has a separate compatibility defect: it treats these redacted readings as authoritative disabled states and blocks both Continue and the Developer options shortcut.

## Evidence and limits

- Device fingerprint read through ADB: `google/grizzly/grizzly:17/CP4A.261105.040.D1/16731845:user/release-keys`.
- Installed app: `dev.lexip.hecate`, version `2.5.1-debug`, versionCode 139, targetSdk 37, appId 10490. The user confirms it is built from current develop.
- Shell reads of `development_settings_enabled` and `adb_enabled` both returned `1`.
- The user reproduced advancement after turning Developer options off and back on while Adaptive Theme remained open, on this same build.
- Actual `/system/framework/framework.jar` and `/system/priv-app/SettingsProvider/SettingsProvider.apk` were copied from the connected phone and disassembled using Android build-tools 37.0.0 `dexdump`.
- The framework's actual DEX annotations on both `Settings.Global.ADB_ENABLED` and `Settings.Global.DEVELOPMENT_SETTINGS_ENABLED` contain `Settings.Readable(redactedValue="0")`.
- Selected disassembly is preserved in `platform-evidence.txt`. Full copied platform files and disassembly remain in the host temporary directory `C:\Users\robin\AppData\Local\Temp\adaptive-theme-setup-probe`.
- App-side runtime values were not captured: debugger attachment did not complete. The mechanism below is verified in the installed platform code; its execution in the reported app session is inferred from that code plus the user's reproduction. No device settings, app permissions, app data, or installed app were changed. Temporary debugger forwarding was removed.

## Mechanism in this exact build

The following pseudocode reconstructs the relevant control flow; it is not verbatim source.

`Settings.NameValueCache.getStringForUser()` reads the live generation and cached entry. It returns a cached value when generations match. When generations differ, it invalidates the cached entry but retains the tracker for a predefined setting. On a cache miss it calls `fetchAndCacheString()` with the live generation.

```text
fetchAndCacheString(..., liveGeneration):
    needsTracker = liveGeneration < 0
    if needsTracker:
        request["_track_generation"] = null
    response = provider.call("GET_global", key, request)
    cache(response.value, response/current generation)
    return response.value
```

The provider's `packageValueForCallResult()` has these two different branches:

```text
if not trackingGeneration:
    return setting.getValue()                 // actual value
else:
    result.value = getEffectiveValue(setting) // applies redaction
    attach generation-tracking data
    return result
```

`getEffectiveValue()` exempts callers with appId below 10000, system apps, and platform-signed apps. For an ordinary application it returns the annotated redacted value if present. Adaptive Theme is not one of those exempt callers. The shell is exempt.

Consequently:

1. First read in a fresh app process: no tracker; provider applies redaction; app sees `0` and caches it.
2. Further polls with no setting change: cached redacted `0` remains valid.
3. User toggles the setting: its generation changes; cached entry becomes invalid, but the tracker remains available.
4. Refetch with the existing tracker: `_track_generation` is omitted; provider returns the actual value through the unredacted branch.
5. Once both app readings are `1`, setup's existing polling advances step 1.

This is a platform redaction inconsistency exposed by the generation cache, rather than evidence that Developer options were actually disabled. A fresh process becoming stuck again after a successful toggle is a prediction of this mechanism, not a test performed in this investigation.

## App impact

- `SetupEnvironmentProvider.kt:40-55`: both settings are read with `Settings.Global.getInt(..., 0) == 1`; exceptions also become `false` without logging.
- `SetupViewModel.kt:139` and `:238`: step 1 requires both settings to read true.
- `A_DeveloperModeScreen.kt:154`: Continue is disabled unless both readings are true.
- `A_DeveloperModeScreen.kt:284`: the Developer options shortcut is disabled when the Developer options reading is false.
- The provided logcat shows About phone opening and the app returning, with polling continuing. It contains no settings-read values or exceptions.

## Recommended application fix

Allow users to open Developer options and manually continue even when these readings cannot confirm the state. Treat the readings as advisory and distinguish an unconfirmed state from a reliable disabled state. Keep actual `WRITE_SECURE_SETTINGS` permission verification as the completion requirement. Do not depend on the unredacted provider branch: Android documents both settings as returning zero to third-party apps, and a platform fix can close this inconsistent path.

No application source changes, APK installation, or behavioral tests were performed.

## Platform artifact hashes

SHA-256:

```text
framework.jar
466BA9AA9934AA5BC113F9EE738E3FD66CF92B0BB3D02A144D0669B59381015F

SettingsProvider.apk
CAA6B509A3E14F8179B013E67AD553C2BF09DD249F64FA75C5BDAFFCC20B01F2
```

## Public references and earlier interpretation

- Upstream report: https://issuetracker.google.com/issues/584687953
- Adaptive Theme issue: https://github.com/xLexip/Adaptive-Theme/issues/238
- https://developer.android.com/reference/android/provider/Settings.Global#DEVELOPMENT_SETTINGS_ENABLED
- https://developer.android.com/reference/android/provider/Settings.Global#ADB_ENABLED
- https://android.googlesource.com/platform/frameworks/base/+/5d466bf8025ed8a68d9d49b47289f4f5cefbc139

The earlier AOSP implementation inspected during this investigation placed redaction before the client cache. That implementation does not describe this Pixel build. Reading the installed platform artifacts established the provider-side branch difference above and corrected that earlier interpretation.

## Subsequent application fix for 2.6.0

Developer options and USB debugging readings now distinguish unconfirmed values from disabled settings. A zero reading on Android 17 or newer is treated as unconfirmed; positive detection still supports automatic advancement. The Developer options shortcut remains available, and manual Continue becomes available after four seconds on step 1 without changing the button label. Continuing does not mark the device settings as detected or bypass the final permission check. The new instructions are localized in all 54 resource files.

Validation: all 273 FOSS JVM tests passed, FOSS Android lint passed, and the Play debug APK built successfully. All 12 setup UI tests passed on the connected Pixel 11 Pro running Android 17, including the disabled-before-four-seconds/enabled-afterwards check and the permission-gated Finish behavior. The first device run could not find Compose hierarchies while the phone was asleep behind the lock screen; the rerun passed after the user unlocked it. The updated debug app was installed for this device run.
