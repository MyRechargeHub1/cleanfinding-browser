/**
 * CleanFinding Browser - Electron Main Process
 * Privacy-focused, family-safe browser for Windows, macOS, and Linux
 *
 * @version 1.4.0
 * @author CleanFinding Browser Team
 */

const { app, BrowserWindow, ipcMain, session, Menu } = require('electron');
const path = require('path');
const Store = require('electron-store');
const DuckPlayerHandler = require('../shared/privacy-handlers/DuckPlayerHandler');

// Initialize electron-store for settings persistence
const store = new Store({
    defaults: {
        privacy: {
            trackerBlocking: true,
            duckPlayer: true,
            emailProtection: true,
            cookieAutoDecline: true,
            globalPrivacyControl: true
        },
        appearance: {
            theme: 'light'
        },
        security: {
            biometricLock: false
        }
    }
});

// Initialize privacy handlers
const duckPlayerHandler = new DuckPlayerHandler();

// Keep a global reference of the window object
let mainWindow = null;
let settingsWindow = null;

// Tracker domains list (subset for initial implementation)
const TRACKER_DOMAINS = [
    'doubleclick.net',
    'google-analytics.com',
    'googletagmanager.com',
    'facebook.com/tr/',
    'connect.facebook.net',
    'analytics.twitter.com',
    'pixel.twitter.com',
    'scorecardresearch.com',
    'quantserve.com',
    'hotjar.com',
    'crazyegg.com',
    'mouseflow.com',
    'fullstory.com',
    'loggly.com',
    'newrelic.com',
    'bugsnag.com',
    'sentry.io',
    'mixpanel.com',
    'segment.com',
    'kissmetrics.com',
    'optimizely.com',
    'googleadservices.com',
    'googlesyndication.com',
    'adservice.google.com'
];

/**
 * Create the main browser window
 */
function createWindow() {
    mainWindow = new BrowserWindow({
        width: 1200,
        height: 800,
        minWidth: 800,
        minHeight: 600,
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            contextIsolation: true,
            nodeIntegration: false,
            sandbox: true,
            webSecurity: true,
            allowRunningInsecureContent: false,
            enableRemoteModule: false
        },
        icon: path.join(__dirname, 'build/icons/icon.png'),
        title: 'CleanFinding Browser',
        backgroundColor: '#ffffff'
    });

    // Load the main UI
    mainWindow.loadFile(path.join(__dirname, 'src/renderer/index.html'));

    // Open DevTools in development mode
    if (process.argv.includes('--dev')) {
        mainWindow.webContents.openDevTools();
    }

    // Handle window closed
    mainWindow.on('closed', () => {
        mainWindow = null;
    });

    // Setup application menu
    setupMenu();

    // Setup privacy features
    setupPrivacyFeatures();
}

/**
 * Setup application menu
 */
