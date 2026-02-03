package com.cleanfinding.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.speech.RecognizerIntent
import android.util.Rational
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlEditText: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var backButton: ImageButton
    private lateinit var forwardButton: ImageButton
    private lateinit var homeButton: ImageButton
    private lateinit var refreshButton: ImageButton
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

    // Scrolling toolbar support
    private lateinit var appBarLayout: com.google.android.material.appbar.AppBarLayout
    private var lastScrollY = 0
    private var isToolbarVisible = true
    private val SCROLL_THRESHOLD = 10  // Minimum scroll distance to trigger hide/show

    // Picture-in-Picture support
    private var isInPipMode = false

    // Reader mode support
    private var isReaderMode = false
    private var originalHtml: String? = null

    // Zoom level (percentage)
    private var currentZoom = 100

    // Gesture detection for swipe navigation
    private lateinit var gestureDetector: GestureDetector
    private val SWIPE_THRESHOLD = 100
    private val SWIPE_VELOCITY_THRESHOLD = 100

    // Voice search launcher
    private val voiceSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val query = matches[0]
                urlEditText.setText(query)
                navigateToUrl(query)
            }
        }
    }

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
        setupGestureDetector()

        // Create initial tab
        createNewTab(intent?.data?.toString() ?: homeUrl)
    }

    /**
     * Setup gesture detector for swipe navigation (back/forward)
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false

                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                // Only detect horizontal swipes (ignore vertical scrolling)
                if (abs(diffX) > abs(diffY) && abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        // Swipe right -> Go back
                        if (webView.canGoBack()) {
                            webView.goBack()
                            return true
                        }
                    } else {
                        // Swipe left -> Go forward
                        if (webView.canGoForward()) {
                            webView.goForward()
                            return true
                        }
                    }
                }
                return false
            }
        })
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        urlEditText = findViewById(R.id.urlEditText)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        backButton = findViewById(R.id.backButton)
        forwardButton = findViewById(R.id.forwardButton)
        homeButton = findViewById(R.id.homeButton)
        refreshButton = findViewById(R.id.refreshButton)
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
        appBarLayout = findViewById(R.id.appBarLayout)
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun setupWebView(wv: WebView, isIncognito: Boolean = false) {
        // CRITICAL FIX: Enable hardware acceleration for video playback
        wv.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // Enable nested scrolling for CoordinatorLayout/AppBarLayout integration
        wv.isNestedScrollingEnabled = true

        // SWIPE NAVIGATION: Detect horizontal swipes for back/forward navigation
        wv.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false // Don't consume the event, let WebView handle it too
        }

        // CHROME-LIKE: Add scroll listener to hide/show toolbar based on scroll direction
        wv.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val diff = scrollY - oldScrollY

            // Only respond to significant scroll changes
            if (Math.abs(diff) > SCROLL_THRESHOLD) {
                if (diff > 0 && isToolbarVisible) {
                    // Scrolling DOWN - hide toolbar
                    hideToolbar()
                } else if (diff < 0 && !isToolbarVisible) {
                    // Scrolling UP - show toolbar
                    showToolbar()
                }
            }

            // Always show toolbar when at top of page
            if (scrollY == 0 && !isToolbarVisible) {
                showToolbar()
            }

            lastScrollY = scrollY
        }

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
            allowContentAccess = true  // CRITICAL: Enable for Pinterest/YouTube content loading

            // CRITICAL FIX: Allow mixed content for sites like Pinterest that may have HTTP resources
            // This is needed for proper image/video loading on many sites
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

            safeBrowsingEnabled = true

            // CRITICAL FIX: Enable modern media features for YouTube/Pinterest
            @Suppress("DEPRECATION")
            setGeolocationEnabled(false)  // Privacy: disable location

            // Enable media playback inline (for iOS-style playsinline behavior)
            // This is crucial for proper video rendering on mobile
            mediaPlaybackRequiresUserGesture = false

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

                // CHROME-LIKE: Set initial scale to fit 1024px desktop content on screen
                val displayMetrics = resources.displayMetrics
                val screenWidthPx = displayMetrics.widthPixels
                val desktopWidth = 1024
                val initialScale = ((screenWidthPx.toFloat() / desktopWidth) * 100).toInt()
                wv.setInitialScale(initialScale)
            } else {
                // Reset initial scale for mobile mode
                wv.setInitialScale(0)
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

            // CRITICAL FIX: Prevent popups but allow necessary window operations
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)

            // CRITICAL FIX: Set default text encoding for proper character display
            defaultTextEncodingName = "UTF-8"

            // CHROME-LIKE: Set user agent string that matches Chrome browser
            // This is critical for YouTube - it detects WebView and serves degraded experience
            userAgentString = if (desktopMode) {
                // Chrome 120 on Windows 10 - matches what Chrome uses for "Request desktop site"
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            } else {
                // CHROME MOBILE user agent - NOT WebView user agent
                // This makes YouTube serve the proper mobile experience with working video
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
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

            // CHROME-LIKE: Return a default video poster (black frame) like Chrome does
            override fun getDefaultVideoPoster(): android.graphics.Bitmap? {
                // Return a 1x1 black pixel bitmap as default video poster
                // This prevents the white/gray placeholder that causes "black screen" appearance
                return android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888).apply {
                    eraseColor(android.graphics.Color.BLACK)
                }
            }

            // CHROME-LIKE: Return a loading view for video (prevents black screen during load)
            override fun getVideoLoadingProgressView(): View? {
                // Create a simple black view with a loading indicator
                val loadingView = FrameLayout(this@MainActivity)
                loadingView.setBackgroundColor(android.graphics.Color.BLACK)

                val progressBar = android.widget.ProgressBar(this@MainActivity).apply {
                    isIndeterminate = true
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        android.view.Gravity.CENTER
                    )
                }
                loadingView.addView(progressBar)
                return loadingView
            }

            // CHROME-LIKE: Handle media permission requests (camera, microphone)
            override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                request?.let {
                    // Grant video-related permissions for proper playback
                    val resources = it.resources
                    val grantedResources = resources.filter { resource ->
                        resource == android.webkit.PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID ||
                        resource == android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE
                    }.toTypedArray()

                    if (grantedResources.isNotEmpty()) {
                        runOnUiThread {
                            it.grant(grantedResources)
                        }
                    } else {
                        it.deny()
                    }
                }
            }

            // CRITICAL FIX: Enable fullscreen video support for YouTube
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                try {
                    if (customView != null) {
                        callback?.onCustomViewHidden()
                        return
                    }

                    customView = view
                    customViewCallback = callback

                    // CHROME-LIKE: Set the view to use hardware layer for smooth video
                    view?.setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    // CHROME-LIKE: Request landscape orientation for fullscreen video
                    requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

                    // Hide main browser content (use the mainContent container)
                    findViewById<androidx.coordinatorlayout.widget.CoordinatorLayout>(R.id.mainContent)?.visibility = View.GONE

                    // CHROME-LIKE: Make system bars transparent/hidden for immersive video
                    window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    )

                    // Show fullscreen video container
                    customViewContainer.visibility = View.VISIBLE
                    customViewContainer.setBackgroundColor(android.graphics.Color.BLACK)
                    customViewContainer.addView(customView, FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    ))
                } catch (e: Exception) {
                    android.util.Log.e("WebView", "Error showing fullscreen: ${e.message}")
                    // Clean up on error
                    customView = null
                    customViewCallback = null
                }
            }

            override fun onHideCustomView() {
                exitFullscreenMode()
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

        refreshButton.setOnClickListener {
            refreshPage()
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
                R.id.menu_refresh -> {
                    refreshPage()
                    true
                }
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
                R.id.menu_copy_url -> {
                    copyUrlToClipboard()
                    true
                }
                R.id.menu_pip -> {
                    enterPipMode()
                    true
                }
                R.id.menu_reader_mode -> {
                    toggleReaderMode()
                    true
                }
                R.id.menu_zoom -> {
                    showZoomControls()
                    true
                }
                R.id.menu_night_mode -> {
                    toggleNightMode()
                    true
                }
                R.id.menu_translate -> {
                    translatePage()
                    true
                }
                R.id.menu_page_info -> {
                    showPageInfo()
                    true
                }
                R.id.menu_read_aloud -> {
                    toggleTextToSpeech()
                    true
                }
                R.id.menu_clear_site_data -> {
                    clearCurrentSiteData()
                    true
                }
                R.id.menu_voice_search -> {
                    startVoiceSearch()
                    true
                }
                R.id.menu_screenshot -> {
                    takeScreenshot()
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

    // Refresh the current page
    private fun refreshPage() {
        // Show refresh indicator
        swipeRefresh.isRefreshing = true

        // Reload the current page
        webView.reload()

        // Show toolbar when refreshing
        showToolbar()
    }

    /**
     * Hide the toolbar with animation (Chrome-like scroll behavior)
     */
    private fun hideToolbar() {
        if (!isToolbarVisible) return
        isToolbarVisible = false

        appBarLayout.animate()
            .translationY(-appBarLayout.height.toFloat())
            .setDuration(200)
            .start()
    }

    /**
     * Show the toolbar with animation (Chrome-like scroll behavior)
     */
    private fun showToolbar() {
        if (isToolbarVisible) return
        isToolbarVisible = true

        appBarLayout.animate()
            .translationY(0f)
            .setDuration(200)
            .start()

        // The refresh indicator will be hidden in onPageFinished
    }

    /**
     * Exit fullscreen video mode safely
     * This is called from both onHideCustomView() and onBackPressed()
     * to ensure proper cleanup without crashes
     */
    private fun exitFullscreenMode() {
        if (customView == null) {
            return
        }

        try {
            // CHROME-LIKE: Restore orientation
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

            // CHROME-LIKE: Restore system UI
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE

            // Hide fullscreen video container
            customViewContainer.visibility = View.GONE

            // Safely remove the custom view
            customView?.let { view ->
                customViewContainer.removeView(view)
            }

            // Show main browser content
            findViewById<androidx.coordinatorlayout.widget.CoordinatorLayout>(R.id.mainContent)?.visibility = View.VISIBLE

            // Notify the callback that we're done (only once)
            val callback = customViewCallback
            customView = null
            customViewCallback = null

            // Call callback AFTER nulling to prevent re-entry
            callback?.onCustomViewHidden()

        } catch (e: Exception) {
            android.util.Log.e("WebView", "Error exiting fullscreen: ${e.message}")
            // Force cleanup even on error
            customView = null
            customViewCallback = null
            customViewContainer.visibility = View.GONE
            findViewById<androidx.coordinatorlayout.widget.CoordinatorLayout>(R.id.mainContent)?.visibility = View.VISIBLE
        }
    }

    /**
     * Enter Picture-in-Picture mode for video playback
     * Allows users to watch videos in a floating window while using other apps
     */
    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Build PiP parameters with 16:9 aspect ratio (standard video)
                val pipParams = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()

                enterPictureInPictureMode(pipParams)
            } catch (e: Exception) {
                android.util.Log.e("PiP", "Error entering PiP mode: ${e.message}")
                Toast.makeText(this, "Picture-in-Picture not available", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Picture-in-Picture requires Android 8.0+", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handle PiP mode changes
     */
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode

        if (isInPictureInPictureMode) {
            // Entered PiP mode - hide UI elements
            appBarLayout.visibility = View.GONE
        } else {
            // Exited PiP mode - restore UI
            appBarLayout.visibility = View.VISIBLE
            showToolbar()
        }
    }

    /**
     * When user leaves the app during fullscreen video, enter PiP mode
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        // If playing fullscreen video, enter PiP mode when user presses home
        if (customView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPipMode()
        }
    }

    /**
     * Copy current URL to clipboard
     */
    private fun copyUrlToClipboard() {
        val url = webView.url ?: return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("URL", url)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    /**
     * Toggle Reader Mode - extracts and displays article content in a clean format
     */
    private fun toggleReaderMode() {
        if (isReaderMode) {
            // Exit reader mode - reload the page
            isReaderMode = false
            webView.reload()
            Toast.makeText(this, "Reader mode disabled", Toast.LENGTH_SHORT).show()
        } else {
            // Enter reader mode - extract article content
            isReaderMode = true
            injectReaderModeScript()
            Toast.makeText(this, "Reader mode enabled", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Inject JavaScript to extract and display article content in reader mode
     */
    private fun injectReaderModeScript() {
        val readerScript = """
            (function() {
                // Simple article extraction
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
                    // Fallback: get all paragraphs
                    var paragraphs = document.querySelectorAll('p');
                    paragraphs.forEach(function(p) {
                        if (p.textContent.length > 50) {
                            content += '<p>' + p.textContent + '</p>';
                        }
                    });
                }

                if (content.length < 100) {
                    alert('Could not extract article content from this page.');
                    return;
                }

                // Create reader mode HTML
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
            })();
        """.trimIndent()

        webView.evaluateJavascript(readerScript, null)
    }

    /**
     * Show zoom controls dialog
     */
    private fun showZoomControls() {
        val zoomLevels = arrayOf("50%", "75%", "100%", "125%", "150%", "175%", "200%")
        val zoomValues = intArrayOf(50, 75, 100, 125, 150, 175, 200)

        val currentIndex = zoomValues.indexOf(currentZoom).takeIf { it >= 0 } ?: 2

        AlertDialog.Builder(this)
            .setTitle("Zoom Level: $currentZoom%")
            .setSingleChoiceItems(zoomLevels, currentIndex) { dialog, which ->
                currentZoom = zoomValues[which]
                webView.settings.textZoom = currentZoom
                Toast.makeText(this, "Zoom: $currentZoom%", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Zoom in by 25%
     */
    private fun zoomIn() {
        if (currentZoom < 200) {
            currentZoom += 25
            webView.settings.textZoom = currentZoom
            Toast.makeText(this, "Zoom: $currentZoom%", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Zoom out by 25%
     */
    private fun zoomOut() {
        if (currentZoom > 50) {
            currentZoom -= 25
            webView.settings.textZoom = currentZoom
            Toast.makeText(this, "Zoom: $currentZoom%", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Toggle night mode / blue light filter
     */
    private fun toggleNightMode() {
        val nightModeScript = """
            (function() {
                var existingFilter = document.getElementById('cleanfinding-night-mode');
                if (existingFilter) {
                    existingFilter.remove();
                    return 'disabled';
                }

                var style = document.createElement('style');
                style.id = 'cleanfinding-night-mode';
                style.textContent = `
                    html {
                        filter: sepia(30%) brightness(90%) !important;
                        background-color: #1a1a1a !important;
                    }
                    body {
                        background-color: #1a1a1a !important;
                    }
                `;
                document.head.appendChild(style);
                return 'enabled';
            })();
        """.trimIndent()

        webView.evaluateJavascript(nightModeScript) { result ->
            val status = result.replace("\"", "")
            Toast.makeText(
                this,
                if (status == "enabled") "Night mode enabled" else "Night mode disabled",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Translate page using Google Translate
     */
    private fun translatePage() {
        val currentUrl = webView.url ?: return

        // Use Google Translate to translate the page
        val translateUrl = "https://translate.google.com/translate?sl=auto&tl=en&u=${Uri.encode(currentUrl)}"
        webView.loadUrl(translateUrl)
        Toast.makeText(this, "Translating page...", Toast.LENGTH_SHORT).show()
    }

    /**
     * Show page information dialog (SSL, trackers blocked, etc.)
     */
    private fun showPageInfo() {
        val url = webView.url ?: "No URL"
        val title = webView.title ?: "No title"

        // Determine SSL status
        val isHttps = url.startsWith("https://")
        val sslStatus = if (isHttps) "Secure (HTTPS)" else "Not Secure (HTTP)"
        val sslIcon = if (isHttps) "🔒" else "⚠️"

        // Get domain
        val domain = try {
            Uri.parse(url).host ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }

        // Build info message
        val infoMessage = """
            |$sslIcon Connection: $sslStatus
            |
            |📍 Domain: $domain
            |
            |🛡️ Trackers Blocked: $currentPageTrackersBlocked
            |
            |📄 Page Title: $title
            |
            |🔗 Full URL: $url
        """.trimMargin()

        AlertDialog.Builder(this)
            .setTitle("Page Information")
            .setMessage(infoMessage)
            .setPositiveButton("OK", null)
            .setNeutralButton("Copy URL") { _, _ ->
                copyUrlToClipboard()
            }
            .show()
    }

    /**
     * Read page content aloud using Text-to-Speech
     */
    private var textToSpeech: android.speech.tts.TextToSpeech? = null
    private var isSpeaking = false

    private fun toggleTextToSpeech() {
        if (isSpeaking) {
            // Stop speaking
            textToSpeech?.stop()
            isSpeaking = false
            Toast.makeText(this, "Stopped reading", Toast.LENGTH_SHORT).show()
            return
        }

        // Initialize TTS if needed
        if (textToSpeech == null) {
            textToSpeech = android.speech.tts.TextToSpeech(this) { status ->
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    textToSpeech?.language = java.util.Locale.getDefault()
                    extractAndSpeak()
                } else {
                    Toast.makeText(this, "Text-to-Speech not available", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            extractAndSpeak()
        }
    }

    private fun extractAndSpeak() {
        // Extract text content from page
        val extractScript = """
            (function() {
                var article = document.querySelector('article') ||
                              document.querySelector('[role="main"]') ||
                              document.querySelector('main') ||
                              document.body;

                var text = article.innerText || article.textContent || '';
                // Clean up and limit text
                text = text.replace(/\\s+/g, ' ').trim();
                return text.substring(0, 5000); // Limit to 5000 chars
            })();
        """.trimIndent()

        webView.evaluateJavascript(extractScript) { result ->
            val text = result?.replace("\"", "")?.replace("\\n", " ")?.trim() ?: ""
            if (text.isNotEmpty() && text != "null") {
                isSpeaking = true
                textToSpeech?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "page_content")
                Toast.makeText(this, "Reading page...", Toast.LENGTH_SHORT).show()

                // Set listener to know when done
                textToSpeech?.setOnUtteranceCompletedListener {
                    runOnUiThread {
                        isSpeaking = false
                    }
                }
            } else {
                Toast.makeText(this, "No text content found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Clear data for current site only
     */
    private fun clearCurrentSiteData() {
        val url = webView.url ?: return
        val domain = try {
            Uri.parse(url).host ?: return
        } catch (e: Exception) {
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Clear Site Data")
            .setMessage("Clear all data for $domain?\n\nThis includes cookies, cache, and stored data.")
            .setPositiveButton("Clear") { _, _ ->
                // Clear cookies for this domain
                val cookieManager = CookieManager.getInstance()
                val cookies = cookieManager.getCookie(url)
                if (cookies != null) {
                    // Remove cookies by setting them to expired
                    cookies.split(";").forEach { cookie ->
                        val cookieName = cookie.split("=")[0].trim()
                        cookieManager.setCookie(url, "$cookieName=; Expires=Thu, 01 Jan 1970 00:00:00 GMT")
                    }
                    cookieManager.flush()
                }

                // Clear WebView cache
                webView.clearCache(true)

                // Reload page
                webView.reload()

                Toast.makeText(this, "Site data cleared for $domain", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Start voice search using speech recognition
     */
    private fun startVoiceSearch() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search...")
            }
            voiceSearchLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice search not available", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Take a screenshot of the current page
     */
    private fun takeScreenshot() {
        try {
            // Create bitmap from WebView
            val bitmap = Bitmap.createBitmap(
                webView.width,
                webView.measuredHeight.coerceAtMost(webView.height),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            webView.draw(canvas)

            // Save to file
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "CleanFinding_$timestamp.png"

            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val cleanfindingDir = File(picturesDir, "CleanFinding")
            if (!cleanfindingDir.exists()) {
                cleanfindingDir.mkdirs()
            }

            val file = File(cleanfindingDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            Toast.makeText(this, "Screenshot saved: $filename", Toast.LENGTH_LONG).show()

            // Offer to share
            AlertDialog.Builder(this)
                .setTitle("Screenshot Saved")
                .setMessage("Would you like to share this screenshot?")
                .setPositiveButton("Share") { _, _ ->
                    shareScreenshot(file)
                }
                .setNegativeButton("Close", null)
                .show()

        } catch (e: Exception) {
            android.util.Log.e("Screenshot", "Error taking screenshot: ${e.message}")
            Toast.makeText(this, "Failed to take screenshot", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share a screenshot file
     */
    private fun shareScreenshot(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Screenshot"))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to share screenshot", Toast.LENGTH_SHORT).show()
        }
    }

    // Desktop mode - Chrome-like implementation
    private fun toggleDesktopMode() {
        desktopMode = !desktopMode
        setupWebView(webView)

        // Apply or restore viewport based on mode
        if (desktopMode) {
            // CHROME-LIKE: Calculate initial zoom to fit desktop content on mobile screen
            // Desktop content is rendered at 1024px width, so we scale to fit screen
            val displayMetrics = resources.displayMetrics
            val screenWidthPx = displayMetrics.widthPixels
            // Calculate scale: (screen width / content width) * 100
            // This makes the 1024px desktop layout fit the mobile screen width
            val desktopWidth = 1024
            val initialScale = ((screenWidthPx.toFloat() / desktopWidth) * 100).toInt()
            webView.setInitialScale(initialScale)
            injectDesktopModeScript(webView)
        } else {
            // Reset to default scale for mobile mode
            webView.setInitialScale(0)  // 0 means use default
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
        // Calculate the initial scale to fit 1024px content on screen
        val displayMetrics = resources.displayMetrics
        val screenWidthPx = displayMetrics.widthPixels
        val desktopWidth = 1024
        val initialScale = screenWidthPx.toFloat() / desktopWidth

        val script = """
            (function() {
                // Remove or modify viewport meta tag to force desktop layout
                var viewport = document.querySelector('meta[name="viewport"]');
                // Calculate scale to fit desktop content on mobile screen
                var initialScale = $initialScale;
                var viewportContent = 'width=$desktopWidth, initial-scale=' + initialScale + ', minimum-scale=' + (initialScale * 0.5) + ', maximum-scale=3.0, user-scalable=yes';

                if (viewport) {
                    // Store original viewport for restoration
                    viewport.setAttribute('data-original-content', viewport.getAttribute('content'));
                    // Set desktop-style viewport with calculated scale to fit screen
                    viewport.setAttribute('content', viewportContent);
                } else {
                    // Create viewport meta if it doesn't exist
                    var meta = document.createElement('meta');
                    meta.name = 'viewport';
                    meta.content = viewportContent;
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

                console.log('CleanFinding: Desktop mode viewport applied with scale ' + initialScale);
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

        // CRITICAL: Whitelist trusted domains - never block these
        // These are major legitimate websites that should always be accessible
        val trustedDomains = listOf(
            "youtube.com", "youtu.be", "m.youtube.com",
            "google.com", "google.co", "gstatic.com", "googleapis.com",
            "facebook.com", "instagram.com", "twitter.com", "x.com",
            "pinterest.com", "linkedin.com", "reddit.com",
            "amazon.com", "ebay.com", "walmart.com",
            "wikipedia.org", "wikimedia.org",
            "github.com", "stackoverflow.com",
            "cleanfinding.com",
            "microsoft.com", "apple.com", "netflix.com"
        )

        // Check if URL is from a trusted domain - if so, don't block
        for (trusted in trustedDomains) {
            if (lowerUrl.contains(trusted)) {
                return false
            }
        }

        // Check tracker/ad domains (only block these on non-trusted sites)
        for (domain in blockedDomains) {
            if (lowerUrl.contains(domain)) {
                return true
            }
        }

        // Check adult content - only check the DOMAIN part, not full URL
        // This prevents false positives from video titles, search queries, etc.
        try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: ""
            for (keyword in adultDomains) {
                if (host.contains(keyword)) {
                    return true
                }
            }
        } catch (e: Exception) {
            // If URL parsing fails, do basic check on host-like portion
            val hostPart = lowerUrl.substringAfter("://").substringBefore("/")
            for (keyword in adultDomains) {
                if (hostPart.contains(keyword)) {
                    return true
                }
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

                // CRITICAL: Skip ad blocking on YouTube to prevent video playback issues
                // YouTube's internal classes contain "ad" patterns that our selectors would incorrectly match
                // YouTube handles its own ad blocking detection and breaks if we interfere
                if (window.location.hostname.indexOf('youtube.com') !== -1 ||
                    window.location.hostname.indexOf('youtu.be') !== -1) {
                    console.log('CleanFinding: Skipping ad blocking on YouTube for proper video playback');
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
        // CRITICAL FIX: Enhanced video and image rendering fixes for Pinterest
        // NOTE: We skip YouTube because our CSS overrides interfere with YouTube's native player
        val cssScript = """
            (function() {
                // CRITICAL: Skip CSS injection on YouTube - their player is complex and our
                // overrides with !important break the native video rendering
                // YouTube works best when left completely alone
                var hostname = window.location.hostname;
                if (hostname.indexOf('youtube.com') !== -1 || hostname.indexOf('youtu.be') !== -1) {
                    console.log('CleanFinding: Skipping media fixes on YouTube - using native player');
                    return;
                }

                if (document.getElementById('cleanfinding-media-fix')) return;
                console.log('CleanFinding: Applying Pinterest media fixes...');

                // Inject comprehensive CSS fixes
                var style = document.createElement('style');
                style.id = 'cleanfinding-media-fix';
                style.textContent = `
                    /* ============ YOUTUBE VIDEO FIXES ============ */
                    /* Force video element visibility */
                    video, .video-stream, .html5-main-video {
                        opacity: 1 !important;
                        visibility: visible !important;
                        display: block !important;
                        background-color: #000 !important;
                        object-fit: contain !important;
                        z-index: 10 !important;
                        -webkit-transform: translateZ(0) !important;
                        transform: translateZ(0) !important;
                    }

                    /* YouTube player container fixes */
                    .html5-video-player,
                    .html5-video-container,
                    #player-container-inner,
                    #movie_player,
                    ytd-player,
                    #ytd-player,
                    .ytd-player {
                        width: 100% !important;
                        min-height: 200px !important;
                        opacity: 1 !important;
                        visibility: visible !important;
                        position: relative !important;
                        background-color: #000 !important;
                        -webkit-transform: translateZ(0) !important;
                    }

                    /* Mobile YouTube specific */
                    ytm-single-column-watch-next-results-renderer,
                    .watch-below-the-player,
                    #player {
                        background-color: #000 !important;
                    }

                    /* YouTube Shorts video fix */
                    ytd-reel-video-renderer video,
                    ytm-reel-video-renderer video {
                        width: 100% !important;
                        height: 100% !important;
                        object-fit: cover !important;
                    }

                    /* ============ PINTEREST FIXES ============ */
                    /* Pinterest image containers */
                    [data-test-id="pin-image"],
                    [data-test-id="pinImg"],
                    .GrowthUnauthPinImage,
                    .PinImage,
                    .hCL.kVc,
                    div[data-test-id="pin"] img,
                    .Jea.MIw.Hsu {
                        width: 100% !important;
                        height: auto !important;
                        max-width: 100% !important;
                        object-fit: contain !important;
                        opacity: 1 !important;
                        visibility: visible !important;
                    }

                    /* Pinterest lazy-loaded images */
                    img[src*="pinimg.com"],
                    img[data-src*="pinimg.com"] {
                        opacity: 1 !important;
                        visibility: visible !important;
                        display: block !important;
                    }

                    /* Pinterest video player */
                    .VideoPlayer,
                    [data-test-id="video-player"],
                    .hwa.iyn.jzS video {
                        width: 100% !important;
                        height: auto !important;
                        min-height: 200px !important;
                        background-color: #000 !important;
                        opacity: 1 !important;
                    }

                    /* ============ GENERAL IMAGE FIXES ============ */
                    img:not([src=""]):not([src="data:"]) {
                        opacity: 1 !important;
                        visibility: visible !important;
                    }

                    /* Fix lazy-loaded images */
                    img[loading="lazy"],
                    img[data-src],
                    img.lazyload,
                    img.lazy {
                        opacity: 1 !important;
                        visibility: visible !important;
                    }

                    /* ============ IFRAME FIXES ============ */
                    iframe[src*="youtube"],
                    iframe[src*="youtu.be"],
                    iframe[src*="youtube-nocookie"],
                    iframe[src*="video"] {
                        max-width: 100% !important;
                        min-height: 200px !important;
                        visibility: visible !important;
                        background-color: #000 !important;
                    }
                `;
                document.head.appendChild(style);

                // Function to fix YouTube video elements
                function fixYouTubeVideo() {
                    var videos = document.querySelectorAll('video, .html5-main-video');
                    videos.forEach(function(video) {
                        video.style.cssText += 'opacity:1!important;visibility:visible!important;display:block!important;background:#000!important;';

                        // Set playsinline attribute for proper mobile playback
                        video.setAttribute('playsinline', '');
                        video.setAttribute('webkit-playsinline', '');

                        // Force hardware acceleration
                        video.style.transform = 'translateZ(0)';
                        video.style.webkitTransform = 'translateZ(0)';

                        // Force repaint
                        video.offsetHeight;
                    });

                    // Fix player containers
                    var players = document.querySelectorAll('.html5-video-player, #movie_player, ytd-player, #player');
                    players.forEach(function(player) {
                        player.style.cssText += 'opacity:1!important;visibility:visible!important;background:#000!important;';
                    });
                }

                // Function to fix Pinterest images
                function fixPinterestImages() {
                    // Force lazy images to load
                    var lazyImages = document.querySelectorAll('img[data-src], img[loading="lazy"]');
                    lazyImages.forEach(function(img) {
                        if (img.dataset.src && !img.src) {
                            img.src = img.dataset.src;
                        }
                        img.style.opacity = '1';
                        img.style.visibility = 'visible';
                    });

                    // Fix Pinterest specific images
                    var pinImages = document.querySelectorAll('[data-test-id="pin-image"] img, img[src*="pinimg.com"]');
                    pinImages.forEach(function(img) {
                        img.style.cssText += 'opacity:1!important;visibility:visible!important;display:block!important;';
                        // Force image reload if needed
                        if (img.complete && img.naturalHeight === 0) {
                            var src = img.src;
                            img.src = '';
                            img.src = src;
                        }
                    });

                    // Fix Pinterest videos
                    var pinVideos = document.querySelectorAll('[data-test-id="video-player"] video, .VideoPlayer video');
                    pinVideos.forEach(function(video) {
                        video.style.cssText += 'opacity:1!important;visibility:visible!important;background:#000!important;';
                        video.setAttribute('playsinline', '');
                    });
                }

                // Apply fixes based on current site
                function applyFixes() {
                    var hostname = window.location.hostname;

                    if (hostname.includes('youtube.com') || hostname.includes('youtu.be')) {
                        fixYouTubeVideo();
                    } else if (hostname.includes('pinterest')) {
                        fixPinterestImages();
                    } else {
                        // Generic fixes for other sites
                        fixYouTubeVideo();
                        fixPinterestImages();
                    }
                }

                // Apply fixes immediately
                applyFixes();

                // Monitor for dynamically loaded content
                var observer = new MutationObserver(function(mutations) {
                    var shouldFix = mutations.some(function(mutation) {
                        return mutation.addedNodes.length > 0;
                    });
                    if (shouldFix) {
                        setTimeout(applyFixes, 100);
                    }
                });

                observer.observe(document.body || document.documentElement, {
                    childList: true,
                    subtree: true
                });

                // Re-apply fixes periodically for first 10 seconds
                var fixCount = 0;
                var fixInterval = setInterval(function() {
                    applyFixes();
                    fixCount++;
                    if (fixCount >= 20) {
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
            // CRITICAL: Check fullscreen mode FIRST - exit fullscreen instead of navigating back
            // This prevents the user from being "kicked out" when pressing back in fullscreen video
            customView != null -> {
                // Use our safe exitFullscreenMode() function to avoid crashes
                exitFullscreenMode()
            }
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
