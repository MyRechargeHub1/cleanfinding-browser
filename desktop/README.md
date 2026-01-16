# CleanFinding Browser - Desktop Edition

Privacy-focused, family-safe browser for Windows, macOS, and Linux built with Electron.

## Version 1.4.0

Cross-platform desktop browser with advanced privacy features matching the Android app.

## Features

### Privacy Features
- **Tracker Blocking** - Block known trackers and analytics scripts
- **Duck Player** - Watch YouTube videos with privacy protection
- **Email Protection** - Block email tracking pixels and link trackers
- **Cookie Auto-Decline** - Automatically decline cookie consent banners
- **Global Privacy Control (GPC)** - Send "Do Not Sell My Data" signal
- **Privacy Dashboard** - Real-time tracking protection statistics

### Browser Features
- **Tab Management** - Multiple tabs with incognito mode support
- **Address Bar** - Smart search and navigation
- **Privacy Grades** - Real-time privacy grade for each website
- **Bookmark Management** - Save and organize favorite sites
- **Download Manager** - Track and manage downloads
- **Settings** - Customizable privacy and appearance settings

## Getting Started

### Prerequisites

- Node.js 16+ and npm 8+
- Platform-specific build tools:
  - **Windows**: Visual Studio Build Tools
  - **macOS**: Xcode Command Line Tools
  - **Linux**: build-essential package

### Installation

```bash
cd desktop
npm install
```

### Development

Run in development mode:

```bash
npm start
# or with DevTools
npm run dev
```

### Building

Build for your current platform:

```bash
npm run build
```

Build for specific platforms:

```bash
# Windows
npm run build:win

# macOS
npm run build:mac

# Linux
npm run build:linux
```

## Project Structure

```
desktop/
├── main.js                 # Electron main process
├── preload.js             # Preload script (context bridge)
├── package.json           # Project configuration
├── src/
│   ├── renderer/
│   │   ├── index.html    # Main browser UI
│   │   ├── settings.html # Settings page
│   │   ├── js/
│   │   │   └── browser.js # Browser logic
│   │   └── styles/
│   │       └── main.css   # Browser styles
│   └── native/           # Platform-specific code (future)
├── build/
│   └── icons/            # Application icons
└── dist/                 # Build output (generated)
```

## Architecture

### Main Process (main.js)
- Window management
- Privacy feature integration
- Network request filtering
- IPC handlers for renderer communication
- Settings persistence via electron-store

### Preload Script (preload.js)
- Secure IPC bridge via contextBridge
- Exposes cleanfindingAPI to renderer
- No direct Node.js access from renderer

### Renderer Process
- Browser UI (tabs, address bar, navigation)
- WebView management for page rendering
- Settings interface
- Privacy dashboard

## Privacy Implementation

### Tracker Blocking
- Network-level blocking via `session.webRequest.onBeforeRequest`
- Configurable tracker domain list
- Real-time blocking statistics

### Duck Player Integration
- Automatic YouTube URL detection
- Redirect to privacy-friendly youtube-nocookie.com
- JavaScript injection for enhanced privacy
- Remove tracking parameters and overlays

### Global Privacy Control
- Automatic Sec-GPC header injection
- Standards-compliant implementation
- Per-request enforcement

## Shared Privacy Handlers

This desktop app uses shared privacy handlers from `../shared/privacy-handlers/`:

- **DuckPlayerHandler.js** - YouTube privacy protection
- Future: EmailProtectionHandler, CookieConsentHandler, etc.

This ensures consistent behavior across Android, Desktop, and future iOS platforms.

## Security

### Content Security Policy
- Strict CSP for renderer processes
- No inline script execution
- Limited external resource loading

### Context Isolation
- Full context isolation enabled
- No nodeIntegration in renderer
- Secure IPC via contextBridge

### URL Validation
- Whitelist-based URL scheme validation
- Block javascript:, data:, file: schemes
- Prevent XSS attacks

### Sandbox Mode
- Renderer processes run in sandbox
- Limited system access
- Remote module disabled