function setupMenu() {
    const template = [
        {
            label: 'File',
            submenu: [
                {
                    label: 'New Tab',
                    accelerator: 'CmdOrCtrl+T',
                    click: () => {
                        mainWindow.webContents.send('new-tab');
                    }
                },
                {
                    label: 'New Incognito Tab',
                    accelerator: 'CmdOrCtrl+Shift+N',
                    click: () => {
                        mainWindow.webContents.send('new-incognito-tab');
                    }
                },
                { type: 'separator' },
                {
                    label: 'Close Tab',
                    accelerator: 'CmdOrCtrl+W',
                    click: () => {
                        mainWindow.webContents.send('close-tab');
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
                { role: 'paste' },
                { role: 'selectAll' }
            ]
        },
        {
            label: 'View',
            submenu: [
                {
                    label: 'Reload',
                    accelerator: 'CmdOrCtrl+R',
                    click: () => {
                        mainWindow.webContents.send('reload-page');
                    }
                },
                {
                    label: 'Force Reload',
                    accelerator: 'CmdOrCtrl+Shift+R',
                    click: () => {
                        mainWindow.webContents.send('force-reload-page');
                    }
                },
                { type: 'separator' },
                { role: 'resetZoom' },
                { role: 'zoomIn' },
                { role: 'zoomOut' },
                { type: 'separator' },
                { role: 'togglefullscreen' }
            ]
        },
        {
            label: 'Privacy',
            submenu: [
                {
                    label: 'Clear Browsing Data',
                    click: () => {
                        mainWindow.webContents.send('clear-browsing-data');
                    }
                },
                {
                    label: 'Privacy Dashboard',
                    click: () => {
                        mainWindow.webContents.send('show-privacy-dashboard');
                    }
                },
                { type: 'separator' },
                {
                    label: 'Settings',
                    accelerator: 'CmdOrCtrl+,',
                    click: () => {
                        openSettings();
                    }
                }
            ]
        },
        {
            label: 'Help',
            submenu: [
                {
                    label: 'About CleanFinding Browser',
                    click: () => {
                        mainWindow.webContents.send('show-about');
                    }
                },
                {
                    label: 'Visit Website',
                    click: () => {
                        require('electron').shell.openExternal('https://cleanfinding.com');
                    }
                }
            ]
        }
    ];

    const menu = Menu.buildFromTemplate(template);
    Menu.setApplicationMenu(menu);
}

/**
 * Setup privacy features (tracker blocking, Duck Player, etc.)
 */
function setupPrivacyFeatures() {
    const ses = session.defaultSession;

    // Enable Do Not Track
    ses.setUserAgent(ses.getUserAgent() + ' DNT/1');

    // Setup request filtering for tracker blocking
    ses.webRequest.onBeforeRequest({ urls: ['*://*/*'] }, (details, callback) => {
        const url = details.url.toLowerCase();

        // Check if tracker blocking is enabled
        if (!store.get('privacy.trackerBlocking')) {
            callback({});
            return;
        }

        // Block known tracker domains
        const isTracker = TRACKER_DOMAINS.some(domain => url.includes(domain));

        if (isTracker) {
            console.log('CleanFinding: Blocked tracker:', details.url);
            callback({ cancel: true });
        } else {
            callback({});
        }
    });

    // Setup Duck Player (YouTube privacy protection)
    ses.webRequest.onBeforeRequest(
        { urls: ['*://youtube.com/watch*', '*://www.youtube.com/watch*', '*://m.youtube.com/watch*', '*://youtu.be/*'] },
        (details, callback) => {
            // Check if Duck Player is enabled
            if (!store.get('privacy.duckPlayer')) {
                callback({});
                return;
            }

            // Only redirect main frame requests (not iframes, images, etc.)
            if (details.resourceType === 'mainFrame' && duckPlayerHandler.isYouTubeUrl(details.url)) {
                const privacyUrl = duckPlayerHandler.convertToPrivacyUrl(details.url);

                if (privacyUrl) {
                    console.log('CleanFinding: Duck Player redirect:', details.url, '->', privacyUrl);

                    // Load Duck Player page instead
                    const videoId = duckPlayerHandler.extractVideoId(details.url);
                    if (videoId) {
                        // Signal to renderer to show Duck Player page
                        mainWindow.webContents.send('load-duck-player', videoId, details.url);
                        callback({ cancel: true });
                        return;
                    }
                }
            }

            callback({});
        }
    );

    // Inject Duck Player enhancements into YouTube pages
    ses.webRequest.onCompleted(
        { urls: ['*://youtube.com/*', '*://www.youtube.com/*', '*://youtube-nocookie.com/*'] },
        (details) => {
            if (store.get('privacy.duckPlayer')) {
                mainWindow.webContents.executeJavaScript(duckPlayerHandler.getInjectionScript())
                    .catch(err => console.error('Failed to inject Duck Player script:', err));
            }
        }
    );

    // Set Global Privacy Control (GPC) header
    if (store.get('privacy.globalPrivacyControl')) {
        ses.webRequest.onBeforeSendHeaders({ urls: ['*://*/*'] }, (details, callback) => {
            details.requestHeaders['Sec-GPC'] = '1';
            callback({ requestHeaders: details.requestHeaders });
        });
    }

    // Block third-party cookies by default
    ses.cookies.set({
        url: 'https://example.com',
        name: 'cleanfinding_cookie_policy',
        value: 'strict',
        sameSite: 'strict'
    });
}

/**
 * Open settings window
 */
function openSettings() {
    if (settingsWindow) {
        settingsWindow.focus();
        return;
    }

    settingsWindow = new BrowserWindow({
        width: 800,
        height: 600,
        minWidth: 600,
        minHeight: 400,
        parent: mainWindow,
        modal: false,
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            contextIsolation: true,
            nodeIntegration: false,
            sandbox: true
        },
        title: 'Settings - CleanFinding Browser'
    });

    settingsWindow.loadFile(path.join(__dirname, 'src/renderer/settings.html'));

    settingsWindow.on('closed', () => {
        settingsWindow = null;
    });
}

// ==================== IPC Handlers ====================

/**
 * Get settings
 */
ipcMain.handle('get-settings', () => {
    return store.store;
});

/**
 * Update setting
 */
ipcMain.handle('update-setting', (event, key, value) => {
    store.set(key, value);

    // Reinitialize privacy features if privacy settings changed
    if (key.startsWith('privacy.')) {
        setupPrivacyFeatures();
    }

    return { success: true };
});

/**
 * Clear browsing data
 */
ipcMain.handle('clear-browsing-data', async (event, options) => {
    const ses = session.defaultSession;

    try {
        if (options.cache) {
            await ses.clearCache();
        }

        if (options.cookies) {
            await ses.clearStorageData({
                storages: ['cookies']
            });
        }

        if (options.history) {
            await ses.clearStorageData({
                storages: ['localstorage', 'indexdb', 'websql']
            });
        }

        return { success: true };
    } catch (error) {
        console.error('Failed to clear browsing data:', error);
        return { success: false, error: error.message };
    }
});

/**
 * Check if URL is YouTube
 */
ipcMain.handle('is-youtube-url', (event, url) => {
    return duckPlayerHandler.isYouTubeUrl(url);
});

/**
 * Convert to privacy URL
 */
ipcMain.handle('convert-to-privacy-url', (event, url) => {
    return duckPlayerHandler.convertToPrivacyUrl(url);
});

/**
 * Get Duck Player page HTML
 */
ipcMain.handle('get-duck-player-page', (event, videoId, timestamp) => {
    return duckPlayerHandler.createDuckPlayerPage(videoId, timestamp);
});

/**
 * Validate URL scheme (security)
 */
ipcMain.handle('validate-url-scheme', (event, url) => {
    const lowerUrl = url.toLowerCase().trim();
    return lowerUrl.startsWith('http://') ||
           lowerUrl.startsWith('https://') ||
           lowerUrl.startsWith('about:') ||
           !lowerUrl.includes(':');
});

// ==================== App Lifecycle ====================

/**
 * App ready - create window
 */
app.whenReady().then(() => {
    createWindow();

    app.on('activate', () => {
        // On macOS, recreate window when dock icon is clicked
        if (BrowserWindow.getAllWindows().length === 0) {
            createWindow();
        }
    });
});

/**
 * Quit when all windows are closed (except on macOS)
 */
app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
        app.quit();
    }
});

/**
 * Handle app quit
 */
app.on('before-quit', () => {
    console.log('CleanFinding Browser shutting down');
});

/**
 * Security: Prevent new window creation from renderer
 */
app.on('web-contents-created', (event, contents) => {
    contents.setWindowOpenHandler(({ url }) => {
        // Open in system browser for external links
        if (url.startsWith('http://') || url.startsWith('https://')) {
            require('electron').shell.openExternal(url);
        }
        return { action: 'deny' };
    });
});
