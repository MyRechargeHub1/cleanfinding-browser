# CleanFinding Browser - Shared Privacy Handlers

**Cross-platform privacy protection modules for all CleanFinding Browser implementations**

Version: 1.4.0
Platform Support: Android, Windows, macOS, Linux, iOS

---

## Overview

This directory contains platform-agnostic JavaScript modules that implement CleanFinding Browser's core privacy features. These modules are designed to work across all platforms (Android, desktop, iOS) to ensure consistent privacy protection.

## Architecture

```
shared/
└── privacy-handlers/          # Privacy protection modules
    ├── package.json           # Package configuration
    ├── index.js               # Main entry point
    ├── DuckPlayerHandler.js   # ✅ YouTube privacy protection
    ├── EmailProtectionHandler.js    # 🔜 Email tracker blocking
    ├── CookieConsentHandler.js      # 🔜 Cookie banner auto-decline
    ├── TrackerBlocker.js            # 🔜 Network-level tracker blocking
    └── PrivacyGradeCalculator.js    # 🔜 Privacy score calculation
```

## Modules

### ✅ DuckPlayerHandler.js (COMPLETE)

**Purpose:** YouTube privacy protection
**Features:**
- Detects YouTube URLs
- Extracts video IDs
- Converts to youtube-nocookie.com embeds
- Injects privacy enhancements (blocks tracking, removes ads)
- Creates custom Duck Player pages

**Usage:**

```javascript
const DuckPlayerHandler = require('./privacy-handlers/DuckPlayerHandler');
const duckPlayer = new DuckPlayerHandler();

// Check if URL is YouTube
if (duckPlayer.isYouTubeUrl('https://youtube.com/watch?v=abc123')) {
    // Convert to privacy URL
    const privacyUrl = duckPlayer.convertToPrivacyUrl(url);
    // Returns: https://www.youtube-nocookie.com/embed/abc123
}

// Get injection script for privacy enhancements
const script = duckPlayer.getInjectionScript();
webview.executeJavaScript(script);
```

**Platform Integration:**
- **Android:** ✅ Integrated in MainActivity.kt
- **Desktop (Electron):** 🔄 Ready to integrate
- **iOS:** 🔜 Pending

### 🔜 EmailProtectionHandler.js (PLANNED)

**Purpose:** Email tracking pixel and link tracker blocking
**Status:** Design complete, implementation pending

### 🔜 CookieConsentHandler.js (PLANNED)

**Purpose:** Automatically decline cookie consent banners
**Status:** Design complete, implementation pending

### 🔜 TrackerBlocker.js (PLANNED)

**Purpose:** Network-level blocking of tracking domains
**Status:** Design complete, implementation pending

### 🔜 PrivacyGradeCalculator.js (PLANNED)

**Purpose:** Calculate privacy scores for websites
**Status:** Design complete, implementation pending

---

## Integration Guide

### Android (Kotlin)

**Current:** Android v1.4.0 uses Kotlin implementations in `android/app/src/main/java/com/cleanfinding/browser/`

**Future:** These JavaScript modules can be used via WebView's `evaluateJavascript()` for consistency across platforms.

### Desktop (Electron)

**Step 1:** Install modules

```bash
cd desktop
npm install ../shared/privacy-handlers
```

**Step 2:** Import in main process

```javascript
// desktop/main.js
const { DuckPlayerHandler } = require('@cleanfinding/privacy-handlers');
const duckPlayer = new DuckPlayerHandler();

// Setup request filtering
session.defaultSession.webRequest.onBeforeRequest({ urls: ['<all_urls>'] }, (details, callback) => {
    if (duckPlayer.isYouTubeUrl(details.url)) {
        const privacyUrl = duckPlayer.convertToPrivacyUrl(details.url);
        callback({ redirectURL: privacyUrl });
        return;
    }
    callback({});
});
```

**Step 3:** Inject scripts on page load

```javascript
// desktop/renderer/browser.js
webview.addEventListener('did-finish-load', () => {
    const script = duckPlayer.getInjectionScript();
    webview.executeJavaScript(script);
});
```

### iOS (Swift/WebKit)

**Step 1:** Add JavaScript bridge

```swift
// iOS/CleanFindingBrowser/PrivacyHandlers.swift
import WebKit

class PrivacyHandlers {
    func getDuckPlayerScript() -> String {
        // Load from shared/privacy-handlers/DuckPlayerHandler.js
        guard let scriptPath = Bundle.main.path(forResource: "DuckPlayerHandler", ofType: "js", inDirectory: "shared/privacy-handlers") else {
            return ""
        }
        return try! String(contentsOfFile: scriptPath)
    }
}
```

**Step 2:** Inject on WKWebView

```swift
let script = privacyHandlers.getDuckPlayerScript()
webView.evaluateJavaScript(script) { (result, error) in
    if let error = error {
        print("Script injection failed: \\(error)")
    }
}
```

---

## Development

### Adding a New Handler

1. **Create module file:** `shared/privacy-handlers/NewHandler.js`

2. **Follow template:**

```javascript
/**
 * NewHandler - Cross-Platform
 * Description of what this handler does
 */
class NewHandler {
    constructor() {
        // Initialize
    }

    // Methods...
}

// Export for Node.js and browser
if (typeof module !== 'undefined' && module.exports) {
    module.exports = NewHandler;
}
if (typeof window !== 'undefined') {
    window.NewHandler = NewHandler;
}
```

3. **Update index.js:** Add to exports

4. **Document:** Update this README

5. **Test:** Verify on all platforms

### Testing

```bash
cd shared/privacy-handlers
node test.js
```

### Linting

```bash
npm run lint
```

---

## Version History

### v1.4.0 (Current)
- ✅ Created shared privacy handlers directory
- ✅ Implemented DuckPlayerHandler.js
- ✅ Cross-platform JavaScript architecture
- 🔄 Desktop (Electron) integration in progress

### v1.3.0
- Android-only Kotlin implementations

---

## Roadmap

### Phase 1: Core Handlers (Week 1-2)
- [x] DuckPlayerHandler.js
- [ ] EmailProtectionHandler.js
- [ ] CookieConsentHandler.js
- [ ] TrackerBlocker.js
- [ ] PrivacyGradeCalculator.js

### Phase 2: Platform Integration (Week 3-4)
- [ ] Electron desktop integration
- [ ] iOS WKWebView integration
- [ ] Android WebView migration (optional)

### Phase 3: Advanced Features (Week 5-6)
- [ ] Real-time tracker statistics
- [ ] User-configurable block lists
- [ ] Sync blocked trackers across devices
- [ ] Advanced privacy metrics

---

## License

MIT License - See LICENSE file

## Support

- **Issues:** https://github.com/MyRechargeHub1/cleanfinding-browser/issues
- **Documentation:** https://docs.cleanfinding.com
- **Email:** support@cleanfinding.com

---

**Note:** These modules are designed to be standalone and platform-agnostic. They contain no platform-specific code and can be used in any JavaScript environment (Node.js, Electron, WebView, WKWebView, etc.).
