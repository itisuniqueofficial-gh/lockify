# Contributing to Lockify

Thanks for your interest in improving Lockify. This document explains how to set
up the project, the conventions we follow, and how releases are made.

## Development setup

Requirements:

- JDK 17
- Android SDK with platform `android-36` and build-tools `36.0.0`
- Android Studio (recommended) or the Gradle CLI

Clone and build:

```bash
git clone https://github.com/itisuniqueofficial-gh/lockify.git
cd lockify
./gradlew :app:assembleDebug
```

## Branching model

```text
main        # stable, released code
develop     # integration branch (optional for larger efforts)
feature/*   # new features
fix/*       # bug fixes
release/*   # release preparation (optional)
```

Open pull requests against `main` (or `develop` when it is in use). Keep PRs
focused and reasonably small.

## Commit conventions

We use [Conventional Commits](https://www.conventionalcommits.org). The release
notes generator (`scripts/generate-changelog.sh`) groups commits by type:

| Prefix | Category |
| --- | --- |
| `feat:` | What's New |
| `fix:` | Bug Fixes |
| `perf:` | Performance |
| `security:` | Security |
| `refactor:` | Refactoring |
| `docs:` | Documentation |
| `chore:` / `build:` / `ci:` / `test:` | Other Changes |

Example:

```text
feat(lockscreen): add configurable auto-lock delay
fix(service): avoid crash when starting foreground on API 26
```

## Before you open a pull request

Run the same checks CI runs:

```bash
./gradlew :app:lintDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

The `Pull Request` workflow re-runs these on GitHub. All checks must pass before
a PR can merge.

### Ground rules

- Never commit secrets, keystores (`*.jks`, `*.keystore`), or personal data.
- Put user-facing strings in `res/values/strings.xml` — do not hardcode them.
- Do not log PINs, passwords, or authentication state.
- Add or update unit tests for security- and logic-related changes.

## Releases

Releases are automated and driven by Git tags. See the
[Continuous Integration & Releases](README.md#continuous-integration--releases)
section of the README for the full flow. In short:

```bash
git tag v1.2.0
git push origin v1.2.0
```

This runs tests and lint, builds a signed APK and AAB, verifies the artifacts,
generates checksums and release notes, and publishes a GitHub Release.

## Reporting security issues

Please report vulnerabilities privately as described in
[`SECURITY.md`](SECURITY.md) rather than opening a public issue.
