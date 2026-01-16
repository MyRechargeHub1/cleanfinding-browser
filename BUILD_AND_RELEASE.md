# Build and Release Instructions for CleanFinding Browser v1.4.0

## 🚀 Complete Step-by-Step Guide

This guide will walk you through building all platform binaries and creating the GitHub Release.

## Prerequisites

### Required Software

**For All Builds:**
- Git
- Node.js 16+ and npm 8+
- GitHub CLI (`gh`) - Install from https://cli.github.com/

**For Android:**
- JDK 11 or higher
- Android SDK (or use the Gradle wrapper)

**For Desktop:**
- Node.js and npm (already required)

### Authentication

Make sure you have a GitHub personal access token with `repo` scope:
```bash
# Set your token
export GITHUB_TOKEN=your_github_token_here
```

## Step 1: Clone and Prepare

```bash
# Clone the repository (if not already)
git clone https://github.com/MyRechargeHub1/cleanfinding-browser.git
cd cleanfinding-browser

# Switch to the release branch
git checkout claude/review-shared-conversation-TblxK

# Make sure you have the latest changes
git pull
```

## Step 2: Build Android APK

```bash
# Navigate to Android directory
cd android

# Make gradlew executable
chmod +x gradlew

# Clean and build release APK
./gradlew clean assembleRelease

# The APK will be at:
# android/app/build/outputs/apk/release/app-release.apk
```

**Expected Output:**
- File: `app-release.apk`
- Size: ~8 MB
- Location: `android/app/build/outputs/apk/release/`

## Step 3: Build Desktop Applications

```bash
# Navigate to desktop directory
cd ../desktop

# Install dependencies
npm install

# This will take a few minutes on first run
```

### Build for Windows (on Windows or Linux/macOS with Wine)

```bash
npm run build:win
```

**Expected Outputs:**
- `CleanFinding-Browser-Setup-1.4.0.exe` (~73 MB) - Installer
- `CleanFinding-Browser-1.4.0-portable.exe` (~73 MB) - Portable
- Location: `desktop/dist/`

### Build for macOS (on macOS only)

```bash
npm run build:mac
```

**Expected Outputs:**
- `CleanFinding-Browser-1.4.0-universal.dmg` (~89 MB)
- `CleanFinding-Browser-1.4.0-universal.zip` (~89 MB)
- Location: `desktop/dist/`

### Build for Linux (on Linux)

```bash
npm run build:linux
```

**Expected Outputs:**
- `CleanFinding-Browser-1.4.0-x86_64.AppImage` (~99 MB)
- `cleanfinding-browser_1.4.0_amd64.deb` (~99 MB)
- `cleanfinding-browser-1.4.0-1.x86_64.rpm` (~99 MB)
- Location: `desktop/dist/`

## Step 4: Organize Release Files

```bash
# Create release directory
mkdir -p releases/v1.4.0

# Copy Android APK
cp android/app/build/outputs/apk/release/app-release.apk \
   releases/v1.4.0/CleanFinding-Browser-1.4.0.apk

# Copy Desktop builds (adjust based on what you built)
cp desktop/dist/CleanFinding-Browser-Setup-*.exe \
   releases/v1.4.0/CleanFinding-Browser-Setup-1.4.0.exe

cp desktop/dist/*portable*.exe \
   releases/v1.4.0/CleanFinding-Browser-1.4.0-portable.exe 2>/dev/null || true

cp desktop/dist/*.dmg \
   releases/v1.4.0/CleanFinding-Browser-1.4.0-universal.dmg 2>/dev/null || true

cp desktop/dist/*.AppImage \
   releases/v1.4.0/CleanFinding-Browser-1.4.0-x86_64.AppImage 2>/dev/null || true

cp desktop/dist/*.deb \
   releases/v1.4.0/cleanfinding-browser_1.4.0_amd64.deb 2>/dev/null || true

cp desktop/dist/*.rpm \
   releases/v1.4.0/cleanfinding-browser-1.4.0-1.x86_64.rpm 2>/dev/null || true
```

## Step 5: Generate Checksums

```bash
cd releases/v1.4.0

# Generate SHA256 checksums
sha256sum * > SHASUMS.txt

# View checksums
cat SHASUMS.txt
```

## Step 6: Create Git Tag

```bash
# Go back to project root
cd ../..

# Create annotated tag
git tag -a v1.4.0 -m "Release v1.4.0 - Major Privacy Features Update"

# Push tag to repository
git push origin v1.4.0
```

## Step 7: Create GitHub Release

### Option A: Using GitHub CLI (Recommended)

```bash
# Set GitHub token (use your personal access token)
export GITHUB_TOKEN=your_github_token_here

# Create release with all files
gh release create v1.4.0 \
  --repo MyRechargeHub1/cleanfinding-browser \
  --title "CleanFinding Browser v1.4.0" \
  --notes-file RELEASE_NOTES_v1.4.0.md \
  releases/v1.4.0/*

# This will:
# - Create the release
# - Upload all files
# - Generate download URLs automatically
```

### Option B: Using GitHub Web Interface

1. Go to: https://github.com/MyRechargeHub1/cleanfinding-browser/releases/new

2. **Tag**: `v1.4.0` (select existing tag)

3. **Release Title**: `CleanFinding Browser v1.4.0`

4. **Description**: Copy content from `RELEASE_NOTES_v1.4.0.md`

