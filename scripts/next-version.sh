#!/usr/bin/env bash
#
# next-version.sh — Determine the next semantic version from Conventional
# Commits made since the most recent official tag.
#
# Bump rules (highest wins):
#   * BREAKING CHANGE / type! (e.g. feat!:, fix!:)  -> MAJOR
#   * feat:                                          -> MINOR
#   * fix: / perf: / security:                       -> PATCH
#   * anything else (docs, chore, ci, test, build,
#     refactor, style, ...)                          -> none (no release)
#
# The base version is the latest v*.*.* tag (default 1.0.0 if none exist).
# A release is only warranted when a feat/fix/perf/security/breaking commit is
# present, preventing accidental releases from docs/chore-only pushes and never
# bumping MAJOR from ambiguous messages.
#
# Usage:
#   scripts/next-version.sh [--force <major|minor|patch>] [--base <ref>]
#
# Output (stdout, and $GITHUB_OUTPUT / $GITHUB_ENV when in GitHub Actions):
#   BUMP=<major|minor|patch|none>
#   BASE_VERSION=<x.y.z>
#   NEXT_VERSION=<x.y.z>          (only when BUMP != none)
#   NEXT_TAG=v<x.y.z>
#   VERSION_CODE=<int>            (MAJOR*1_000_000 + MINOR*1_000 + PATCH)
#
set -euo pipefail

FORCE="" BASE_REF=""
while [ "$#" -gt 0 ]; do
  case "$1" in
    --force) FORCE="$2"; shift 2 ;;
    --base)  BASE_REF="$2"; shift 2 ;;
    *) echo "::error::Unknown argument: $1" >&2; exit 1 ;;
  esac
done

emit() {
  printf '%s\n' "$@"
  [ -n "${GITHUB_OUTPUT:-}" ] && printf '%s\n' "$@" >> "$GITHUB_OUTPUT" || true
  [ -n "${GITHUB_ENV:-}" ]    && printf '%s\n' "$@" >> "$GITHUB_ENV"    || true
}

# --- Resolve base version --------------------------------------------------
LATEST_TAG="$(git tag --list 'v*.*.*' --sort=-v:refname | head -n1 || true)"
BASE_VERSION="${LATEST_TAG#v}"
[ -n "$BASE_VERSION" ] || BASE_VERSION="1.0.0"

if ! printf '%s' "$BASE_VERSION" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+$'; then
  echo "::error::Base version '$BASE_VERSION' is not valid semver" >&2; exit 1
fi
IFS='.' read -r MAJOR MINOR PATCH <<EOF
$BASE_VERSION
EOF

# --- Determine bump --------------------------------------------------------
bump="none"

if [ -n "$FORCE" ]; then
  case "$FORCE" in
    major|minor|patch) bump="$FORCE" ;;
    *) echo "::error::--force must be major|minor|patch" >&2; exit 1 ;;
  esac
else
  # Commit range: since the base tag (exclusive) to HEAD; all history if no tag.
  if [ -n "$BASE_REF" ]; then
    RANGE="${BASE_REF}..HEAD"
  elif [ -n "$LATEST_TAG" ]; then
    RANGE="${LATEST_TAG}..HEAD"
  else
    RANGE="HEAD"
  fi

  subjects="$(git log --no-merges --pretty=format:'%s' "$RANGE" 2>/dev/null || true)"
  bodies="$(git log --no-merges --pretty=format:'%B' "$RANGE" 2>/dev/null || true)"

  has_breaking=false; has_feat=false; has_patch=false
  # BREAKING CHANGE in body, or a "!" before the colon in the subject type.
  if printf '%s' "$bodies" | grep -qE 'BREAKING[ -]CHANGE'; then has_breaking=true; fi
  if printf '%s' "$subjects" | grep -qE '^[a-zA-Z]+(\([^)]*\))?!:'; then has_breaking=true; fi
  if printf '%s' "$subjects" | grep -qE '^feat(\([^)]*\))?:'; then has_feat=true; fi
  if printf '%s' "$subjects" | grep -qE '^(fix|perf|security)(\([^)]*\))?:'; then has_patch=true; fi

  if $has_breaking; then bump="major"
  elif $has_feat;   then bump="minor"
  elif $has_patch;  then bump="patch"
  else bump="none"; fi
fi

# --- Apply bump ------------------------------------------------------------
if [ "$bump" = "none" ]; then
  emit "BUMP=none" "BASE_VERSION=$BASE_VERSION"
  exit 0
fi

case "$bump" in
  major) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0 ;;
  minor) MINOR=$((MINOR + 1)); PATCH=0 ;;
  patch) PATCH=$((PATCH + 1)) ;;
esac

if [ "$MINOR" -gt 999 ] || [ "$PATCH" -gt 999 ]; then
  echo "::error::MINOR/PATCH exceeded 999 (got ${MAJOR}.${MINOR}.${PATCH})" >&2; exit 1
fi

NEXT_VERSION="${MAJOR}.${MINOR}.${PATCH}"
VERSION_CODE=$(( MAJOR * 1000000 + MINOR * 1000 + PATCH ))

emit \
  "BUMP=$bump" \
  "BASE_VERSION=$BASE_VERSION" \
  "NEXT_VERSION=$NEXT_VERSION" \
  "NEXT_TAG=v$NEXT_VERSION" \
  "VERSION_CODE=$VERSION_CODE"
