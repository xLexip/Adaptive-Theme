[![Adaptive Theme in the Google Play Store](https://i.ibb.co/bjHmz8Sh/feature-graphic-gh.png)](https://play.google.com/store/apps/details?id=dev.lexip.hecate)

# Adaptive Theme: Smart Dark Mode

Adaptive Theme intelligently automates your device's theme settings, switching between **Light and
Dark mode** based on your environment's **ambient light** — not just the time of day.

Get the readability of Light mode in bright daylight and the eye-comfort of Dark mode in low light.
This allows for a true auto dark mode experience that native Android doesn't offer.

[![Get it on Google Play](https://i.ibb.co/4RNvZBvK/Get-It-On-Google-Play-Badge-Web-color-English-1.png)](https://play.google.com/store/apps/details?id=dev.lexip.hecate)


---

## 📋 Table of Contents

- [💡 Why use Adaptive Theme?](#-why-use-adaptive-theme)
- [✨ Key Highlights](#-key-highlights)
- [🛠️ One-Time Setup](#%EF%B8%8F-one-time-setup)
- [⚙️ How it Works](#%EF%B8%8F-how-it-works)
- [✅ Safety](#-safety)
- [❓ FAQ](#-faq)
- [❤️ Support the Project](#%EF%B8%8F-support-the-project)
- [📱 Screenshots](https://play.google.com/store/apps/details?id=dev.lexip.hecate)

---

## 💡 Why use Adaptive Theme?

Most Android phones only switch themes at sunset or based on a fixed schedule. Adaptive Theme uses
your **light sensor** to switch intelligently, optimizing both **eye comfort** and **battery life**.

## ✨ Key Highlights

* 🌤️ **Smart Ambient Detection:** Uses your device's physical light sensor to toggle the system
  theme.
* ⚙️ **Full Customization:** Set your specific lux threshold (brightness level) and use the Quick
  Settings tile to quickly pause/resume the service.
* 🚀 **Modern & Native:** Built with **Jetpack Compose** and **Material You** for a smooth,
  crash-free experience.
* 🔋 **Battery Friendly:** The app is passive. It only checks the sensor when you turn the screen
  on — zero battery drain in the background.
* 🔒 **Privacy First:** Open Source, completely free, and no ads at all.
* 🗝️ **No Root Required:** Root access is not required (but is supported as an alternative setup
  method).
* 🐱 **Optional Shizuku Support:** One of multiple setup options is
  using [Shizuku](https://github.com/RikkaApps/Shizuku).

---

## 🛠️ One-Time Setup

Android restricts apps from changing system themes by default. To unlock this feature, a specific
permission (`WRITE_SECURE_SETTINGS`) is needed. After installing the app, you can choose any of the
following methods:

#### Method 1: Web Tool (Recommended)

Use our browser-based setup tool on a secondary device (Computer, Tablet, or Phone). No code or ADB
installation required (WebADB).
👉 **[lexip.dev/setup](https://lexip.dev/setup)**

#### Method 2: Shizuku (No PC)

If you have **Shizuku** installed and configured (via Wireless Debugging or Root), you can grant the
permission directly within the Adaptive Theme app.

#### Method 3: Root

If your device is rooted, you can grant the permission with one click inside the app.

#### Method 4: Manual ADB

If you have ADB installed on your computer, you can run the ADB grant command manually via your
terminal.

---

## ⚙️ How it Works

**Why didn't the theme change immediately?**

To prevent unnecessary battery drain and screen flickering, Adaptive Theme obeys the following
rules:

1. It checks the light sensor only **immediately after the screen turns on**.
2. It verifies that the light sensor is **not covered**.
3. It switches the theme **instantly** before you start interacting with the UI.

---

## ✅ Safety

The required permission does **not** grant root access or read any user data. It only allows the app
to change settings such as "Dark Mode" in the system settings. This is absolutely safe and
completely reversible by uninstalling the app.

---

## ❓ FAQ

**1. Does this require Root?**
No. It works on stock devices. However, if you have Root, it can optionally be used to set up the
service faster.

**2. Does it work with custom skins (MIUI, OneUI)?**
In most cases, yes. It works with any system that respects the native Android Dark Mode
implementation.

**Support & Feedback:** If Adaptive Theme not work for you or if you have any questions, please
create an Issue or send feedback via the app.

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

**🇩🇪 Made in Germany** – Engineered with precision (and 🥨 🍺).

---

## 📱 Screenshots

#### [More Screenshots](https://play.google.com/store/apps/details?id=dev.lexip.hecate)