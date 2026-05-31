# Lockify v1.1.0 Release Notes (Upcoming)

## What's New in v1.1.0

- Hidden Vault — encrypt photos, videos, and files with AES-256 (hardware-backed key)
- Scheduled Locking — auto-lock selected apps by time and day of week
- Intruder Detection — silently capture a front-camera photo after repeated failed unlocks (stored encrypted on your device only)
- Location & Wi-Fi Rules — relax locking on trusted networks or at trusted places
- Notification Privacy — hide notification content for locked apps
- Usage Statistics — 7-day app usage insights with charts
- Lock Profiles & Child Mode — multiple profiles, each with its own PIN
- Encrypted Backup & Restore — password-protected local backups
- Quick Settings Tile, Home-Screen Widget & Shake-to-Lock
- Brute-Force Protection — escalating cooldown after repeated wrong attempts
- Scrambled PIN Keypad, wrong-PIN shake animation, and configurable PIN length

---

# Lockify v1.0.3 Release Notes

## What's New in v1.0.3

- Improved overall app stability and internal logic
- Enhanced privacy protection and recent-app security
- Improved app locking responsiveness and behavior
- Performance optimizations
- Bug fixes and general refinements

---

# Lockify v1.0.2 Release Notes

## What's New in v1.0.2

- Fixed Anti-Uninstall feature not intercepting removal attempts for protected apps
- Improved lock screen trigger speed — no visible preview of protected app content
- Fixed security logs export always failing with "Failed to export" error
- Performance improvements and smoother overall experience
- Bug fixes and stability improvements

---

# Lockify v1.0.1 Release Notes

## What's New in v1.0.1

- Improved performance and overall app smoothness
- Fixed minor bugs and stability issues
- Enhanced app locking reliability
- Optimized user experience
- General improvements and refinements

---

# Lockify v1.0.0 — Initial Release

## Core Features

- Full app lock protection — lock any installed app with a secure overlay
- PIN, pattern, and password authentication
- Biometric authentication (fingerprint / face) support
- Anti-uninstall protection via Device Administrator
- Forgot password recovery using device lock credentials
- Unlock time duration — keep apps unlocked for configurable period
- Trigger exclusions — exclude apps from triggering lock
- Auto unlock on correct PIN entry (optional)
- Maximum brightness option on lock screen
- Haptic feedback toggle
- Debug logging with export capability
- Usage Stats backend as alternative detection method

## Security

- Privacy-first design — zero data collection, no cloud sync
- Secure credential storage using Android SharedPreferences
- Anti-bypass protection — back press and app switching cannot bypass lock

## Performance

- Zero-lag lock screen overlay with instant trigger
- Optimized background service — minimal battery and memory usage
- App icon and label caching for smooth list rendering
