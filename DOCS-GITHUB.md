# Android GitHub Automated Release Guide

## 1. What this workflow does

The workflow in `.github/workflows/android-release.yml` automatically builds a signed Android App Bundle when you push code to GitHub.

It does the following:

- Runs on pushes to `main` or `master`.
- Runs on release tags like `v1.0.1`.
- Runs manually from the GitHub Actions page.
- Generates `versionCode` from the GitHub Actions run number.
- Generates `versionName` from the Git tag or run number.
- Decodes your upload keystore from a GitHub Secret.
- Builds a signed release `.aab` using `bundleRelease`.
- Renames the output to a file like `app-release-v1.0.3-code123.aab`.
- Uploads the signed AAB as a GitHub Actions artifact.
- Creates a GitHub Release and uploads the AAB when a `v*` tag is pushed.
- Deletes the temporary keystore from the runner after the build.

## 2. Required GitHub Secrets

Add these repository secrets before running the release workflow:

- `ANDROID_KEYSTORE_BASE64` = content of `keystore-base64.txt`
- `ANDROID_KEYSTORE_PASSWORD` = your keystore password
- `ANDROID_KEY_ALIAS` = `upload`
- `ANDROID_KEY_PASSWORD` = your key password

## 3. Required GitHub Variables if needed

No GitHub repository variables are required.

The workflow uses built-in GitHub Actions values:

- `GITHUB_RUN_NUMBER`
- `GITHUB_REF`
- `GITHUB_REF_NAME`

## 4. How to generate upload keystore

Run this command on your local machine:

```bash
keytool -genkeypair -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

Keep this file private. Do not commit it to Git.

## 5. How to convert keystore to Base64

On Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-keystore.jks")) | Out-File -Encoding ascii keystore-base64.txt
```

On Linux/macOS:

```bash
base64 -w 0 upload-keystore.jks > keystore-base64.txt
```

If your macOS `base64` command does not support `-w`, use:

```bash
base64 upload-keystore.jks | tr -d '\n' > keystore-base64.txt
```

## 6. How to add secrets in GitHub

Open this path in your repository:

GitHub Repository -> Settings -> Secrets and variables -> Actions -> New repository secret

Add these secrets:

- `ANDROID_KEYSTORE_BASE64` = content of `keystore-base64.txt`
- `ANDROID_KEYSTORE_PASSWORD` = your keystore password
- `ANDROID_KEY_ALIAS` = `upload`
- `ANDROID_KEY_PASSWORD` = your key password

Do not paste passwords into workflow files, Gradle files, README files, issues, or commit messages.

## 7. How automatic versioning works

Gradle reads these environment variables during CI:

- `VERSION_CODE`
- `VERSION_NAME`

The workflow generates them automatically:

- Tag `v1.0.1` -> `versionName 1.0.1`
- Normal push -> `versionName 1.0.RUN_NUMBER`
- `versionCode` -> GitHub run number

Example:

- GitHub run number: `123`
- Normal push version name: `1.0.123`
- Release tag: `v1.0.1`
- Tag version name: `1.0.1`
- AAB file name: `app-release-v1.0.1-code123.aab`

Every new workflow run gets a higher GitHub run number, so every generated Play Store upload gets a higher `versionCode`.

## 8. How to push normal build

Use a normal Git push to `main`:

```bash
git add .
git commit -m "feat: update app"
git push origin main
```

This creates a signed AAB artifact with a generated version name like `1.0.123`.

## 9. How to create Play Store release build using Git tags

Create and push a tag:

```bash
git tag v1.0.1
git push origin v1.0.1
```

This creates a signed AAB artifact and a GitHub Release with the AAB attached.

## 10. Where to download AAB artifact

Open this path in GitHub:

GitHub -> Actions -> Android Release AAB -> Artifacts -> Download signed AAB

The artifact is named `lockify-signed-release-aab`.

Inside it, the file name will look like this:

```text
app-release-v1.0.1-code123.aab
```

## 11. How to upload AAB to Google Play Console

Open this path in Google Play Console:

Play Console -> App -> Release -> Testing/Production -> Create new release -> Upload .aab

Upload the signed `.aab` downloaded from GitHub Actions.

If this is your first Play Store upload, make sure the app package name matches the Play Console app package:

```text
com.itisuniqueofficial.lockify
```

## 12. Troubleshooting

If the workflow says a secret is missing, add all required secrets again and check the exact secret names.

If signing fails, verify these values match the keystore:

- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

If Google Play rejects the AAB because of `versionCode`, push another commit or run the workflow again. The next GitHub run number will be higher.

If Google Play says the upload key is wrong, you are not using the upload keystore registered with Play App Signing. Use the correct upload keystore or reset the upload key in Play Console.

If the workflow cannot find the AAB, check that `:app:bundleRelease` completed successfully and that the app module is still named `app`.

## 13. Security best practices

Never commit these files:

- `.jks`
- `.keystore`
- `keystore.properties`
- `release-key.*`
- `upload-key.*`
- `signing.properties`
- `.env`
- `.env.*`

Keep a backup of `upload-keystore.jks` in a secure password manager or encrypted storage.

Do not print secrets in GitHub Actions logs.

Do not share `keystore-base64.txt`.

Rotate the upload key if you suspect it was exposed.
