const { contextBridge, ipcRenderer } = require('electron');

// Expose protected methods to renderer process
contextBridge.exposeInMainWorld('browserAPI', {
  // Settings
  getSettings: () => ipcRenderer.invoke('get-settings'),
  setSetting: (key, value) => ipcRenderer.invoke('set-setting', key, value),
  getHomepage: () => ipcRenderer.invoke('get-homepage'),
  getSearchEngine: () => ipcRenderer.invoke('get-search-engine'),

  // Navigation events from main process
  onNewTab: (callback) => ipcRenderer.on('new-tab', callback),
  onCloseTab: (callback) => ipcRenderer.on('close-tab', callback),
  onOpenSettings: (callback) => ipcRenderer.on('open-settings', callback),
  onGoBack: (callback) => ipcRenderer.on('go-back', callback),
  onGoForward: (callback) => ipcRenderer.on('go-forward', callback),
  onGoHome: (callback) => ipcRenderer.on('go-home', callback),
  onNavigate: (callback) => ipcRenderer.on('navigate', (event, url) => callback(url)),
  onOpenUrlInNewTab: (callback) => ipcRenderer.on('open-url-in-new-tab', (event, url) => callback(url)),

  // Remove listeners
  removeAllListeners: (channel) => ipcRenderer.removeAllListeners(channel)
});

// Log when preload script is loaded
console.log('CleanFinding Browser preload script loaded');
