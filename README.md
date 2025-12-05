[![GooglePlay](https://upload.wikimedia.org/wikipedia/commons/7/78/Google_Play_Store_badge_EN.svg)](https://lexip.dev/hecate/play)

**Adaptive Theme** automatically switches your device between light and dark mode based on ambient  
brightness — bright and readable in sunlight, easy on the eyes (and battery) in the dark.

Theme changes only happen right after the screen turns on and only when the device is uncovered to  
avoid flicker and unnecessary sensor usage.

**Package:** dev.lexip.hecate
  
---  

### Highlights

- Smart automatic light/dark theme switching
- Customizable brightness threshold
- Quick Settings tile for quick service toggling
- Lightweight and battery-friendly
- System-wide theme control
- No root required
- Modern Jetpack Compose app with native Material You design
- Only switches when the device is uncovered

---  

### One-time setup

To change the system theme, the app needs the WRITE_SECURE_SETTINGS permission. Because this is a  
system-level permission, you have to grant it via ADB:

- Enable developer options and USB debugging on your phone.
- Connect your phone to a computer with ADB installed.
- Run this command:  
  *adb shell pm grant dev.lexip.hecate android.permission.WRITE_SECURE_SETTINGS*

The app doesn’t need any special access beyond this permission. It is used only for theme switching.
  
---  

That’s it — open the app, set your threshold, and you’re done.

[![SonarCloud](https://sonarcloud.io/api/project_badges/quality_gate?project=xLexip_Hecate)](https://sonarcloud.io/summary/new_code?id=xLexip_Hecate)
