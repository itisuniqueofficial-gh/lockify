# Android GitHub Automated Release Guide

## 1. What automation does

The primary release workflow is `.github/workflows/android-auto-release.yml`.

When you push to `main` or `master`, or run the workflow manually, GitHub Actions automatically:

- Finds the latest version tag matching `vMAJOR.MINOR.PATCH`.
- Starts from `v1.0.0` if no version tag exists.
- Increments the patch version to create the next tag, such as `v1.0.1`, `v1.0.2`, or `v1.0.3`.
- Pushes the new tag using the GitHub Actions bot.
- Uses the generated tag as the app `versionName` without the `v` prefix.
- Uses the GitHub Actions run number as the app `versionCode`.
- Builds a signed release Android App Bundle with `:app:bundleRelease`.
- Builds a signed release APK with `:app:assembleRelease`.
- Renames both outputs into the `release/` directory.
- Uploads both signed files as a workflow artifact.
- Creates a GitHub Release for the generated tag.
- Uploads both signed files to that GitHub Release.

The older `.github/workflows/android-release.yml` workflow is manual-only to avoid duplicate releases when the automatic workflow pushes tags.

## 2. AAB vs APK

The workflow creates two signed release files:

- AAB: Android App Bundle for Google Play Console upload.
- APK: Android package for direct install and testing outside the Play Store.

Use the `.aab` for Play Store releases. Use the `.apk` only for direct testing, QA sharing, or installing outside Play Store.

Both files are signed with the same release upload key from GitHub Secrets.

Expected output:

```text
release/
  app-release-1.0.1-code123.aab
  app-release-1.0.1-code123.apk
```

## 3. Required GitHub Secrets

Create these repository secrets before running the release workflow:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Secret values:

- `ANDROID_KEYSTORE_BASE64` = content of `keystore-base64.txt`
- `ANDROID_KEYSTORE_PASSWORD` = your keystore password
- `ANDROID_KEY_ALIAS` = `upload`
- `ANDROID_KEY_PASSWORD` = your key password

No secrets are hardcoded in the workflow or Gradle files.

## 4. How to generate upload keystore

Run this command on your local machine:

```bash
keytool -genkeypair -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

Use strong passwords and store them securely. Keep `upload-keystore.jks` private and backed up.

## 5. How to convert keystore to base64 on Windows/Linux/macOS

Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-keystore.jks")) | Out-File -Encoding ascii keystore-base64.txt
```

Linux:

```bash
base64 -w 0 upload-keystore.jks > keystore-base64.txt
```

macOS:

```bash
base64 upload-keystore.jks | tr -d '\n' > keystore-base64.txt
```

Do not commit `keystore-base64.txt`.

## 6. How to add GitHub secrets

Open this path in your repository:

GitHub Repository -> Settings -> Secrets and variables -> Actions -> New repository secret

Add each secret exactly as named:

- `ANDROID_KEYSTORE_BASE64` = content of `keystore-base64.txt`
- `ANDROID_KEYSTORE_PASSWORD` = your keystore password
- `ANDROID_KEY_ALIAS` = `upload`
- `ANDROID_KEY_PASSWORD` = your key password

## 7. How automatic release tags work

The workflow checks all Git tags and finds the newest semantic version tag matching this pattern:

```text
v1.0.0
```

It increments only the patch number.

Examples:

- Latest tag `v1.0.0` -> next tag `v1.0.1`
- Latest tag `v1.0.1` -> next tag `v1.0.2`
- Latest tag `v1.0.2` -> next tag `v1.0.3`

If no matching tag exists, the workflow uses `v1.0.0` as the base and creates `v1.0.1`.

The workflow fails if the generated tag already exists. It never overwrites existing tags.

## 8. How versionName/versionCode work

Gradle reads these environment variables:

- `VERSION_NAME`
- `VERSION_CODE`

The workflow exports them automatically:

- `VERSION_NAME` = generated tag without `v`, such as `1.0.1`
- `VERSION_CODE` = `GITHUB_RUN_NUMBER`

Example:

- Generated tag: `v1.0.1`
- `VERSION_NAME`: `1.0.1`
- `VERSION_CODE`: `123`
- AAB file: `app-release-1.0.1-code123.aab`
- APK file: `app-release-1.0.1-code123.apk`

GitHub run numbers increase on each workflow run, so Play Store `versionCode` increases automatically.

## 9. How to trigger release by pushing to main

Push your code normally:

```bash
git add .
git commit -m "feat: update app"
git push origin main
```

If your repository uses `master`, push to `master` instead:

```bash
git push origin master
```

The workflow creates the next tag and release automatically. You do not need to run `git tag` manually.

## 10. Where to download AAB and APK

Open this path in GitHub:

GitHub -> Actions -> Android Auto Release -> latest workflow run -> Artifacts

Download the artifact named like this:

```text
lockify-signed-release-v1.0.1
```

Inside it, both signed files are included:

```text
app-release-1.0.1-code123.aab
app-release-1.0.1-code123.apk
```

## 11. Where GitHub Release appears

Open this path in GitHub:

GitHub -> Releases

The release name is the generated tag, such as `v1.0.1`.

The automatic release attaches both files:

- `app-release-1.0.1-code123.aab`
- `app-release-1.0.1-code123.apk`

## 12. How to upload AAB to Play Console

Open this path in Google Play Console:

Play Console -> App -> Release -> Testing/Production -> Create new release -> Upload .aab

Upload the signed `.aab` from the GitHub Release or workflow artifact.

The package name is:

```text
com.itisuniqueofficial.lockify
```

Do not upload the APK to Play Console for production if your app is distributed through Play App Bundles. The APK is for direct install/testing outside Play Store.

## 13. Troubleshooting

If the workflow fails with a missing secret error, add all required secrets and verify the names exactly match.

If signing fails, confirm the keystore, alias, store password, and key password all belong to the same upload keystore.

If Google Play rejects the AAB because of `versionCode`, run the workflow again after confirming GitHub run number is higher than the previously uploaded Play Store version code.

If tag creation fails, check whether the generated tag already exists. The workflow intentionally fails instead of overwriting tags.

If no GitHub Release appears, check the `Create GitHub Release` step and confirm the workflow has `contents: write` permission.

If the workflow cannot find the AAB or APK, confirm `:app:bundleRelease` and `:app:assembleRelease` succeeded and the Android app module is still named `app`.

## 14. Security best practices

Never commit these files:

- `.jks`
- `.keystore`
- `keystore.properties`
- `signing.properties`
- `.env`
- `.env.*`
- `release/`

Keep `upload-keystore.jks` in encrypted storage or a trusted password manager.

Do not print secrets in logs.

Do not paste secrets into issues, pull requests, commit messages, README files, or workflow files.

Delete `keystore-base64.txt` after adding it to GitHub Secrets.

Rotate or reset your upload key in Play Console if you suspect it was exposed.
