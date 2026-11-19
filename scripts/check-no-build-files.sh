#!/usr/bin/env bash
set -euo pipefail

# Fails if Maven build output (target/) or macOS .DS_Store files are tracked
# by git. .gitignore only stops NEW files; anything committed before the
# ignore rule keeps being tracked until removed with `git rm --cached`.

tracked=$(git ls-files | grep -E '(^target/|(^|/)\.DS_Store$)' || true)

if [ -n "$tracked" ]; then
  echo "$tracked"
  echo "❌ Build or OS files are tracked by git (see above)."
  echo "Remove them with: git rm -r --cached <path>"
  exit 1
fi

echo "✅ No build output or .DS_Store files are tracked."
