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
- `.github/workflows/ci.yml` — runs on pushes and manual dispatch: wrapper validation, lint, unit tests, and a debug APK build (no signing).
- `.github/workflows/pull-request.yml` — runs on pull requests: lint, unit tests, and debug build verification (merge gate).
- `.github/workflows/release.yml` — runs on `v*.*.*` tag pushes or manual dispatch: builds and signs the APK + AAB, verifies signatures/version, generates checksums and release notes, and creates the GitHub Release.
- `.github/workflows/nightly.yml` — scheduled debug build + tests; uploads a `Nightly Build` artifact only (never publishes a release).
- Editing workflow files requires a GitHub token with `workflow` scope.

See the README "Continuous Integration & Releases" section for the full flow and required secrets.
