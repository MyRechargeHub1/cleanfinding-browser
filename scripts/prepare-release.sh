#!/bin/bash

# CleanFinding Browser - Release Preparation Script
# Version: 1.4.0
# Description: Automates building and preparing release artifacts for all platforms

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Version
VERSION="1.5.0"

# Directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ANDROID_DIR="$PROJECT_ROOT/android"
DESKTOP_DIR="$PROJECT_ROOT/desktop"
RELEASE_DIR="$PROJECT_ROOT/releases/v$VERSION"

echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   CleanFinding Browser - Release Build Script v$VERSION${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Create release directory
mkdir -p "$RELEASE_DIR"
echo -e "${GREEN}✓${NC} Created release directory: $RELEASE_DIR"
echo ""

# Function to print section header
print_header() {
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
print_header "1. Checking Prerequisites"

# Check Node.js
if command_exists node; then
    NODE_VERSION=$(node --version)
    echo -e "${GREEN}✓${NC} Node.js: $NODE_VERSION"
else
    echo -e "${RED}✗${NC} Node.js not found. Please install Node.js 16 or higher."
    exit 1
fi

# Check npm
if command_exists npm; then
    NPM_VERSION=$(npm --version)
    echo -e "${GREEN}✓${NC} npm: $NPM_VERSION"
else
    echo -e "${RED}✗${NC} npm not found. Please install npm."
    exit 1
fi

# Check Java (for Android builds)
if command_exists java; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    echo -e "${GREEN}✓${NC} Java: $JAVA_VERSION"
else
    echo -e "${YELLOW}⚠${NC} Java not found. Android build may fail."
fi

# Check Gradle (for Android builds)
if [ -f "$ANDROID_DIR/gradlew" ]; then
    echo -e "${GREEN}✓${NC} Gradle wrapper found"
else
    echo -e "${YELLOW}⚠${NC} Gradle wrapper not found. Android build may fail."
fi

echo ""

# Build Android APK
print_header "2. Building Android APK"

if [ -d "$ANDROID_DIR" ]; then
    echo "Building Android release APK..."
    cd "$ANDROID_DIR"

    if [ -f "gradlew" ]; then
        chmod +x gradlew
        ./gradlew clean assembleRelease

        APK_PATH="app/build/outputs/apk/release/app-release.apk"
        if [ -f "$APK_PATH" ]; then
            cp "$APK_PATH" "$RELEASE_DIR/CleanFinding-Browser-$VERSION.apk"
            APK_SIZE=$(du -h "$RELEASE_DIR/CleanFinding-Browser-$VERSION.apk" | cut -f1)
            echo -e "${GREEN}✓${NC} Android APK built successfully ($APK_SIZE)"
            echo -e "   Location: $RELEASE_DIR/CleanFinding-Browser-$VERSION.apk"
        else
            echo -e "${RED}✗${NC} APK not found at expected location"
        fi
    else
        echo -e "${YELLOW}⚠${NC} Gradle wrapper not found, skipping Android build"
    fi
else
    echo -e "${YELLOW}⚠${NC} Android directory not found, skipping"
fi

cd "$PROJECT_ROOT"
echo ""

# Build Desktop Applications
print_header "3. Building Desktop Applications"

if [ -d "$DESKTOP_DIR" ]; then
    cd "$DESKTOP_DIR"

    # Install dependencies
    echo "Installing desktop dependencies..."
    npm install
    echo ""

    # Determine current platform
    PLATFORM=$(uname -s)

    case "$PLATFORM" in
        Linux*)
            echo -e "${BLUE}Building for Linux...${NC}"
            npm run build:linux

            # Copy Linux artifacts
            if [ -d "dist" ]; then
                find dist -name "*.AppImage" -exec cp {} "$RELEASE_DIR/CleanFinding-Browser-$VERSION-x86_64.AppImage" \;
                find dist -name "*.deb" -exec cp {} "$RELEASE_DIR/cleanfinding-browser_${VERSION}_amd64.deb" \;
                find dist -name "*.rpm" -exec cp {} "$RELEASE_DIR/cleanfinding-browser-${VERSION}-1.x86_64.rpm" \;
                echo -e "${GREEN}✓${NC} Linux builds completed"
            fi
            ;;

        Darwin*)
            echo -e "${BLUE}Building for macOS...${NC}"
            npm run build:mac

            # Copy macOS artifacts
            if [ -d "dist" ]; then
                find dist -name "*.dmg" -exec cp {} "$RELEASE_DIR/CleanFinding-Browser-$VERSION-universal.dmg" \;
                find dist -name "*.zip" -exec cp {} "$RELEASE_DIR/CleanFinding-Browser-$VERSION-universal.zip" \;
                echo -e "${GREEN}✓${NC} macOS builds completed"
            fi
            ;;

        MINGW*|MSYS*|CYGWIN*)
            echo -e "${BLUE}Building for Windows...${NC}"
            npm run build:win

            # Copy Windows artifacts
            if [ -d "dist" ]; then
                find dist -name "*Setup*.exe" -exec cp {} "$RELEASE_DIR/CleanFinding-Browser-Setup-$VERSION.exe" \;
                find dist -name "*portable*.exe" -exec cp {} "$RELEASE_DIR/CleanFinding-Browser-$VERSION-portable.exe" \;
                echo -e "${GREEN}✓${NC} Windows builds completed"
            fi
            ;;

        *)
            echo -e "${YELLOW}⚠${NC} Unknown platform: $PLATFORM"
            echo "You can manually build for your platform:"
            echo "  - Windows: npm run build:win"
            echo "  - macOS:   npm run build:mac"
            echo "  - Linux:   npm run build:linux"
            ;;
    esac
