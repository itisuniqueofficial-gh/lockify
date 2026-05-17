# Agent Notes

## Project Shape
- Android Gradle project with modules `:app`, `:appintro`, `:hidden-api`, and `:patternlock`; root includes are in `settings.gradle.kts`.
- Main app package/application ID is `com.itisuniqueofficial.lockify`; Android entrypoint is `app/src/main/java/com/itisuniqueofficial/lockify/MainActivity.kt`.
- App-lock runtime code is mostly under `app/src/main/java/com/itisuniqueofficial/lockify/services/` and `features/lockscreen/`; the reusable pattern widget lives in `patternlock/src/main/java/com/mrhwsn/composelock/`.
- `hidden-api` is a compile-only/stub module used by the app with HiddenApiBypass/refine; do not treat it like normal app code.
- `web-app/` is a separate Jekyll site deployed by GitHub Pages, not part of the Android build.

## Commands That Matter
- Windows local debug build used in this workspace:
  ` $env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"; $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"; $env:ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"; .\gradlew.bat clean :app:assembleDebug`
- Linux/macOS debug build: `./gradlew clean :app:assembleDebug`.
- CI intentionally builds debug only with `./gradlew clean :app:assembleDebug`; `./gradlew assemble` also packages release and fails without release signing secrets.
- Release build command in auto-release workflow: `./gradlew --no-daemon clean bundleRelease assembleRelease`.
- Website local serve: `cd web-app && bundle install && bundle exec jekyll serve`.

## Build And Signing Gotchas
- Android modules use `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`; do not bump Compose/material3 beyond versions compatible with compile SDK 36 unless also updating SDK install steps.
- Release signing reads Gradle properties first, then env vars: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- GitHub workflows pass signing via `ORG_GRADLE_PROJECT_KEYSTORE_*`; preserve these when editing release workflows.
- Version name/code for release builds come from `VERSION_NAME` and `VERSION_CODE` env vars, with app defaults `1.0.3` and `4` for local builds.
- `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: true` is set in Android workflows to avoid Node 20 action deprecation warnings.

## CI And Release Notes
- `.github/workflows/android.yml` is debug CI and uploads `app/build/outputs/apk/debug` only.
- `.github/workflows/android-auto-release.yml` runs on pushes to `main`/`master`, creates the next patch tag, builds signed AAB/APK, and creates a GitHub Release.
- The auto-release tag lookup must tolerate no semver tags under `bash -e -o pipefail`; keep the `|| true` fallback behavior.
- Editing `.github/workflows/*` requires a GitHub token with `workflow` scope; `repo` alone will be rejected on push.
- `deploy-web.yml` only runs for `web-app/**` or that workflow file and builds Jekyll from `web-app/`.

## Implementation Hotspots
- Accessibility detection and split/floating-window handling: `AppLockAccessibilityService.kt`.
- Usage-stats fallback backend: `ExperimentalAppLockService.kt`.
- Shared lock state and restart throttling: `AppLockManager.kt`.
- PIN overlay activity/UI: `PasswordOverlayScreen.kt`; pattern overlay UI: `PatternLockScreen.kt`.
- Pattern gesture behavior and line rendering: `patternlock/.../PatternLock.kt`.

## Local Environment Notes
- This workspace has had stale persistent `JAVA_HOME`; use the session override above if Gradle reports an invalid JDK path.
- This workspace may not have `ANDROID_HOME` exported; default SDK path used successfully is `$env:LOCALAPPDATA\Android\Sdk`.
