[![Feature Graphic with App Screenshot](https://i.ibb.co/bjHmz8Sh/feature-graphic-gh.png)](https://play.google.com/store/apps/details?id=dev.lexip.hecate)

# Adaptive Theme: Auto Dark Mode by Ambient Light 

Adaptive Theme automatically switches between **Light** and **Dark mode** using your phone’s
**ambient light sensor (lux)** — not a fixed schedule.

It adapts to real lighting conditions to improve **readability**, **eye comfort**, and **battery
life**.

---

## 🚀 Quick Start (2 minutes)

<a href="https://play.google.com/store/apps/details?id=dev.lexip.hecate">
    <img src=".github/resources/get-it-on-google-play.svg" alt="Get Adaptive Theme on Google Play" width="200"/>
</a>

1. **Install** Adaptive Theme.
2. **Grant the permission** with our [web-tool](https://lexip.dev/setup), Shizuku, or other methods
   below.
3. **Pick your lux threshold** and you’re done. ✅

---

## 📋 Table of Contents

- [✨ Features & Highlights](#-features--highlights)
- [🛠️ One-Time Setup](#%EF%B8%8F-one-time-setup)
- [⚙️ How it Works](#%EF%B8%8F-how-it-works)
- [✅ Safety](#-safety)
- [❓ FAQ](#-faq)
- [❤️ Support the Project](#%EF%B8%8F-support-the-project)
- [🏗️ Architecture & Tech Stack](#%EF%B8%8F-architecture--tech-stack)

---

## ✨ Features & Highlights

* 🌤️ **Smart Detection:** Uses your devices physical light sensor to switch the system
  theme.
* ⚙️ **Custom brightness threshold:** Choose exactly when Light ↔ Dark should flip.
* 🔋 **Battery Friendly:** The app is passive. Its event-driven architecture only checks the sensor
  when you turn on the screen — zero battery drain in the background.
* 🗝️ **No Root Required:** Root access is not required (but is supported as an alternative setup
  method).
* 🐱 **Shizuku Support:** One of multiple setup options is
  using [Shizuku](https://github.com/RikkaApps/Shizuku).
* 🚀 **Modern & Native:** Built with best-practices using Kotlin, Jetpack Compose and Material You
  for a smooth and solid experience.
* 🔒 **Transparent:** Free, open-source, no-ads.

---

## 🛠️ One-Time Setup

Android restricts apps from changing system themes by default. To unlock this feature, the
permission (`WRITE_SECURE_SETTINGS`) has to be granted.

The app comes with an easy step-by-step setup process, that lets you choose one of the following
methods to do so:

* Web Tool (Recommended) – Use our browser-based setup tool on a secondary device (Computer, Tablet,
  or Phone). No code or ADB
  installation required (WebADB).
  👉 **[lexip.dev/setup](https://lexip.dev/setup)**

* **Shizuku** – If you have **[Shizuku](https://github.com/RikkaApps/Shizuku)** installed and
  configured, you can
  grant the permission directly within the Adaptive Theme app.

* **Root** – If your device is rooted, you can grant the permission with one tap inside the app.

* **Manual ADB** – If you have ADB installed on your computer, you can run the ADB grant
  command manually via your
  terminal.

---

## ⚙️ How it Works

**Wondering why the theme didn't change immediately?**

To avoid screen flicker and unnecessary background work, Adaptive Theme follows strict rules:

- **Event-driven:** It checks the light sensor only immediately after the screen turns on.
- **Validity check:** It verifies that the sensor is not obstructed (e.g., by a hand or pocket).
- **Seamless switch:** It switches the theme instantly, ensuring the UI is ready before you start
  interacting with it.

---

## ✅ Safety

The required permission only allows the app to change system settings such as the dark mode. This is
absolutely safe and
completely reversible by uninstalling the app. It does **not** grant root access or read any user
data.

---

## ❓ FAQ

**Does this require root?**

* No. It works on stock devices. However, if you have Root, it can optionally be used to set up the
  service faster.

**Does it work with custom Android skins (Xiaomi MIUI, Samsung OneUI, etc.)?**

* In most cases, yes. It works with any system that respects the native Android Dark Mode
  implementation.

**My theme doesn’t change — what should I check?**

- Keep in mind that the theme only switched immediately after the screen is turned on, to optimize
  sensor usage and to not interrupt
  your device usage.
- Check that your sensor isn’t covered when you turn the screen on.
- Adjust your lux threshold and test in clearly bright/dim conditions.

### Support & Feedback

If Adaptive Theme doesn’t work for you — or if you have any questions or ideas — please open an
issue here or send feedback via the app.

---

## ❤️ Support the Project

Adaptive Theme is **completely free**, **ad-free**, **open source**, and developed in my free time.

If you enjoy using the app, there are three simple ways you can support the project:

⭐ **Star on GitHub:** Give this repository a star to help others find it.

🌟 **Rate on Google Play:**
A [5-star rating](https://play.google.com/store/apps/details?id=dev.lexip.hecate)
is the best way to boost the ranking.

☕ **Buy me a Coffee:** If you are feeling generous, you can
also [buy me a coffee](https://buymeacoffee.com/lexip).

📣 **Spread the Word:** Share the app to help the project grow.

---

## 🏗️ Architecture & Tech Stack

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack-Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=Jetpack%20Compose&logoColor=white)](https://developer.android.com/compose)
[![Material-Design](https://img.shields.io/badge/material%20design-757575?style=for-the-badge&logo=material%20design&logoColor=white)](https://m3.material.io/)
[![Gradle](https://img.shields.io/badge/Gradle-02303A.svg?style=for-the-badge&logo=Gradle&logoColor=white)](https://gradle.org/)
[![SonarQube](https://img.shields.io/badge/Sonarqube-5190cf?style=for-the-badge&logoColor=white&logo=sonarr)](https://sonarcloud.io/)

Adaptive Theme is built with modern Android engineering standards to ensure a lightweight,
maintainable, and production-ready codebase.

**Modern Codebase:** Written entirely in Kotlin with Jetpack Compose and Material 3 (Material You).

**Architecture:** Follows the MVVM pattern with a Single-Activity architecture.

**Reactive Data:** ViewModels expose data via Kotlin Flows and manage concurrency with Coroutines.

**Persistence:** Type-safe settings storage with Jetpack DataStore.

**Background Work:** Sensor operations run event-driven – only upon screen-on
broadcasts – ensuring zero unnecessary battery drain in the background.

### **Made with 🥨 in Germany.**

> ~~> Keywords: theme switcher · android automation · night mode · dark sense · automatic dark
mode ·
brightness-based ·
light-based · based on lux · google pixel · auto dark theme · shizuku apps · android 14 · android
15 · android 16 ·
android 17 <~~
