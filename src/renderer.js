// CleanFinding Browser - Renderer Process

// State
let tabs = [];
let activeTabId = null;
let tabCounter = 0;

// DOM Elements
const tabsContainer = document.getElementById('tabsContainer');
const webviewContainer = document.getElementById('webviewContainer');
const urlInput = document.getElementById('urlInput');
const backBtn = document.getElementById('backBtn');
const forwardBtn = document.getElementById('forwardBtn');
const refreshBtn = document.getElementById('refreshBtn');
const homeBtn = document.getElementById('homeBtn');
const newTabBtn = document.getElementById('newTabBtn');
const settingsBtn = document.getElementById('settingsBtn');
const settingsPanel = document.getElementById('settingsPanel');
const closeSettings = document.getElementById('closeSettings');
const goBtn = document.getElementById('goBtn');
const securityIndicator = document.getElementById('securityIndicator');

// Settings elements
const settingBlockTrackers = document.getElementById('settingBlockTrackers');
const settingBlockAds = document.getElementById('settingBlockAds');
const settingBlockAdult = document.getElementById('settingBlockAdult');
const settingClearOnExit = document.getElementById('settingClearOnExit');
const settingDarkMode = document.getElementById('settingDarkMode');

// Homepage
let homepage = 'https://cleanfinding.com';
let searchEngine = 'https://cleanfinding.com/search?q=';

// Initialize
async function init() {
    // Load settings
    homepage = await window.browserAPI.getHomepage();
    searchEngine = await window.browserAPI.getSearchEngine();

    const settings = await window.browserAPI.getSettings();
    settingBlockTrackers.checked = settings.blockTrackers;
    settingBlockAds.checked = settings.blockAds;
    settingBlockAdult.checked = settings.blockAdultContent;
    settingClearOnExit.checked = settings.clearDataOnExit;
    settingDarkMode.checked = settings.darkMode;

    if (settings.darkMode) {
        document.body.classList.remove('light-mode');
    }

    // Create first tab
    createTab(homepage);

    // Setup event listeners
    setupEventListeners();
    setupIPCListeners();
}

// Tab Management
function createTab(url = homepage) {
    const tabId = ++tabCounter;

    // Create tab element
    const tab = document.createElement('div');
    tab.className = 'tab';
    tab.dataset.tabId = tabId;
    tab.innerHTML = `
        <img class="tab-favicon" src="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16'><circle cx='8' cy='8' r='6' fill='%23667eea'/></svg>" alt="">
        <span class="tab-title">New Tab</span>
        <button class="tab-close" title="Close tab">&times;</button>
    `;

    // Tab click handlers
    tab.addEventListener('click', (e) => {
        if (!e.target.classList.contains('tab-close')) {
            switchTab(tabId);
        }
    });

    tab.querySelector('.tab-close').addEventListener('click', (e) => {
        e.stopPropagation();
        closeTab(tabId);
    });

    tabsContainer.appendChild(tab);

    // Create webview
    const webview = document.createElement('webview');
    webview.id = `webview-${tabId}`;
    webview.src = url;
    webview.setAttribute('allowpopups', 'false');
    webview.setAttribute('webpreferences', 'contextIsolation=yes, nodeIntegration=no');

    // Webview event handlers
    webview.addEventListener('did-start-loading', () => {
        updateLoadingState(tabId, true);
    });

    webview.addEventListener('did-stop-loading', () => {
        updateLoadingState(tabId, false);
    });

    webview.addEventListener('did-navigate', (e) => {
        updateTabInfo(tabId, e.url);
    });

    webview.addEventListener('did-navigate-in-page', (e) => {
        if (e.isMainFrame) {
            updateTabInfo(tabId, e.url);
        }
    });

    webview.addEventListener('page-title-updated', (e) => {
        updateTabTitle(tabId, e.title);
    });

    webview.addEventListener('page-favicon-updated', (e) => {
        if (e.favicons && e.favicons.length > 0) {
            updateTabFavicon(tabId, e.favicons[0]);
        }
    });

    webview.addEventListener('did-fail-load', (e) => {
        if (e.errorCode !== -3) { // -3 is aborted, ignore
            console.error('Failed to load:', e.errorDescription);
        }
    });

    webviewContainer.appendChild(webview);

    // Add to tabs array
    tabs.push({
        id: tabId,
        url: url,
        title: 'New Tab',
        favicon: null
    });

    // Switch to new tab
    switchTab(tabId);

    return tabId;
}

