# CleanFinding Browser

A privacy-focused, family-safe web browser built with Electron.

## Features

- **SafeSearch Always On** - Cannot be disabled, ensures safe search results
- **Tracker Blocking** - Blocks Google Analytics, Facebook Pixel, and 20+ trackers
- **Ad Blocking** - Blocks common advertising networks
- **Adult Content Filtering** - Blocks inappropriate websites
- **Privacy Focused** - No telemetry, option to clear data on exit
- **CleanFinding Integration** - Default homepage and search engine
- **Modern UI** - Clean, dark-themed interface with tab support

## Requirements

- Node.js 18+
- npm or yarn

## Installation

```bash
# Navigate to browser directory
cd browser

# Install dependencies
npm install
```

## Development

```bash
# Run in development mode
npm start

# Run with DevTools
npm run dev
```

## Building

### Windows (.exe)
```bash
npm run build:win
```
Output: `dist/CleanFinding Browser Setup x.x.x.exe`

### macOS (.dmg)
```bash
npm run build:mac
```
Output: `dist/CleanFinding Browser-x.x.x.dmg`

### Linux (.AppImage)
```bash
npm run build:linux
```
Output: `dist/CleanFinding Browser-x.x.x.AppImage`

### All Platforms
```bash
npm run build
```

## Project Structure

```
browser/
├── main.js           # Electron main process
├── preload.js        # Security bridge (IPC)
├── package.json      # Project config & build settings
├── src/
│   ├── index.html    # Browser UI
│   ├── styles.css    # UI styling
│   └── renderer.js   # Tab management & navigation
├── lib/              # Utility modules
├── assets/
│   └── icons/        # App icons
├── build/            # Build resources
│   ├── icon.ico      # Windows icon (256x256)
│   ├── icon.icns     # macOS icon
│   └── icons/        # Linux icons (various sizes)
└── dist/             # Built executables
```

## Security Features

### Tracker Blocking
Blocks requests to known tracking domains including:
- Google Analytics
- Facebook Pixel
- Mixpanel
- Hotjar
- And 20+ more

### SafeSearch Enforcement
Forces safe search on:
- Google (`safe=active`)
- Bing (`adlt=strict`)
- DuckDuckGo (`kp=1`)
- YouTube (restricted mode)

### Content Filtering
- Adult domain blocklist
- Keyword-based URL filtering
- Customizable blocklists

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| Ctrl+T | New tab |
| Ctrl+W | Close tab |
| Ctrl+L | Focus URL bar |
| F5 | Refresh |
| Alt+Left | Back |
| Alt+Right | Forward |
| Alt+Home | Home |
| Escape | Close settings |

## Configuration

Settings are stored using `electron-store` and persist across sessions:

- `homepage` - Default: https://cleanfinding.com
- `searchEngine` - Default: https://cleanfinding.com/search?q=
- `blockAds` - Default: true
- `blockTrackers` - Default: true
- `forceSafeSearch` - Default: true (cannot be changed)
- `blockAdultContent` - Default: true
- `clearDataOnExit` - Default: false
- `darkMode` - Default: false

## Code Signing (for distribution)

To avoid "Unknown publisher" warnings:

### Windows
Purchase an EV Code Signing Certificate (~$300-500/year) from:
- DigiCert
- Sectigo
- GlobalSign

### macOS
Enroll in Apple Developer Program ($99/year)

## License

MIT License - See LICENSE file

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## Support

Visit https://cleanfinding.com for support and documentation.
