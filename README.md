# CleanFinding Browser

**A privacy-focused, family-safe web browser built with Electron.**

![Version](https://img.shields.io/badge/version-1.0.1-blue)
![License](https://img.shields.io/badge/license-MIT-green)
![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey)

## 🌟 Features

### 🔐 Privacy & Security
- **Zero Tracking** - No telemetry or data collection
- **Tracker Blocking** - Blocks Google Analytics, Facebook Pixel, and 25+ trackers
- **Ad Blocking** - Blocks advertising networks
- **Clear Data on Exit** - Optional automatic data clearing

### ✨ Family-Safe Browsing
- **SafeSearch Always On** - Enforced on Google, Bing, YouTube (cannot be disabled)
- **Adult Content Filtering** - Domain-based blocking
- **YouTube Restricted Mode** - Automatically enabled
- **Protected by Design** - Built for families and schools

### ⚡ Performance
- **Hardware Acceleration** - GPU-accelerated video playback
- **Fast Rendering** - Chromium-based engine
- **Video Playback Fix** - Proper rendering of YouTube, Pinterest, and all media sites
- **Mobile-Optimized** - Responsive viewport handling

### 🎨 Modern Interface
- **Dark Theme** - Easy on the eyes
- **Clean Design** - Distraction-free browsing
- **Keyboard Shortcuts** - Power user friendly
- **Settings Panel** - Easy customization

---

## 📦 Installation

### Download Pre-Built Binaries

Visit [CleanFinding.com/download-browser](https://cleanfinding.com/download-browser) to download for:

- **Windows** - `.exe` installer
- **macOS** - `.dmg` installer
- **Linux** - `.AppImage`

### Build from Source

**Requirements:**
- Node.js 18+
- npm or yarn

**Steps:**

```bash
# Clone the repository
git clone https://github.com/MyRechargeHub1/cleanfinding-browser.git
cd cleanfinding-browser

# Install dependencies
npm install

# Run in development mode
npm start

# Build for your platform
npm run build:win    # Windows
npm run build:mac    # macOS
npm run build:linux  # Linux

# Output will be in the dist/ folder
```

---

## 🚀 Usage

### Navigation
- **Address Bar** - Type URL or search query
- **Back/Forward** - Navigate history
- **Home** - Return to homepage
- **Refresh** - Reload page

### Keyboard Shortcuts
- `Ctrl+L` - Focus address bar
- `F5` - Refresh page
- `Alt+←` - Go back
- `Alt+→` - Go forward
- `Ctrl+T` - New tab
- `Ctrl+Q` - Quit

### Settings
Click the ⚙ gear icon to access:
- Homepage customization
- Search engine preference
- Privacy toggles
- Safety features

---

## 🐛 Bug Fixes (v1.0.1)

### Video Playback Issues - FIXED ✅
- **Problem**: Videos showing black screens, not playing properly
- **Fix**: Enabled hardware acceleration, proper GPU flags
- **Fix**: Added autoplay policy for media
- **Fix**: Injected CSS to fix video container sizing
- **Fix**: Fixed viewport scaling for mobile content

### Image Rendering - FIXED ✅
- **Problem**: Images being cut off or not displaying correctly
- **Fix**: Proper BrowserView bounds calculation
- **Fix**: Auto-resize enabled for content scaling
- **Fix**: Fixed mobile viewport handling

---

## 📧 Support

- Website: [cleanfinding.com](https://cleanfinding.com)
- GitHub: [Issues](https://github.com/MyRechargeHub1/cleanfinding-browser/issues)

---

**Made with ❤️ by CleanFinding**