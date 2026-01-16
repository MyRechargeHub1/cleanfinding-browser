#!/bin/bash

# Verify GitHub Release URLs
# Checks if all release files are accessible

set -e

VERSION="1.4.0"
REPO="MyRechargeHub1/cleanfinding-browser"
BASE_URL="https://github.com/${REPO}/releases/download/v${VERSION}"

echo "╔═══════════════════════════════════════════════════════╗"
echo "║  CleanFinding Browser - Verify Release URLs          ║"
echo "║  Version: ${VERSION}                                     ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo ""

# Files to check
declare -a FILES=(
    "CleanFinding-Browser-${VERSION}.apk"
    "CleanFinding-Browser-Setup-${VERSION}.exe"
    "CleanFinding-Browser-${VERSION}-portable.exe"
    "CleanFinding-Browser-${VERSION}-universal.dmg"
    "CleanFinding-Browser-${VERSION}-universal.zip"
    "CleanFinding-Browser-${VERSION}-x86_64.AppImage"
    "cleanfinding-browser_${VERSION}_amd64.deb"
    "cleanfinding-browser-${VERSION}-1.x86_64.rpm"
    "SHASUMS.txt"
)

SUCCESS=0
FAILED=0

echo "Checking release files..."
echo ""

for file in "${FILES[@]}"; do
    URL="${BASE_URL}/${file}"

    # Check if URL is accessible (HEAD request)
    if curl --head --silent --fail "$URL" >/dev/null 2>&1; then
        echo "✓ ${file}"
        SUCCESS=$((SUCCESS + 1))
    else
        echo "✗ ${file} - NOT FOUND"
        echo "  URL: ${URL}"
        FAILED=$((FAILED + 1))
    fi
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Results:"
echo "  ✓ Success: ${SUCCESS} files"
echo "  ✗ Failed:  ${FAILED} files"
echo ""

if [ $FAILED -eq 0 ]; then
    echo "🎉 All release files are accessible!"
    echo ""
    echo "Next steps:"
    echo "1. Update download links on website"
    echo "2. Test downloads from cleanfinding.com"
    echo "3. Announce the release"
    exit 0
else
    echo "⚠️  Some files are missing!"
    echo ""
    echo "Possible causes:"
    echo "1. Release not yet published"
    echo "2. Files not uploaded to release"
    echo "3. Incorrect file names"
    echo ""
    echo "Check release at:"
    echo "https://github.com/${REPO}/releases/tag/v${VERSION}"
    exit 1
fi
