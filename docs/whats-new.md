# Release announcements

The main screen shows “What’s new” after an existing user moves to a newer major/minor
release. The identity comes from `BuildConfig.VERSION_NAME`; patches and build suffixes
share the same identity (for example, `2.6.0`, `2.6.1`, and `2.6.0-beta` all use `2.6`).

Closing the dialog, using Back/outside dismissal, or successfully opening the changelog
saves that identity in Preferences DataStore. Merely displaying it does not acknowledge
it. An unread announcement survives process restarts and patch updates. Skipped versions
show only the installed release’s announcement; downgrades preserve the newer acknowledgement.

Fresh installs save the installed release as their baseline before setup finishes. For
the first rollout, users with completed setup and no saved identity receive the current
announcement. The dialog waits for setup permission, monitoring startup errors, and
other dialogs. After it appears, GitHub star and review prompts are suppressed for that
ViewModel session.

## Preparing a release

For device testing, debug builds currently ignore the saved acknowledgement and skip
all announcement preference writes. The dialog can be closed for the current ViewModel
session and appears again on the next app launch, including a direct debug installation
without completed setup, secure-settings permission, or a previous version. Monitoring
startup errors also do not block the debug preview. Other open dialogs still take priority.
Release and beta builds keep the normal persistent behavior. Set the `previewWhatsNew` default to `false` in
`MainViewModel` to restore normal behavior in debug builds as well.

- Keep release notes up to date on GitHub. The highlighted changelog button always
  opens `https://github.com/xLexip/Adaptive-Theme/releases`, regardless of the installed
  version. The dialog contains only a localized invitation to read the changelog;
  no release-specific descriptions are bundled in the app. Close remains a text button.
- Announcement decisions and bundled text work offline. GitHub opens only after the
  user chooses “View changelog”. If no browser can open it, the dialog stays
  available and a message explains where to find the notes.

## Diagnostics

The `WhatsNew` Logcat tag records impressions, dismissal, and changelog opening outcomes.
Play builds send the corresponding `whats_new` event through the existing analytics gate,
with `action`, `outcome`, and `release_version`. FOSS remote logging is a no-op. Logging
failures do not prevent dismissal or acknowledgement. Impressions are deduplicated by
the ViewModel and recorded only while the dialog’s lifecycle is resumed.
