# Repository Organization Strategy for CleanFinding Browser

## 📊 Two-Repository Approach (RECOMMENDED)

### Repository 1: cleanfinding.com (Website)
**GitHub**: https://github.com/MyRechargeHub1/cleanfinding.com.git
**Purpose**: Public website, marketing, documentation
**Contents**: HTML pages, blog, assets, no source code

### Repository 2: cleanfinding-browser (Application)
**GitHub**: https://github.com/MyRechargeHub1/cleanfinding-browser.git
**Purpose**: Browser source code, development, builds
**Contents**: Android/Desktop source code, shared modules, releases

## 🎯 File Distribution Strategy

### cleanfinding.com Repository
```
✅ Keep:
- index.html (homepage)
- download-browser.html (download page)
- about.html, privacy.html, terms.html
- blog posts and articles
- CSS, JavaScript, images
- Extension files (browser extension)
- Marketing materials

❌ Don't Store:
- Source code (belongs in browser repo)
- Large binary files (APK, EXE, DMG)
- Build artifacts
```

### cleanfinding-browser Repository
```
✅ Keep:
- android/ (source code)
- desktop/ (source code)
- shared/ (cross-platform modules)
- Documentation (README, CROSS_PLATFORM_IMPLEMENTATION.md)
- Build configurations
- GitHub Actions workflows

❌ Don't Store:
- Website HTML files (belongs in website repo)
- Large binary builds (use GitHub Releases instead)
```

## 🚀 GitHub Releases Strategy (RECOMMENDED)

**Use GitHub Releases on cleanfinding-browser for distribution files.**

### Advantages:
✅ Unlimited bandwidth for downloads
✅ Automatic CDN delivery
✅ Version management built-in
✅ Direct download links
✅ Release notes with each version
✅ Keeps repository size small
✅ Tracks download statistics

### Release Structure:
```
cleanfinding-browser/releases/v1.4.0/
├── CleanFinding-Browser-1.4.0.apk                    (~8 MB)
├── CleanFinding-Browser-Setup-1.4.0.exe              (~73 MB)
├── CleanFinding-Browser-1.4.0-portable.exe           (~73 MB)
├── CleanFinding-Browser-1.4.0-universal.dmg          (~89 MB)
├── CleanFinding-Browser-1.4.0-universal.zip          (~89 MB)
├── CleanFinding-Browser-1.4.0-x86_64.AppImage        (~99 MB)
├── cleanfinding-browser_1.4.0_amd64.deb              (~99 MB)
├── cleanfinding-browser-1.4.0-1.x86_64.rpm           (~99 MB)
└── SHASUMS.txt (checksums for verification)
```

## 📝 Step-by-Step Release Process

### 1. Build All Platforms

```bash
# Build Android APK
cd android
./gradlew assembleRelease
# Output: android/app/build/outputs/apk/release/app-release.apk

# Build Desktop Applications
cd ../desktop
npm install

# Windows
npm run build:win
# Output: desktop/dist/CleanFinding-Browser-Setup-1.4.0.exe
#         desktop/dist/CleanFinding-Browser-1.4.0-portable.exe

# macOS
npm run build:mac
# Output: desktop/dist/CleanFinding-Browser-1.4.0-universal.dmg
#         desktop/dist/CleanFinding-Browser-1.4.0-universal.zip

# Linux
npm run build:linux
# Output: desktop/dist/CleanFinding-Browser-1.4.0-x86_64.AppImage
#         desktop/dist/cleanfinding-browser_1.4.0_amd64.deb
#         desktop/dist/cleanfinding-browser-1.4.0-1.x86_64.rpm
```

### 2. Create GitHub Release

```bash
# Tag the release
git tag -a v1.4.0 -m "Release v1.4.0 - Major Privacy Features Update"
git push browser-origin v1.4.0

# Create release using GitHub CLI (gh) or web interface
gh release create v1.4.0 \
  --title "CleanFinding Browser v1.4.0" \
  --notes-file RELEASE_NOTES.md \
  android/app/build/outputs/apk/release/app-release.apk#CleanFinding-Browser-1.4.0.apk \
  desktop/dist/CleanFinding-Browser-Setup-1.4.0.exe \
  desktop/dist/CleanFinding-Browser-1.4.0-portable.exe \
  desktop/dist/CleanFinding-Browser-1.4.0-universal.dmg \
  desktop/dist/CleanFinding-Browser-1.4.0-universal.zip \
  desktop/dist/CleanFinding-Browser-1.4.0-x86_64.AppImage \
  desktop/dist/cleanfinding-browser_1.4.0_amd64.deb \
  desktop/dist/cleanfinding-browser-1.4.0-1.x86_64.rpm
```

