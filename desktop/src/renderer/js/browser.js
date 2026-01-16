/**
 * CleanFinding Browser - Browser Logic
 * @version 1.4.0
 */

// Browser state
const browser = {
    tabs: [],
    activeTabId: null,
    nextTabId: 1,
    privacyStats: {
        trackersBlocked: 0,
        cookiesBlocked: 0
    }
};

// DOM Elements
const elements = {
    tabsContainer: document.getElementById('tabs-container'),
    webviewContainer: document.getElementById('webview-container'),
    addressBar: document.getElementById('address-bar'),
    backBtn: document.getElementById('back-btn'),
    forwardBtn: document.getElementById('forward-btn'),
    reloadBtn: document.getElementById('reload-btn'),
    newTabBtn: document.getElementById('new-tab-btn'),
    privacyIndicator: document.getElementById('privacy-indicator'),
    privacyGrade: document.getElementById('privacy-grade'),
    blockedCount: document.getElementById('blocked-count'),
    menuBtn: document.getElementById('menu-btn'),
    menuDropdown: document.getElementById('menu-dropdown'),
    privacyDashboardBtn: document.getElementById('privacy-dashboard-btn'),
    privacyDashboardModal: document.getElementById('privacy-dashboard-modal'),
    aboutModal: document.getElementById('about-modal'),
    clearDataModal: document.getElementById('clear-data-modal')
};

/**
 * Initialize browser
 */
function init() {
    console.log('CleanFinding Browser initializing...');

    // Create initial tab
    createTab('https://cleanfinding.com');

    // Setup event listeners
    setupEventListeners();

    // Setup IPC listeners from main process
    setupIPCListeners();

    console.log('CleanFinding Browser initialized');
}

/**
 * Setup event listeners
 */
function setupEventListeners() {
    // Tab controls
    elements.newTabBtn.addEventListener('click', () => {
        createTab('https://cleanfinding.com');
    });

    // Navigation controls
    elements.backBtn.addEventListener('click', () => {
        const tab = getActiveTab();
        if (tab && tab.webview) {
            tab.webview.goBack();
        }
    });

    elements.forwardBtn.addEventListener('click', () => {
        const tab = getActiveTab();
        if (tab && tab.webview) {
            tab.webview.goForward();
        }
    });

    elements.reloadBtn.addEventListener('click', () => {
        const tab = getActiveTab();
        if (tab && tab.webview) {
            tab.webview.reload();
        }
    });

    // Address bar
    elements.addressBar.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            navigateToUrl(elements.addressBar.value);
        }
    });

    elements.addressBar.addEventListener('focus', () => {
        elements.addressBar.select();
    });

    // Menu
    elements.menuBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        toggleMenu();
    });

    // Close menu when clicking outside
    document.addEventListener('click', () => {
        elements.menuDropdown.style.display = 'none';
    });

    // Menu items
    document.querySelectorAll('.menu-item').forEach(item => {
        item.addEventListener('click', () => {
            handleMenuAction(item.dataset.action);
            elements.menuDropdown.style.display = 'none';
        });
    });

    // Privacy Dashboard
    elements.privacyDashboardBtn.addEventListener('click', () => {
        showPrivacyDashboard();
    });

    document.getElementById('close-privacy-dashboard').addEventListener('click', () => {
        elements.privacyDashboardModal.style.display = 'none';
    });

    // About modal
    document.getElementById('close-about').addEventListener('click', () => {
        elements.aboutModal.style.display = 'none';
    });

    // Clear data modal
    document.getElementById('close-clear-data').addEventListener('click', () => {
        elements.clearDataModal.style.display = 'none';
    });

    document.getElementById('confirm-clear-data').addEventListener('click', () => {
        clearBrowsingData();
    });

    // Close modals on background click
    [elements.privacyDashboardModal, elements.aboutModal, elements.clearDataModal].forEach(modal => {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.style.display = 'none';
            }
        });
    });

    // Keyboard shortcuts
    document.addEventListener('keydown', (e) => {
        if (e.ctrlKey || e.metaKey) {
            switch (e.key) {
                case 't':
                    e.preventDefault();
                    createTab('https://cleanfinding.com');
                    break;
                case 'w':
                    e.preventDefault();
                    closeTab(browser.activeTabId);
                    break;
                case 'r':
                    e.preventDefault();
                    const tab = getActiveTab();
                    if (tab && tab.webview) {
                        tab.webview.reload();
                    }
                    break;
                case 'l':
                    e.preventDefault();
                    elements.addressBar.focus();
                    break;
            }
        }
    });
}

