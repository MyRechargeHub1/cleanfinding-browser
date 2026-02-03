/**
 * CleanFinding Browser - Browser Logic
 * @version 1.7.0
 */

// Browser state
const browser = {
    tabs: [],
    activeTabId: null,
    nextTabId: 1,
    privacyStats: {
        trackersBlocked: 0,
        cookiesBlocked: 0
    },
    // Scrolling toolbar state
    isToolbarVisible: true,
    lastScrollY: 0,
    SCROLL_THRESHOLD: 30,
    // Reader mode state
    isReaderMode: false,
    // Zoom level (percentage)
    currentZoom: 100,
    // Night mode state
    isNightMode: false
};

// Trusted domains - never block these (same as Android)
const trustedDomains = [
    'youtube.com', 'youtu.be', 'm.youtube.com',
    'google.com', 'google.co', 'gstatic.com', 'googleapis.com',
    'facebook.com', 'instagram.com', 'twitter.com', 'x.com',
    'pinterest.com', 'linkedin.com', 'reddit.com',
    'amazon.com', 'ebay.com', 'walmart.com',
    'wikipedia.org', 'wikimedia.org',
    'github.com', 'stackoverflow.com',
    'cleanfinding.com',
    'microsoft.com', 'apple.com', 'netflix.com'
];

// Blocked tracker domains
const blockedDomains = [
    'google-analytics.com', 'googletagmanager.com', 'doubleclick.net',
    'facebook.net', 'connect.facebook.net', 'analytics.google.com',
    'adservice.google.com', 'googlesyndication.com', 'googleadservices.com',
    'mixpanel.com', 'hotjar.com', 'fullstory.com', 'amplitude.com',
    'segment.com', 'heapanalytics.com', 'crazyegg.com', 'clarity.ms',
    'adnxs.com', 'criteo.com', 'taboola.com', 'outbrain.com'
];