function switchTab(tabId) {
    // Update active tab state
    activeTabId = tabId;

    // Update tab UI
    document.querySelectorAll('.tab').forEach(tab => {
        tab.classList.toggle('active', parseInt(tab.dataset.tabId) === tabId);
    });

    // Update webview visibility
    document.querySelectorAll('webview').forEach(wv => {
        wv.classList.toggle('active', wv.id === `webview-${tabId}`);
    });

    // Update URL bar
    const tab = tabs.find(t => t.id === tabId);
    if (tab) {
        urlInput.value = tab.url;
        updateSecurityIndicator(tab.url);
    }

    // Update navigation buttons
    updateNavButtons();
}

function closeTab(tabId) {
    const tabIndex = tabs.findIndex(t => t.id === tabId);
    if (tabIndex === -1) return;

    // Don't close if it's the last tab
    if (tabs.length === 1) {
        // Navigate to homepage instead
        navigateToUrl(homepage);
        return;
    }

    // Remove tab element
    const tabElement = document.querySelector(`.tab[data-tab-id="${tabId}"]`);
    if (tabElement) {
        tabElement.remove();
    }

    // Remove webview
    const webview = document.getElementById(`webview-${tabId}`);
    if (webview) {
        webview.remove();
    }

    // Remove from tabs array
    tabs.splice(tabIndex, 1);

    // Switch to another tab if this was active
    if (activeTabId === tabId) {
        const newActiveTab = tabs[Math.min(tabIndex, tabs.length - 1)];
        if (newActiveTab) {
            switchTab(newActiveTab.id);
        }
    }
}

function updateTabInfo(tabId, url) {
    const tab = tabs.find(t => t.id === tabId);
    if (tab) {
        tab.url = url;
    }

    if (tabId === activeTabId) {
        urlInput.value = url;
        updateSecurityIndicator(url);
        updateNavButtons();
    }
}

function updateTabTitle(tabId, title) {
    const tab = tabs.find(t => t.id === tabId);
    if (tab) {
        tab.title = title;
    }

    const tabElement = document.querySelector(`.tab[data-tab-id="${tabId}"] .tab-title`);
    if (tabElement) {
        tabElement.textContent = title || 'New Tab';
        tabElement.title = title || 'New Tab';
    }
}

function updateTabFavicon(tabId, faviconUrl) {
    const tab = tabs.find(t => t.id === tabId);
    if (tab) {
        tab.favicon = faviconUrl;
    }

    const faviconElement = document.querySelector(`.tab[data-tab-id="${tabId}"] .tab-favicon`);
    if (faviconElement) {
        faviconElement.src = faviconUrl;
        faviconElement.onerror = () => {
            faviconElement.src = "data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16'><circle cx='8' cy='8' r='6' fill='%23667eea'/></svg>";
        };
    }
}

function updateLoadingState(tabId, isLoading) {
    // Could add loading indicator here
}

// Navigation
function navigateToUrl(url) {
    // Check if it's a search query or URL
    if (!url.includes('.') && !url.startsWith('http') && !url.startsWith('file:')) {
        // It's a search query
        url = searchEngine + encodeURIComponent(url);
    } else if (!url.startsWith('http') && !url.startsWith('file:')) {
        url = 'https://' + url;
    }

    const webview = document.getElementById(`webview-${activeTabId}`);
    if (webview) {
        webview.src = url;
    }
}

function goBack() {
    const webview = document.getElementById(`webview-${activeTabId}`);
    if (webview && webview.canGoBack()) {
        webview.goBack();
    }
}

function goForward() {
    const webview = document.getElementById(`webview-${activeTabId}`);
    if (webview && webview.canGoForward()) {
        webview.goForward();
    }
}

function refresh() {
    const webview = document.getElementById(`webview-${activeTabId}`);
    if (webview) {
        webview.reload();
    }
}

function goHome() {
    navigateToUrl(homepage);
}

function updateNavButtons() {
    const webview = document.getElementById(`webview-${activeTabId}`);
    if (webview) {
        // Need to wait for webview to be ready
        setTimeout(() => {
            backBtn.disabled = !webview.canGoBack();
            forwardBtn.disabled = !webview.canGoForward();
        }, 100);
    }
}