/**
 * Setup IPC listeners from main process
 */
function setupIPCListeners() {
    window.cleanfindingAPI.onNewTab(() => {
        createTab('https://cleanfinding.com');
    });

    window.cleanfindingAPI.onNewIncognitoTab(() => {
        createTab('https://cleanfinding.com', true);
    });

    window.cleanfindingAPI.onCloseTab(() => {
        closeTab(browser.activeTabId);
    });

    window.cleanfindingAPI.onReloadPage(() => {
        const tab = getActiveTab();
        if (tab && tab.webview) {
            tab.webview.reload();
        }
    });

    window.cleanfindingAPI.onForceReloadPage(() => {
        const tab = getActiveTab();
        if (tab && tab.webview) {
            tab.webview.reloadIgnoringCache();
        }
    });

    window.cleanfindingAPI.onClearBrowsingData(() => {
        elements.clearDataModal.style.display = 'flex';
    });

    window.cleanfindingAPI.onShowPrivacyDashboard(() => {
        showPrivacyDashboard();
    });

    window.cleanfindingAPI.onShowAbout(() => {
        elements.aboutModal.style.display = 'flex';
    });

    window.cleanfindingAPI.onLoadDuckPlayer((videoId, originalUrl) => {
        loadDuckPlayerPage(videoId, originalUrl);
    });
}

/**
 * Create new tab
 */
function createTab(url = 'https://cleanfinding.com', isIncognito = false) {
    const tabId = browser.nextTabId++;

    // Create tab object
    const tab = {
        id: tabId,
        url: url,
        title: 'New Tab',
        isIncognito: isIncognito,
        canGoBack: false,
        canGoForward: false
    };

    // Create tab UI
    const tabElement = document.createElement('div');
    tabElement.className = `tab ${isIncognito ? 'incognito' : ''}`;
    tabElement.dataset.tabId = tabId;

    tabElement.innerHTML = `
        <img class="tab-icon" src="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23666'><circle cx='12' cy='12' r='10'/></svg>" alt="">
        <span class="tab-title">${isIncognito ? '🔒 Incognito' : 'New Tab'}</span>
        <button class="tab-close">&times;</button>
    `;

    // Tab click handler
    tabElement.addEventListener('click', (e) => {
        if (!e.target.classList.contains('tab-close')) {
            switchToTab(tabId);
        }
    });

    // Close button handler
    tabElement.querySelector('.tab-close').addEventListener('click', (e) => {
        e.stopPropagation();
        closeTab(tabId);
    });

    elements.tabsContainer.appendChild(tabElement);
    tab.element = tabElement;

    // Create webview
    const webview = document.createElement('webview');
    webview.src = url;
    webview.className = 'hidden';
    webview.dataset.tabId = tabId;

    // Webview event listeners
    setupWebviewListeners(webview, tab);

    elements.webviewContainer.appendChild(webview);
    tab.webview = webview;

    // Add to tabs array
    browser.tabs.push(tab);

    // Switch to new tab
    switchToTab(tabId);

    return tab;
}

/**
 * Setup webview event listeners
 */