### 3. Update Download Links

Once the release is created, update download-browser.html with GitHub Release URLs:

```html
<!-- Android -->
<a href="https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0.apk"
   class="download-btn">Download APK</a>

<!-- Windows Installer -->
<a href="https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-Setup-1.4.0.exe"
   class="download-btn">Download Installer</a>

<!-- Windows Portable -->
<a href="https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0-portable.exe"
   class="alternative-link">Download Portable →</a>

<!-- macOS DMG -->
<a href="https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0-universal.dmg"
   class="download-btn">Download DMG</a>

<!-- macOS ZIP -->
<a href="https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0-universal.zip"
   class="alternative-link">Download ZIP →</a>

<!-- Linux AppImage -->
<a href="https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0-x86_64.AppImage"
   class="download-btn">Download AppImage</a>

<!-- Linux DEB -->
<a href="https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/cleanfinding-browser_1.4.0_amd64.deb"
   class="alternative-link">Download DEB →</a>

<!-- Linux RPM -->
<a href="https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/cleanfinding-browser-1.4.0-1.x86_64.rpm"
   class="alternative-link">Download RPM →</a>
```

## 🔄 Alternative: Custom Download Server

If you prefer hosting on your own server:

```
your-server.com/downloads/
├── android/
│   └── CleanFinding-Browser-1.4.0.apk
├── windows/
│   ├── CleanFinding-Browser-Setup-1.4.0.exe
│   └── CleanFinding-Browser-1.4.0-portable.exe
├── macos/
│   ├── CleanFinding-Browser-1.4.0-universal.dmg
│   └── CleanFinding-Browser-1.4.0-universal.zip
└── linux/
    ├── CleanFinding-Browser-1.4.0-x86_64.AppImage
    ├── cleanfinding-browser_1.4.0_amd64.deb
    └── cleanfinding-browser-1.4.0-1.x86_64.rpm
```

Then update links to:
```html
<a href="https://cleanfinding.com/downloads/android/CleanFinding-Browser-1.4.0.apk">
```

## 📦 What Goes Where - Quick Reference

| Content Type | Repository | Location |
|--------------|-----------|----------|
| Website HTML/CSS/JS | cleanfinding.com | Root directory |
| Android Source Code | cleanfinding-browser | android/ |
| Desktop Source Code | cleanfinding-browser | desktop/ |
| Shared Modules | cleanfinding-browser | shared/ |
| Build Artifacts | GitHub Releases | cleanfinding-browser/releases |
| Documentation | Both | README.md files |
| Marketing Assets | cleanfinding.com | assets/ or images/ |
| Blog Posts | cleanfinding.com | blog/ |
| Privacy Policy | cleanfinding.com | privacy.html |

## ✅ Final Recommendation

**Use the Two-Repository + GitHub Releases approach:**

1. **cleanfinding.com**: Website only
2. **cleanfinding-browser**: Source code only
3. **GitHub Releases**: Distribution files (APK, EXE, DMG, etc.)

**Benefits:**
- Clean separation of concerns
- Smaller repository sizes
- Free CDN bandwidth from GitHub
- Automatic version management
- Better organization
- Easier maintenance

## 🚀 Next Steps

1. Build all platform binaries (see build instructions above)
2. Create v1.4.0 release on cleanfinding-browser repository
3. Upload all binaries to the release
4. Update download links in cleanfinding.com/download-browser.html
5. Test all download links
6. Announce release!

---

**Current Status:**
- ✅ Source code: cleanfinding-browser repository
- ✅ Website: cleanfinding.com repository
- ⏳ Builds: Need to generate (see build instructions)
- ⏳ Release: Need to create with binaries
- ⏳ Download links: Need to update after release
