# Background monitoring guidance

The in-app card is a troubleshooting aid for phones with locally observed, repeated monitoring interruptions. It does not detect the state of OEM battery settings or prove that a manufacturer policy caused an interruption.

## Display policy

All conditions must hold:

- The app is showing its main screen.
- Adaptive Theme is enabled in preferences.
- The manufacturer is Motorola, OnePlus, Xiaomi, Redmi, POCO, realme, or OPPO (case-insensitive). Redmi and POCO use the Xiaomi guide. Other manufacturers, including Google, do not get this card.
- The local tracker has observed at least two qualifying exits with original exit timestamps within the past seven days.
- The card has not been dismissed. Dismissal is saved in DataStore and is not reset by new interruptions, app updates, rebooting, or toggling monitoring.

The Don’t Kill My App guide remains the filled primary action. App settings is secondary. There is no help-menu item or Pixel testing override.

## Qualifying interruption

At each application startup, the existing process-exit detector passes Android's historical exit records to a separate local tracker. Only the newest main-process record is evaluated, even if a newer one has an excluded reason. Older records cannot supply a trustworthy recovery gap: they may already have recovered in an earlier process. A separately persisted timestamp prevents double-counting or reconsidering a promptly recovered exit on a later startup.

An exit qualifies only if:

1. Its original timestamp is valid, within seven days, and at least 60 seconds before this application startup.
2. Its recorded importance is `IMPORTANCE_FOREGROUND_SERVICE`.
3. Its `ApplicationExitInfo.processStateSummary` contains this feature's versioned active-monitoring marker, written after the screen-on receiver was successfully registered.
4. The marker predates the exit, was not invalidated by an intentional stop, and matches the current `Settings.Global.BOOT_COUNT`. Missing boot identity or a different boot is ineligible.
5. The reason is `REASON_LOW_MEMORY`, `REASON_SIGNALED` with `SIGKILL`, or `REASON_OTHER` accompanied by an observed cleanup signature: `OneKeyClean`, `GameClean`, `MemoryGuardian`, `SmartClean`, `o-kill(`, `o-stop(`, or a description starting with `[KILL UID] Foreground kill`. Matching ignores case.

Crashes, ANRs, application resource-limit errors, updates, permission changes, user stops, generic `OTHER`/`UNKNOWN`, cached-process records, and child-process exits do not qualify. No special permissions or root are needed.

Marker updates occur only on monitoring lifecycle transitions. Initialization failures, explicit controller/notification stops, and service destruction clear the marker. A saved stop timestamp guards against a stale OS marker if Android throttles the clear call. If marker writing is unavailable, the feature fails closed. This tracker does not change service restart behavior or theme switching.

Only the two newest qualifying timestamps and bookkeeping timestamps are retained in a dedicated local preferences file. The file is excluded from cloud backup and device transfer. UI eligibility is recalculated when the ViewModel is created and when the app resumes, allowing old evidence to expire without a periodic job. The original permanent-dismissal preference is preserved.

## Limits

- The 60-second threshold measures the gap from the last process exit to the next application startup, not a measured delay to receiver readiness or a missed theme switch.
- Exit descriptions are human-readable, vendor-specific, and unstable. The narrow allowlist can miss real kills; unknown formats deliberately leave the card hidden.
- An explicit cleanup action can produce the same record as automatic cleanup. The card says monitoring was interrupted, not that the vendor killed it automatically or that the guide guarantees a fix.
- Android retains finite history. This policy intentionally ignores older backfilled entries and records from before active tracking was present, including immediately after an upgrade.
- This cannot detect a service, broadcast, or sensor stall while the process remains alive. It also excludes exits across a reboot and fast automatic process restarts.
- Analytics logging and sampling are separate. Local eligibility works in Play and FOSS builds, without network access or Firebase collection.

## Validation on a device

- After an upgrade or fresh install, enable monitoring: the card should remain hidden on every phone without qualifying local evidence.
- On a Pixel, the card remains hidden even if exit evidence exists because the manufacturer is unsupported.
- Normal monitoring off/on, force-stop, reboot, or an app update must not create qualifying evidence.
- On a supported vendor, once real qualifying interruptions make the card eligible, verify the manufacturer guide, app settings action, and permanent dismissal after closing/reopening the app.
- Do not use force-stop to simulate an eligible kill: Android classifies that as user-requested. Automated tests inject exit records to exercise two interruptions, rapid recovery, reboot, unknown records, expiration, and OS-marker failure without changing the physical phone installation.

Platform references: [ActivityManager.setProcessStateSummary](https://developer.android.com/reference/android/app/ActivityManager#setProcessStateSummary(byte[])) and [ApplicationExitInfo](https://developer.android.com/reference/android/app/ApplicationExitInfo).
