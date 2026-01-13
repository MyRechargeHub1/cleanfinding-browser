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

    private lateinit var bookmarkManager: BookmarkManager

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

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bookmarkManager = BookmarkManager(this)

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
        customViewContainer = findViewById(R.id.customViewContainer)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(wv: WebView) {
        // CRITICAL FIX: Enable hardware acceleration for video playback
        wv.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            safeBrowsingEnabled = true

            // CRITICAL FIX: Enable video playback without user gesture
            mediaPlaybackRequiresUserGesture = false

            // CRITICAL FIX: Better layout algorithm for images and content
            layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING

            // CRITICAL FIX: Enable all media features
            javaScriptCanOpenWindowsAutomatically = false
            loadsImagesAutomatically = true
            blockNetworkImage = false

            // CRITICAL FIX: Set viewport meta tag support
            setSupportMultipleWindows(false)

            userAgentString = if (desktopMode) {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 CleanFindingBrowser/1.0"
            } else {
                userAgentString.replace("; wv", "") + " CleanFindingBrowser/1.0"
            }
        }

        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false

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
                    progressBar.visibility = View.VISIBLE
                    urlEditText.setText(url)
                    updateNavigationButtons()
                    updateCurrentTabUrl(url ?: homeUrl)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (view == webView) {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    updateNavigationButtons()
                    injectBlockingScript(view)
                    injectVideoFixCSS(view)
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
    private fun createNewTab(url: String) {
        val tab = Tab(url = url, title = "New Tab")
        tabs.add(tab)

        val newWebView = WebView(this)
        setupWebView(newWebView)
        tabWebViews[tab.id] = newWebView

        switchToTab(tabs.size - 1)
        loadUrl(url)
        updateTabBar()
    }

    private fun switchToTab(index: Int) {
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
        tabWebViews[tab.id]?.destroy()
        tabWebViews.remove(tab.id)
        tabs.removeAt(index)

        if (activeTabIndex >= tabs.size) {
            activeTabIndex = tabs.size - 1
        } else if (index < activeTabIndex) {
            activeTabIndex--
        }

        switchToTab(activeTabIndex)
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

            val titleText = tabView.findViewById<TextView>(R.id.tabTitle)
            val closeButton = tabView.findViewById<ImageButton>(R.id.tabCloseButton)

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
                R.id.menu_bookmark -> {
                    toggleBookmark()
                    true
                }
                R.id.menu_bookmarks -> {
                    showBookmarks()
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
                    Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show()
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

    // Desktop mode
    private fun toggleDesktopMode() {
        desktopMode = !desktopMode
        setupWebView(webView)
        webView.reload()
        Toast.makeText(this, if (desktopMode) "Desktop mode enabled" else "Mobile mode enabled", Toast.LENGTH_SHORT).show()
    }

    private fun loadUrl(input: String) {
        var url = input.trim()

        if (!url.contains(".") || url.contains(" ")) {
            url = "https://cleanfinding.com/search?q=${Uri.encode(url)}"
        } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        if (isBlockedUrl(url)) {
            showBlockedMessage(url)
            return
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
        val script = """
            (function() {
                var blockedDomains = ${blockedDomains.joinToString(",", "[", "]") { "\"$it\"" }};

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

                var adSelectors = [
                    '[class*="ad-"]', '[class*="ads-"]', '[id*="ad-"]', '[id*="ads-"]',
                    '[class*="advertisement"]', '[class*="sponsored"]',
                    'ins.adsbygoogle', '.google-ad', '.doubleclick-ad'
                ];

                function removeAds() {
                    adSelectors.forEach(function(selector) {
                        document.querySelectorAll(selector).forEach(function(el) {
                            el.style.display = 'none';
                        });
                    });
                }

                removeAds();
                setInterval(removeAds, 2000);
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

    override fun onDestroy() {
        super.onDestroy()
        tabWebViews.values.forEach { it.destroy() }
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
