#!/usr/bin/env bash
#
# version.sh — Derive Lockify's semantic version and a deterministic,
# monotonically increasing Android versionCode.
#
# The versionName follows Semantic Versioning: MAJOR.MINOR.PATCH
# The versionCode is derived deterministically from the version:
#
#     versionCode = MAJOR * 1_000_000 + MINOR * 1_000 + PATCH
#
# This guarantees that a higher semantic version always produces a higher
# versionCode, that the same tag always produces the same versionCode
# (reproducible), and that duplicate version codes are impossible for distinct
# versions. It supports MINOR and PATCH up to 999 and MAJOR up to ~2146, which
# stays within Android's versionCode ceiling of 2,100,000,000.
#
# Usage:
#     scripts/version.sh [<version-or-tag>]
#
# The version source is resolved in this order:
#   1. The first CLI argument (e.g. "1.2.0" or "v1.2.0")
#   2. $GITHUB_REF_NAME when it looks like a tag (release workflow)
#   3. The most recent v*.*.* git tag reachable from HEAD
#   4. "0.0.0" as a last-resort development fallback
#
# Output: writes KEY=VALUE lines to stdout and, when running inside GitHub
# Actions, appends them to $GITHUB_OUTPUT and $GITHUB_ENV.
#
set -euo pipefail

log() { printf '%s\n' "$*" >&2; }
die() { log "::error::$*"; exit 1; }

raw_version=""

if [ "$#" -ge 1 ] && [ -n "${1:-}" ]; then
  raw_version="$1"
elif [ -n "${GITHUB_REF_NAME:-}" ] && printf '%s' "${GITHUB_REF_NAME}" | grep -qE '^v[0-9]+\.[0-9]+\.[0-9]+'; then
  raw_version="${GITHUB_REF_NAME}"
elif git rev-parse --git-dir >/dev/null 2>&1; then
  raw_version="$(git tag --list 'v*.*.*' --sort=-v:refname | head -n1 || true)"
fi

if [ -z "${raw_version}" ]; then
  log "No version supplied and no v*.*.* tag found; falling back to 0.0.0"
  raw_version="0.0.0"
fi

# Normalise: strip a leading "v" and any pre-release/build metadata suffix.
version="${raw_version#v}"
core="${version%%[-+]*}"

if ! printf '%s' "${core}" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+$'; then
  die "Invalid semantic version: '${raw_version}' (expected MAJOR.MINOR.PATCH)"
fi

IFS='.' read -r MAJOR MINOR PATCH <<EOF
${core}
EOF

if [ "${MINOR}" -gt 999 ] || [ "${PATCH}" -gt 999 ]; then
  die "MINOR and PATCH must each be <= 999 (got ${core})"
fi

VERSION_NAME="${MAJOR}.${MINOR}.${PATCH}"
VERSION_CODE=$(( MAJOR * 1000000 + MINOR * 1000 + PATCH ))

# Emit for humans / shell capture.
printf 'VERSION_NAME=%s\n' "${VERSION_NAME}"
printf 'VERSION_CODE=%s\n' "${VERSION_CODE}"

# Emit for GitHub Actions when available.
if [ -n "${GITHUB_OUTPUT:-}" ]; then
  {
    printf 'version_name=%s\n' "${VERSION_NAME}"
    printf 'version_code=%s\n' "${VERSION_CODE}"
  } >> "${GITHUB_OUTPUT}"
fi
if [ -n "${GITHUB_ENV:-}" ]; then
  {
    printf 'VERSION_NAME=%s\n' "${VERSION_NAME}"
    printf 'VERSION_CODE=%s\n' "${VERSION_CODE}"
  } >> "${GITHUB_ENV}"
fi
