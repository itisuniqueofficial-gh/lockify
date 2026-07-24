<div align="center">
  <img src="fastlane/metadata/android/en-US/images/featureGraphic.png" alt="AppLock Android Privacy Security" width="600" />
</div>

  <h1 align="center">Lockify</h1>
<p align="center"><b>Secure App Lock by It Is Unique Official</b></p>

<p align="center">
  <a href="https://opensource.org/licenses/MIT">
    <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License">
  </a>
  <a href="https://github.com/itisuniqueofficial-gh/lockify">
    <img src="https://img.shields.io/badge/GitHub-lockify-black?logo=github" alt="GitHub Repository">
  </a>
  <a href="https://lockify.itisuniqueofficial.com/">
    <img src="https://img.shields.io/badge/Website-Lockify-2ea44f" alt="Website">
  </a>
</p>

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="30%" alt="App List"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="30%" alt="Settings"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="30%" alt="Password Screen"/>
</p>

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="30%" alt="Set Password"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" width="30%" alt="Unlock time"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" width="30%" alt="Unlock time"/>
</p>
<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/7.png" width="30%" alt="Set Password"/>
</p>

---

## Overview

Lockify is an Android app locker maintained by It Is Unique Official.
It helps protect selected apps with PIN, pattern, and biometric authentication while keeping
the core experience lightweight and on-device.

<br/>

