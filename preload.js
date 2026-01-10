const { contextBridge, ipcRenderer } = require('electron');

// Expose protected methods to renderer process
contextBridge.exposeInMainWorld('browserAPI', {
  // Navigation
  navigate: (url) => ipcRenderer.invoke('navigate', url),
  goBack: () => ipcRenderer.invoke('go-back'),
  goForward: () => ipcRenderer.invoke('go-forward'),
  goHome: () => ipcRenderer.invoke('go-home'),
  refresh: () => ipcRenderer.invoke('refresh'),

  // Settings
  getSettings: () => ipcRenderer.invoke('get-settings'),
  setSetting: (key, value) => ipcRenderer.invoke('set-setting', key, value),
  getHomepage: () => ipcRenderer.invoke('get-homepage'),
  getSearchEngine: () => ipcRenderer.invoke('get-search-engine'),

  // Events
  onUrlChanged: (callback) => ipcRenderer.on('url-changed', (event, url) => callback(url)),
  onTitleChanged: (callback) => ipcRenderer.on('title-changed', (event, title) => callback(title)),
  onLoadingStart: (callback) => ipcRenderer.on('loading-start', callback),
  onLoadingStop: (callback) => ipcRenderer.on('loading-stop', callback),
  onNewTab: (callback) => ipcRenderer.on('new-tab', callback),
  onCloseTab: (callback) => ipcRenderer.on('close-tab', callback),
  onOpenSettings: (callback) => ipcRenderer.on('open-settings', callback)
});
