/**
 * CleanFinding Browser - Electron Preload Script
 * Provides secure bridge between main and renderer processes
 *
 * @version 1.4.0
 * @author CleanFinding Browser Team
 */

const { contextBridge, ipcRenderer } = require('electron');

/**
 * Expose safe API to renderer process
 * Uses contextBridge for security (prevents direct access to Node.js)
 */
contextBridge.exposeInMainWorld('cleanfindingAPI', {
    // Settings API
    getSettings: () => ipcRenderer.invoke('get-settings'),
    updateSetting: (key, value) => ipcRenderer.invoke('update-setting', key, value),

    // Privacy API
    clearBrowsingData: (options) => ipcRenderer.invoke('clear-browsing-data', options),
    isYouTubeUrl: (url) => ipcRenderer.invoke('is-youtube-url', url),
    convertToPrivacyUrl: (url) => ipcRenderer.invoke('convert-to-privacy-url', url),
    getDuckPlayerPage: (videoId, timestamp) => ipcRenderer.invoke('get-duck-player-page', videoId, timestamp),
    validateUrlScheme: (url) => ipcRenderer.invoke('validate-url-scheme', url),

    // Renderer event listeners (one-way from main -> renderer)
    onNewTab: (callback) => {
        ipcRenderer.on('new-tab', () => callback());
    },
    onNewIncognitoTab: (callback) => {
        ipcRenderer.on('new-incognito-tab', () => callback());
    },
    onCloseTab: (callback) => {
        ipcRenderer.on('close-tab', () => callback());
    },
    onReloadPage: (callback) => {
        ipcRenderer.on('reload-page', () => callback());
    },
    onForceReloadPage: (callback) => {
        ipcRenderer.on('force-reload-page', () => callback());
    },
    onClearBrowsingData: (callback) => {
        ipcRenderer.on('clear-browsing-data', () => callback());
    },
    onShowPrivacyDashboard: (callback) => {
        ipcRenderer.on('show-privacy-dashboard', () => callback());
    },
    onShowAbout: (callback) => {
        ipcRenderer.on('show-about', () => callback());
    },
    onLoadDuckPlayer: (callback) => {
        ipcRenderer.on('load-duck-player', (event, videoId, originalUrl) => {
            callback(videoId, originalUrl);
        });
    },

    // Remove listener (cleanup)
    removeAllListeners: (channel) => {
        ipcRenderer.removeAllListeners(channel);
    }
});

// Log preload initialization
console.log('CleanFinding Browser: Preload script initialized');