function setupWebviewListeners(webview, tab) {
    webview.addEventListener('did-start-loading', () => {
        if (tab.id === browser.activeTabId) {
            updateNavigationButtons();
        }
    });

    webview.addEventListener('did-stop-loading', () => {
        if (tab.id === browser.activeTabId) {
            updateNavigationButtons();
        }
    });

    webview.addEventListener('page-title-updated', (e) => {
        tab.title = e.title;
        updateTabTitle(tab);
    });

    webview.addEventListener('page-favicon-updated', (e) => {
        if (e.favicons && e.favicons.length > 0) {
            updateTabIcon(tab, e.favicons[0]);
        }
    });

    webview.addEventListener('did-navigate', (e) => {
        tab.url = e.url;
        if (tab.id === browser.activeTabId) {
            elements.addressBar.value = e.url;
            updateNavigationButtons();
            updatePrivacyGrade(e.url);
        }
    });

    webview.addEventListener('did-navigate-in-page', (e) => {
        tab.url = e.url;
        if (tab.id === browser.activeTabId) {
            elements.addressBar.value = e.url;
        }
    });

    webview.addEventListener('new-window', (e) => {
        e.preventDefault();
        createTab(e.url);
    });

    // Update navigation state
    webview.addEventListener('did-navigate', () => {
        tab.canGoBack = webview.canGoBack();
        tab.canGoForward = webview.canGoForward();
        if (tab.id === browser.activeTabId) {
            updateNavigationButtons();
        }
    });
}

/**
 * Switch to tab
 */
function switchToTab(tabId) {
    const tab = browser.tabs.find(t => t.id === tabId);
    if (!tab) return;

    // Hide all webviews
    document.querySelectorAll('webview').forEach(wv => {
        wv.classList.add('hidden');
    });

    // Remove active class from all tabs
    document.querySelectorAll('.tab').forEach(t => {
        t.classList.remove('active');
    });

    // Show active webview and tab
    tab.webview.classList.remove('hidden');
    tab.element.classList.add('active');

    browser.activeTabId = tabId;

    // Update UI
    elements.addressBar.value = tab.url;
    updateNavigationButtons();
    updatePrivacyGrade(tab.url);
}

/**
 * Close tab
 */
function closeTab(tabId) {
    const tabIndex = browser.tabs.findIndex(t => t.id === tabId);
    if (tabIndex === -1) return;

    const tab = browser.tabs[tabIndex];

    // Remove from DOM
    tab.element.remove();
    tab.webview.remove();

    // Remove from array
    browser.tabs.splice(tabIndex, 1);

    // If closing active tab, switch to another
    if (browser.activeTabId === tabId) {
        if (browser.tabs.length > 0) {
            const newIndex = Math.min(tabIndex, browser.tabs.length - 1);
            switchToTab(browser.tabs[newIndex].id);
        } else {
            // Create new tab if all tabs closed
            createTab('https://cleanfinding.com');
        }
    }
}

/**
 * Navigate to URL
 */
async function navigateToUrl(input) {
    const tab = getActiveTab();
    if (!tab) return;

    let url = input.trim();

    // Search if not a URL
    if (!url.includes('.') && !url.startsWith('http://') && !url.startsWith('https://')) {
        url = `https://duckduckgo.com/?q=${encodeURIComponent(url)}`;
    } else if (!url.startsWith('http://') && !url.startsWith('https://')) {
        url = `https://${url}`;
    }

    // Validate URL scheme
    const isValid = await window.cleanfindingAPI.validateUrlScheme(url);
    if (!isValid) {
        alert('Invalid URL scheme. Only http:// and https:// URLs are allowed.');
        return;
    }

    tab.webview.src = url;
}

/**
 * Update navigation buttons
 */
function updateNavigationButtons() {
    const tab = getActiveTab();
    if (!tab) return;

    elements.backBtn.disabled = !tab.webview.canGoBack();
    elements.forwardBtn.disabled = !tab.webview.canGoForward();
}

