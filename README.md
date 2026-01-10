# CleanFinding Mobile Browser

Privacy-focused, family-safe mobile browser for Android and iOS.

## Features

- **SafeSearch Always On**: Cannot be disabled
- **Tracker Blocking**: Blocks 25+ tracking domains
- **Ad Blocking**: Built-in ad blocker
- **Adult Content Filtering**: Blocks inappropriate websites
- **Privacy Focused**: No data collection

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
