#!/usr/bin/env bash
# Renames the base package and applicationId for a new project, e.g.
#   scripts/rename-package.sh com.example.myapp
# Moves every src/*/kotlin/androidshowcase/reflex tree, the Room schema folder, and rewrites the
# package in Kotlin, Gradle, XML, ProGuard, properties, Markdown, Compose config and service-loader files.
# Run it on a clean working tree, then ./gradlew clean test assembleDebug.
set -euo pipefail

OLD="androidshowcase.reflex"
NEW="${1:-}"
if [[ ! "$NEW" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]]; then
  echo "usage: $0 <new.package.name>   (lowercase segments, at least two, e.g. com.example.myapp)" >&2
  exit 1
fi
if [[ "$NEW" == "$OLD" ]]; then
  echo "Package is already $OLD" >&2
  exit 1
fi

cd "$(dirname "$0")/.."
OLD_PATH="${OLD//.//}"
NEW_PATH="${NEW//.//}"
PRUNE=(-name build -o -name .gradle -o -name .git -o -name .idea -o -name .kotlin)

# 1. Source trees. Move through a temp name so a new package nested in the old one also works.
while IFS= read -r -d '' dir; do
  root="${dir%/"$OLD_PATH"}"
  tmp="$root/.rename-package-tmp"
  mv "$dir" "$tmp"
  find "$root" -mindepth 1 -type d -empty -delete
  mkdir -p "$(dirname "$root/$NEW_PATH")"
  mv "$tmp" "$root/$NEW_PATH"
done < <(find . \( "${PRUNE[@]}" \) -prune -o -type d -path "*/src/*/kotlin/$OLD_PATH" -print0)

# 2. Room schema folders are named after the database class.
for schema in core/data/schemas/"$OLD".*; do
  [[ -e "$schema" ]] && mv "$schema" "core/data/schemas/$NEW${schema#core/data/schemas/"$OLD"}"
done

# 3. Text references (package declarations, imports, namespaces, applicationId, socket names, docs).
while IFS= read -r -d '' file; do
  if grep -qF "$OLD" "$file"; then
    OLD="$OLD" NEW="$NEW" perl -pi -e 's/\Q$ENV{OLD}\E/$ENV{NEW}/g' "$file"
  fi
done < <(find . \( "${PRUNE[@]}" \) -prune -o -type f \( -name '*.kt' -o -name '*.kts' -o -name '*.xml' \
  -o -name '*.pro' -o -name '*.properties' -o -name '*.md' -o -name '*.conf' -o -path '*/META-INF/services/*' \) \
  -print0)

# Keep this script reusable for a later rename.
OLD="$OLD" NEW="$NEW" perl -pi -e 's/^OLD="\Q$ENV{OLD}\E"/OLD="$ENV{NEW}"/' "$0"

echo "Renamed $OLD -> $NEW. Also consider: rootProject.name, app_name, Theme.Reflex, launcher icons."
