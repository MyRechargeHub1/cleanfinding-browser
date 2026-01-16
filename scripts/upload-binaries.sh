#!/bin/bash

# Upload Binaries to GitHub Release
# For CleanFinding Browser v1.4.0

set -e

VERSION="1.4.0"
REPO="MyRechargeHub1/cleanfinding-browser"
RELEASE_ID="277292087"
GITHUB_TOKEN="${GITHUB_TOKEN:-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}"

echo "╔═══════════════════════════════════════════════════════╗"
echo "║  CleanFinding Browser - Upload Release Binaries      ║"
echo "║  Version: ${VERSION}                                     ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo ""

# Check if releases directory exists
if [ ! -d "releases/v${VERSION}" ]; then
    echo "❌ Error: releases/v${VERSION} directory not found"
    echo ""
    echo "Please build the binaries first:"
    echo "  ./scripts/prepare-release.sh"
    exit 1
fi

cd "releases/v${VERSION}"

# Count files to upload
FILE_COUNT=$(ls -1 | wc -l)
echo "Found ${FILE_COUNT} files to upload"
echo ""

# Upload each file
UPLOAD_COUNT=0
FAILED_COUNT=0

for file in *; do
    if [ -f "$file" ]; then
        echo "⬆️  Uploading: $file"

        # Get file size for progress
        SIZE=$(du -h "$file" | cut -f1)
        echo "   Size: $SIZE"

        # Upload using GitHub API
        RESPONSE=$(curl -s -w "\n%{http_code}" \
            -X POST \
            -H "Accept: application/vnd.github+json" \
            -H "Authorization: Bearer ${GITHUB_TOKEN}" \
            -H "Content-Type: application/octet-stream" \
            --data-binary "@${file}" \
            "https://uploads.github.com/repos/${REPO}/releases/${RELEASE_ID}/assets?name=${file}")

        # Extract HTTP status code
        HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)

        if [ "$HTTP_CODE" = "201" ]; then
            echo "   ✓ Success"
            UPLOAD_COUNT=$((UPLOAD_COUNT + 1))
        else
            echo "   ✗ Failed (HTTP $HTTP_CODE)"
            FAILED_COUNT=$((FAILED_COUNT + 1))
        fi

        echo ""
    fi
done

# Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Upload Summary:"
echo "  ✓ Successful: ${UPLOAD_COUNT} files"
echo "  ✗ Failed:     ${FAILED_COUNT} files"
echo ""

if [ $FAILED_COUNT -eq 0 ]; then
    echo "🎉 All files uploaded successfully!"
    echo ""
    echo "View release at:"
    echo "https://github.com/${REPO}/releases/tag/v${VERSION}"
    echo ""
    echo "Next steps:"
    echo "1. Test all download links"
    echo "2. Update download-browser.html on cleanfinding.com"
    echo "3. Announce the release"
else
    echo "⚠️  Some uploads failed. Please retry or check:"
    echo "- Network connection"
    echo "- GitHub token permissions"
    echo "- File sizes (max 2GB per file)"
fi