// Adult content keywords (check domain only, not full URL)
const adultKeywords = [
    'pornhub', 'xvideos', 'xnxx', 'redtube', 'youporn',
    'xhamster', 'porn', 'xxx', 'adult'
];

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
    clearDataModal: document.getElementById('clear-data-modal'),
    browserChrome: document.querySelector('.browser-chrome')
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
                case '+':
                case '=':
                    e.preventDefault();
                    zoomIn();
                    break;
                case '-':
                    e.preventDefault();
                    zoomOut();
                    break;
                case '0':
                    e.preventDefault();
                    setZoom(100);
                    break;
                case 'f':
                    e.preventDefault();
                    showFindInPage();
                    break;
                case 'g':
                    e.preventDefault();
                    if (e.shiftKey) {
                        findPrevious();
                    } else {
                        findNext();
                    }
                    break;
            }
        }
        // Escape to clear find
        if (e.key === 'Escape' && findInPageActive) {
            clearFind();
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

    // Create webview with proper web preferences for search functionality
    const webview = document.createElement('webview');
    webview.src = url;
    webview.className = 'hidden';
    webview.dataset.tabId = tabId;
    // CRITICAL: Enable JavaScript and DOM storage for search API calls to work
    webview.setAttribute('webpreferences', 'javascript=yes, webSecurity=yes, allowRunningInsecureContent=no');
    webview.setAttribute('allowpopups', 'true');

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
            // Show toolbar when loading starts
            showToolbar();
        }
    });

    webview.addEventListener('did-stop-loading', () => {
        if (tab.id === browser.activeTabId) {
            updateNavigationButtons();
        }
    });

    // Inject scroll detection after page loads
    webview.addEventListener('did-finish-load', () => {
        injectScrollDetection(webview);
    });

    // Capture console messages for scroll detection
    webview.addEventListener('console-message', (e) => {
        if (e.message && e.message.startsWith('__CLEANFINDING_SCROLL__:')) {
            const scrollY = parseInt(e.message.split(':')[1], 10);
            if (!isNaN(scrollY) && tab.id === browser.activeTabId) {
                handleWebviewScroll(scrollY);
            }
        }
    });

    // Handle fullscreen requests (for YouTube videos)
    webview.addEventListener('enter-html-full-screen', () => {
        document.body.classList.add('fullscreen-video');
        elements.browserChrome.style.display = 'none';
    });

    webview.addEventListener('leave-html-full-screen', () => {
        document.body.classList.remove('fullscreen-video');
        elements.browserChrome.style.display = '';
        showToolbar();
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
        // Check if the new URL should be blocked
        if (isBlockedUrl(e.url)) {
            console.log('Blocked new window URL:', e.url);
            return;
        }
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

    // Search if not a URL - use CleanFinding search (same as Android/iOS)
    if (!url.includes('.') && !url.startsWith('http://') && !url.startsWith('https://')) {
        url = `https://cleanfinding.com/search?q=${encodeURIComponent(url)}`;
    } else if (!url.startsWith('http://') && !url.startsWith('https://')) {
        url = `https://${url}`;
    }

    // Validate URL scheme
    const isValid = await window.cleanfindingAPI.validateUrlScheme(url);
    if (!isValid) {
        alert('Invalid URL scheme. Only http:// and https:// URLs are allowed.');
        return;
    }

    // Check if URL should be blocked
    if (isBlockedUrl(url)) {
        alert('This content has been blocked by CleanFinding Browser for your safety.');
        return;
    }

    // Show toolbar when navigating
    showToolbar();

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
        case 'share':
            shareCurrentPage();
            break;
        case 'copy-url':
            copyUrlToClipboard();
            break;
        case 'pip':
            enterPictureInPicture();
            break;
        case 'reader-mode':
            toggleReaderMode();
            break;
        case 'zoom-in':
            zoomIn();
            break;
        case 'zoom-out':
            zoomOut();
            break;
        case 'zoom-reset':
            setZoom(100);
            break;
        case 'night-mode':
            toggleNightMode();
            break;
        case 'translate':
            translatePage();
            break;
        case 'find':
            showFindInPage();
            break;
        case 'page-info':
            showPageInfo();
            break;
        case 'read-aloud':
            toggleReadAloud();
            break;
        case 'clear-site-data':
            clearCurrentSiteData();
            break;
        case 'voice-search':
            startVoiceSearch();
            break;
        case 'screenshot':
            takeScreenshot();
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

/**
 * Enter Picture-in-Picture mode for the current video
 * Allows watching videos in a floating window
 */
async function enterPictureInPicture() {
    const tab = getActiveTab();
    if (!tab || !tab.webview) return;

    try {
        // Inject script to find and enter PiP for any playing video
        const script = `
            (function() {
                const videos = document.querySelectorAll('video');
                for (const video of videos) {
                    if (!video.paused || video.currentTime > 0) {
                        if (document.pictureInPictureEnabled && !video.disablePictureInPicture) {
                            video.requestPictureInPicture()
                                .then(() => console.log('PiP: Entered Picture-in-Picture mode'))
                                .catch(err => console.log('PiP: Failed - ' + err.message));
                            return true;
                        }
                    }
                }
                // If no active video found, try the first video
                if (videos.length > 0) {
                    const video = videos[0];
                    if (document.pictureInPictureEnabled && !video.disablePictureInPicture) {
                        video.requestPictureInPicture()
                            .then(() => console.log('PiP: Entered Picture-in-Picture mode'))
                            .catch(err => console.log('PiP: Failed - ' + err.message));
                        return true;
                    }
                }
                return false;
            })();
        `;

        const result = await tab.webview.executeJavaScript(script);
        if (!result) {
            alert('No video found on this page, or Picture-in-Picture is not supported.');
        }
    } catch (error) {
        console.error('PiP error:', error);
        alert('Picture-in-Picture is not available for this page.');
    }
}

/**
 * Copy current URL to clipboard
 */
function copyUrlToClipboard() {
    const tab = getActiveTab();
    if (!tab) return;

    const url = tab.url || elements.addressBar.value;
    if (url) {
        navigator.clipboard.writeText(url).then(() => {
            // Show a brief notification
            const originalPlaceholder = elements.addressBar.placeholder;
            elements.addressBar.placeholder = 'URL copied!';
            setTimeout(() => {
                elements.addressBar.placeholder = originalPlaceholder;
            }, 1500);
        }).catch(err => {
            console.error('Failed to copy URL:', err);
            alert('Failed to copy URL to clipboard');
        });
    }
}

/**
 * Share current page
 */
function shareCurrentPage() {
    const tab = getActiveTab();
    if (!tab) return;

    const url = tab.url || elements.addressBar.value;
    const title = tab.title || 'Check this out';

    if (navigator.share) {
        navigator.share({
            title: title,
            url: url
        }).catch(err => {
            console.log('Share cancelled or failed:', err);
        });
    } else {
        // Fallback: copy to clipboard
        copyUrlToClipboard();
    }
}

/**
 * Toggle Reader Mode - extracts article content for clean reading
 */
async function toggleReaderMode() {
    const tab = getActiveTab();
    if (!tab || !tab.webview) return;

    browser.isReaderMode = !browser.isReaderMode;

    if (browser.isReaderMode) {
        const readerScript = `
            (function() {
                var article = document.querySelector('article') ||
                              document.querySelector('[role="main"]') ||
                              document.querySelector('main') ||
                              document.querySelector('.post-content') ||
                              document.querySelector('.article-content') ||
                              document.querySelector('.entry-content');

                var title = document.querySelector('h1')?.textContent || document.title;
                var content = '';

                if (article) {
                    content = article.innerHTML;
                } else {
                    var paragraphs = document.querySelectorAll('p');
                    paragraphs.forEach(function(p) {
                        if (p.textContent.length > 50) {
                            content += '<p>' + p.textContent + '</p>';
                        }
                    });
                }

                if (content.length < 100) {
                    return { success: false };
                }

                var readerHtml = '<!DOCTYPE html><html><head>' +
                    '<meta name="viewport" content="width=device-width, initial-scale=1">' +
                    '<style>' +
                    'body { font-family: Georgia, serif; max-width: 680px; margin: 0 auto; padding: 20px; ' +
                    'line-height: 1.8; font-size: 18px; color: #333; background: #fafafa; }' +
                    'h1 { font-size: 28px; line-height: 1.3; margin-bottom: 20px; }' +
                    'img { max-width: 100%; height: auto; }' +
                    'a { color: #0066cc; }' +
                    '@media (prefers-color-scheme: dark) {' +
                    'body { background: #1a1a1a; color: #e0e0e0; }' +
                    'a { color: #6699ff; }' +
                    '}' +
                    '</style></head><body>' +
                    '<h1>' + title + '</h1>' +
                    content +
                    '</body></html>';

                document.open();
                document.write(readerHtml);
                document.close();
                return { success: true };
            })();
        `;

        try {
            const result = await tab.webview.executeJavaScript(readerScript);
            if (!result.success) {
                alert('Could not extract article content from this page.');
                browser.isReaderMode = false;
            }
        } catch (err) {
            console.error('Reader mode error:', err);
            browser.isReaderMode = false;
        }
    } else {
        // Exit reader mode - reload the page
        tab.webview.reload();
    }
}

/**
 * Show zoom controls dialog
 */
function showZoomDialog() {
    const zoomLevels = [50, 75, 100, 125, 150, 175, 200];
    const currentIndex = zoomLevels.indexOf(browser.currentZoom);

    const newZoom = prompt(`Current zoom: ${browser.currentZoom}%\nEnter new zoom level (50-200):`, browser.currentZoom);
    if (newZoom !== null) {
        const zoom = parseInt(newZoom, 10);
        if (zoom >= 50 && zoom <= 200) {
            setZoom(zoom);
        } else {
            alert('Please enter a value between 50 and 200');
        }
    }
}

/**
 * Set zoom level
 */
function setZoom(level) {
    const tab = getActiveTab();
    if (!tab || !tab.webview) return;

    browser.currentZoom = level;
    tab.webview.setZoomFactor(level / 100);
}

/**
 * Zoom in by 25%
 */
function zoomIn() {
    if (browser.currentZoom < 200) {
        setZoom(browser.currentZoom + 25);
    }
}

/**
 * Zoom out by 25%
 */
function zoomOut() {
    if (browser.currentZoom > 50) {
        setZoom(browser.currentZoom - 25);
    }
}

/**
 * Toggle night mode / blue light filter
 */
async function toggleNightMode() {
    const tab = getActiveTab();
    if (!tab || !tab.webview) return;

    browser.isNightMode = !browser.isNightMode;

    const nightModeScript = `
        (function() {
            var existingFilter = document.getElementById('cleanfinding-night-mode');
            if (existingFilter) {
                existingFilter.remove();
                return 'disabled';
            }

            var style = document.createElement('style');
            style.id = 'cleanfinding-night-mode';
            style.textContent = \`
                html {
                    filter: sepia(30%) brightness(90%) !important;
                    background-color: #1a1a1a !important;
                }
                body {
                    background-color: #1a1a1a !important;
                }
            \`;
            document.head.appendChild(style);
            return 'enabled';
        })();
    `;

    try {
        await tab.webview.executeJavaScript(nightModeScript);
    } catch (err) {
        console.error('Night mode error:', err);
    }
}

/**
 * Translate the current page using Google Translate
 */
function translatePage() {
    const tab = getActiveTab();
    if (!tab) return;

    const currentUrl = tab.url || elements.addressBar.value;
    if (currentUrl) {
        const translateUrl = `https://translate.google.com/translate?sl=auto&tl=en&u=${encodeURIComponent(currentUrl)}`;
        tab.webview.src = translateUrl;
    }
}

/**
 * Show page information dialog
 */
function showPageInfo() {
    const tab = getActiveTab();
    if (!tab) return;

    const url = tab.url || elements.addressBar.value;
    const title = tab.title || 'No title';

    let domain = 'Unknown';
    let isHttps = false;
    try {
        const urlObj = new URL(url);
        domain = urlObj.hostname;
        isHttps = urlObj.protocol === 'https:';
    } catch (e) {}

    const sslStatus = isHttps ? '🔒 Secure (HTTPS)' : '⚠️ Not Secure (HTTP)';

    const info = `Connection: ${sslStatus}

Domain: ${domain}

Trackers Blocked: ${browser.privacyStats.trackersBlocked}

Page Title: ${title}

Full URL: ${url}`;

    alert(info);
}

/**
 * Read page content aloud using Web Speech API
 */
let speechSynthesis = window.speechSynthesis;
let currentUtterance = null;
let isSpeaking = false;

function toggleReadAloud() {
    if (isSpeaking) {
        speechSynthesis.cancel();
        isSpeaking = false;
        return;
    }

    const tab = getActiveTab();
    if (!tab || !tab.webview) return;

    const extractScript = `
        (function() {
            var article = document.querySelector('article') ||
                          document.querySelector('[role="main"]') ||
                          document.querySelector('main') ||
                          document.body;

            var text = article.innerText || article.textContent || '';
            text = text.replace(/\\s+/g, ' ').trim();
            return text.substring(0, 5000);
        })();
    `;

    tab.webview.executeJavaScript(extractScript).then(text => {
        if (text && text.length > 0) {
            currentUtterance = new SpeechSynthesisUtterance(text);
            currentUtterance.onend = () => {
                isSpeaking = false;
            };
            currentUtterance.onerror = () => {
                isSpeaking = false;
            };
            isSpeaking = true;
            speechSynthesis.speak(currentUtterance);
        } else {
            alert('No text content found on this page.');
        }
    }).catch(err => {
        console.error('Read aloud error:', err);
        alert('Could not read page content.');
    });
}

/**
 * Clear data for current site only
 */
async function clearCurrentSiteData() {
    const tab = getActiveTab();
    if (!tab) return;

    const url = tab.url || elements.addressBar.value;
    let domain = '';
    try {
        domain = new URL(url).hostname;
    } catch (e) {
        alert('Invalid URL');
        return;
    }

    if (confirm(`Clear all data for ${domain}?\n\nThis includes cookies, cache, and stored data.`)) {
        try {
            // Clear via Electron API
            const result = await window.cleanfindingAPI.clearSiteData(domain);
            if (result && result.success) {
                // Reload the page
                tab.webview.reload();
                alert(`Site data cleared for ${domain}`);
            } else {
                // Fallback: just clear cache and reload
                tab.webview.reloadIgnoringCache();
                alert(`Cache cleared for ${domain}`);
            }
        } catch (err) {
            // Fallback
            tab.webview.reloadIgnoringCache();
            alert(`Cache cleared for ${domain}`);
        }
    }
}

/**
 * Start voice search using Web Speech API
 */
let recognition = null;

function startVoiceSearch() {
    // Check if speech recognition is supported
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;

    if (!SpeechRecognition) {
        alert('Voice search is not supported in this browser.');
        return;
    }

    if (recognition) {
        recognition.stop();
        recognition = null;
        return;
    }

    recognition = new SpeechRecognition();
    recognition.continuous = false;
    recognition.interimResults = false;
    recognition.lang = navigator.language || 'en-US';

    // Show listening indicator
    const originalPlaceholder = elements.addressBar.placeholder;
    elements.addressBar.placeholder = '🎤 Listening...';
    elements.addressBar.focus();

    recognition.onresult = (event) => {
        const transcript = event.results[0][0].transcript;
        elements.addressBar.value = transcript;
        elements.addressBar.placeholder = originalPlaceholder;
        navigateToUrl(transcript);
        recognition = null;
    };

    recognition.onerror = (event) => {
        console.error('Speech recognition error:', event.error);
        elements.addressBar.placeholder = originalPlaceholder;
        if (event.error !== 'aborted') {
            alert('Voice search failed: ' + event.error);
        }
        recognition = null;
    };

    recognition.onend = () => {
        elements.addressBar.placeholder = originalPlaceholder;
    };

    try {
        recognition.start();
    } catch (err) {
        console.error('Failed to start speech recognition:', err);
        elements.addressBar.placeholder = originalPlaceholder;
        alert('Failed to start voice search.');
        recognition = null;
    }
}

/**
 * Take a screenshot of the current page
 */
async function takeScreenshot() {
    const tab = getActiveTab();
    if (!tab || !tab.webview) return;

    try {
        // Use Electron's capturePage API via webview
        const image = await tab.webview.capturePage();

        if (image && !image.isEmpty()) {
            // Convert to data URL and offer download
            const dataUrl = image.toDataURL();

            // Create download link
            const link = document.createElement('a');
            const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
            link.download = `CleanFinding_${timestamp}.png`;
            link.href = dataUrl;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);

            // Notify user
            const originalPlaceholder = elements.addressBar.placeholder;
            elements.addressBar.placeholder = 'Screenshot saved!';
            setTimeout(() => {
                elements.addressBar.placeholder = originalPlaceholder;
            }, 2000);
        } else {
            alert('Failed to capture screenshot - page may be empty.');
        }
    } catch (err) {
        console.error('Screenshot error:', err);
        // Fallback: use html2canvas approach via injection
        try {
            const script = `
                (function() {
                    // Simple screenshot via canvas (visible area only)
                    var canvas = document.createElement('canvas');
                    canvas.width = window.innerWidth;
                    canvas.height = window.innerHeight;
                    // This is a simplified fallback - real implementation would need html2canvas
                    return 'Screenshot not available in fallback mode';
                })();
            `;
            await tab.webview.executeJavaScript(script);
            alert('Screenshot feature requires Electron native API.');
        } catch (e) {
            alert('Failed to take screenshot.');
        }
    }
}

/**
 * Find in page functionality
 */
let findInPageActive = false;
let findInPageQuery = '';

function showFindInPage() {
    const query = prompt('Find in page:', findInPageQuery);
    if (query !== null && query.trim() !== '') {
        findInPageQuery = query.trim();
        findInPageActive = true;
        performFind(findInPageQuery);
    } else if (query === '') {
        clearFind();
    }
}

function performFind(query) {
    const tab = getActiveTab();
    if (!tab || !tab.webview) return;

    // Use Electron's findInPage API
    tab.webview.findInPage(query);
}

function findNext() {
    if (findInPageActive && findInPageQuery) {
        const tab = getActiveTab();
        if (tab && tab.webview) {
            tab.webview.findInPage(findInPageQuery, { forward: true, findNext: true });
        }
    }
}

function findPrevious() {
    if (findInPageActive && findInPageQuery) {
        const tab = getActiveTab();
        if (tab && tab.webview) {
            tab.webview.findInPage(findInPageQuery, { forward: false, findNext: true });
        }
    }
}

function clearFind() {
    const tab = getActiveTab();
    if (tab && tab.webview) {
        tab.webview.stopFindInPage('clearSelection');
    }
    findInPageActive = false;
    findInPageQuery = '';
}

/**
 * Check if URL should be blocked (same logic as Android)
 */
function isBlockedUrl(url) {
    const lowerUrl = url.toLowerCase();

    // CRITICAL: Whitelist trusted domains - never block these
    for (const trusted of trustedDomains) {
        if (lowerUrl.includes(trusted)) {
            return false;
        }
    }

    // Check tracker/ad domains
    for (const domain of blockedDomains) {
        if (lowerUrl.includes(domain)) {
            return true;
        }
    }

    // Check adult content - only check the DOMAIN part, not full URL
    try {
        const urlObj = new URL(url);
        const host = urlObj.hostname.toLowerCase();
        for (const keyword of adultKeywords) {
            if (host.includes(keyword)) {
                return true;
            }
        }
    } catch (e) {
        // If URL parsing fails, do basic check
        const hostPart = lowerUrl.split('://')[1]?.split('/')[0] || '';
        for (const keyword of adultKeywords) {
            if (hostPart.includes(keyword)) {
                return true;
            }
        }
    }

    return false;
}

/**
 * Hide toolbar with animation (Chrome-like scroll behavior)
 */
function hideToolbar() {
    if (!browser.isToolbarVisible) return;
    browser.isToolbarVisible = false;

    if (elements.browserChrome) {
        elements.browserChrome.style.transition = 'transform 0.2s ease-out';
        elements.browserChrome.style.transform = 'translateY(-100%)';
    }
}

/**
 * Show toolbar with animation (Chrome-like scroll behavior)
 */
function showToolbar() {
    if (browser.isToolbarVisible) return;
    browser.isToolbarVisible = true;

    if (elements.browserChrome) {
        elements.browserChrome.style.transition = 'transform 0.2s ease-out';
        elements.browserChrome.style.transform = 'translateY(0)';
    }
}

/**
 * Handle scroll events from webview
 */
function handleWebviewScroll(scrollY) {
    const diff = scrollY - browser.lastScrollY;

    // Only respond to significant scroll changes
    if (Math.abs(diff) > browser.SCROLL_THRESHOLD) {
        if (diff > 0 && browser.isToolbarVisible) {
            // Scrolling DOWN - hide toolbar
            hideToolbar();
        } else if (diff < 0 && !browser.isToolbarVisible) {
            // Scrolling UP - show toolbar
            showToolbar();
        }
    }

    // Always show toolbar when at top of page
    if (scrollY <= 10 && !browser.isToolbarVisible) {
        showToolbar();
    }

    browser.lastScrollY = scrollY;
}

/**
 * Inject scroll detection script into webview
 */
function injectScrollDetection(webview) {
    const script = `
        (function() {
            if (window._cleanfindingScrollSetup) return;
            window._cleanfindingScrollSetup = true;

            let lastScrollY = 0;
            let ticking = false;

            function onScroll() {
                if (!ticking) {
                    window.requestAnimationFrame(function() {
                        // Send scroll position to parent via console (will be captured)
                        console.log('__CLEANFINDING_SCROLL__:' + window.scrollY);
                        ticking = false;
                    });
                    ticking = true;
                }
            }

            window.addEventListener('scroll', onScroll, { passive: true });
        })();
    `;

    webview.executeJavaScript(script).catch(err => {
        // Ignore errors - script might not be ready yet
    });
}

// Initialize on load
document.addEventListener('DOMContentLoaded', init);