5. **Attach Files**: Drag and drop files from `releases/v1.4.0/`
   - CleanFinding-Browser-1.4.0.apk
   - CleanFinding-Browser-Setup-1.4.0.exe
   - CleanFinding-Browser-1.4.0-portable.exe
   - CleanFinding-Browser-1.4.0-universal.dmg
   - CleanFinding-Browser-1.4.0-universal.zip
   - CleanFinding-Browser-1.4.0-x86_64.AppImage
   - cleanfinding-browser_1.4.0_amd64.deb
   - cleanfinding-browser-1.4.0-1.x86_64.rpm
   - SHASUMS.txt

6. **Publish**: Click "Publish release"

## Step 8: Update Download Links

After the release is published, download URLs will be available at:

```
https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/[filename]
```

### Update cleanfinding.com Repository

```bash
# Clone website repository
git clone https://github.com/MyRechargeHub1/cleanfinding.com.git
cd cleanfinding.com

# Edit download-browser.html
# Replace all href="#" with actual GitHub Release URLs
```

**Find and replace these URLs in `download-browser.html`:**

| Platform | Old | New |
|----------|-----|-----|
| Android APK | `href="#"` | `href="https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0.apk"` |
| Windows Installer | `href="#"` | `href="https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-Setup-1.4.0.exe"` |
| Windows Portable | `href="#"` | `href="https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0-portable.exe"` |
| macOS DMG | `href="#"` | `href="https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0-universal.dmg"` |
| macOS ZIP | `href="#"` | `href="https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0-universal.zip"` |
| Linux AppImage | `href="#"` | `href="https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0-x86_64.AppImage"` |
| Linux DEB | `href="#"` | `href="https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/cleanfinding-browser_1.4.0_amd64.deb"` |
| Linux RPM | `href="#"` | `href="https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/cleanfinding-browser-1.4.0-1.x86_64.rpm"` |

**Commit and push:**

```bash
git add download-browser.html
git commit -m "Update download links to v1.4.0 GitHub Release URLs"
git push origin main
```

## Step 9: Test Download Links

Visit https://cleanfinding.com/download-browser and test each download link:

- [ ] Android APK downloads correctly
- [ ] Windows Installer downloads correctly
- [ ] Windows Portable downloads correctly
- [ ] macOS DMG downloads correctly
- [ ] macOS ZIP downloads correctly
- [ ] Linux AppImage downloads correctly
- [ ] Linux DEB downloads correctly
- [ ] Linux RPM downloads correctly

## Step 10: Announce Release

### On GitHub
- The release is already announced automatically
- Pin the release if desired

### On Website
Create a blog post announcing v1.4.0 with:
- Feature highlights
- Download links
- Upgrade instructions

### Social Media
Share the release announcement:
- Twitter/X
- LinkedIn
- Reddit (r/privacy, r/opensource)
- Product Hunt

## Troubleshooting

### Android Build Fails

**Issue**: Gradle can't download dependencies
**Solution**: Check internet connection, try clearing Gradle cache:
```bash
rm -rf ~/.gradle/caches
./gradlew clean assembleRelease
```

**Issue**: SDK not found
**Solution**: Set ANDROID_HOME environment variable:
```bash
export ANDROID_HOME=/path/to/android/sdk
```

### Desktop Build Fails

**Issue**: npm install fails
**Solution**: Clear npm cache and retry:
```bash
rm -rf node_modules package-lock.json
npm cache clean --force
npm install
```

**Issue**: electron-builder fails
**Solution**: Install platform-specific dependencies:
- **macOS**: `xcode-select --install`
- **Linux**: `sudo apt-get install -y rpm`
- **Windows**: Install Visual Studio Build Tools

### GitHub Release Fails

**Issue**: Authentication error
**Solution**: Check token permissions:
- Token needs `repo` scope
- Token must not be expired

**Issue**: File too large
**Solution**: GitHub allows up to 2GB per file. If needed, compress:
```bash
zip CleanFinding-Browser-1.4.0.zip large-file.exe
```

## Build Time Estimates

- **Android APK**: 5-10 minutes (first build)
- **Windows builds**: 10-15 minutes
- **macOS builds**: 10-15 minutes
- **Linux builds**: 10-15 minutes
- **Total**: 30-60 minutes for all platforms

## File Size Summary

| Platform | File | Size |
|----------|------|------|
| Android | APK | ~8 MB |
| Windows | Installer | ~73 MB |
| Windows | Portable | ~73 MB |
| macOS | DMG | ~89 MB |
| macOS | ZIP | ~89 MB |
| Linux | AppImage | ~99 MB |
| Linux | DEB | ~99 MB |
| Linux | RPM | ~99 MB |
| **Total** | | **~620 MB** |

## Quick Command Reference

```bash
# Build everything (Linux)
cd android && ./gradlew assembleRelease && cd ../desktop && npm install && npm run build:linux

# Create tag and push
git tag -a v1.4.0 -m "Release v1.4.0" && git push origin v1.4.0

# Create GitHub release
gh release create v1.4.0 --repo MyRechargeHub1/cleanfinding-browser --title "CleanFinding Browser v1.4.0" --notes-file RELEASE_NOTES_v1.4.0.md releases/v1.4.0/*

# Update website (in cleanfinding.com repo)
# Edit download-browser.html, then:
git add download-browser.html && git commit -m "Update to v1.4.0" && git push origin main
```

## Success Checklist

- [ ] All platforms built successfully
- [ ] Checksums generated
- [ ] Git tag created and pushed
- [ ] GitHub Release published with all files
- [ ] Download URLs updated on website
- [ ] All download links tested
- [ ] Release announced
- [ ] Documentation updated

## Support

If you encounter issues:
1. Check the troubleshooting section above
2. Review build logs for specific errors
3. Ensure all prerequisites are installed
4. Check GitHub token permissions

---

**Estimated Total Time**: 1-2 hours (including builds, testing, and deployment)

Good luck with the release! 🚀
