# Permission Guide

## Accessibility Service
- Required for primary real-time app detection.
- Show prominent disclosure before opening system Accessibility settings.
- If disabled, mark protection as limited and guide the user to re-enable it.

## Usage Stats
- Optional fallback backend.
- Use only for foreground app package detection.
- Avoid aggressive polling when the device is locked or protection is disabled.

## Display Over Other Apps
- Required to show Lockify above protected apps.
- If denied, do not claim protected apps are fully locked.

## Device Admin
- Optional anti-uninstall protection.
- Must remain transparent and user-controlled.

## Notifications
- Needed for foreground-service notification behavior on supported Android versions.
- If denied, keep core app UI functional and explain any service limitation.
