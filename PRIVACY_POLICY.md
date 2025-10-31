# Privacy Policy — Adaptive Theme (aka. Hecate)

**Last updated:** 2025-10-31

## 1. Overview

Adaptive Theme (aka. Hecate) (the “App”). This Privacy Policy explains what
information the App collects (if any), how it is used and shared, and your choices. This policy
applies to the App distributed via Google Play and any in-app disclosures.

- App name shown on store: Adaptive Theme (aka. Hecate
- Package name (replace if different): `dev.lexip.hecate`
- Publisher / contact: X. Lexip — x@lexip.dev

## 2. Core principles

We follow a minimal-data principle. The App is designed to work without collecting personal
information. When we do collect technical or diagnostic data it is strictly limited to what is
necessary to run, diagnose, and improve the App.

## 3. Data we may collect and why

### 3.1 No personal information

The App does not collect personal information (name, email, account ID, payment information) as part
of normal usage. The only time personal information may be provided is if you voluntarily send it to
us (for example, an email when contacting support).

### 3.2 Device, diagnostics, and usage data

We use Firebase services (Firebase Analytics and Firebase Crashlytics) to collect aggregated and
diagnostic usage information. Examples of data sent to Firebase include:

- Crash stacks and related device metadata (Android version, device model, app version)
- Anonymous analytics events for app usage patterns

This data helps us fix crashes, improve stability, and improve user experience. Firebase is operated
by Google; data processing by Firebase is subject to Google’s privacy policies. You can review
Firebase’s privacy docs here: https://firebase.google.com/support/privacy

By using the App you explicitly consent to the collection, processing, and transfer of the
analytics and diagnostic data described above by Firebase Analytics and Firebase Crashlytics. If you
do not agree to this processing, please do not use the App; alternatively contact us at
x@lexip.dev and we'll advise on available options.

### 3.3 Sensitive capability: WRITE_SECURE_SETTINGS

The App implements features that require the Android capability `WRITE_SECURE_SETTINGS`. Important
points:

- `WRITE_SECURE_SETTINGS` cannot be granted by a normal runtime permission prompt. It must be
  granted externally (for example, with ADB or via an enterprise device-management policy).
- The App does not request or obtain this capability silently. If the capability is not granted the
  App will disable the related features and show an explanatory message to the user.
- We use this capability only to perform the specific feature described in the UI: modify the system
  theme (adaptive theme behavior). We do not use it to collect or transmit personal data.

## 4. Analytics, crash reporting, and third parties

We use Firebase Analytics and Firebase Crashlytics. These services process data on our behalf and
are contractually limited to that purpose. They may collect aggregated and device-level diagnostic
information. We do not sell user data or share it with other third parties for their own independent
use.

## 5. Data retention and deletion

- Crash reports and analytics data are retained by Firebase according to Firebase retention
  policies. Aggregated or anonymized analytics data may be kept indefinitely for product
  improvement.
- If you wish to request deletion of data associated with you, contact us at x@lexip.dev with a
  clear description of the request. We'll respond and take reasonable steps to comply, subject to
  any legal obligations to retain certain records.

## 6. Security

We implement reasonable technical and organizational measures to protect data we process. However,
no method of transmission or storage is completely secure. If you believe your data has been
compromised, contact us immediately at x@lexip.dev.

## 7. International data transfers

Firebase (Google) and other service providers may process data in countries outside your own. By
using the App you consent to the transfer and processing described in this policy.

## 8. Changes to this privacy policy

We may update this Privacy Policy. When we do, we will update the “Last updated” date above.