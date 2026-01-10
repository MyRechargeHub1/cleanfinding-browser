const { app, BrowserWindow, BrowserView, ipcMain, session, Menu, dialog } = require('electron');
const path = require('path');
const Store = require('electron-store');

// CRITICAL FIX: Enable hardware acceleration for video playback
// Don't disable it - videos need GPU acceleration
app.commandLine.appendSwitch('enable-features', 'VaapiVideoDecoder');
app.commandLine.appendSwitch('enable-zero-copy');
app.commandLine.appendSwitch('enable-gpu-rasterization');
app.commandLine.appendSwitch('enable-native-gpu-memory-buffers');
app.commandLine.appendSwitch('ignore-gpu-blocklist');

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
  'facebook.com/tr',
  'connect.facebook.net',
  'pixel.facebook.com',
  'analytics.twitter.com',
  'static.ads-twitter.com',
  'bat.bing.com',
  'ads.linkedin.com',
  'px.ads.linkedin.com',
  'scorecardresearch.com',
  'sb.scorecardresearch.com',
  'quantserve.com',
  'tiqcdn.com',
  'hotjar.com',
  'mouseflow.com',
  'crazyegg.com',
  'luckyorange.com',
  'inspectlet.com',
  'segment.com',
  'mixpanel.com',
  'amplitude.com',
  'heap.io',
  'fullstory.com'
];

// Blocked ad domains
const AD_DOMAINS = [
  'googlesyndication.com',
  'adservice.google.com',
  'pagead2.googlesyndication.com',
  'googleadservices.com',
  'doubleclick.net',
  'amazon-adsystem.com',
  'advertising.com',
  'media.net',
  'taboola.com',
  'outbrain.com',
  'criteo.com',
  'adnxs.com'
];

// Adult content domains (basic list)
const ADULT_DOMAINS = [
  'pornhub.com',
  'xvideos.com',
  'xnxx.com',
  'redtube.com',
  'youporn.com',
  'xhamster.com',
  'spankbang.com',
  'tube8.com',
  'porn.com',
  'sex.com'
];

let mainWindow;
let currentView = null;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 800,
    minHeight: 600,
    frame: false,
    titleBarStyle: 'hidden',
    backgroundColor: '#1a1a2e',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      enableRemoteModule: false,
      webSecurity: true,
      // CRITICAL FIX: Enable features for video playback
      webgl: true,
      plugins: true,
      // Allow autoplay for media
      autoplayPolicy: 'no-user-gesture-required'
    },
    icon: path.join(__dirname, 'assets', 'icons', 'icon.png')
  });

  // Load browser UI
  mainWindow.loadFile(path.join(__dirname, 'src', 'index.html'));

  // Create initial browser view
  createBrowserView();

  // Create application menu
  createMenu();

  // Set up content blocking
  setupContentBlocking();

  // Handle window resize
  mainWindow.on('resize', () => {
    if (currentView) {
      updateBrowserViewBounds();
    }
  });

  // Clear data on exit if enabled
  mainWindow.on('close', async (e) => {
    if (store.get('clearDataOnExit')) {
      e.preventDefault();
      await session.defaultSession.clearStorageData();
      mainWindow.destroy();
    }
  });
}

function createBrowserView() {
  if (currentView) {
    mainWindow.removeBrowserView(currentView);
    currentView.webContents.destroy();
  }

  currentView = new BrowserView({
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      webSecurity: true,
      // CRITICAL FIX: Enable all features for video/media
      experimentalFeatures: true,
      webgl: true,
      plugins: true,
      javascript: true,
      images: true,
      // Enable hardware acceleration for video
      enableBlinkFeatures: 'AutoplayIgnoresWebAudio,MediaCapabilities',
      // Allow autoplay
      autoplayPolicy: 'no-user-gesture-required',
      // Disable features that might break video
      disableBlinkFeatures: '',
      // Enable web codecs
      enableWebSQL: false,
      // Allow video/audio
      backgroundThrottling: false
    }
  });

  mainWindow.addBrowserView(currentView);
  updateBrowserViewBounds();

  // Load homepage
  const homepage = store.get('homepage');
  currentView.webContents.loadURL(homepage);

  // Setup navigation handlers
  setupViewHandlers(currentView);
}