/**
 * Update tab title
 */
function updateTabTitle(tab) {
    const titleElement = tab.element.querySelector('.tab-title');
    if (titleElement) {
        titleElement.textContent = tab.isIncognito ? `🔒 ${tab.title}` : tab.title;
    }
}

/**
 * Update tab icon
 */
function updateTabIcon(tab, iconUrl) {
    const iconElement = tab.element.querySelector('.tab-icon');
    if (iconElement) {
        iconElement.src = iconUrl;
    }
}

/**
 * Update privacy grade
 */
function updatePrivacyGrade(url) {
    // Simple privacy grading based on domain
    let grade = 'A+';
    let gradeClass = 'grade-a';

    try {
        const domain = new URL(url).hostname;

        // Known tracking-heavy domains
        if (domain.includes('facebook.com') || domain.includes('google.com')) {
            grade = 'D';
            gradeClass = 'grade-d';
        } else if (domain.includes('twitter.com') || domain.includes('amazon.com')) {
            grade = 'C';
            gradeClass = 'grade-c';
        }
    } catch (e) {
        // Invalid URL
    }

    elements.privacyGrade.textContent = grade;
    elements.privacyIndicator.className = `privacy-indicator ${gradeClass}`;
}

/**
 * Get active tab
 */
function getActiveTab() {
    return browser.tabs.find(t => t.id === browser.activeTabId);
}

/**
 * Toggle menu
 */
function toggleMenu() {
    const isVisible = elements.menuDropdown.style.display === 'block';
    elements.menuDropdown.style.display = isVisible ? 'none' : 'block';
}

/**
 * Handle menu actions
 */
function handleMenuAction(action) {
    switch (action) {
        case 'new-tab':
            createTab('https://cleanfinding.com');
            break;
        case 'new-incognito-tab':
            createTab('https://cleanfinding.com', true);
            break;
        case 'history':
            createTab('cleanfinding://history');
            break;
        case 'downloads':
            createTab('cleanfinding://downloads');
            break;
        case 'bookmarks':
            createTab('cleanfinding://bookmarks');
            break;
        case 'settings':
            createTab('cleanfinding://settings');
            break;
        case 'clear-data':
            elements.clearDataModal.style.display = 'flex';
            break;
        case 'about':
            elements.aboutModal.style.display = 'flex';
            break;
    }
}

/**
 * Show privacy dashboard
 */
function showPrivacyDashboard() {
    // Update stats
    document.getElementById('trackers-blocked').textContent = browser.privacyStats.trackersBlocked;
    document.getElementById('cookies-blocked').textContent = browser.privacyStats.cookiesBlocked;
    document.getElementById('privacy-grade-stat').textContent = elements.privacyGrade.textContent;

    elements.privacyDashboardModal.style.display = 'flex';
}

/**
 * Clear browsing data
 */
async function clearBrowsingData() {
    const options = {
        cache: document.getElementById('clear-cache').checked,
        cookies: document.getElementById('clear-cookies').checked,
        history: document.getElementById('clear-history').checked
    };

    const result = await window.cleanfindingAPI.clearBrowsingData(options);

    if (result.success) {
        alert('Browsing data cleared successfully');
        elements.clearDataModal.style.display = 'none';
    } else {
        alert('Failed to clear browsing data: ' + result.error);
    }
}

/**
 * Load Duck Player page
 */
async function loadDuckPlayerPage(videoId, originalUrl) {
    const tab = getActiveTab();
    if (!tab) return;

    const html = await window.cleanfindingAPI.getDuckPlayerPage(videoId, '');

    // Load Duck Player HTML into webview
    tab.webview.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(html)}`);

    // Update tracker count
    browser.privacyStats.trackersBlocked++;
    elements.blockedCount.textContent = browser.privacyStats.trackersBlocked;
}

// Initialize on load
document.addEventListener('DOMContentLoaded', init);
