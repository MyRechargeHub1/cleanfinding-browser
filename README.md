# CleanFinding Mobile Browser

**Privacy-focused, family-safe mobile browser for Android and iOS.**

![Version](https://img.shields.io/badge/version-1.0.1-blue)
![Platform](https://img.shields.io/badge/platform-Android%20%7C%20iOS-green)

## ✨ Features

- **SafeSearch Always On**: Cannot be disabled, enforced on Google/Bing/YouTube
- **Tracker Blocking**: Blocks 25+ tracking domains (Google Analytics, Facebook, etc.)
- **Ad Blocking**: Built-in ad blocker
- **Adult Content Filtering**: Blocks inappropriate websites
- **Privacy Focused**: No data collection or telemetry
- **Hardware Accelerated**: GPU-accelerated video playback
- **Video Playback Fix**: Proper rendering on YouTube, Pinterest, all media sites
- **Image Rendering Fix**: No more cut-off images or content overflow

## 🐛 Bug Fixes (v1.0.1)

### Video Playback Issues - FIXED ✅
- **Problem**: Videos showing black screens, not playing properly
- **Fix**: Enabled hardware acceleration in WebView and AndroidManifest
- **Fix**: Set `mediaPlaybackRequiresUserGesture = false` for autoplay
- **Fix**: Injected CSS to fix video container sizing
- **Fix**: Added `setLayerType(LAYER_TYPE_HARDWARE)` for GPU acceleration

### Image Rendering - FIXED ✅
- **Problem**: Images being cut off or not displaying correctly (Pinterest, etc.)
- **Fix**: Injected CSS to fix image max-width and object-fit
- **Fix**: Added `layoutAlgorithm = TEXT_AUTOSIZING` for better layout
- **Fix**: Fixed viewport handling with `useWideViewPort = true`
- **Fix**: Prevented content overflow with CSS fixes

## Android

### Build Requirements
- Android Studio Arctic Fox or later
- JDK 17
- Android SDK 34

### Build APK
```bash
cd android
./gradlew assembleRelease
```

### Build AAB (for Play Store)
```bash
cd android
./gradlew bundleRelease
```

## iOS

### Build Requirements
- Xcode 15+
- macOS 13+
- Apple Developer Account

### Build
Open `ios/CleanFindingBrowser.xcodeproj` in Xcode and build.

## Download

Pre-built versions available at:
- [cleanfinding.com/download-browser](https://cleanfinding.com/download-browser)
- [GitHub Releases](https://github.com/MyRechargeHub1/cleanfinding-browser-mobile/releases)

## License

MIT License - See LICENSE file
