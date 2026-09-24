#!/usr/bin/env bash
# ==============================================================================
# Script: package-release.sh
# Purpose: Packages the clean source code of csv-vat-calculator for release delivery.
# Excludes: target/, .git/, .idea/, .vscode/, .settings/, bin/, logs, previous zips.
# ==============================================================================

set -euo pipefail

OUTPUT_ZIP="csv-vat-calculator-final.zip"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [ -f "$OUTPUT_ZIP" ]; then
    echo "Removing existing $OUTPUT_ZIP..."
    rm -f "$OUTPUT_ZIP"
fi

echo "Packaging project into $OUTPUT_ZIP..."

if command -v zip >/dev/null 2>&1; then
    zip -r -q "$OUTPUT_ZIP" . \
        -x "target/*" \
        -x ".git/*" \
        -x ".idea/*" \
        -x ".vscode/*" \
        -x ".settings/*" \
        -x ".classpath" \
        -x ".project" \
        -x "bin/*" \
        -x "*.zip" \
        -x "*.log" \
        -x ".DS_Store"
    echo "Generated $OUTPUT_ZIP successfully using zip command."
elif command -v tar >/dev/null 2>&1; then
    tar -a -cf "$OUTPUT_ZIP" \
        --exclude="target" \
        --exclude=".git" \
        --exclude=".idea" \
        --exclude=".vscode" \
        --exclude=".settings" \
        --exclude=".classpath" \
        --exclude=".project" \
        --exclude="bin" \
        --exclude="*.zip" \
        --exclude="*.log" \
        *
    echo "Generated $OUTPUT_ZIP successfully using tar command."
else
    echo "Error: Neither 'zip' nor 'tar' is available." >&2
    exit 1
fi

ls -lh "$OUTPUT_ZIP"
echo "Package is ready for corporate hand-over!"
