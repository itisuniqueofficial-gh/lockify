# Play Store Compliance Notes

## Accessibility Service
- Purpose: detect selected protected apps and show authentication before app content is used.
- User disclosure must be shown before opening Accessibility settings.
- Do not use Accessibility data for analytics, remote collection, advertising, or hidden behavior.

## Device Admin
- Purpose: optional anti-uninstall protection controlled from Lockify settings.
- The app must explain how to disable anti-uninstall before uninstalling.
- Do not prevent removal deceptively or without clear user opt-in.

## Overlay Permission
- Purpose: display the lock screen above protected apps.
- If permission is denied, show a safe fallback message and do not claim protection is active.

## Usage Stats
- Purpose: optional fallback backend for foreground app detection.
- If permission is denied, keep Accessibility as the primary supported backend.