## Build Distribution

### Windows
- **NSIS Installer** - Standard Windows installer
- **Portable** - No-install executable
- Artifact: `CleanFinding-Browser-Setup-1.4.0.exe`

### macOS
- **DMG** - Disk image installer
- **ZIP** - Portable archive
- Universal binary (x64 + ARM64)
- Artifact: `CleanFinding-Browser-1.4.0-universal.dmg`

### Linux
- **AppImage** - Universal Linux package
- **DEB** - Debian/Ubuntu package
- **RPM** - Fedora/RHEL package
- Artifact: `CleanFinding-Browser-1.4.0-x86_64.AppImage`

## Platform-Specific Features

### Windows
- Windows Hello integration (planned)
- Native notifications
- Auto-update support

### macOS
- Touch ID/Face ID integration (planned)
- Native menu bar
- macOS-specific shortcuts

### Linux
- System theme integration
- Desktop environment integration
- Package manager support

## Configuration

Settings are stored using electron-store:

**Location:**
- Windows: `%APPDATA%\cleanfinding-browser-desktop\config.json`
- macOS: `~/Library/Application Support/cleanfinding-browser-desktop/config.json`
- Linux: `~/.config/cleanfinding-browser-desktop/config.json`

**Default Settings:**
```json
{
  "privacy": {
    "trackerBlocking": true,
    "duckPlayer": true,
    "emailProtection": true,
    "cookieAutoDecline": true,
    "globalPrivacyControl": true
  },
  "appearance": {
    "theme": "light"
  },
  "security": {
    "biometricLock": false
  }
}
```

## Development Roadmap

### Phase 1: Core Browser (✅ Complete)
- [x] Basic browser UI with tabs
- [x] WebView integration
- [x] Navigation controls
- [x] Settings page

### Phase 2: Privacy Features (✅ Complete)
- [x] Tracker blocking
- [x] Duck Player integration
- [x] Global Privacy Control
- [x] Privacy dashboard

### Phase 3: Enhanced Features (Planned)
- [ ] Email Protection handler
- [ ] Cookie consent auto-decline
- [ ] History management
- [ ] Bookmark sync
- [ ] Download manager UI

### Phase 4: Platform Integration (Planned)
- [ ] Biometric authentication
- [ ] Auto-update mechanism
- [ ] System tray integration
- [ ] Native context menus

### Phase 5: Polish & Optimization (Planned)
- [ ] Performance optimization
- [ ] Memory management
- [ ] Accessibility improvements
- [ ] Localization support

## Known Limitations

1. **WebView vs BrowserView**: Currently uses `<webview>` tag. Consider migrating to Electron BrowserView for better performance.

2. **Biometric Auth**: Platform-specific implementation required for Windows Hello, Touch ID, etc.

3. **Icons**: Placeholder icons included. Replace with proper multi-resolution icons for production.

4. **Auto-Update**: Not yet implemented. Requires code signing and update server.

5. **Extension Support**: Chrome extensions not supported. Consider adding extension API compatibility.

## Testing

```bash
# Manual testing
npm start

# Test specific features
npm start -- --dev  # Opens DevTools for debugging
```

## Debugging

Enable DevTools in development:
- Press F12 in the application
- Or launch with `--dev` flag
- Main process logs in terminal
- Renderer process logs in DevTools

## Contributing

When adding features:
1. Keep privacy handlers in `../shared/privacy-handlers/` for cross-platform use
2. Follow existing code structure
3. Test on all target platforms
4. Update this README

## License

MIT License - See LICENSE file for details

## Support

- Website: https://cleanfinding.com
- Issues: https://github.com/MyRechargeHub1/cleanfinding-browser/issues
- Email: support@cleanfinding.com

## Version History

### v1.4.0 (2024-01-16)
- Initial desktop release
- Core browser functionality
- Privacy features (Tracker Blocking, Duck Player, GPC)
- Settings management
- Cross-platform support (Windows, macOS, Linux)

---

**Built with privacy by default** 🛡️
