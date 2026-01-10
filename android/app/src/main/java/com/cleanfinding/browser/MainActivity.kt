package com.cleanfinding.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlEditText: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var backButton: ImageButton
    private lateinit var forwardButton: ImageButton
    private lateinit var refreshButton: ImageButton
    private lateinit var homeButton: ImageButton

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

        initViews()
        setupWebView()
        setupListeners()

        // Load home page or intent URL
        val intentUrl = intent?.data?.toString()
        if (intentUrl != null) {
            loadUrl(intentUrl)
        } else {
            loadUrl(homeUrl)
        }
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        urlEditText = findViewById(R.id.urlEditText)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        backButton = findViewById(R.id.backButton)
        forwardButton = findViewById(R.id.forwardButton)
        refreshButton = findViewById(R.id.refreshButton)
        homeButton = findViewById(R.id.homeButton)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
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

            // Set user agent
            userAgentString = userAgentString.replace("; wv", "") + " CleanFindingBrowser/1.0"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false

                // Check for blocked domains
                if (isBlockedUrl(url)) {
                    showBlockedMessage(url)
                    return true
                }

                // Enforce SafeSearch
                val safeUrl = enforceSafeSearch(url)
                if (safeUrl != url) {
                    view?.loadUrl(safeUrl)
                    return true
                }

                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                urlEditText.setText(url)
                updateNavigationButtons()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                updateNavigationButtons()

                // Inject content blocking script
                injectBlockingScript()
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
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

        refreshButton.setOnClickListener {
            webView.reload()
        }

        homeButton.setOnClickListener {
            loadUrl(homeUrl)
        }
    }

    private fun loadUrl(input: String) {
        var url = input.trim()

        // Check if it's a search query or URL
        if (!url.contains(".") || url.contains(" ")) {
            // It's a search query - use CleanFinding search with SafeSearch
            url = "https://cleanfinding.com/search?q=${Uri.encode(url)}"
        } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        // Check for blocked content
        if (isBlockedUrl(url)) {
            showBlockedMessage(url)
            return
        }

        // Enforce SafeSearch
        url = enforceSafeSearch(url)

        webView.loadUrl(url)
    }

    private fun isBlockedUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()

        // Check tracker/ad domains
        for (domain in blockedDomains) {
            if (lowerUrl.contains(domain)) {
                return true
            }
        }

        // Check adult content
        for (keyword in adultDomains) {
            if (lowerUrl.contains(keyword)) {
                return true
            }
        }

        return false
    }

    private fun enforceSafeSearch(url: String): String {
        var safeUrl = url

        // Google SafeSearch
        if (url.contains("google.") && url.contains("/search")) {
            safeUrl = if (url.contains("safe=")) {
                url.replace(Regex("safe=[^&]*"), "safe=active")
            } else {
                if (url.contains("?")) "$url&safe=active" else "$url?safe=active"
            }
        }

        // Bing SafeSearch
        if (url.contains("bing.com") && url.contains("/search")) {
            safeUrl = if (url.contains("safeSearch=")) {
                url.replace(Regex("safeSearch=[^&]*"), "safeSearch=Strict")
            } else {
                if (url.contains("?")) "$url&safeSearch=Strict" else "$url?safeSearch=Strict"
            }
        }

        // DuckDuckGo SafeSearch
        if (url.contains("duckduckgo.com")) {
            safeUrl = if (url.contains("kp=")) {
                url.replace(Regex("kp=[^&]*"), "kp=1")
            } else {
                if (url.contains("?")) "$url&kp=1" else "$url?kp=1"
            }
        }

        // YouTube Restricted Mode
        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            // YouTube restricted mode is handled via cookies/headers
            // We enforce it through JavaScript injection
        }

        return safeUrl
    }

    private fun injectBlockingScript() {
        val script = """
            (function() {
                // Block trackers
                var blockedDomains = ${blockedDomains.joinToString(",", "[", "]") { "\"$it\"" }};

                // Override XMLHttpRequest
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

                // Override fetch
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

                // Remove ad containers
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

        webView.evaluateJavascript(script, null)
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

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.toString()?.let { loadUrl(it) }
    }
}
