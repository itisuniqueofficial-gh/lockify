#!/usr/bin/env bash
#
# verify-release.sh — Validate signed Lockify release artifacts before they are
# published, and generate SHA-256 checksums.
#
# Checks performed:
#   * APK exists and is signed (APK Signature Scheme v2/v3 via apksigner).
#   * AAB exists and is signed (JAR signature via jarsigner).
#   * APK package id matches the expected applicationId.
#   * APK versionName / versionCode match the expected values.
#   * SHA-256 checksums are generated for every asset.
#
# Any failure exits non-zero so the release workflow stops before publishing.
#
# Usage:
#   scripts/verify-release.sh \
#       --apk <path> --aab <path> \
#       --version <name> --code <code> \
#       [--package <applicationId>] [--out <dir>]
#
# Requires: apksigner and aapt2 on PATH or under $ANDROID_HOME/build-tools/*,
# plus jarsigner from the JDK.
#
set -euo pipefail

log()  { printf '%s\n' "$*" >&2; }
die()  { printf '::error::%s\n' "$*" >&2; exit 1; }
ok()   { printf '  \033[32m✓\033[0m %s\n' "$*" >&2; }

APK="" AAB="" EXPECTED_VERSION="" EXPECTED_CODE=""
PACKAGE="com.itisuniqueofficial.lockify" OUT_DIR="release"

while [ "$#" -gt 0 ]; do
  case "$1" in
    --apk)     APK="$2"; shift 2 ;;
    --aab)     AAB="$2"; shift 2 ;;
    --version) EXPECTED_VERSION="${2#v}"; shift 2 ;;
    --code)    EXPECTED_CODE="$2"; shift 2 ;;
    --package) PACKAGE="$2"; shift 2 ;;
    --out)     OUT_DIR="$2"; shift 2 ;;
    *) die "Unknown argument: $1" ;;
  esac
done

[ -n "${EXPECTED_VERSION}" ] || die "--version is required"
[ -n "${EXPECTED_CODE}" ]    || die "--code is required"

# --- Locate Android build-tools (apksigner, aapt2) ------------------------
find_build_tool() {
  local tool="$1" candidate
  if command -v "${tool}" >/dev/null 2>&1; then command -v "${tool}"; return; fi
  if [ -n "${ANDROID_HOME:-}" ]; then
    candidate="$(find "${ANDROID_HOME}/build-tools" -name "${tool}" 2>/dev/null | sort -V | tail -n1)"
    [ -n "${candidate}" ] && { printf '%s' "${candidate}"; return; }
  fi
  die "Required tool not found: ${tool}"
}

APKSIGNER="$(find_build_tool apksigner)"
AAPT2="$(find_build_tool aapt2)"
command -v jarsigner >/dev/null 2>&1 || die "jarsigner not found on PATH"

log "==> Verifying release v${EXPECTED_VERSION} (code ${EXPECTED_CODE})"

# --- APK checks -----------------------------------------------------------
[ -n "${APK}" ] && [ -f "${APK}" ] || die "APK not found: ${APK:-<unset>}"
"${APKSIGNER}" verify --min-sdk-version 26 "${APK}" >/dev/null 2>&1 \
  || die "APK signature verification failed: ${APK}"
ok "APK is signed and verified"

badging="$("${AAPT2}" dump badging "${APK}" 2>/dev/null | grep -E '^package:' | head -n1)"
apk_pkg="$(printf '%s\n'  "${badging}" | grep -oE "name='[^']*'"         | head -n1 | cut -d"'" -f2)"
apk_name="$(printf '%s\n' "${badging}" | grep -oE " versionName='[^']*'" | head -n1 | cut -d"'" -f2)"
apk_code="$(printf '%s\n' "${badging}" | grep -oE "versionCode='[0-9]*'" | head -n1 | cut -d"'" -f2)"

[ "${apk_pkg}" = "${PACKAGE}" ]           || die "APK package '${apk_pkg}' != '${PACKAGE}'"
[ "${apk_name}" = "${EXPECTED_VERSION}" ] || die "APK versionName '${apk_name}' != '${EXPECTED_VERSION}'"
[ "${apk_code}" = "${EXPECTED_CODE}" ]    || die "APK versionCode '${apk_code}' != '${EXPECTED_CODE}'"
ok "APK package=${apk_pkg} versionName=${apk_name} versionCode=${apk_code}"

# --- AAB checks -----------------------------------------------------------
[ -n "${AAB}" ] && [ -f "${AAB}" ] || die "AAB not found: ${AAB:-<unset>}"
jarsigner -verify "${AAB}" >/dev/null 2>&1 \
  || die "AAB signature verification failed: ${AAB}"
ok "AAB is signed and verified"

# --- Checksums ------------------------------------------------------------
mkdir -p "${OUT_DIR}"
sums_file="${OUT_DIR}/Lockify-v${EXPECTED_VERSION}-SHA256SUMS.txt"
: > "${sums_file}"
for f in "${APK}" "${AAB}"; do
  ( cd "$(dirname "${f}")" && sha256sum "$(basename "${f}")" ) >> "${sums_file}"
done
ok "Checksums written to ${sums_file}"
cat "${sums_file}" >&2

log "==> Release verification passed"
