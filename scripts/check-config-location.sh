#!/usr/bin/env bash
set -euo pipefail

# Fails CI if the TOP-LEVEL class in a src/test file (matching the filename)
# is itself annotated @Configuration/@Component/@Service/@Repository/
# @RestController/@Controller — meaning production code is misplaced in
# src/test. Ignores comments and nested test-double classes (e.g. a small
# @RestController defined inside a test to exercise MockMvc), which are
# a normal, intentional testing pattern.

ANNOTATIONS_REGEX='^[[:space:]]*@(Configuration|Component|Service|Repository|RestController|Controller)([(].*)?$'
FAILED=0

while IFS= read -r -d '' file; do
  classname=$(basename "$file" .java)
  line_num=$(grep -nE "^[[:space:]]*public[[:space:]]+(final[[:space:]]+)?(class|interface)[[:space:]]+${classname}\b" "$file" | head -1 | cut -d: -f1 || true)
  [ -z "$line_num" ] && continue

  check_line=$((line_num - 1))
  found_annotation=""
  while [ "$check_line" -gt 0 ]; do
    trimmed=$(sed -n "${check_line}p" "$file" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    if [ -z "$trimmed" ]; then check_line=$((check_line - 1)); continue; fi
    if echo "$trimmed" | grep -qE "$ANNOTATIONS_REGEX"; then
      found_annotation="$trimmed"; check_line=$((check_line - 1)); continue
    fi
    if echo "$trimmed" | grep -qE '^@'; then check_line=$((check_line - 1)); continue; fi
    break
  done

  if [ -n "$found_annotation" ]; then
    echo "$file  (annotated: $found_annotation)"
    FAILED=1
  fi
done < <(find src/test -name "*.java" -print0)

if [ "$FAILED" -eq 1 ]; then
  echo "❌ Found top-level production-style annotated classes under src/test (see above)."
  echo "These belong under src/main, not src/test. Move them before merging."
  exit 1
fi

echo "✅ No misplaced @Configuration/@Component/etc. top-level classes found in src/test."