function updateBrowserViewBounds() {
  if (!currentView) return;

  const windowBounds = mainWindow.getBounds();
  const TAB_HEIGHT = 36;
  const NAV_HEIGHT = 44;

  // CRITICAL FIX: Proper sizing for all content including videos
  currentView.setBounds({
    x: 0,
    y: TAB_HEIGHT + NAV_HEIGHT,
    width: windowBounds.width,
    height: windowBounds.height - TAB_HEIGHT - NAV_HEIGHT
  });

  // Force resize to ensure video elements scale properly
  currentView.setAutoResize({
    width: true,
    height: true,
    horizontal: false,
    vertical: false
  });
}

function setupViewHandlers(view) {
  const webContents = view.webContents;

  // Send URL updates to renderer
  webContents.on('did-navigate', (event, url) => {
    mainWindow.webContents.send('url-changed', url);
  });

  webContents.on('did-navigate-in-page', (event, url) => {
    mainWindow.webContents.send('url-changed', url);
  });

  // Handle page title changes
  webContents.on('page-title-updated', (event, title) => {
    mainWindow.webContents.send('title-changed', title);
  });

  // Handle loading state
  webContents.on('did-start-loading', () => {
    mainWindow.webContents.send('loading-start');
  });

  webContents.on('did-stop-loading', () => {
    mainWindow.webContents.send('loading-stop');
  });

  // Handle new windows
  webContents.setWindowOpenHandler(({ url }) => {
    // Open in current view instead of new window
    webContents.loadURL(url);
    return { action: 'deny' };
  });

  // CRITICAL FIX: Inject CSS to fix video rendering issues
  webContents.on('did-finish-load', () => {
    webContents.insertCSS(`
      /* Fix video container sizing */
      video, iframe {
        max-width: 100% !important;
        height: auto !important;
      }

      /* Fix YouTube player */
      #player, .html5-video-player {
        width: 100% !important;
        height: 100% !important;
      }

      /* Fix black bars on videos */
      .video-stream {
        width: 100% !important;
        height: 100% !important;
        object-fit: contain !important;
      }
    `);
  });
}

function setupContentBlocking() {
  const filter = {
    urls: ['*://*/*']
  };

  session.defaultSession.webRequest.onBeforeRequest(filter, (details, callback) => {
    const url = details.url.toLowerCase();
    const domain = new URL(details.url).hostname;

    // Don't block media files
    const mediaExtensions = ['.mp4', '.webm', '.ogg', '.mp3', '.wav', '.m3u8', '.ts'];
    const isMedia = mediaExtensions.some(ext => url.includes(ext));
    if (isMedia) {
      callback({ cancel: false });
      return;
    }

    // Check if domain should be blocked
    const shouldBlock =
      (store.get('blockTrackers') && TRACKER_DOMAINS.some(d => domain.includes(d))) ||
      (store.get('blockAds') && AD_DOMAINS.some(d => domain.includes(d))) ||
      (store.get('blockAdultContent') && ADULT_DOMAINS.some(d => domain.includes(d)));

    if (shouldBlock) {
      console.log('Blocked:', url);
      callback({ cancel: true });
    } else {
      callback({ cancel: false });
    }
  });

  // Force SafeSearch on search engines
  if (store.get('forceSafeSearch')) {
    session.defaultSession.webRequest.onBeforeRequest(
      { urls: ['*://*.google.com/search*', '*://*.bing.com/search*', '*://www.youtube.com/*'] },
      (details, callback) => {
        let url = details.url;

        // Force SafeSearch on Google
        if (url.includes('google.com/search')) {
          url = url.includes('?')
            ? url + '&safe=active'
            : url + '?safe=active';
        }

        // Force SafeSearch on Bing
        if (url.includes('bing.com/search')) {
          url = url.includes('?')
            ? url + '&safesearch=strict'
            : url + '?safesearch=strict';
        }

        // Force Restricted Mode on YouTube
        if (url.includes('youtube.com')) {
          if (!url.includes('restrict_mode')) {
            // Add YouTube restricted mode cookie
            session.defaultSession.cookies.set({
              url: 'https://www.youtube.com',
              name: 'PREF',
              value: 'f2=8000000',
              domain: '.youtube.com',
              path: '/',
              secure: true,
              httpOnly: false
            });
          }
        }

        if (url !== details.url) {
          callback({ redirectURL: url });
        } else {
          callback({ cancel: false });
        }
      }
    );
  }
}

