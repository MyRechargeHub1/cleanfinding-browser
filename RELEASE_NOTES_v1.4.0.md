# CleanFinding Browser v1.4.0

**Release Date**: January 16, 2024

Privacy-focused, family-safe browser with major new privacy features across all platforms.

## 🎉 What's New

### Major Features

#### 🦆 Duck Player - YouTube Privacy Protection
Watch YouTube videos with enhanced privacy. Duck Player automatically redirects to youtube-nocookie.com, removes tracking parameters, blocks recommendations, and prevents Google from tracking your viewing habits.

#### 📧 Email Protection (Android)
Blocks email tracking pixels and link trackers in Gmail, Outlook, Yahoo Mail, and ProtonMail. Prevents senders from knowing when you open emails or click links.

#### 🔒 Biometric Lock (Android)
Protect incognito tabs with fingerprint or face authentication. Adds an extra layer of security to your private browsing sessions.

#### 🍪 Cookie Auto-Decline (Android)
Automatically detects and declines cookie consent banners. Refuses tracking cookies while accepting only essential functionality cookies.

#### 🌐 Global Privacy Control (GPC)
Sends "Do Not Sell My Data" signals to websites. Legally binding under CCPA and GDPR, telling websites not to sell or share your personal information.

#### 💻 Desktop Browser Release
Windows, macOS, and Linux versions now available with full privacy feature parity.

### Privacy & Security Enhancements

#### 🔐 Security Fixes
- **Fixed JavaScript Injection Vulnerability**: Proper input escaping prevents XSS attacks through malicious tracker domains
- **Added URL Scheme Validation**: Blocks javascript:, data:, and file: schemes to prevent code execution and information disclosure
- **Content Security Policy**: Enforced CSP for renderer processes on desktop prevents malicious script injection

#### 🧠 Memory Leak Fixes
- Fixed BroadcastReceiver memory leak in download manager
- Fixed WebView cleanup to prevent memory leaks in tab management
- Fixed uncancelled Coroutine scopes causing background task leaks
- Fixed custom view callbacks not being properly released

### Performance Improvements
- ⚡ Optimized tracker blocking for faster page loads
- 📉 Reduced memory usage with proper resource cleanup
- 🏃 Faster tab switching with improved WebView management
- 🔋 Better battery efficiency with optimized background tasks

### UI/UX Improvements
- 🎨 Redesigned settings interface with categorized options
- 📊 Privacy dashboard modal with real-time statistics
- 🏷️ Improved tab management UI with incognito indicators
- 🔍 Enhanced address bar with privacy grade display (A+ to F)
- ✨ Modern, clean UI for desktop applications

## 📥 Download

### Android
- **Version**: 1.4.0
- **Size**: ~8 MB
- **Requirements**: Android 8.0 (Oreo) or later
- **Download**: [CleanFinding-Browser-1.4.0.apk](https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0.apk)

### Windows
- **Version**: 1.4.0
- **Size**: ~73 MB
- **Requirements**: Windows 10/11 (64-bit)
- **Download**:
  - [Installer](https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-Setup-1.4.0.exe)
  - [Portable](https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0-portable.exe)

### macOS
- **Version**: 1.4.0
- **Size**: ~89 MB
- **Requirements**: macOS 10.13 or later (Intel & Apple Silicon)
- **Download**:
  - [DMG](https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0-universal.dmg)
  - [ZIP](https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0-universal.zip)