function updateSecurityIndicator(url) {
    try {
        const urlObj = new URL(url);
        if (urlObj.protocol === 'https:') {
            securityIndicator.classList.remove('insecure');
            securityIndicator.title = 'Secure Connection';
        } else {
            securityIndicator.classList.add('insecure');
            securityIndicator.title = 'Not Secure';
        }
    } catch (e) {
        securityIndicator.classList.add('insecure');
    }
}

// Settings
function openSettings() {
    settingsPanel.classList.add('open');
}

function closeSettingsPanel() {
    settingsPanel.classList.remove('open');
}

async function saveSetting(key, value) {
    await window.browserAPI.setSetting(key, value);
}

// Event Listeners
function setupEventListeners() {
    // Navigation
    backBtn.addEventListener('click', goBack);
    forwardBtn.addEventListener('click', goForward);
    refreshBtn.addEventListener('click', refresh);
    homeBtn.addEventListener('click', goHome);
    newTabBtn.addEventListener('click', () => createTab());

    // URL bar
    urlInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            navigateToUrl(urlInput.value);
        }
    });

    urlInput.addEventListener('focus', () => {
        urlInput.select();
    });

    goBtn.addEventListener('click', () => {
        navigateToUrl(urlInput.value);
    });

    // Settings
    settingsBtn.addEventListener('click', openSettings);
    closeSettings.addEventListener('click', closeSettingsPanel);

    settingBlockTrackers.addEventListener('change', (e) => {
        saveSetting('blockTrackers', e.target.checked);
    });

    settingBlockAds.addEventListener('change', (e) => {
        saveSetting('blockAds', e.target.checked);
    });

    settingBlockAdult.addEventListener('change', (e) => {
        saveSetting('blockAdultContent', e.target.checked);
    });

    settingClearOnExit.addEventListener('change', (e) => {
        saveSetting('clearDataOnExit', e.target.checked);
    });

    settingDarkMode.addEventListener('change', (e) => {
        saveSetting('darkMode', e.target.checked);
        document.body.classList.toggle('light-mode', !e.target.checked);
    });

    // Keyboard shortcuts
    document.addEventListener('keydown', (e) => {
        // Ctrl+T - New tab
        if (e.ctrlKey && e.key === 't') {
            e.preventDefault();
            createTab();
        }

        // Ctrl+W - Close tab
        if (e.ctrlKey && e.key === 'w') {
            e.preventDefault();
            closeTab(activeTabId);
        }

        // Ctrl+L - Focus URL bar
        if (e.ctrlKey && e.key === 'l') {
            e.preventDefault();
            urlInput.focus();
            urlInput.select();
        }

        // F5 - Refresh
        if (e.key === 'F5') {
            e.preventDefault();
            refresh();
        }

        // Alt+Left - Back
        if (e.altKey && e.key === 'ArrowLeft') {
            e.preventDefault();
            goBack();
        }

        // Alt+Right - Forward
        if (e.altKey && e.key === 'ArrowRight') {
            e.preventDefault();
            goForward();
        }

        // Escape - Close settings
        if (e.key === 'Escape') {
            closeSettingsPanel();
        }
    });

    // Click outside settings to close
    document.addEventListener('click', (e) => {
        if (settingsPanel.classList.contains('open') &&
            !settingsPanel.contains(e.target) &&
            e.target !== settingsBtn) {
            closeSettingsPanel();
        }
    });
}

// IPC Listeners (from main process)
function setupIPCListeners() {
    window.browserAPI.onNewTab(() => createTab());
    window.browserAPI.onCloseTab(() => closeTab(activeTabId));
    window.browserAPI.onOpenSettings(() => openSettings());
    window.browserAPI.onGoBack(() => goBack());
    window.browserAPI.onGoForward(() => goForward());
    window.browserAPI.onGoHome(() => goHome());
    window.browserAPI.onNavigate((url) => navigateToUrl(url));
    window.browserAPI.onOpenUrlInNewTab((url) => createTab(url));
}

// Helper function for settings link
function navigateToUrlFromSettings(url) {
    closeSettingsPanel();
    navigateToUrl(url);
}

// Make navigateToUrl available globally for onclick handlers
window.navigateToUrl = navigateToUrlFromSettings;

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', init);
