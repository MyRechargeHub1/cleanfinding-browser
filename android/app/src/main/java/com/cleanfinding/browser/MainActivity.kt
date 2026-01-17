package com.cleanfinding.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlEditText: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var backButton: ImageButton
    private lateinit var forwardButton: ImageButton
    private lateinit var homeButton: ImageButton
    private lateinit var menuButton: ImageButton
    private lateinit var tabsButton: FrameLayout
    private lateinit var tabCountText: TextView
    private lateinit var newTabButton: ImageButton
    private lateinit var tabContainer: LinearLayout
    private lateinit var findBar: LinearLayout
    private lateinit var findEditText: EditText
    private lateinit var findResultsText: TextView
    private lateinit var findPrevButton: ImageButton
    private lateinit var findNextButton: ImageButton
    private lateinit var findCloseButton: ImageButton
    private lateinit var httpsIcon: TextView
    private lateinit var privacyGradeBadge: TextView

    private lateinit var bookmarkManager: BookmarkManager
    private lateinit var historyManager: HistoryManager
    private lateinit var downloadManager: DownloadManagerHelper
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var privacyGradeCalculator: PrivacyGradeCalculator
    private lateinit var privacyStatsManager: PrivacyStatsManager
    private lateinit var cookieConsentHandler: CookieConsentHandler
    private lateinit var gpcHandler: GlobalPrivacyControlHandler
    private lateinit var biometricAuthHelper: BiometricAuthHelper
    private lateinit var duckPlayerHandler: DuckPlayerHandler
    private lateinit var emailProtectionHandler: EmailProtectionHandler

    // Privacy tracking
    private var currentPageTrackersBlocked = 0
    private val currentPageBlockedDomains = mutableListOf<String>()

    // Tab management
    private val tabs = mutableListOf<Tab>()
    private var activeTabIndex = 0
    private val tabWebViews = mutableMapOf<Long, WebView>()

    private var desktopMode = false

    // Fullscreen video support
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private lateinit var customViewContainer: FrameLayout

    // Tracker domains to block
    private val blockedDomains = listOf(
        "google-analytics.com", "googletagmanager.com", "doubleclick.net",
        "facebook.net", "facebook.com/tr", "connect.facebook.net",
        "analytics.google.com", "adservice.google.com",
        "googlesyndication.com", "googleadservices.com",
        "ads.google.com", "pagead2.googlesyndication.com",
        "mixpanel.com", "hotjar.com", "fullstory.com",
        "amplitude.com", "segment.com", "heapanalytics.com",
        "crazyegg.com", "mouseflow.com", "luckyorange.com",
        "clicktale.com", "inspectlet.com", "logrocket.com",
        "smartlook.com", "clarity.ms", "newrelic.com",
        "nr-data.net", "sentry.io", "bugsnag.com",
        "rollbar.com", "trackjs.com",
        "adnxs.com", "adsrvr.org", "criteo.com",
        "taboola.com", "outbrain.com", "revcontent.com",
        "mgid.com", "contentad.net", "adblade.com"
    )

    // Adult content domains to block
    private val adultDomains = listOf(
        "pornhub", "xvideos", "xnxx", "redtube", "youporn",
        "xhamster", "porn", "xxx", "adult", "sex"
    )

    private val homeUrl = "https://cleanfinding.com"

    companion object {
        private const val REQUEST_HISTORY = 1001
        private const val REQUEST_DOWNLOADS = 1002
        private const val REQUEST_SETTINGS = 1003

        /**
         * Escape a string for safe injection into JavaScript code
         * Prevents JavaScript injection vulnerabilities
         */
        private fun escapeJavaScriptString(str: String): String {
            return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
        }

        /**
         * Validate URL scheme to prevent XSS and other security vulnerabilities
         * Only allows http, https, and about schemes
         */
        private fun isUrlSchemeAllowed(url: String): Boolean {
            val lowerUrl = url.lowercase().trim()
            return lowerUrl.startsWith("http://") ||
                    lowerUrl.startsWith("https://") ||
                    lowerUrl.startsWith("about:") ||
                    !lowerUrl.contains(":")  // Allow plain URLs without scheme
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bookmarkManager = BookmarkManager(this)
        historyManager = HistoryManager(this)
        downloadManager = DownloadManagerHelper(this)
        preferencesManager = PreferencesManager(this)
        privacyGradeCalculator = PrivacyGradeCalculator()
        privacyStatsManager = PrivacyStatsManager(this)
        cookieConsentHandler = CookieConsentHandler()
        gpcHandler = GlobalPrivacyControlHandler()
        biometricAuthHelper = BiometricAuthHelper(this)
        duckPlayerHandler = DuckPlayerHandler()
        emailProtectionHandler = EmailProtectionHandler()

        initViews()
        setupListeners()

        // Create initial tab
        createNewTab(intent?.data?.toString() ?: homeUrl)
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        urlEditText = findViewById(R.id.urlEditText)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        backButton = findViewById(R.id.backButton)
        forwardButton = findViewById(R.id.forwardButton)
        homeButton = findViewById(R.id.homeButton)
        menuButton = findViewById(R.id.menuButton)
        tabsButton = findViewById(R.id.tabsButton)
        tabCountText = findViewById(R.id.tabCountText)
        newTabButton = findViewById(R.id.newTabButton)
        tabContainer = findViewById(R.id.tabContainer)
        findBar = findViewById(R.id.findBar)
        findEditText = findViewById(R.id.findEditText)
        findResultsText = findViewById(R.id.findResultsText)
        findPrevButton = findViewById(R.id.findPrevButton)
        findNextButton = findViewById(R.id.findNextButton)
        findCloseButton = findViewById(R.id.findCloseButton)
        httpsIcon = findViewById(R.id.httpsIcon)
        privacyGradeBadge = findViewById(R.id.privacyGradeBadge)
        customViewContainer = findViewById(R.id.customViewContainer)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(wv: WebView, isIncognito: Boolean = false) {
        // CRITICAL FIX: Enable hardware acceleration for video playback
        wv.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        wv.settings.apply {
            // CRITICAL: Always enable JavaScript, DOM storage, and database for search to work
            // Modern websites (including cleanfinding.com search) require these features
            // Note: Even in incognito mode, we need DOM storage enabled for sites to function
            // Privacy is maintained by clearing storage when incognito tab is closed
            javaScriptEnabled = true
            domStorageEnabled = true  // Always enable - required for search API calls
            databaseEnabled = true  // Always enable - required for modern web features
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            safeBrowsingEnabled = true

            // DESKTOP MODE: Configure viewport and layout settings like Chrome
            if (desktopMode) {
                // Chrome desktop mode settings:
                // 1. Use wide viewport to show desktop layouts
                useWideViewPort = true
                // 2. Load page zoomed out to show full width (like Chrome)
                loadWithOverviewMode = true
                // 3. Set minimum logical screen width to desktop size (Chrome uses ~980-1024px)
                // This forces websites to render their desktop version
                @Suppress("DEPRECATION")
                minimumLogicalFontSize = 8  // Smaller fonts allowed in desktop mode
                // 4. Use NORMAL layout algorithm (not TEXT_AUTOSIZING) for true desktop rendering
                layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                // 5. Disable text auto-sizing that's designed for mobile
                textZoom = 100  // Fixed zoom, no mobile scaling
            } else {
                // Mobile mode settings
                useWideViewPort = true
                loadWithOverviewMode = true
                // Use TEXT_AUTOSIZING for better mobile readability
                layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                // Apply user's text size preference
                textZoom = preferencesManager.getTextSize()
            }

            // Apply cache mode settings
            if (isIncognito) {
                cacheMode = WebSettings.LOAD_NO_CACHE
                // Note: setAppCacheEnabled() was removed from Android API
            } else {
                cacheMode = when (preferencesManager.getCacheMode()) {
                    "normal" -> WebSettings.LOAD_DEFAULT
                    "prefer_cache" -> WebSettings.LOAD_CACHE_ELSE_NETWORK
                    "no_cache" -> WebSettings.LOAD_NO_CACHE
                    "cache_only" -> WebSettings.LOAD_CACHE_ONLY
                    else -> WebSettings.LOAD_DEFAULT
                }
            }

            // Apply image loading setting (only in non-desktop mode, desktop always loads images)
            if (!desktopMode) {
                loadsImagesAutomatically = if (isIncognito) true else preferencesManager.getShowImages()
                blockNetworkImage = if (isIncognito) false else !preferencesManager.getShowImages()
            } else {
                loadsImagesAutomatically = true
                blockNetworkImage = false
            }

            // CRITICAL FIX: Enable video playback without user gesture
            mediaPlaybackRequiresUserGesture = false

            // CRITICAL FIX: Enable all media features
            javaScriptCanOpenWindowsAutomatically = false

            // CRITICAL FIX: Set viewport meta tag support
            setSupportMultipleWindows(false)

            // DESKTOP MODE: Set appropriate user agent string
            userAgentString = if (desktopMode) {
                // Chrome 120 on Windows 10 - matches what Chrome uses for "Request desktop site"
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            } else {
                // Mobile user agent with CleanFinding identifier
                userAgentString.replace("; wv", "") + " CleanFindingBrowser/1.0"
            }
        }

        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false

                // SECURITY: Block dangerous URL schemes (javascript:, data:, file:, etc.)
                if (!isUrlSchemeAllowed(url)) {
                    showBlockedMessage(url)
                    return true
                }

                // Check if Duck Player is enabled and this is a YouTube URL
                if (preferencesManager.getDuckPlayer() && duckPlayerHandler.isYouTubeUrl(url)) {
                    val privacyUrl = duckPlayerHandler.convertToPrivacyUrl(url)
                    if (privacyUrl != null) {
                        view?.loadUrl(privacyUrl)
                        return true
                    }
                }

                if (isBlockedUrl(url)) {
                    showBlockedMessage(url)
                    return true
                }

                val safeUrl = enforceSafeSearch(url)
                if (safeUrl != url) {
                    view?.loadUrl(safeUrl)
                    return true
                }

                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (view == webView) {
                    // Reset tracker counter for new page
                    currentPageTrackersBlocked = 0
                    currentPageBlockedDomains.clear()

                    progressBar.visibility = View.VISIBLE
                    urlEditText.setText(url)
                    updateNavigationButtons()
                    updateCurrentTabUrl(url ?: homeUrl)
                }
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)

                // Check if URL contains blocked domain
                for (domain in blockedDomains) {
                    if (url.contains(domain)) {
                        // Increment tracker count
                        if (view == webView) {
                            currentPageTrackersBlocked++
                            if (!currentPageBlockedDomains.contains(domain)) {
                                currentPageBlockedDomains.add(domain)
                            }
                        }

                        // Return empty response to block the request
                        return WebResourceResponse("text/plain", "utf-8", null)
                    }
                }

                // Check if Email Protection is enabled and URL is a tracking pixel
                if (preferencesManager.getEmailProtection() && emailProtectionHandler.isTrackingPixel(url)) {
                    // Increment tracker count
                    if (view == webView) {
                        currentPageTrackersBlocked++
                    }

                    // Return empty response to block the tracking pixel
                    return WebResourceResponse("image/png", "utf-8", null)
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (view == webView) {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    updateNavigationButtons()
                    injectBlockingScript(view)
                    injectVideoFixCSS(view)

                    // Auto-decline cookie consent banners
                    cookieConsentHandler.injectAutoDeclineScript(view)
                    cookieConsentHandler.injectConsentBannerHiding(view)

                    // Enable Global Privacy Control (GPC)
                    gpcHandler.injectGPCSignal(view)
                    gpcHandler.monitorGPCViolations(view)

                    // Apply Duck Player enhancements for YouTube
                    if (preferencesManager.getDuckPlayer() && url?.let { duckPlayerHandler.isYouTubeUrl(it) } == true) {
                        duckPlayerHandler.injectDuckPlayerEnhancements(view)
                    }

                    // Apply Email Protection for webmail services
                    if (preferencesManager.getEmailProtection() && url != null) {
                        emailProtectionHandler.injectEmailProtection(view, url)
                    }

                    // Apply desktop mode viewport if enabled (Chrome-like behavior)
                    if (desktopMode) {
                        injectDesktopModeScript(view)
                    }

                    // Record page visit in history (if not incognito)
                    url?.let {
                        val title = view.title ?: ""
                        val isIncognito = if (activeTabIndex < tabs.size) tabs[activeTabIndex].isIncognito else false
                        historyManager.recordVisit(it, title, isIncognito)
                    }

                    // Calculate and display privacy grade
                    url?.let {
                        updatePrivacyGrade(it)
                    }
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (view == webView) {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                }
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (view == webView) {
                    progressBar.progress = newProgress
                    if (newProgress == 100) {
                        progressBar.visibility = View.GONE
                    }
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (view == webView && title != null) {
                    updateCurrentTabTitle(title)
                }
            }

            // CRITICAL FIX: Enable fullscreen video support for YouTube
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }

                customView = view
                customViewCallback = callback

                // Hide normal content
                findViewById<LinearLayout>(R.id.tabBar)?.visibility = View.GONE
                findViewById<LinearLayout>(R.id.urlEditText)?.parent?.let {
                    (it as View).visibility = View.GONE
                }
                webView.visibility = View.GONE

                // Show fullscreen video
                customViewContainer.visibility = View.VISIBLE
                customViewContainer.addView(customView, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ))
            }

            override fun onHideCustomView() {
                if (customView == null) {
                    return
                }

                // Hide fullscreen video
                customViewContainer.visibility = View.GONE
                customViewContainer.removeView(customView)

                // Show normal content
                findViewById<LinearLayout>(R.id.tabBar)?.visibility = View.VISIBLE
                findViewById<LinearLayout>(R.id.urlEditText)?.parent?.let {
                    (it as View).visibility = View.VISIBLE
                }
                webView.visibility = View.VISIBLE

                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
            }

            // Log JavaScript console messages for debugging
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    android.util.Log.d("WebView", "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
                }
                return super.onConsoleMessage(consoleMessage)
            }
        }

        // Download listener
        wv.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            downloadManager.startDownload(url, null, mimeType, userAgent, contentDisposition)
            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
        }

        // Apply cookie settings
        if (isIncognito) {
            CookieManager.getInstance().setAcceptCookie(false)
        } else {
            CookieManager.getInstance().setAcceptCookie(preferencesManager.getCookiesEnabled())
        }
    }

    private fun setupListeners() {
        urlEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                loadUrl(urlEditText.text.toString())
                true
            } else {
                false
            }
        }

        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }

        backButton.setOnClickListener {
            if (webView.canGoBack()) {
                webView.goBack()
            }
        }

        forwardButton.setOnClickListener {
            if (webView.canGoForward()) {
                webView.goForward()
            }
        }

        homeButton.setOnClickListener {
            loadUrl(homeUrl)
        }

        menuButton.setOnClickListener {
            showMenu()
        }

        tabsButton.setOnClickListener {
            toggleTabBar()
        }

        newTabButton.setOnClickListener {
            createNewTab(homeUrl)
        }

        // Find in page listeners
        findEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performFind()
                true
            } else {
                false
            }
        }

        findPrevButton.setOnClickListener {
            webView.findNext(false)
        }

        findNextButton.setOnClickListener {
            webView.findNext(true)
        }

        findCloseButton.setOnClickListener {
            closeFindBar()
        }

        webView.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
            if (isDoneCounting) {
                findResultsText.text = if (numberOfMatches > 0) {
                    "${activeMatchOrdinal + 1}/$numberOfMatches"
                } else {
                    "0/0"
                }
            }
        }
    }

    // Tab Management
    private fun createNewTab(url: String, isIncognito: Boolean = false) {
        // Check if biometric lock is required for incognito mode
        if (isIncognito && preferencesManager.getBiometricLockIncognito()) {
            if (!biometricAuthHelper.isBiometricAvailable()) {
                // Biometric not available, show message and don't create tab
                AlertDialog.Builder(this)
                    .setTitle("Biometric Unavailable")
                    .setMessage("${biometricAuthHelper.getBiometricStatusMessage()}\n\nPlease disable biometric lock in Settings or set up biometric authentication on your device.")
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Settings") { _, _ ->
                        showSettings()
                    }
                    .show()
                return
            }

            // Show biometric prompt
            biometricAuthHelper.authenticate(
                title = "Unlock Incognito Mode",
                subtitle = "Use biometric to create incognito tab",
                onSuccess = {
                    // Authentication successful, create incognito tab
                    createNewTabInternal(url, isIncognito)
                },
                onError = { errorMessage ->
                    Toast.makeText(this, "Authentication error: $errorMessage", Toast.LENGTH_SHORT).show()
                },
                onCancel = {
                    // User cancelled, don't create tab
                    Toast.makeText(this, "Incognito tab creation cancelled", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            // No biometric lock required, create tab normally
            createNewTabInternal(url, isIncognito)
        }
    }

    private fun createNewTabInternal(url: String, isIncognito: Boolean = false) {
        val tab = Tab(url = url, title = if (isIncognito) "Incognito Tab" else "New Tab", isIncognito = isIncognito)
        tabs.add(tab)

        val newWebView = WebView(this)
        setupWebView(newWebView, isIncognito)
        tabWebViews[tab.id] = newWebView

        // Bypass biometric check since we already authenticated when creating the tab
        switchToTab(tabs.size - 1, bypassBiometric = true)
        loadUrl(url)
        updateTabBar()
    }

    private fun switchToTab(index: Int, bypassBiometric: Boolean = false) {
        if (index < 0 || index >= tabs.size) return

        // Check if biometric lock is required for incognito tabs
        val targetTab = tabs[index]
        if (!bypassBiometric && targetTab.isIncognito && preferencesManager.getBiometricLockIncognito()) {
            // Don't require auth if we're already on an incognito tab (switching between incognito tabs)
            val currentTab = if (activeTabIndex >= 0 && activeTabIndex < tabs.size) tabs[activeTabIndex] else null
            if (currentTab?.isIncognito == true) {
                // Already authenticated, allow switch
                switchToTabInternal(index)
                return
            }

            if (!biometricAuthHelper.isBiometricAvailable()) {
                // Biometric not available, show message and don't switch
                Toast.makeText(this, "Biometric authentication required to access incognito tabs", Toast.LENGTH_SHORT).show()
                return
            }

            // Show biometric prompt
            biometricAuthHelper.authenticate(
                title = "Unlock Incognito Tab",
                subtitle = "Use biometric to access incognito tab",
                onSuccess = {
                    // Authentication successful, switch to tab
                    switchToTabInternal(index)
                },
                onError = { errorMessage ->
                    Toast.makeText(this, "Authentication error: $errorMessage", Toast.LENGTH_SHORT).show()
                },
                onCancel = {
                    // User cancelled, don't switch tab
                    Toast.makeText(this, "Tab switch cancelled", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            // No biometric lock required, switch tab normally
            switchToTabInternal(index)
        }
    }

    private fun switchToTabInternal(index: Int) {
        if (index < 0 || index >= tabs.size) return

        // Save current webview state
        if (tabs.isNotEmpty() && activeTabIndex < tabs.size) {
            tabs[activeTabIndex].isActive = false
        }

        activeTabIndex = index
        tabs[activeTabIndex].isActive = true

        // Switch webview
        val tab = tabs[activeTabIndex]
        val tabWebView = tabWebViews[tab.id]

        if (tabWebView != null) {
            swipeRefresh.removeAllViews()
            swipeRefresh.addView(tabWebView)
            webView = tabWebView
            urlEditText.setText(tab.url)
            updateNavigationButtons()
        }

        updateTabBar()
        updateTabCount()
    }

    private fun closeTab(index: Int) {
        if (tabs.size <= 1) {
            // Don't close last tab, just go home
            loadUrl(homeUrl)
            return
        }

        val tab = tabs[index]
        // Remove WebView from parent before destroying to prevent memory leak
        tabWebViews[tab.id]?.let { webView ->
            // Clear data for incognito tabs to maintain privacy
            if (tab.isIncognito) {
                webView.clearCache(true)
                webView.clearFormData()
                webView.clearHistory()
                // Clear WebStorage (localStorage/sessionStorage) for this WebView
                android.webkit.WebStorage.getInstance().deleteAllData()
            }
            (webView.parent as? android.view.ViewGroup)?.removeView(webView)
            webView.destroy()
        }
        tabWebViews.remove(tab.id)
        tabs.removeAt(index)

        if (activeTabIndex >= tabs.size) {
            activeTabIndex = tabs.size - 1
        } else if (index < activeTabIndex) {
            activeTabIndex--
        }

        switchToTab(activeTabIndex)
    }

    private fun closeAllIncognitoTabs() {
        val incognitoCount = tabs.count { it.isIncognito }

        if (incognitoCount == 0) {
            Toast.makeText(this, "No incognito tabs to close", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Close Incognito Tabs?")
            .setMessage("Close all $incognitoCount incognito tab${if (incognitoCount != 1) "s" else ""}?")
            .setPositiveButton("Close") { _, _ ->
                // Close incognito tabs from end to beginning to avoid index issues
                for (i in tabs.size - 1 downTo 0) {
                    if (tabs[i].isIncognito) {
                        val tab = tabs[i]
                        tabWebViews[tab.id]?.apply {
                            // Clear WebView data for incognito tabs
                            clearCache(true)
                            clearFormData()
                            clearHistory()
                            // Remove from parent before destroying to prevent memory leak
                            (parent as? android.view.ViewGroup)?.removeView(this)
                            destroy()
                        }
                        // Clear WebStorage for all incognito tabs
                        android.webkit.WebStorage.getInstance().deleteAllData()
                        tabWebViews.remove(tab.id)
                        tabs.removeAt(i)

                        // Adjust active tab index if necessary
                        if (i < activeTabIndex) {
                            activeTabIndex--
                        } else if (i == activeTabIndex) {
                            // Active tab was closed, will switch to another
                            activeTabIndex = -1
                        }
                    }
                }

                // Create a new tab if all tabs were closed
                if (tabs.isEmpty()) {
                    createNewTab(homeUrl)
                } else {
                    // Switch to valid tab if active was closed
                    if (activeTabIndex < 0 || activeTabIndex >= tabs.size) {
                        activeTabIndex = minOf(activeTabIndex, tabs.size - 1).coerceAtLeast(0)
                    }
                    switchToTab(activeTabIndex)
                }

                // Clear incognito cookies and cache
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()

                Toast.makeText(this, "Closed $incognitoCount incognito tab${if (incognitoCount != 1) "s" else ""}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateCurrentTabUrl(url: String) {
        if (activeTabIndex < tabs.size) {
            tabs[activeTabIndex].url = url
        }
    }

    private fun updateCurrentTabTitle(title: String) {
        if (activeTabIndex < tabs.size) {
            tabs[activeTabIndex].title = title
            updateTabBar()
        }
    }

    private fun updateTabBar() {
        tabContainer.removeAllViews()

        tabs.forEachIndexed { index, tab ->
            val tabView = LayoutInflater.from(this).inflate(R.layout.tab_item, tabContainer, false)

            val incognitoIcon = tabView.findViewById<TextView>(R.id.incognitoIcon)
            val titleText = tabView.findViewById<TextView>(R.id.tabTitle)
            val closeButton = tabView.findViewById<ImageButton>(R.id.tabCloseButton)

            // Show incognito icon for incognito tabs
            incognitoIcon.visibility = if (tab.isIncognito) View.VISIBLE else View.GONE

            titleText.text = tab.title
            tabView.isSelected = index == activeTabIndex

            tabView.setOnClickListener {
                switchToTab(index)
            }

            closeButton.setOnClickListener {
                closeTab(index)
            }

            tabContainer.addView(tabView)
        }
    }

    private fun updateTabCount() {
        tabCountText.text = tabs.size.toString()
    }

    private fun toggleTabBar() {
        val tabBar = findViewById<LinearLayout>(R.id.tabBar)
        tabBar.visibility = if (tabBar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    // Menu
    private fun showMenu() {
        val popup = PopupMenu(this, menuButton)
        popup.menuInflater.inflate(R.menu.browser_menu, popup.menu)

        // Update bookmark item text
        val currentUrl = webView.url ?: ""
        val bookmarkItem = popup.menu.findItem(R.id.menu_bookmark)
        bookmarkItem.title = if (bookmarkManager.isBookmarked(currentUrl)) {
            "Remove bookmark"
        } else {
            "Add bookmark"
        }

        // Update desktop mode checkbox
        popup.menu.findItem(R.id.menu_desktop_site).isChecked = desktopMode

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_new_tab -> {
                    createNewTab(homeUrl)
                    true
                }
                R.id.menu_new_incognito_tab -> {
                    createNewTab(homeUrl, isIncognito = true)
                    Toast.makeText(this, "Incognito tab created", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_close_incognito_tabs -> {
                    closeAllIncognitoTabs()
                    true
                }
                R.id.menu_bookmark -> {
                    toggleBookmark()
                    true
                }
                R.id.menu_bookmarks -> {
                    showBookmarks()
                    true
                }
                R.id.menu_history -> {
                    showHistory()
                    true
                }
                R.id.menu_downloads -> {
                    showDownloads()
                    true
                }
                R.id.menu_privacy_dashboard -> {
                    showPrivacyDashboard()
                    true
                }
                R.id.menu_find -> {
                    showFindBar()
                    true
                }
                R.id.menu_share -> {
                    shareCurrentPage()
                    true
                }
                R.id.menu_desktop_site -> {
                    toggleDesktopMode()
                    true
                }
                R.id.menu_settings -> {
                    showSettings()
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    // Bookmarks
    private fun toggleBookmark() {
        val url = webView.url ?: return
        val title = webView.title ?: url

        if (bookmarkManager.isBookmarked(url)) {
            bookmarkManager.removeBookmark(url)
            Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show()
        } else {
            bookmarkManager.addBookmark(Bookmark(url = url, title = title))
            Toast.makeText(this, "Bookmark added", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBookmarks() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_bookmarks, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.bookmarksList)
        val emptyText = dialogView.findViewById<TextView>(R.id.emptyText)

        val bookmarks = bookmarkManager.getBookmarks()

        val dialog = AlertDialog.Builder(this, R.style.Theme_CleanFindingBrowser_Dialog)
            .setView(dialogView)
            .create()

        if (bookmarks.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyText.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyText.visibility = View.GONE

            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = BookmarkAdapter(bookmarks, { bookmark ->
                loadUrl(bookmark.url)
                dialog.dismiss()
            }, { bookmark ->
                bookmarkManager.removeBookmark(bookmark.url)
                showBookmarks()
                dialog.dismiss()
            })
        }

        dialog.show()
    }

    private fun showHistory() {
        val intent = Intent(this, HistoryActivity::class.java)
        startActivityForResult(intent, REQUEST_HISTORY)
    }

    private fun showDownloads() {
        val intent = Intent(this, DownloadsActivity::class.java)
        startActivityForResult(intent, REQUEST_DOWNLOADS)
    }

    private fun showPrivacyDashboard() {
        val intent = Intent(this, PrivacyDashboardActivity::class.java)
        startActivity(intent)
    }

    private fun showSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivityForResult(intent, REQUEST_SETTINGS)
    }

    // Find in page
    private fun showFindBar() {
        findBar.visibility = View.VISIBLE
        findEditText.requestFocus()
        findEditText.text.clear()
        findResultsText.text = "0/0"
    }

    private fun closeFindBar() {
        findBar.visibility = View.GONE
        webView.clearMatches()
        findEditText.text.clear()
    }

    private fun performFind() {
        val query = findEditText.text.toString()
        if (query.isNotEmpty()) {
            webView.findAllAsync(query)
        }
    }

    // Share
    private fun shareCurrentPage() {
        val url = webView.url ?: return
        val title = webView.title ?: "Check this out"

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title\n$url")
        }

        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    // Desktop mode - Chrome-like implementation
    private fun toggleDesktopMode() {
        desktopMode = !desktopMode
        setupWebView(webView)

        // Apply or restore viewport based on mode
        if (desktopMode) {
            injectDesktopModeScript(webView)
        } else {
            restoreMobileViewport(webView)
        }

        webView.reload()
        Toast.makeText(
            this,
            if (desktopMode) "Desktop site • Viewing full site" else "Mobile site",
            Toast.LENGTH_SHORT
        ).show()
    }

    // Inject JavaScript to force desktop viewport (Chrome-like behavior)
    private fun injectDesktopModeScript(view: WebView?) {
        val script = """
            (function() {
                // Remove or modify viewport meta tag to force desktop layout
                var viewport = document.querySelector('meta[name="viewport"]');
                if (viewport) {
                    // Store original viewport for restoration
                    viewport.setAttribute('data-original-content', viewport.getAttribute('content'));
                    // Set desktop-style viewport (width=1024 is common desktop breakpoint)
                    viewport.setAttribute('content', 'width=1024, initial-scale=1.0, user-scalable=yes');
                } else {
                    // Create viewport meta if it doesn't exist
                    var meta = document.createElement('meta');
                    meta.name = 'viewport';
                    meta.content = 'width=1024, initial-scale=1.0, user-scalable=yes';
                    document.head.appendChild(meta);
                }

                // Force desktop media queries by setting a wide screen width
                // This tricks CSS media queries into thinking we're on desktop
                try {
                    var style = document.createElement('style');
                    style.id = 'cleanfinding-desktop-mode';
                    style.textContent = '/* Desktop mode active */';
                    document.head.appendChild(style);
                } catch(e) {}

                console.log('CleanFinding: Desktop mode viewport applied');
            })();
        """.trimIndent()

        view?.evaluateJavascript(script, null)
    }

    // Restore mobile viewport when switching back
    private fun restoreMobileViewport(view: WebView?) {
        val script = """
            (function() {
                var viewport = document.querySelector('meta[name="viewport"]');
                if (viewport && viewport.hasAttribute('data-original-content')) {
                    viewport.setAttribute('content', viewport.getAttribute('data-original-content'));
                    viewport.removeAttribute('data-original-content');
                }

                // Remove desktop mode style
                var desktopStyle = document.getElementById('cleanfinding-desktop-mode');
                if (desktopStyle) desktopStyle.remove();

                console.log('CleanFinding: Mobile viewport restored');
            })();
        """.trimIndent()

        view?.evaluateJavascript(script, null)
    }

    private fun loadUrl(input: String) {
        var url = input.trim()

        if (!url.contains(".") || url.contains(" ")) {
            url = "https://cleanfinding.com/search?q=${Uri.encode(url)}"
        } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        // SECURITY: Block dangerous URL schemes (javascript:, data:, file:, etc.)
        if (!isUrlSchemeAllowed(url)) {
            showBlockedMessage(url)
            return
        }

        if (isBlockedUrl(url)) {
            showBlockedMessage(url)
            return
        }

        // Check if Duck Player is enabled and this is a YouTube URL
        if (preferencesManager.getDuckPlayer() && duckPlayerHandler.isYouTubeUrl(url)) {
            val privacyUrl = duckPlayerHandler.convertToPrivacyUrl(url)
            if (privacyUrl != null) {
                webView.loadUrl(privacyUrl)
                return
            }
        }

        url = enforceSafeSearch(url)
        webView.loadUrl(url)
    }

    private fun isBlockedUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()

        for (domain in blockedDomains) {
            if (lowerUrl.contains(domain)) {
                return true
            }
        }

        for (keyword in adultDomains) {
            if (lowerUrl.contains(keyword)) {
                return true
            }
        }

        return false
    }

    private fun enforceSafeSearch(url: String): String {
        var safeUrl = url

        if (url.contains("google.") && url.contains("/search")) {
            safeUrl = if (url.contains("safe=")) {
                url.replace(Regex("safe=[^&]*"), "safe=active")
            } else {
                if (url.contains("?")) "$url&safe=active" else "$url?safe=active"
            }
        }

        if (url.contains("bing.com") && url.contains("/search")) {
            safeUrl = if (url.contains("safeSearch=")) {
                url.replace(Regex("safeSearch=[^&]*"), "safeSearch=Strict")
            } else {
                if (url.contains("?")) "$url&safeSearch=Strict" else "$url?safeSearch=Strict"
            }
        }

        if (url.contains("duckduckgo.com")) {
            safeUrl = if (url.contains("kp=")) {
                url.replace(Regex("kp=[^&]*"), "kp=1")
            } else {
                if (url.contains("?")) "$url&kp=1" else "$url?kp=1"
            }
        }

        return safeUrl
    }

    private fun injectBlockingScript(view: WebView?) {
        // Escape blocked domains to prevent JavaScript injection vulnerabilities
        val escapedDomains = blockedDomains.map { escapeJavaScriptString(it) }
        val script = """
            (function() {
                // CRITICAL: Skip ad blocking on CleanFinding.com to prevent breaking search functionality
                // The ad blocking CSS selectors are too broad and can hide legitimate page elements
                if (window.location.hostname.indexOf('cleanfinding.com') !== -1) {
                    console.log('CleanFinding: Skipping ad blocking on cleanfinding.com');
                    return;
                }

                var blockedDomains = ${escapedDomains.joinToString(",", "[", "]") { "\"$it\"" }};

                var originalXHR = window.XMLHttpRequest;
                window.XMLHttpRequest = function() {
                    var xhr = new originalXHR();
                    var originalOpen = xhr.open;
                    xhr.open = function(method, url) {
                        for (var i = 0; i < blockedDomains.length; i++) {
                            if (url && url.indexOf(blockedDomains[i]) !== -1) {
                                console.log('CleanFinding: Blocked tracker - ' + url);
                                return;
                            }
                        }
                        return originalOpen.apply(this, arguments);
                    };
                    return xhr;
                };

                var originalFetch = window.fetch;
                window.fetch = function(url, options) {
                    for (var i = 0; i < blockedDomains.length; i++) {
                        if (url && url.toString().indexOf(blockedDomains[i]) !== -1) {
                            console.log('CleanFinding: Blocked fetch - ' + url);
                            return Promise.reject(new Error('Blocked by CleanFinding'));
                        }
                    }
                    return originalFetch.apply(this, arguments);
                };

                // Enhanced ad blocking selectors
                var adSelectors = [
                    // Generic ad containers
                    '[class*="ad-"]', '[class*="ads-"]', '[id*="ad-"]', '[id*="ads-"]',
                    '[class*="advertisement"]', '[class*="advert"]',
                    '[class*="sponsored"]', '[class*="sponsor-"]',
                    '[data-ad]', '[data-ads]', '[data-advertisement]',

                    // Google Ads
                    'ins.adsbygoogle', '.google-ad', '.doubleclick-ad',
                    '#google_ads_iframe', '[id*="google_ads"]',
                    '.ad-slot', '.ad-container', '.ad-banner',

                    // Common ad networks
                    '[class*="taboola"]', '[id*="taboola"]',
                    '[class*="outbrain"]', '[id*="outbrain"]',
                    '[class*="adsbymedia"]', '[id*="adsbymedia"]',

                    // Sponsored content
                    '[class*="promo"]', '[class*="promotional"]',
                    'article[class*="sponsor"]', 'div[class*="sponsor"]',

                    // Ad frames and iframes
                    'iframe[src*="ads"]', 'iframe[src*="doubleclick"]',
                    'iframe[src*="googlesyndication"]',

                    // Native ads
                    '[class*="native-ad"]', '[class*="native_ad"]',
                    '[data-native-ad]', '[data-sponsored]',

                    // Video ads
                    '[class*="video-ad"]', '[class*="preroll"]',
                    '[class*="midroll"]', '[class*="postroll"]'
                ];

                // Inject CSS for more aggressive ad blocking
                var adBlockStyle = document.createElement('style');
                adBlockStyle.id = 'cleanfinding-ad-block';
                adBlockStyle.textContent = `
                    /* Hide ad containers */
                    [class*="ad-"], [class*="ads-"], [id*="ad-"], [id*="ads-"],
                    [class*="advertisement"], [class*="sponsored"],
                    ins.adsbygoogle, .google-ad, .doubleclick-ad {
                        display: none !important;
                        visibility: hidden !important;
                        opacity: 0 !important;
                        height: 0 !important;
                        width: 0 !important;
                        position: absolute !important;
                        left: -9999px !important;
                    }

                    /* Remove ad placeholders */
                    [data-ad], [data-ads], [data-advertisement] {
                        display: none !important;
                    }

                    /* Hide Taboola and Outbrain */
                    [class*="taboola"], [id*="taboola"],
                    [class*="outbrain"], [id*="outbrain"] {
                        display: none !important;
                    }
                `;

                if (!document.getElementById('cleanfinding-ad-block')) {
                    document.head.appendChild(adBlockStyle);
                }

                // Function to remove ads
                function removeAds() {
                    var removed = 0;
                    adSelectors.forEach(function(selector) {
                        try {
                            document.querySelectorAll(selector).forEach(function(el) {
                                if (el && el.parentNode) {
                                    el.remove();
                                    removed++;
                                }
                            });
                        } catch (e) {
                            // Continue with next selector
                        }
                    });
                    if (removed > 0) {
                        console.log('CleanFinding: Removed ' + removed + ' ad elements');
                    }
                }

                // Remove ads immediately
                removeAds();

                // Continue removing ads periodically
                setInterval(removeAds, 2000);

                // Monitor for dynamically added ads
                var adObserver = new MutationObserver(function(mutations) {
                    var checkAds = false;
                    mutations.forEach(function(mutation) {
                        if (mutation.addedNodes.length) {
                            mutation.addedNodes.forEach(function(node) {
                                if (node.nodeType === 1) {
                                    var nodeClass = (node.className || '').toString().toLowerCase();
                                    var nodeId = (node.id || '').toLowerCase();
                                    if (nodeClass.includes('ad') || nodeClass.includes('sponsor') ||
                                        nodeId.includes('ad') || nodeId.includes('sponsor')) {
                                        checkAds = true;
                                    }
                                }
                            });
                        }
                    });
                    if (checkAds) {
                        setTimeout(removeAds, 100);
                    }
                });

                adObserver.observe(document.body, {
                    childList: true,
                    subtree: true
                });
            })();
        """.trimIndent()

        view?.evaluateJavascript(script, null)
    }

    private fun injectVideoFixCSS(view: WebView?) {
        // CRITICAL FIX: Enhanced video and image rendering fixes with dynamic monitoring
        val cssScript = """
            (function() {
                console.log('CleanFinding: Applying video/image fixes...');

                // Inject CSS fixes
                var style = document.createElement('style');
                style.id = 'cleanfinding-video-fix';
                style.textContent = `
                    /* Force video visibility and proper layering */
                    video, .video-stream {
                        opacity: 1 !important;
                        visibility: visible !important;
                        display: block !important;
                        background-color: #000 !important;
                        width: 100% !important;
                        height: 100% !important;
                        object-fit: contain !important;
                        z-index: 1 !important;
                        position: relative !important;
                    }

                    /* YouTube mobile player fixes */
                    .html5-video-player,
                    .html5-video-container,
                    #player-container-inner,
                    #movie_player {
                        width: 100% !important;
                        height: auto !important;
                        min-height: 200px !important;
                        opacity: 1 !important;
                        visibility: visible !important;
                        position: relative !important;
                    }

                    /* Force YouTube video to show */
                    ytm-single-column-watch-next-results-renderer,
                    .watch-below-the-player {
                        margin-top: 0 !important;
                    }

                    /* Fix iframe embedding */
                    iframe[src*="youtube"], iframe[src*="video"] {
                        max-width: 100% !important;
                        height: auto !important;
                        min-height: 200px !important;
                        visibility: visible !important;
                    }

                    /* Fix image rendering */
                    img {
                        max-width: 100% !important;
                        height: auto !important;
                        display: block !important;
                        object-fit: contain !important;
                    }

                    /* Pinterest image fixes */
                    [data-test-id="pin-image"], .GrowthUnauthPinImage {
                        width: 100% !important;
                        height: auto !important;
                        object-fit: contain !important;
                    }

                    /* Prevent content overflow */
                    body {
                        overflow-x: hidden !important;
                    }
                `;
                document.head.appendChild(style);

                // Function to force video element visibility
                function forceVideoVisibility() {
                    // Find all video elements
                    var videos = document.querySelectorAll('video');
                    videos.forEach(function(video) {
                        video.style.opacity = '1';
                        video.style.visibility = 'visible';
                        video.style.display = 'block';
                        video.style.backgroundColor = '#000';

                        // Force repaint
                        video.offsetHeight;

                        // Try to play if paused (for autoplay videos)
                        if (video.paused && video.autoplay) {
                            video.play().catch(function() {});
                        }
                    });

                    // Fix YouTube player containers
                    var players = document.querySelectorAll('.html5-video-player, #movie_player');
                    players.forEach(function(player) {
                        player.style.opacity = '1';
                        player.style.visibility = 'visible';
                        player.style.position = 'relative';
                    });
                }

                // Apply fixes immediately
                forceVideoVisibility();

                // Monitor for dynamically loaded videos (YouTube loads content dynamically)
                var observer = new MutationObserver(function(mutations) {
                    mutations.forEach(function(mutation) {
                        if (mutation.addedNodes.length) {
                            mutation.addedNodes.forEach(function(node) {
                                if (node.tagName === 'VIDEO' ||
                                    (node.querySelector && node.querySelector('video'))) {
                                    setTimeout(forceVideoVisibility, 100);
                                }
                            });
                        }
                    });
                });

                // Start observing document for changes
                observer.observe(document.body, {
                    childList: true,
                    subtree: true
                });

                // Re-apply fixes periodically for first 5 seconds (for slow-loading pages)
                var fixCount = 0;
                var fixInterval = setInterval(function() {
                    forceVideoVisibility();
                    fixCount++;
                    if (fixCount >= 10) {
                        clearInterval(fixInterval);
                    }
                }, 500);

                console.log('CleanFinding: Video/Image rendering fixes applied with monitoring');
            })();
        """.trimIndent()

        view?.evaluateJavascript(cssScript, null)
    }

    private fun showBlockedMessage(url: String) {
        AlertDialog.Builder(this)
            .setTitle("Content Blocked")
            .setMessage("This content has been blocked by CleanFinding Browser for your safety.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Go Home") { _, _ ->
                loadUrl(homeUrl)
            }
            .show()
    }

    private fun updateNavigationButtons() {
        backButton.isEnabled = webView.canGoBack()
        backButton.alpha = if (webView.canGoBack()) 1.0f else 0.5f
        forwardButton.isEnabled = webView.canGoForward()
        forwardButton.alpha = if (webView.canGoForward()) 1.0f else 0.5f
    }

    private fun updatePrivacyGrade(url: String) {
        // Calculate privacy grade
        val privacyScore = privacyGradeCalculator.calculateGrade(
            url = url,
            trackersBlocked = currentPageTrackersBlocked,
            blockedDomains = currentPageBlockedDomains
        )

        // Update HTTPS lock icon
        if (privacyScore.isHttps) {
            httpsIcon.visibility = View.VISIBLE
        } else {
            httpsIcon.visibility = View.GONE
        }

        // Update privacy grade badge
        privacyGradeBadge.visibility = View.VISIBLE
        privacyGradeBadge.text = privacyScore.grade

        // Set badge background color based on grade
        val backgroundColor = android.graphics.Color.parseColor(privacyScore.color)
        val drawable = privacyGradeBadge.background
        if (drawable is android.graphics.drawable.GradientDrawable) {
            drawable.setColor(backgroundColor)
        }

        // Make badge clickable to show details
        privacyGradeBadge.setOnClickListener {
            showPrivacyDetails(privacyScore)
        }

        // Record privacy stats for dashboard
        privacyStatsManager.recordPageStats(
            url = url,
            trackersBlocked = currentPageTrackersBlocked,
            blockedDomains = currentPageBlockedDomains,
            privacyGrade = privacyScore.grade,
            isHttps = privacyScore.isHttps
        )
    }

    private fun showPrivacyDetails(privacyScore: PrivacyGradeCalculator.PrivacyScore) {
        val detailsReport = privacyGradeCalculator.getDetailedReport(privacyScore)

        AlertDialog.Builder(this)
            .setTitle("${privacyGradeCalculator.getGradeEmoji(privacyScore.grade)} Privacy Report")
            .setMessage(detailsReport)
            .setPositiveButton("OK", null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            findBar.visibility == View.VISIBLE -> closeFindBar()
            webView.canGoBack() -> webView.goBack()
            else -> super.onBackPressed()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.toString()?.let { loadUrl(it) }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_HISTORY -> {
                if (resultCode == RESULT_OK) {
                    data?.getStringExtra("url")?.let { url ->
                        loadUrl(url)
                    }
                }
            }
            REQUEST_DOWNLOADS -> {
                if (resultCode == RESULT_OK) {
                    data?.getStringExtra("url")?.let { url ->
                        loadUrl(url)
                    }
                }
            }
            REQUEST_SETTINGS -> {
                if (resultCode == RESULT_OK) {
                    // Settings were changed, reload all WebViews with new settings
                    tabWebViews.values.forEach { wv ->
                        val tab = tabs.find { tabWebViews[it.id] == wv }
                        setupWebView(wv, tab?.isIncognito ?: false)
                    }
                    webView.reload()
                    Toast.makeText(this, "Settings applied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up WebViews (remove from parent first to prevent memory leak)
        tabWebViews.values.forEach { webView ->
            (webView.parent as? android.view.ViewGroup)?.removeView(webView)
            webView.destroy()
        }

        // Clean up download manager (prevents BroadcastReceiver leak)
        downloadManager.cleanup()

        // Clean up history manager (cancels coroutine scope)
        historyManager.cleanup()

        // Clear custom view callback to prevent leak
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        customView = null
    }
}

// Bookmark Adapter
class BookmarkAdapter(
    private val bookmarks: List<Bookmark>,
    private val onItemClick: (Bookmark) -> Unit,
    private val onDeleteClick: (Bookmark) -> Unit
) : RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.bookmarkTitle)
        val urlText: TextView = view.findViewById(R.id.bookmarkUrl)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bookmark, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bookmark = bookmarks[position]
        holder.titleText.text = bookmark.title
        holder.urlText.text = bookmark.url
        holder.itemView.setOnClickListener { onItemClick(bookmark) }
        holder.deleteButton.setOnClickListener { onDeleteClick(bookmark) }
    }

    override fun getItemCount() = bookmarks.size
}
