# Testing

## Automated Checks
Windows PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat clean :app:testDebugUnitTest :app:assembleDebug
```

Linux/macOS:

```bash
./gradlew clean :app:testDebugUnitTest :app:assembleDebug
```

## Focus Areas
- Credential hashing and legacy migration.
- Accessibility app launch and app switch detection.
- Recents, split-screen, floating window, and lock-on-minimize behavior.
- PIN, pattern, biometric, and device credential unlock flows.
- Permission denial and re-enable flows.

See `docs/manual-qa-checklist.md` for device QA steps.