### Linux
- **Version**: 1.4.0
- **Size**: ~99 MB
- **Requirements**: Ubuntu 18.04+ / Debian / Fedora
- **Download**:
  - [AppImage](https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/CleanFinding-Browser-1.4.0-x86_64.AppImage)
  - [DEB](https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/cleanfinding-browser_1.4.0_amd64.deb)
  - [RPM](https://github.com/MyRechargeHub1/cleanfinding-browser/releases/download/v1.4.0/cleanfinding-browser-1.4.0-1.x86_64.rpm)

## 🎯 Features by Platform

| Feature | Android | Windows | macOS | Linux |
|---------|---------|---------|-------|-------|
| Tracker Blocking | ✓ | ✓ | ✓ | ✓ |
| Duck Player | ✓ | ✓ | ✓ | ✓ |
| Email Protection | ✓ | Planned | Planned | Planned |
| Cookie Auto-Decline | ✓ | Planned | Planned | Planned |
| Global Privacy Control | ✓ | ✓ | ✓ | ✓ |
| Biometric Lock | ✓ | Planned | Planned | Password |
| Privacy Dashboard | ✓ | ✓ | ✓ | ✓ |
| Incognito Mode | ✓ | ✓ | ✓ | ✓ |

## 🐛 Bug Fixes

### Android
- Fixed memory leak in DownloadManagerHelper (BroadcastReceiver not unregistered)
- Fixed WebView memory leaks when closing tabs
- Fixed Coroutine scope not cancelled on Activity destruction
- Fixed custom view callbacks causing memory retention
- Improved error handling for network requests

### Desktop
- Fixed context isolation security issues
- Fixed renderer process sandbox configuration
- Improved IPC security with proper validation

## 📝 Changelog

### Added
- Duck Player for YouTube privacy protection
- Email Protection for webmail tracking blocking
- Biometric authentication for incognito tabs (Android)
- Cookie consent auto-decline functionality
- Global Privacy Control (GPC) headers
- Desktop browser for Windows, macOS, Linux
- Privacy dashboard with real-time statistics
- Privacy grade indicator (A+ to F) per website
- URL scheme validation for security
- Content Security Policy enforcement (Desktop)

### Fixed
- JavaScript injection vulnerability with proper escaping
- BroadcastReceiver memory leak in download manager
- WebView cleanup memory leaks
- Uncancelled Coroutine scopes
- Custom view callback memory retention

### Improved
- Tracker blocking performance
- Memory management across all platforms
- Tab switching speed
- Battery efficiency
- Settings UI organization
- Privacy dashboard presentation

### Security
- Added input sanitization for injected JavaScript
- Implemented URL scheme whitelist validation
- Enforced Content Security Policy
- Enhanced context isolation in renderer processes
- Improved sandbox configuration

## 🔧 Technical Details

### Dependencies
- **Android**: AndroidX Biometric 1.2.0-alpha05
- **Desktop**: Electron 28.0.0, electron-store 8.1.0

### Build Information
- **Android**: Gradle 8.0, AGP 8.1.0
- **Desktop**: Node.js 16+, npm 8+
- **Frameworks**: Kotlin, JavaScript/Node.js

### Architecture
- **Android**: Native Kotlin with WebView
- **Desktop**: Electron with Chromium renderer
- **Shared**: JavaScript privacy handlers (cross-platform)

## 🚀 Upgrade Instructions

### Android
1. Download new APK
2. Install over existing version (settings preserved)
3. Grant any new permissions if prompted

### Desktop
1. Download installer for your platform
2. Run installer (will upgrade existing installation)
3. Settings automatically migrated

## ⚠️ Known Issues

1. **Biometric Lock**: Desktop platform-specific implementations pending (Windows Hello, Touch ID)
2. **Email Protection**: Desktop version in development
3. **Cookie Auto-Decline**: Desktop version in development
4. **Auto-Update**: Not yet implemented for desktop versions

## 🔜 Coming Soon

- iOS version with feature parity
- Browser extension support (Chrome/Firefox)
- Sync across devices
- Custom filter lists
- Enhanced parental controls
- Password manager integration

## 📚 Documentation

- [Installation Guide](https://github.com/MyRechargeHub1/cleanfinding-browser#installation)
- [User Manual](https://cleanfinding.com/docs)
- [Privacy Policy](https://cleanfinding.com/privacy)
- [FAQ](https://cleanfinding.com/download-browser#faq)

## 💬 Support

- **Website**: https://cleanfinding.com
- **Issues**: https://github.com/MyRechargeHub1/cleanfinding-browser/issues
- **Email**: support@cleanfinding.com

## 🙏 Acknowledgments

Thank you to all users who provided feedback and bug reports. Your input helps make CleanFinding Browser better for everyone.

---

**Full Changelog**: https://github.com/MyRechargeHub1/cleanfinding-browser/compare/v1.3.0...v1.4.0