> [!CAUTION]
> Google Play Protect may warn during install or update because Lockify relies on overlay and accessibility-related permissions to secure apps. It may show a false pretext of "this app may try to access sensitive information"
> without any base or information. If this happens to you, consider disabling Play Protect temporarily as mentioned [here](https://www.airdroid.com/quick-guides/disable-google-play-protect).
>
> You may enable it back later after you install the app. We understand this introduces unnecessary friction but there's nothing we can do about it. Google does not like it
> when other developers try to fill the gaps they create themselves.

<br/>

> [!NOTE]
> Verify the app and source before installing any security-sensitive software.
>
> This repository contains the full Android source for Lockify.

<br/>

## Features

- Material You design, adapts to your theme
- Biometric and PIN authentication
- Fingerprint, Face Unlock, and PIN support
- Lock any app on your device
- Anti-uninstall protection
- Unlock timeout for convenience
- No root required
- One-tap app locking
- All data stays on your device
- Real-time background protection
- Lightweight and fast

### New in v1.1.0 (upcoming)

- Hidden vault — encrypt photos, videos, and files (AES-256, hardware-backed key)
- Scheduled locking by time and day of week
- Intruder detection — silent front-camera capture, encrypted on-device only
- Location and trusted Wi-Fi rules
- Notification privacy for locked apps
- Usage statistics with on-device charts
- Lock profiles and child (whitelist) mode
- Encrypted backup and restore
- Brute-force cooldown and scrambled PIN keypad
- Quick Settings tile, home-screen widget, and shake-to-lock

<br/>

## Play Store

App name: `Lockify`

Package: `com.itisuniqueofficial.lockify`

Short description: `Secure your apps with smart protection - Lockify by It Is Unique Official.`

Website: https://lockify.itisuniqueofficial.com/

## Development

### Requirements

- Android Studio
- JDK 17+
- Android SDK configured locally

### Build

```bash
./gradlew assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

### Build a release locally

```bash
# Debug build
./gradlew :app:assembleDebug

# Release build (falls back to debug signing if no keystore is configured)
./gradlew :app:assembleRelease :app:bundleRelease
```

To produce a *release-signed* build locally, provide the signing inputs as
Gradle properties or environment variables: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`.

## Continuous Integration & Releases

Lockify uses GitHub Actions for a fully automated, secure release pipeline.

### Workflows

| Workflow | Trigger | What it does |
| --- | --- | --- |
| `ci.yml` | push to `main`/`master`/`develop`, manual dispatch | wrapper validation, lint, unit tests, debug APK, uploads reports & APK |
| `pull-request.yml` | pull requests | lint, unit tests, debug build verification (merge gate) |
| `auto-release.yml` | push to `main`/`master`, manual dispatch | **per-commit** signed APK + AAB published as a **prerelease** GitHub Release (`auto-build-*`) |
| `release.yml` | push tag `v*.*.*`, manual dispatch (version input) | **official** signed release: tests, lint, signed APK + AAB, verification, checksums, release notes, GitHub Release (marked latest) |
| `nightly.yml` | nightly schedule, manual dispatch | debug build + tests, uploads a `Nightly Build` artifact (no release) |

### Automatic build releases (every commit)

Every push to the default branch triggers `auto-release.yml`, which runs tests +
lint, builds and signs the APK and AAB, verifies signatures and version,
generates checksums and detailed release notes (with build + commit info), and
publishes a **prerelease** GitHub Release:

```text
Push to main  →  Auto Release workflow  →  signed APK + AAB  →  prerelease GitHub Release
```

- **Tag / build id:** `auto-build-<run-number>-<short-sha>` (unique per commit; re-runs are idempotent).
- **Version:** `versionName = <latest-official>-build.<run-number>`, `versionCode = <run-number>` (strictly increasing).
- **Assets:** `Lockify-v<version>-release.apk`, `Lockify-v<version>-release.aab`, `Lockify-v<version>-SHA256SUMS.txt`, and `Lockify-v<version>-mapping.txt`.
- These builds are **prereleases** and never take the repository's "Latest release" badge — that is reserved for official `v*.*.*` releases, which are produced separately by `release.yml`.

Find the newest automated APK/AAB on the [Releases page](https://github.com/itisuniqueofficial-gh/lockify/releases) at the top of the list (marked *Pre-release*). A failed build never publishes a release.

To trigger one manually: *Actions → Auto Release (per-commit build) → Run workflow*.

### Cut a release

Everyday development just needs a push — CI runs automatically:

```bash
git add .
git commit -m "feat: improve app locking"
git push
```

Publishing a production release is intentional and tag-driven:

```bash
git tag v1.2.0
git push origin v1.2.0
```

That single tag push triggers:

```text
Push tag v1.2.0
      ↓
Resolve & validate version (semver)
      ↓
Run unit tests + lint
      ↓
Build signed release APK + AAB
      ↓
Verify signatures, package id, versionName/versionCode
      ↓
Generate SHA-256 checksums
      ↓
Generate release notes (from Conventional Commits)
      ↓
Create GitHub Release + upload APK, AAB, SHA256SUMS, mapping
```

Alternatively, maintainers can run the **Release** workflow manually via
*Actions → Release → Run workflow* and enter a version (e.g. `1.2.0`). The
workflow validates that the version is new and newer than the latest release,
then creates and pushes the tag for you.

### Versioning

Lockify follows [Semantic Versioning](https://semver.org): `MAJOR.MINOR.PATCH`.
The Android `versionCode` is derived deterministically from the version by
`scripts/version.sh`:

```text
versionCode = MAJOR * 1_000_000 + MINOR * 1_000 + PATCH
# e.g. 1.2.0 -> 1002000
```

This guarantees monotonically increasing, reproducible, collision-free version
codes. `versionName` and `versionCode` are injected into the build via
`-PVERSION_NAME` / `-PVERSION_CODE` (or the matching environment variables).

### Release artifacts

Each GitHub Release contains:

```text
Lockify-v<version>-release.apk      # direct installation
Lockify-v<version>-release.aab      # Google Play upload
Lockify-v<version>-SHA256SUMS.txt   # checksums
Lockify-v<version>-mapping.txt      # R8/ProGuard mapping (deobfuscation)
```

Verify a downloaded build:

```bash
sha256sum -c Lockify-v<version>-SHA256SUMS.txt
```

### Required GitHub Secrets

Store these in the repository under a protected **`production`** environment
(*Settings → Environments → production → Secrets*). Never commit their values.

| Secret | Purpose |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | base64-encoded release keystore (`base64 -w0 release.jks`) |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | signing key alias |
| `ANDROID_KEY_PASSWORD` | signing key password |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | *(future)* Google Play publishing — not required yet |

The release workflow decodes the keystore to a temporary file, signs the APK
and AAB, verifies the signatures, and shreds the keystore at the end of the run.
Passwords are passed only through GitHub Secrets and are never printed.

### Maintainer operations

- **Re-run a failed release:** re-run the failed *Release* workflow run from the
  Actions tab. The build is reproducible; existing valid assets are not deleted.
- **Rotate signing keys:** generate a new keystore, update the four
  `ANDROID_*` secrets, and cut a new release. (Note: Play requires the same
  upload key or Play App Signing key rotation.)
- **Recover from a bad tag:** delete the tag and its draft/release, fix the
  issue, and push the tag again.
- **Branch protection (recommended):** require the `Pull Request` checks to pass
  before merging into `main`.

Additional production notes:
- `PRIVACY.md`
- `SECURITY.md`
- `PLAY-STORE-COMPLIANCE.md`
- `CONTRIBUTING.md`
- `docs/permission-guide.md`
- `docs/manual-qa-checklist.md`

## Use Cases

- Shared devices
- Parental controls
- Protecting work apps
- General privacy

<br/>

## Maintainer

Maintained by It Is Unique Official

* Website: https://lockify.itisuniqueofficial.com/
* Portfolio: https://my.itisuniqueofficial.com
* YouTube: https://www.youtube.com/@itisuniqueofficial_yt
* Instagram: https://www.instagram.com/jayadtt_khodave
* LinkedIn: https://in.linkedin.com/in/iamjaydatt

---

## License

Released under the MIT License. See `LICENSE`.
