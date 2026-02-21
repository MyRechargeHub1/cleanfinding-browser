# Contributing to CleanFinding Browser

Thanks for your interest in contributing to CleanFinding Browser.

## Development setup

### Prerequisites
- Git
- Java 17 (for Android builds)
- Android Studio (latest stable)
- Node.js 18+ (for desktop tooling)
- Xcode 15+ on macOS (for iOS builds)

### Clone and branch
```bash
git clone <your-fork-url>
cd cleanfinding-browser
git checkout -b feature/short-description
```

## Project structure
- `android/` Android app source
- `ios/` iOS app source
- `desktop/` Electron desktop app source
- `.github/workflows/` CI workflows

## Build and test

### Android
```bash
cd android
./gradlew assembleDebug
./gradlew test
```

### Desktop
```bash
cd desktop
npm install
npm run lint
```

### iOS
See [ios/README.md](ios/README.md) for detailed local build instructions.

## Commit guidelines
- Use clear commit messages (e.g., `fix(android): prevent tab crash on rotation`).
- Keep commits focused and small.
- Update documentation when behavior changes.

## Pull request checklist
- [ ] Code builds locally for the target platform
- [ ] Tests or checks relevant to the change pass
- [ ] Documentation updated (if needed)
- [ ] No secrets or generated artifacts added

## Reporting issues
When filing issues, include:
- Platform (Android / iOS / Desktop)
- App version
- Steps to reproduce
- Expected vs. actual behavior
- Screenshots/logs if available
