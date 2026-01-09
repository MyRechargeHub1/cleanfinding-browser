const { app, BrowserWindow, BrowserView, ipcMain, session, Menu, dialog } = require('electron');
const path = require('path');
const Store = require('electron-store');

// Initialize settings store
const store = new Store({
  defaults: {
    homepage: 'https://cleanfinding.com',
    searchEngine: 'https://cleanfinding.com/search?q=',
    blockAds: true,
    blockTrackers: true,
    forceSafeSearch: true,
    blockAdultContent: true,
    clearDataOnExit: false,
    darkMode: false
  }
});

// Blocked tracker domains
const TRACKER_DOMAINS = [
  'google-analytics.com',
  'googletagmanager.com',
  'doubleclick.net',
  'facebook.net',
  'facebook.com/tr',
  'connect.facebook.net',
  'analytics.twitter.com',
  'ads.twitter.com',
  'pixel.wp.com',
  'stats.wp.com',
  'hotjar.com',
  'fullstory.com',
  'mixpanel.com',
  'segment.io',
  'segment.com',
  'amplitude.com',
  'heapanalytics.com',
  'crazyegg.com',
  'optimizely.com',
  'mouseflow.com',
  'luckyorange.com',
  'clicktale.net',
  'quantserve.com',
  'scorecardresearch.com',
  'adroll.com',
  'criteo.com',
  'taboola.com',
  'outbrain.com'
];

// Blocked adult content domains (basic list - expand as needed)
const ADULT_DOMAINS = [
  'pornhub.com',
  'xvideos.com',
  'xnxx.com',
  'xhamster.com',
  'redtube.com'
  // Add more as needed
];

// Ad domains to block
const AD_DOMAINS = [
  'doubleclick.net',
  'googlesyndication.com',
  'googleadservices.com',
  'moatads.com',
  'amazon-adsystem.com',
  'ads.yahoo.com',
  'advertising.com',
  'adnxs.com',
  'adsrvr.org',
  'rubiconproject.com',
  'pubmatic.com',
  'openx.net',
  'casalemedia.com'
];

let mainWindow;

function createWindow() {
  // Create the browser window
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 800,
    minHeight: 600,
    title: 'CleanFinding Browser',
    icon: path.join(__dirname, 'assets', 'icons', 'icon.png'),
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js'),
      webviewTag: true
    },
    frame: true,
    backgroundColor: '#1a1a2e'
  });

  // Load the browser UI
  mainWindow.loadFile(path.join(__dirname, 'src', 'index.html'));

  // Set up content blocking
  setupContentBlocking();

  // Set up SafeSearch enforcement
  setupSafeSearch();

  // Remove default menu (optional - creates cleaner look)
  // Menu.setApplicationMenu(null);

  // Create custom menu
  createMenu();

  // Handle window close
  mainWindow.on('closed', () => {
    mainWindow = null;
  });

  // Clear data on exit if enabled
  mainWindow.on('close', async () => {
    if (store.get('clearDataOnExit')) {
      await session.defaultSession.clearStorageData();
      await session.defaultSession.clearCache();
    }
  });
}

function setupContentBlocking() {
  const filter = {
    urls: ['*://*/*']
  };

  // Block trackers, ads, and adult content
  session.defaultSession.webRequest.onBeforeRequest(filter, (details, callback) => {
    const url = details.url.toLowerCase();

    // Block trackers
    if (store.get('blockTrackers')) {
      for (const domain of TRACKER_DOMAINS) {
        if (url.includes(domain)) {
          console.log('[BLOCKED TRACKER]', domain);
          callback({ cancel: true });
          return;
        }
      }
    }

    // Block ads
    if (store.get('blockAds')) {
      for (const domain of AD_DOMAINS) {
        if (url.includes(domain)) {
          console.log('[BLOCKED AD]', domain);
          callback({ cancel: true });
          return;
        }
      }
    }

    // Block adult content
    if (store.get('blockAdultContent')) {
      for (const domain of ADULT_DOMAINS) {
        if (url.includes(domain)) {
          console.log('[BLOCKED ADULT]', domain);
          callback({ cancel: true });
          return;
        }
      }
    }

    callback({ cancel: false });
  });

  // Modify response headers for privacy
  session.defaultSession.webRequest.onHeadersReceived((details, callback) => {
    callback({
      responseHeaders: {
        ...details.responseHeaders,
        'Content-Security-Policy': ["default-src 'self' 'unsafe-inline' 'unsafe-eval' https: data: blob:"]
      }
    });
  });
}