function createMenu() {
  const template = [
    {
      label: 'File',
      submenu: [
        {
          label: 'New Tab',
          accelerator: 'CmdOrCtrl+T',
          click: () => {
            createBrowserView();
            mainWindow.webContents.send('new-tab');
          }
        },
        { type: 'separator' },
        {
          label: 'Exit',
          accelerator: 'CmdOrCtrl+Q',
          click: () => {
            app.quit();
          }
        }
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
        { role: 'paste' }
      ]
    },
    {
      label: 'View',
      submenu: [
        {
          label: 'Reload',
          accelerator: 'F5',
          click: () => {
            if (currentView) {
              currentView.webContents.reload();
            }
          }
        },
        { role: 'toggleDevTools' },
        { type: 'separator' },
        { role: 'resetZoom' },
        { role: 'zoomIn' },
        { role: 'zoomOut' },
        { type: 'separator' },
        { role: 'togglefullscreen' }
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
              message: 'CleanFinding Browser v1.0.1',
              detail: 'A privacy-focused, family-safe web browser.\n\nFeatures:\n• SafeSearch always enabled\n• Tracker & ad blocking\n• Adult content filtering\n• Privacy by design\n\n© 2025 CleanFinding'
            });
          }
        }
      ]
    }
  ];

  const menu = Menu.buildFromTemplate(template);
  Menu.setApplicationMenu(menu);
}

// IPC Handlers
ipcMain.handle('navigate', (event, url) => {
  if (currentView) {
    // Ensure URL has protocol
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      // Check if it's a search query or URL
      if (url.includes('.') && !url.includes(' ')) {
        url = 'https://' + url;
      } else {
        // Treat as search query
        url = store.get('searchEngine') + encodeURIComponent(url);
      }
    }
    currentView.webContents.loadURL(url);
  }
});

ipcMain.handle('go-back', () => {
  if (currentView && currentView.webContents.canGoBack()) {
    currentView.webContents.goBack();
  }
});

ipcMain.handle('go-forward', () => {
  if (currentView && currentView.webContents.canGoForward()) {
    currentView.webContents.goForward();
  }
});

ipcMain.handle('go-home', () => {
  if (currentView) {
    const homepage = store.get('homepage');
    currentView.webContents.loadURL(homepage);
  }
});

ipcMain.handle('refresh', () => {
  if (currentView) {
    currentView.webContents.reload();
  }
});

ipcMain.handle('get-settings', () => {
  return {
    homepage: store.get('homepage'),
    searchEngine: store.get('searchEngine'),
    blockAds: store.get('blockAds'),
    blockTrackers: store.get('blockTrackers'),
    forceSafeSearch: store.get('forceSafeSearch'),
    blockAdultContent: store.get('blockAdultContent'),
    clearDataOnExit: store.get('clearDataOnExit'),
    darkMode: store.get('darkMode')
  };
});

ipcMain.handle('set-setting', (event, key, value) => {
  store.set(key, value);
  return true;
});

ipcMain.handle('get-homepage', () => {
  return store.get('homepage');
});

ipcMain.handle('get-search-engine', () => {
  return store.get('searchEngine');
});

// App lifecycle
app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});
