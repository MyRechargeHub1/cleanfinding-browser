// CleanFinding Browser - Renderer Process

// DOM Elements
const urlInput = document.getElementById('urlInput');
const backBtn = document.getElementById('backBtn');
const forwardBtn = document.getElementById('forwardBtn');
const refreshBtn = document.getElementById('refreshBtn');
const homeBtn = document.getElementById('homeBtn');
const goBtn = document.getElementById('goBtn');
const settingsBtn = document.getElementById('settingsBtn');
const settingsPanel = document.getElementById('settingsPanel');
const closeSettings = document.getElementById('closeSettings');
const minimizeBtn = document.getElementById('minimizeBtn');
const maximizeBtn = document.getElementById('maximizeBtn');
const closeBtn = document.getElementById('closeBtn');

// Settings inputs
const homepageInput = document.getElementById('homepageInput');
const searchEngineInput = document.getElementById('searchEngineInput');
const saveHomepage = document.getElementById('saveHomepage');
const saveSearchEngine = document.getElementById('saveSearchEngine');
const blockAdsCheckbox = document.getElementById('blockAdsCheckbox');
const blockTrackersCheckbox = document.getElementById('blockTrackersCheckbox');
const forceSafeSearchCheckbox = document.getElementById('forceSafeSearchCheckbox');
const blockAdultContentCheckbox = document.getElementById('blockAdultContentCheckbox');
const clearDataOnExitCheckbox = document.getElementById('clearDataOnExitCheckbox');

// Load settings on startup
async function loadSettings() {
    const settings = await window.browserAPI.getSettings();

    homepageInput.value = settings.homepage;
    searchEngineInput.value = settings.searchEngine;
    blockAdsCheckbox.checked = settings.blockAds;
    blockTrackersCheckbox.checked = settings.blockTrackers;
    forceSafeSearchCheckbox.checked = settings.forceSafeSearch;
    blockAdultContentCheckbox.checked = settings.blockAdultContent;
    clearDataOnExitCheckbox.checked = settings.clearDataOnExit;
}

loadSettings();

// Navigation
goBtn.addEventListener('click', () => {
    const url = urlInput.value.trim();
    if (url) {
        window.browserAPI.navigate(url);
    }
});

urlInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        const url = urlInput.value.trim();
        if (url) {
            window.browserAPI.navigate(url);
        }
    }
});

backBtn.addEventListener('click', () => {
    window.browserAPI.goBack();
});

forwardBtn.addEventListener('click', () => {
    window.browserAPI.goForward();
});

refreshBtn.addEventListener('click', () => {
    window.browserAPI.refresh();
});

homeBtn.addEventListener('click', () => {
    window.browserAPI.goHome();
});

// Settings panel
settingsBtn.addEventListener('click', () => {
    settingsPanel.classList.toggle('open');
});

closeSettings.addEventListener('click', () => {
    settingsPanel.classList.remove('open');
});

// Save settings
saveHomepage.addEventListener('click', async () => {
    await window.browserAPI.setSetting('homepage', homepageInput.value);
    showNotification('Homepage saved!');
});

saveSearchEngine.addEventListener('click', async () => {
    await window.browserAPI.setSetting('searchEngine', searchEngineInput.value);
    showNotification('Search engine saved!');
});

// Checkbox settings (save immediately)
blockAdsCheckbox.addEventListener('change', async (e) => {
    await window.browserAPI.setSetting('blockAds', e.target.checked);
    showNotification(e.target.checked ? 'Ad blocking enabled' : 'Ad blocking disabled');
});

blockTrackersCheckbox.addEventListener('change', async (e) => {
    await window.browserAPI.setSetting('blockTrackers', e.target.checked);
    showNotification(e.target.checked ? 'Tracker blocking enabled' : 'Tracker blocking disabled');
});

forceSafeSearchCheckbox.addEventListener('change', async (e) => {
    await window.browserAPI.setSetting('forceSafeSearch', e.target.checked);
    showNotification(e.target.checked ? 'SafeSearch enabled' : 'SafeSearch disabled');
});

blockAdultContentCheckbox.addEventListener('change', async (e) => {
    await window.browserAPI.setSetting('blockAdultContent', e.target.checked);
    showNotification(e.target.checked ? 'Adult content blocking enabled' : 'Adult content blocking disabled');
});

clearDataOnExitCheckbox.addEventListener('change', async (e) => {
    await window.browserAPI.setSetting('clearDataOnExit', e.target.checked);
    showNotification(e.target.checked ? 'Data will clear on exit' : 'Data will be kept');
});

// Window controls
minimizeBtn.addEventListener('click', () => {
    require('electron').remote.getCurrentWindow().minimize();
});

maximizeBtn.addEventListener('click', () => {
    const win = require('electron').remote.getCurrentWindow();
    if (win.isMaximized()) {
        win.unmaximize();
    } else {
        win.maximize();
    }
});

closeBtn.addEventListener('click', () => {
    require('electron').remote.getCurrentWindow().close();
});

// Listen for URL changes from main process
window.browserAPI.onUrlChanged((url) => {
    urlInput.value = url;
});

window.browserAPI.onTitleChanged((title) => {
    document.title = title;
});

// Loading state
window.browserAPI.onLoadingStart(() => {
    document.querySelector('.url-bar').classList.add('loading');
});

window.browserAPI.onLoadingStop(() => {
    document.querySelector('.url-bar').classList.remove('loading');
});

// Keyboard shortcuts
document.addEventListener('keydown', (e) => {
    // Ctrl/Cmd + L - Focus URL bar
    if ((e.ctrlKey || e.metaKey) && e.key === 'l') {
        e.preventDefault();
        urlInput.focus();
        urlInput.select();
    }

    // F5 - Refresh
    if (e.key === 'F5') {
        e.preventDefault();
        window.browserAPI.refresh();
    }

    // Alt + Left - Back
    if (e.altKey && e.key === 'ArrowLeft') {
        e.preventDefault();
        window.browserAPI.goBack();
    }

    // Alt + Right - Forward
    if (e.altKey && e.key === 'ArrowRight') {
        e.preventDefault();
        window.browserAPI.goForward();
    }
});

// Notification helper
function showNotification(message) {
    // Create notification element
    const notification = document.createElement('div');
    notification.textContent = message;
    notification.style.cssText = `
        position: fixed;
        top: 80px;
        right: 20px;
        background: var(--accent);
        color: white;
        padding: 12px 20px;
        border-radius: 8px;
        font-size: 14px;
        font-weight: 600;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
        z-index: 10000;
        animation: slideIn 0.3s ease;
    `;

    document.body.appendChild(notification);

    // Remove after 3 seconds
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// Add animation styles
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(400px);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }

    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(400px);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);
