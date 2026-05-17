# Privacy

Lockify is designed to keep app-lock data on the device.

## Data Stored Locally
- Locked package names and trigger exclusions are stored in private app preferences.
- PINs and patterns are stored as salted PBKDF2 hashes, not plain text.
- Unlock session state is in memory and cleared on screen off, recents/minimize security events, and service resets.

## Sensitive Permissions
- Accessibility Service is used to detect protected app windows and show the lock screen.
- Usage Stats is an optional fallback to detect foreground apps when Accessibility is not used.
- Display over other apps is used to present the lock screen over protected apps.
- Device Admin is used only for transparent anti-uninstall protection controlled by the user.

## What Lockify Must Not Do
- Do not log PINs, patterns, biometric data, or user-entered secrets.
- Do not upload locked app lists or permission data.
- Do not hide Accessibility or Device Admin purpose from users.
