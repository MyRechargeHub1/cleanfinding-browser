# CleanFinding Browser iOS Build Guide

This document explains how to build, run, and archive the iOS app.

## Requirements
- macOS 13+
- Xcode 15+
- Apple Developer account (for device install/TestFlight/App Store)

## Open the project
1. Open Xcode.
2. Select **File → Open...**.
3. Choose `ios/CleanFindingBrowser/CleanFindingBrowser.xcodeproj`.

## Configure signing
1. Select the **CleanFindingBrowser** project in the navigator.
2. Select the **CleanFindingBrowser** target.
3. Open **Signing & Capabilities**.
4. Choose your Team.
5. Ensure the Bundle Identifier is unique for local testing if needed.

## Run on Simulator
1. Choose an iPhone simulator (for example, iPhone 15).
2. Press **⌘R** to build and run.

## Run on physical device
1. Connect your device and trust the development certificate.
2. Select your device in Xcode.
3. Press **⌘R**.

## Create a release archive
1. Select **Any iOS Device (arm64)** in destination.
2. Choose **Product → Archive**.
3. In Organizer, select the archive and click **Distribute App**.
4. Choose TestFlight or App Store Connect and follow prompts.

## Troubleshooting
- **Signing failed**: Re-check Team and provisioning profile settings.
- **Bundle identifier conflict**: Change Bundle ID for local testing.
- **Build errors after Xcode update**: Clean build folder (**Shift+⌘K**) and rebuild.