function setupSafeSearch() {
  if (!store.get('forceSafeSearch')) return;

  const filter = {
    urls: [
      '*://*.google.com/*',
      '*://*.google.co.*/*',
      '*://*.bing.com/*',
      '*://*.youtube.com/*',
      '*://*.duckduckgo.com/*'
    ]
  };

  session.defaultSession.webRequest.onBeforeRequest(filter, (details, callback) => {
    let url = details.url;
    let modified = false;

    // Google SafeSearch
    if (url.includes('google.com') && url.includes('/search')) {
      if (!url.includes('safe=active')) {
        url = url.includes('?') ? `${url}&safe=active` : `${url}?safe=active`;
        modified = true;
      }
    }

    // Bing SafeSearch
    if (url.includes('bing.com') && url.includes('/search')) {
      if (!url.includes('adlt=strict')) {
        url = url.includes('?') ? `${url}&adlt=strict` : `${url}?adlt=strict`;
        modified = true;
      }
    }

    // YouTube Restricted Mode (via cookie)
    if (url.includes('youtube.com')) {
      // YouTube restricted mode is handled via preferences
    }

    // DuckDuckGo SafeSearch
    if (url.includes('duckduckgo.com')) {
      if (!url.includes('kp=1')) {
        url = url.includes('?') ? `${url}&kp=1` : `${url}?kp=1`;
        modified = true;
      }
    }

    if (modified) {
      callback({ redirectURL: url });
    } else {
      callback({ cancel: false });
    }
  });
}

function createMenu() {
  const template = [
    {
      label: 'File',
      submenu: [
        {
          label: 'New Tab',
          accelerator: 'CmdOrCtrl+T',
          click: () => mainWindow.webContents.send('new-tab')
        },
        {
          label: 'Close Tab',
          accelerator: 'CmdOrCtrl+W',
          click: () => mainWindow.webContents.send('close-tab')
        },
        { type: 'separator' },
        {
          label: 'Settings',
          accelerator: 'CmdOrCtrl+,',
          click: () => mainWindow.webContents.send('open-settings')
        },
        { type: 'separator' },
        { role: 'quit' }
      ]
    },
    {
      label: 'Edit',
      submenu: [
        { role: 'undo' },
        { role: 'redo' },
        { type: 'separator' },
        { role: 'cut' },
        { role: 'copy' },
        { role: 'paste' },
        { role: 'selectall' }
      ]
    },
    {
      label: 'View',
      submenu: [
        { role: 'reload' },
        { role: 'forceReload' },
        { type: 'separator' },
        { role: 'zoomIn' },
        { role: 'zoomOut' },
        { role: 'resetZoom' },
        { type: 'separator' },
        { role: 'togglefullscreen' },
        { type: 'separator' },
        { role: 'toggleDevTools' }
      ]
    },
    {
      label: 'History',
      submenu: [
        {
          label: 'Back',
          accelerator: 'Alt+Left',
          click: () => mainWindow.webContents.send('go-back')
        },
        {
          label: 'Forward',
          accelerator: 'Alt+Right',
          click: () => mainWindow.webContents.send('go-forward')
        },
        { type: 'separator' },
        {
          label: 'Home',
          accelerator: 'Alt+Home',
          click: () => mainWindow.webContents.send('go-home')
        }
      ]
    },
    {
      label: 'Help',
      submenu: [
        {
          label: 'About CleanFinding Browser',
          click: () => {
            dialog.showMessageBox(mainWindow, {
              type: 'info',
              title: 'About CleanFinding Browser',
              message: 'CleanFinding Browser v1.0.0',
              detail: 'A privacy-focused, family-safe web browser.\n\nBuilt with security in mind:\n• SafeSearch always enabled\n• Tracker blocking\n• Ad blocking\n• Adult content filtering\n\nhttps://cleanfinding.com'
            });
          }
        },
        {
          label: 'Visit CleanFinding',
          click: () => mainWindow.webContents.send('navigate', 'https://cleanfinding.com')
        }
      ]
    }
  ];

  const menu = Menu.buildFromTemplate(template);
  Menu.setApplicationMenu(menu);
}

// IPC Handlers
ipcMain.handle('get-settings', () => {
  return store.store;
});

ipcMain.handle('set-setting', (event, key, value) => {
  store.set(key, value);

  // Re-apply settings if needed
  if (key === 'forceSafeSearch') {
    setupSafeSearch();
  }

  return true;
});

ipcMain.handle('get-homepage', () => {
  return store.get('homepage');
});

ipcMain.handle('get-search-engine', () => {
  return store.get('searchEngine');
});

// App lifecycle
app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

// Security: Prevent new window creation from webviews
app.on('web-contents-created', (event, contents) => {
  contents.setWindowOpenHandler(({ url }) => {
    // Send to renderer to open in new tab
    mainWindow.webContents.send('open-url-in-new-tab', url);
    return { action: 'deny' };
  });
});