else
    echo -e "${YELLOW}⚠${NC} Desktop directory not found, skipping"
fi

cd "$PROJECT_ROOT"
echo ""

# Generate checksums
print_header "4. Generating Checksums"

if [ -d "$RELEASE_DIR" ] && [ "$(ls -A $RELEASE_DIR)" ]; then
    cd "$RELEASE_DIR"

    if command_exists sha256sum; then
        sha256sum * > SHASUMS.txt 2>/dev/null
        echo -e "${GREEN}✓${NC} SHA256 checksums generated"
        echo -e "   Location: $RELEASE_DIR/SHASUMS.txt"
    elif command_exists shasum; then
        shasum -a 256 * > SHASUMS.txt 2>/dev/null
        echo -e "${GREEN}✓${NC} SHA256 checksums generated"
        echo -e "   Location: $RELEASE_DIR/SHASUMS.txt"
    else
        echo -e "${YELLOW}⚠${NC} sha256sum/shasum not found, skipping checksums"
    fi

    cd "$PROJECT_ROOT"
else
    echo -e "${YELLOW}⚠${NC} No files to checksum"
fi

echo ""

# Summary
print_header "5. Release Summary"

echo -e "${BLUE}Release Version:${NC} $VERSION"
echo -e "${BLUE}Release Directory:${NC} $RELEASE_DIR"
echo ""
echo -e "${BLUE}Artifacts Built:${NC}"

if [ -d "$RELEASE_DIR" ]; then
    for file in "$RELEASE_DIR"/*; do
        if [ -f "$file" ]; then
            filename=$(basename "$file")
            filesize=$(du -h "$file" | cut -f1)
            echo -e "  ${GREEN}✓${NC} $filename ($filesize)"
        fi
    done
else
    echo -e "  ${YELLOW}No artifacts found${NC}"
fi

echo ""
print_header "6. Next Steps"

echo "1. Test all built artifacts on their respective platforms"
echo ""
echo "2. Create Git tag:"
echo -e "   ${YELLOW}git tag -a v$VERSION -m 'Release v$VERSION'${NC}"
echo -e "   ${YELLOW}git push origin v$VERSION${NC}"
echo ""
echo "3. Create GitHub Release:"
echo -e "   ${YELLOW}gh release create v$VERSION \\${NC}"
echo -e "   ${YELLOW}  --title 'CleanFinding Browser v$VERSION' \\${NC}"
echo -e "   ${YELLOW}  --notes-file RELEASE_NOTES_v$VERSION.md \\${NC}"
echo -e "   ${YELLOW}  releases/v$VERSION/*${NC}"
echo ""
echo "4. Update download links in cleanfinding.com repository"
echo ""
echo "5. Announce release on:"
echo "   - Website blog"
echo "   - Social media"
echo "   - GitHub Discussions"
echo ""

echo -e "${GREEN}═══════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}   Release preparation complete!${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════════${NC}"
