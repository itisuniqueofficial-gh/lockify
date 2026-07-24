#!/usr/bin/env bash
#
# generate-changelog.sh — Generate categorized release notes from the git
# commit history between the previous release tag and a target ref.
#
# Commits are grouped by Conventional Commit type:
#   feat:      -> What's New
#   fix:       -> Bug Fixes
#   perf:      -> Performance
#   security:  -> Security
#   refactor:  -> Refactoring
#   docs:      -> Documentation
#   chore/etc  -> Other Changes
#
# Usage:
#     scripts/generate-changelog.sh <version> [<current-ref>] [<previous-tag>]
#
# Arguments:
#   version       Semantic version for the heading, e.g. "1.2.0" or "v1.2.0".
#   current-ref   Ref to generate notes up to (default: HEAD).
#   previous-tag  Ref to generate notes from (default: previous v*.*.* tag).
#
# The Markdown release notes are written to stdout.
#
set -euo pipefail

VERSION="${1:?usage: generate-changelog.sh <version> [current-ref] [previous-tag]}"
CURRENT_REF="${2:-HEAD}"
VERSION="${VERSION#v}"

# Resolve the previous tag to diff against.
if [ "$#" -ge 3 ] && [ -n "${3:-}" ]; then
  PREV_TAG="$3"
else
  # The most recent v*.*.* tag that is an ancestor of the current ref,
  # excluding the current ref itself when it happens to be a tag.
  PREV_TAG="$(git tag --list 'v*.*.*' --sort=-v:refname --merged "${CURRENT_REF}" 2>/dev/null \
    | grep -vxF "v${VERSION}" | head -n1 || true)"
fi

if [ -n "${PREV_TAG}" ]; then
  RANGE="${PREV_TAG}..${CURRENT_REF}"
  COMPARE_NOTE="Changes since ${PREV_TAG}."
else
  RANGE="${CURRENT_REF}"
  COMPARE_NOTE="Initial tracked release."
fi

# Collect commit subjects for the range.
commits="$(git log --no-merges --pretty=format:'%s' "${RANGE}" 2>/dev/null || true)"

section() {
  # $1 = heading, $2 = grep pattern for the conventional type prefix
  local heading="$1" pattern="$2" body
  body="$(printf '%s\n' "${commits}" \
    | grep -iE "${pattern}" \
    | sed -E "s/^[a-zA-Z]+(\([^)]*\))?!?:[[:space:]]*//" \
    | sed -E 's/^/- /' || true)"
  if [ -n "${body}" ]; then
    printf '\n## %s\n\n%s\n' "${heading}" "${body}"
  fi
}

printf '# Lockify v%s\n\n%s\n' "${VERSION}" "${COMPARE_NOTE}"

section "What's New"      '^feat(\([^)]*\))?!?:'
section "Bug Fixes"       '^fix(\([^)]*\))?!?:'
section "Performance"     '^perf(\([^)]*\))?!?:'
section "Security"        '^security(\([^)]*\))?!?:'
section "Refactoring"     '^refactor(\([^)]*\))?!?:'
section "Documentation"   '^docs(\([^)]*\))?!?:'
section "Other Changes"   '^(chore|build|ci|style|test|revert)(\([^)]*\))?!?:'

cat <<'EOF'

## Downloads

- **APK** — direct installation on a device (`Lockify-v<version>-release.apk`).
- **AAB** — Google Play publishing (`Lockify-v<version>-release.aab`).

## Checksums

SHA-256 checksums for all binary assets are provided in
`Lockify-v<version>-SHA256SUMS.txt`. Verify with:

```bash
sha256sum -c Lockify-v<version>-SHA256SUMS.txt
```
EOF
