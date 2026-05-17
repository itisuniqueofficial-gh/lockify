# Manual QA Checklist

## Devices And Modes
- Pixel or stock Android, Samsung, Xiaomi/MIUI, Oppo/ColorOS, Vivo/Funtouch, Realme, OnePlus/OxygenOS, Motorola.
- Android 8+ through latest supported emulator/device available.
- Small phone, tablet, landscape, split-screen, and large font scale.

## Lock Flow
- Launch locked app from launcher, notification, deep link, recents, and after reboot.
- Switch from unlocked protected app to another app and back.
- Press Home, Back, Recents, rotate device, open notification shade, and lock/unlock screen.
- Verify close/back on Lockify overlay returns Home and does not reveal protected app content.

## Privacy
- Confirm screenshots and recents snapshots hide protected content.
- Verify lock-on-minimize relocks protected apps when policy requires it.
- Test split-screen, floating window, and picture-in-picture where the OEM supports them.

## Authentication
- PIN success, failure, rapid taps, auto-unlock, and forgot-password device credential flow.
- Pattern success, failure, skipped dots, interrupted gestures, and rotation.
- Biometric success, cancel, lockout, and fallback to PIN/pattern.

## Permissions
- First-run permission onboarding for Accessibility, Overlay, Usage Stats, Device Admin, Notifications.
- Deny each permission and verify the app shows a safe limitation state.
- Disable and re-enable Accessibility Service while Lockify is running.

## Release Smoke Test
- Install signed release APK.
- Confirm no debug-only UI/logging is visible.
- Verify app version and generated APK/AAB names match release workflow outputs.
