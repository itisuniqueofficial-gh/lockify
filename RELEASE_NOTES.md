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
