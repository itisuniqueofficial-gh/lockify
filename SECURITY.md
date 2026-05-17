# Security

## Credential Handling
- PIN and pattern secrets are hashed with `PBKDF2WithHmacSHA256`, per-secret random salt, and constant-time comparison.
- Legacy plain-text PIN/pattern values are migrated to hashes after a successful validation.
- Biometric unlock uses AndroidX Biometric and never exposes biometric material to app code.

## Lock Screen Protection
- Lock activities apply `FLAG_SECURE` and hide overlay windows on supported Android versions.
- Close/back on the lock overlay returns to Home instead of revealing the protected app.
- Recents privacy protection uses a placeholder activity so protected app snapshots are not exposed.

## Release Hardening
- Release builds enable minification and resource shrinking in `app/build.gradle.kts`.
- Release signing requires `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`, preferably via `ORG_GRADLE_PROJECT_KEYSTORE_*` in CI.

## Reporting
- Do not include PINs, patterns, keystores, passwords, or private package lists in bug reports.
