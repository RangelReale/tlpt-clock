#!/bin/sh
# Prints the CHANGELOG.md section for a version, without its heading.
# Usage: release-notes.sh 0.1.0 [CHANGELOG.md]
# Prints nothing if the changelog has no section for that version.
version="$1"
changelog="${2:-CHANGELOG.md}"

awk -v heading="## [$version]" '
    /^## \[/ {
        if (found) exit
        if (index($0, heading) == 1) { found = 1; next }
    }
    # The link definitions at the end of the file are not part of any section.
    /^\[[^]]+\]: / { next }
    found { print }
' "$changelog" | sed -e '/./,$!d' | sed -e :a -e '/^\n*$/{$d;N;ba' -e '}'
