# Changelog

## 1.1.0 — Unreleased
Added
- Hidden vault: encrypt photos, videos, and files with AES-256-GCM keyed from the Android KeyStore (streaming, large-file safe).
- Scheduled locking: time and day-of-week windows with exact-alarm support.
- Intruder detection: silent front-camera capture after repeated failed unlocks, stored encrypted on-device only (no off-device upload).
- Location and Wi-Fi rules: relax locking on trusted networks or inside trusted geofences.
- Notification privacy: redact locked-app notification content via a notification listener.
- Usage statistics: 7-day per-app usage insights with charts.
- Lock profiles and child (whitelist) mode, each with its own PIN.
- Encrypted backup and restore (password-based PBKDF2 + AES-256-GCM).
- Quick Settings tile, home-screen widget, and shake-to-lock.

Security
- Brute-force protection: escalating cooldown after repeated failed attempts (3 → 30s, 5 → 2m, 10 → 10m).
- Scrambled PIN keypad option to resist shoulder-surfing.

Improved
- Wrong-PIN shake animation and configurable minimum PIN length.

## Earlier Unreleased
- Hardened PIN/pattern storage with salted PBKDF2 hashes and legacy migration.
- Made lock overlay close/back return to Home instead of revealing protected content.
- Added unit tests for credential hashing.
- Documented privacy, security, Play Store compliance, release, and manual QA expectations.
