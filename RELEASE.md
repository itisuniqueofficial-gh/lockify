# Release

## Local Debug Build
Windows PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat clean :app:assembleDebug
```

Linux/macOS:

```bash
./gradlew clean :app:assembleDebug
```

## Signed Release Build
Required signing inputs:

```text
KEYSTORE_FILE
KEYSTORE_PASSWORD
KEY_ALIAS
KEY_PASSWORD
```

Command used by auto-release CI:

```bash
./gradlew --no-daemon clean bundleRelease assembleRelease
```

## GitHub Actions
- `.github/workflows/android.yml` builds debug only and should not run `assemble` without signing secrets.
- `.github/workflows/android-auto-release.yml` creates the next patch tag, builds signed APK/AAB, and creates a GitHub Release.
- Editing workflow files requires a GitHub token with `workflow` scope.
