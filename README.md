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

# Release build — requires a release keystore (see below)
./gradlew :app:assembleRelease :app:bundleRelease
```

Provide the signing inputs as Gradle properties or environment variables:
`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. A release
build is always signed with a real release key — every published build shares
one stable certificate so it installs cleanly as an update. If no keystore is
configured the release build **fails loudly** rather than emitting an unsigned
or debug-signed APK (which would cause "App not installed" on update). For a
throwaway local build only, you may opt in explicitly:

```bash
./gradlew :app:assembleRelease -PallowDebugSigningForRelease=true
```

> Never distribute a build produced this way — it is signed with the local
> debug key and cannot update an official release.

### Troubleshooting: "App not installed"

Official Lockify APKs are signed with a single, stable release key and use the
v2 + v3 APK signature schemes, so they install and update cleanly on Android
8.0 (API 26) and newer. If you still see "App not installed":

- **A build signed with a different key is already installed** (e.g. an older
  sideloaded/debug build). Android blocks updates across different signing
  certificates — uninstall the existing copy, then install the official APK.
- **Incomplete download** — re-download and verify with the release's
  `Lockify-v<version>-SHA256SUMS.txt` (`sha256sum -c`).
- **Sideloading disabled** — allow installation from your browser/file manager
  in Android settings.

## Continuous Integration & Releases

Lockify uses GitHub Actions for a fully automated, secure release pipeline.

### Workflows

| Workflow | Trigger | What it does |
| --- | --- | --- |
| `ci.yml` | push to `main`/`master`/`develop`, manual dispatch | wrapper validation, lint, unit tests, debug APK, uploads reports & APK |
| `pull-request.yml` | pull requests | lint, unit tests, debug build verification (merge gate) |
| `auto-version.yml` | push to `main`/`master`, manual dispatch | **automatic semantic-version bump** from commits → signed APK + AAB → official `Lockify vX.Y.Z` release (only when a `feat`/`fix`/`perf`/`security`/breaking change is present) |
| `auto-release.yml` | push to `main`/`master`, manual dispatch | **per-commit** signed prerelease build (`auto-build-*`), only for pushes that do **not** warrant an official release |
| `release.yml` | push tag `v*.*.*`, manual dispatch (version input) | manual/official signed release path (same outputs as `auto-version.yml`) |
| `nightly.yml` | nightly schedule, manual dispatch | debug build + tests, uploads a `Nightly Build` artifact (no release) |
| `security.yml` | push, PR, weekly schedule, dispatch | secret/keystore scan, Android release-config audit, PR dependency review |
| `issue-detection.yml` | on failure of CI/release workflows | opens/updates a deduplicated GitHub Issue with failure context |
| `automated-fix.yml` | manual dispatch (issue #) or `auto-fix` label | applies safe deterministic fixes on a branch, validates, opens a PR (never touches `main`) |

### Automatic versioning & official releases

Every push to the default branch runs `auto-version.yml`, which calculates the
next version from [Conventional Commits](https://www.conventionalcommits.org)
since the last tag:

| Commit type | Bump | Example |
| --- | --- | --- |
| `fix:` / `perf:` / `security:` | PATCH | 1.2.3 → 1.2.4 |
| `feat:` | MINOR | 1.2.3 → 1.3.0 |
| `feat!:` / `BREAKING CHANGE:` | MAJOR | 1.2.3 → 2.0.0 |
| docs / chore / ci / refactor / test only | none | no official release |

When a bump is warranted it builds and signs the APK + AAB, enforces strict
version consistency, updates `CHANGELOG.md`, creates the tag, and publishes the
official release — all values are guaranteed identical:

```text
versionName = tag (vX.Y.Z) = release title (Lockify vX.Y.Z)
            = Lockify-vX.Y.Z.apk = Lockify-vX.Y.Z.aab = CHANGELOG heading
```

`versionCode = MAJOR*1_000_000 + MINOR*1_000 + PATCH` (deterministic, monotonic).
The changelog/tag commit is pushed with `GITHUB_TOKEN`, which does not re-trigger
workflows, so there are no release loops.

### Per-commit prerelease builds

Pushes that do **not** warrant an official release (docs/chore/ci-only) instead
produce a signed **prerelease** via `auto-release.yml`, so every commit still has
a downloadable build:

- **Tag / build id:** `auto-build-<run-number>-<short-sha>` (unique; re-runs idempotent).
- **Version:** `versionName = <latest-official>-build.<run-number>`, `versionCode = <run-number>`.
- Marked **prerelease** with `make_latest: false`, so it never takes the repo's "Latest release" badge (reserved for official `vX.Y.Z` releases).

The two push workflows are mutually exclusive — exactly one build/release is
produced per push. A failed build never publishes a release.

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
Lockify-v<version>.apk              # direct installation
Lockify-v<version>.aab              # Google Play upload
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
