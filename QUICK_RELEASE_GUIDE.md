# Quick Release Guide - CleanFinding Browser v1.4.0

## 📦 TL;DR - Fastest Way to Release

```bash
# 1. Build everything
./scripts/prepare-release.sh

# 2. Create GitHub Release
git tag -a v1.4.0 -m "Release v1.4.0"
git push browser-origin v1.4.0

gh release create v1.4.0 \
  --title "CleanFinding Browser v1.4.0" \
  --notes-file RELEASE_NOTES_v1.4.0.md \
  releases/v1.4.0/*

# 3. Update download page (already done on cleanfinding.com!)
```

## 🎯 Answer to Your Question

**Q: Which repository should host the files?**

**A: Use BOTH repositories with different purposes:**

### ✅ Recommended Structure:

1. **cleanfinding-browser** (Source Code)
   - All source code (Android, Desktop)
   - Development files
   - **GitHub Releases** for distribution files (APK, EXE, DMG, etc.)

2. **cleanfinding.com** (Website)
   - HTML/CSS/JavaScript for website
   - Marketing content
   - Blog posts
   - **NO binary files, NO source code**

### 💡 Distribution Strategy:

Use **GitHub Releases** on `cleanfinding-browser` repository:
- ✅ Free unlimited bandwidth
- ✅ Automatic CDN
- ✅ Version management
- ✅ Download statistics
- ✅ Keeps repos small

## 🚀 Complete Release Workflow

### Step 1: Build All Platforms

```bash
cd /home/user/cleanfinding-browser

# Run automated build script
./scripts/prepare-release.sh
```

This will:
- Build Android APK
- Build Windows (Installer + Portable)
- Build macOS (DMG + ZIP)
- Build Linux (AppImage + DEB + RPM)
- Generate checksums
- Place everything in `releases/v1.4.0/`

### Step 2: Test Builds

Test each platform's binary before releasing:
- Android: Install APK on device
- Windows: Run installer and portable
- macOS: Test DMG installation
- Linux: Test AppImage

### Step 3: Create GitHub Release

```bash
# Tag the version
git tag -a v1.4.0 -m "Release v1.4.0 - Major Privacy Features"
git push browser-origin v1.4.0

# Create release with GitHub CLI
gh release create v1.4.0 \
  --repo MyRechargeHub1/cleanfinding-browser \
  --title "CleanFinding Browser v1.4.0" \
  --notes-file RELEASE_NOTES_v1.4.0.md \
  releases/v1.4.0/*
```

Or manually via GitHub web interface:
1. Go to https://github.com/MyRechargeHub1/cleanfinding-browser/releases
2. Click "Draft a new release"
3. Tag: v1.4.0
4. Title: CleanFinding Browser v1.4.0
5. Copy content from RELEASE_NOTES_v1.4.0.md
6. Upload files from releases/v1.4.0/
7. Click "Publish release"

### Step 4: Update Download Links

The download page is **already updated** on cleanfinding.com!

Just need to change `href="#"` to actual URLs after release is created.

Example URLs will be:
```
https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0.apk
https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-Setup-1.4.0.exe
etc.
```

## 📁 Current File Locations

| File Type | Current Location | Should Go To |
|-----------|-----------------|--------------|
| Source code (Android/Desktop) | ✅ cleanfinding-browser repo | Keep here |
| Website HTML | ✅ cleanfinding.com repo | Keep here |
| Download page | ✅ cleanfinding.com repo | Keep here |
| Built APK/EXE/DMG | ⏳ Not built yet | GitHub Releases |

## 🎯 What's Already Done

✅ Source code in cleanfinding-browser repo
✅ Website in cleanfinding.com repo
✅ Download page updated on cleanfinding.com (live!)
✅ Repository organization documented
✅ Release notes prepared
✅ Build script created

## ⏳ What You Need to Do

1. **Run build script**: `./scripts/prepare-release.sh`
2. **Test builds**: Install and test on each platform
3. **Create release**: Push tag and create GitHub release
4. **Update links**: Change `#` to actual download URLs
5. **Announce**: Blog post, social media, etc.

## 🔗 Download URL Format

After creating the GitHub release, your download URLs will be:

```
Android:
https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0.apk

Windows Installer:
https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-Setup-1.4.0.exe

Windows Portable:
https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0-portable.exe

macOS DMG:
https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0-universal.dmg

macOS ZIP:
https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0-universal.zip

Linux AppImage:
https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0-x86_64.AppImage

Linux DEB:
https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/cleanfinding-browser_1.4.0_amd64.deb

Linux RPM:
https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/cleanfinding-browser-1.4.0-1.x86_64.rpm
```

## 📝 Files Created for You

1. **REPOSITORY_ORGANIZATION.md** - Complete repository strategy
2. **RELEASE_NOTES_v1.4.0.md** - Release notes for GitHub
3. **scripts/prepare-release.sh** - Automated build script
4. **QUICK_RELEASE_GUIDE.md** - This file

## ❓ Common Questions

**Q: Do I need to store binaries in git?**
A: No! Use GitHub Releases. They're designed for this.

**Q: What about bandwidth costs?**
A: GitHub Releases are free with unlimited bandwidth via CDN.

**Q: Can I use my own server?**
A: Yes, but GitHub Releases is easier and free.

**Q: What about versioning?**
A: GitHub Releases handles versioning automatically via tags.

**Q: What if files are too large?**
A: GitHub allows files up to 2GB. Your largest is ~99 MB (Linux).

## 🎉 Summary

**Best Practice:**
- **cleanfinding-browser**: Source code only
- **cleanfinding.com**: Website only
- **GitHub Releases**: Distribution files

**You're 80% done!** Just need to:
1. Build binaries
2. Create GitHub Release
3. Update download URLs

---

**Need Help?**
- Check REPOSITORY_ORGANIZATION.md for details
- See RELEASE_NOTES_v1.4.0.md for release content
- Run `./scripts/prepare-release.sh` for automated builds
