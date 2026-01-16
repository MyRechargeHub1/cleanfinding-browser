#!/bin/bash

# Update Download Links Script
# Updates the download-browser.html with GitHub Release URLs

set -e

VERSION="1.4.0"
REPO="MyRechargeHub1/cleanfinding-browser"
BASE_URL="https://github.com/${REPO}/releases/download/v${VERSION}"

echo "╔═══════════════════════════════════════════════════════╗"
echo "║  CleanFinding Browser - Update Download Links        ║"
echo "║  Version: ${VERSION}                                     ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo ""

# Check if we're in the cleanfinding.com repository
if [ ! -f "download-browser.html" ]; then
    echo "❌ Error: download-browser.html not found"
    echo ""
    echo "Make sure you're in the cleanfinding.com repository:"
    echo "  cd /path/to/cleanfinding.com"
    echo "  bash /path/to/this-script.sh"
    exit 1
fi

echo "✓ Found download-browser.html"
echo ""

# Create backup
cp download-browser.html download-browser.html.backup
echo "✓ Created backup: download-browser.html.backup"
echo ""

# Define file URLs
declare -A URLS=(
    ["ANDROID_APK"]="${BASE_URL}/CleanFinding-Browser-${VERSION}.apk"
    ["WINDOWS_INSTALLER"]="${BASE_URL}/CleanFinding-Browser-Setup-${VERSION}.exe"
    ["WINDOWS_PORTABLE"]="${BASE_URL}/CleanFinding-Browser-${VERSION}-portable.exe"
    ["MACOS_DMG"]="${BASE_URL}/CleanFinding-Browser-${VERSION}-universal.dmg"
    ["MACOS_ZIP"]="${BASE_URL}/CleanFinding-Browser-${VERSION}-universal.zip"
    ["LINUX_APPIMAGE"]="${BASE_URL}/CleanFinding-Browser-${VERSION}-x86_64.AppImage"
    ["LINUX_DEB"]="${BASE_URL}/cleanfinding-browser_${VERSION}_amd64.deb"
    ["LINUX_RPM"]="${BASE_URL}/cleanfinding-browser-${VERSION}-1.x86_64.rpm"
)

echo "Updating download links..."
echo ""

# Read the file
CONTENT=$(cat download-browser.html)

# Update Android APK link (main download button)
CONTENT=$(echo "$CONTENT" | sed -E 's|(<a[^>]*class="download-btn"[^>]*>Download APK</a>)|<a href="'"${URLS[ANDROID_APK]}"'" class="download-btn">Download APK</a>|g')

# Update Windows Installer (main download button)
CONTENT=$(echo "$CONTENT" | sed -E 's|(<a[^>]*class="download-btn"[^>]*>Download Installer</a>)|<a href="'"${URLS[WINDOWS_INSTALLER]}"'" class="download-btn">Download Installer</a>|g')

# Update Windows Portable (alternative link)
CONTENT=$(echo "$CONTENT" | sed -E 's|(<a[^>]*class="alternative-link"[^>]*>Download Portable[^<]*</a>)|<a href="'"${URLS[WINDOWS_PORTABLE]}"'" class="alternative-link">Download Portable →</a>|g')

# Update macOS DMG (main download button)
CONTENT=$(echo "$CONTENT" | sed -E 's|(<a[^>]*class="download-btn"[^>]*>Download DMG</a>)|<a href="'"${URLS[MACOS_DMG]}"'" class="download-btn">Download DMG</a>|g')

# Update macOS ZIP (alternative link) - need to find the right one
CONTENT=$(echo "$CONTENT" | perl -pe 's|(<a[^>]*class="alternative-link"[^>]*>Download ZIP[^<]*</a>)|<a href="'"${URLS[MACOS_ZIP]}"'" class="alternative-link">Download ZIP →</a>|g')

# Update Linux AppImage (main download button)
CONTENT=$(echo "$CONTENT" | sed -E 's|(<a[^>]*class="download-btn"[^>]*>Download AppImage</a>)|<a href="'"${URLS[LINUX_APPIMAGE]}"'" class="download-btn">Download AppImage</a>|g')

# Update Linux DEB (alternative link) - find first occurrence after AppImage
CONTENT=$(echo "$CONTENT" | perl -pe 's|(<a[^>]*class="alternative-link"[^>]*>Download DEB[^<]*</a>)|<a href="'"${URLS[LINUX_DEB]}"'" class="alternative-link">Download DEB →</a>|')

# Update Linux RPM (alternative link) - find second occurrence
CONTENT=$(echo "$CONTENT" | perl -pe 's|(<a[^>]*class="alternative-link"[^>]*>Download RPM[^<]*</a>)|<a href="'"${URLS[LINUX_RPM]}"'" class="alternative-link">Download RPM →</a>|')

# Write updated content
echo "$CONTENT" > download-browser.html

echo "✓ Updated download links:"
echo ""
for key in "${!URLS[@]}"; do
    printf "  %-20s %s\n" "$key:" "${URLS[$key]}"
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Next steps:"
echo ""
echo "1. Review changes:"
echo "   git diff download-browser.html"
echo ""
echo "2. Test locally (if you have a web server):"
echo "   python3 -m http.server 8000"
echo "   # Open http://localhost:8000/download-browser.html"
echo ""
echo "3. Commit and push:"
echo "   git add download-browser.html"
echo "   git commit -m \"Update download links to v${VERSION}\""
echo "   git push origin main"
echo ""
echo "4. Test on live site:"
echo "   https://cleanfinding.com/download-browser"
echo ""
echo "✓ Done!"
