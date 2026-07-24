#!/usr/bin/env bash
#
# auto-fix.sh — Apply SAFE, deterministic, low-risk fixes only.
#
# Scope (intentionally conservative — never changes program logic):
#   * Strip trailing whitespace from source files.
#   * Ensure source files end with exactly one trailing newline.
#   * Ensure gradlew is executable.
#
# It deliberately does NOT: modify Kotlin/Java logic, edit signing or security
# configuration, touch tests, or suppress warnings. Broader fixes must be done
# by a human or a reviewed AI-agent step and always go through a pull request.
#
# Exit code is always 0; inspect `git status` afterwards to see whether anything
# changed.
#
set -euo pipefail

# File types we are willing to normalise.
mapfile -t FILES < <(git ls-files -- \
  '*.kt' '*.kts' '*.java' '*.xml' '*.pro' '*.md' '*.yml' '*.yaml' '*.sh' \
  ':!web-app/**' 2>/dev/null || true)

changed=0
for f in "${FILES[@]}"; do
  [ -f "$f" ] || continue
  # Skip binary files defensively.
  if grep -Iq . "$f" 2>/dev/null; then
    before="$(sha1sum "$f" | cut -d' ' -f1)"
    # Strip trailing whitespace.
    sed -i 's/[[:space:]]\+$//' "$f"
    # Ensure the file ends with a newline.
    if [ -s "$f" ] && [ "$(tail -c1 "$f"; echo x)" != $'\nx' ]; then
      printf '\n' >> "$f"
    fi
    after="$(sha1sum "$f" | cut -d' ' -f1)"
    [ "$before" != "$after" ] && { echo "normalised: $f"; changed=$((changed + 1)); }
  fi
done

# Ensure the Gradle wrapper is executable.
if [ -f gradlew ] && [ ! -x gradlew ]; then
  chmod +x gradlew
  echo "made gradlew executable"
  changed=$((changed + 1))
fi

echo "auto-fix: ${changed} file(s) changed"
