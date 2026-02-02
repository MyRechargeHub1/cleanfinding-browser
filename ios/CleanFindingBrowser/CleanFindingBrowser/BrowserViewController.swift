import UIKit
import WebKit

class BrowserViewController: UIViewController {

    // MARK: - Properties

    private var webView: WKWebView!
    private var urlTextField: UITextField!
    private var progressView: UIProgressView!
    private var toolbar: UIToolbar!

    private let homeURL = URL(string: "https://cleanfinding.com")!

    // CRITICAL: Trusted domains - never block these (same as Android)
    private let trustedDomains = [
        "youtube.com", "youtu.be", "m.youtube.com",
        "google.com", "google.co", "gstatic.com", "googleapis.com",
        "facebook.com", "instagram.com", "twitter.com", "x.com",
        "pinterest.com", "linkedin.com", "reddit.com",
        "amazon.com", "ebay.com", "walmart.com",
        "wikipedia.org", "wikimedia.org",
        "github.com", "stackoverflow.com",
        "cleanfinding.com",
        "microsoft.com", "apple.com", "netflix.com"
    ]

    // Blocked domains
    private let blockedDomains = [
        "google-analytics.com", "googletagmanager.com", "doubleclick.net",
        "facebook.net", "connect.facebook.net", "analytics.google.com",
        "adservice.google.com", "googlesyndication.com", "googleadservices.com",
        "mixpanel.com", "hotjar.com", "fullstory.com", "amplitude.com",
        "segment.com", "heapanalytics.com", "crazyegg.com", "clarity.ms",
        "adnxs.com", "criteo.com", "taboola.com", "outbrain.com"
    ]

    // Adult content keywords (check domain only, not full URL)
    private let adultKeywords = [
        "pornhub", "xvideos", "xnxx", "redtube", "youporn",
        "xhamster", "porn", "xxx", "adult"
    ]

    // Scrolling toolbar state
    private var isToolbarVisible = true
    private var lastScrollY: CGFloat = 0
    private let scrollThreshold: CGFloat = 30
    private var urlBarTopConstraint: NSLayoutConstraint?
    private var toolbarBottomConstraint: NSLayoutConstraint?
    private var urlBarView: UIView?

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        loadHomePage()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        webView.addObserver(self, forKeyPath: #keyPath(WKWebView.estimatedProgress), options: .new, context: nil)
        webView.addObserver(self, forKeyPath: #keyPath(WKWebView.url), options: .new, context: nil)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        webView.removeObserver(self, forKeyPath: #keyPath(WKWebView.estimatedProgress))
        webView.removeObserver(self, forKeyPath: #keyPath(WKWebView.url))
    }

    // MARK: - Setup

    private func setupUI() {
        view.backgroundColor = UIColor(red: 0.10, green: 0.10, blue: 0.18, alpha: 1.0)

        setupURLBar()
        setupProgressView()
        setupWebView()
        setupToolbar()
    }

    private func setupURLBar() {
        let urlBar = UIView()
        urlBar.backgroundColor = UIColor(red: 0.09, green: 0.13, blue: 0.24, alpha: 1.0)
        urlBar.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(urlBar)

        urlTextField = UITextField()
        urlTextField.backgroundColor = UIColor(red: 0.06, green: 0.20, blue: 0.38, alpha: 1.0)
        urlTextField.textColor = .white
        urlTextField.attributedPlaceholder = NSAttributedString(
            string: "Search or enter URL",
            attributes: [.foregroundColor: UIColor.gray]
        )
        urlTextField.layer.cornerRadius = 20
        urlTextField.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 16, height: 0))
        urlTextField.leftViewMode = .always
        urlTextField.rightView = UIView(frame: CGRect(x: 0, y: 0, width: 16, height: 0))
        urlTextField.rightViewMode = .always
        urlTextField.keyboardType = .webSearch
        urlTextField.returnKeyType = .go
        urlTextField.autocapitalizationType = .none
        urlTextField.autocorrectionType = .no
        urlTextField.delegate = self
        urlTextField.translatesAutoresizingMaskIntoConstraints = false
        urlBar.addSubview(urlTextField)

        NSLayoutConstraint.activate([
            urlBar.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            urlBar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            urlBar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            urlBar.heightAnchor.constraint(equalToConstant: 56),

            urlTextField.centerYAnchor.constraint(equalTo: urlBar.centerYAnchor),
            urlTextField.leadingAnchor.constraint(equalTo: urlBar.leadingAnchor, constant: 16),
            urlTextField.trailingAnchor.constraint(equalTo: urlBar.trailingAnchor, constant: -16),
            urlTextField.heightAnchor.constraint(equalToConstant: 40)
        ])
    }

    private func setupProgressView() {
        progressView = UIProgressView(progressViewStyle: .default)
        progressView.progressTintColor = UIColor(red: 0.40, green: 0.49, blue: 0.92, alpha: 1.0)
        progressView.trackTintColor = .clear
        progressView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(progressView)

        NSLayoutConstraint.activate([
            progressView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 56),
            progressView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            progressView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            progressView.heightAnchor.constraint(equalToConstant: 2)
        ])
    }

    private func setupWebView() {
        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true

        // CRITICAL: Enable JavaScript and configure preferences for search to work
        let preferences = WKPreferences()
        preferences.javaScriptEnabled = true
        configuration.preferences = preferences

        // CRITICAL: Enable DOM storage via default data store for search API calls
        configuration.websiteDataStore = WKWebsiteDataStore.default()

        // Add content blocker
        let contentController = WKUserContentController()
        let blockingScript = createBlockingScript()
        let userScript = WKUserScript(source: blockingScript, injectionTime: .atDocumentEnd, forMainFrameOnly: false)
        contentController.addUserScript(userScript)
        configuration.userContentController = contentController

        webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = self
        webView.uiDelegate = self
        webView.allowsBackForwardNavigationGestures = true
        webView.scrollView.contentInsetAdjustmentBehavior = .automatic
        webView.scrollView.delegate = self  // Add scroll delegate for toolbar hide/show
        webView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(webView)

        NSLayoutConstraint.activate([
            webView.topAnchor.constraint(equalTo: progressView.bottomAnchor),
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            webView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -44)
        ])
    }

    private func setupToolbar() {
        toolbar = UIToolbar()
        toolbar.barTintColor = UIColor(red: 0.09, green: 0.13, blue: 0.24, alpha: 1.0)
        toolbar.tintColor = UIColor(red: 0.63, green: 0.68, blue: 0.78, alpha: 1.0)
        toolbar.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(toolbar)

        let backButton = UIBarButtonItem(image: UIImage(systemName: "chevron.left"), style: .plain, target: self, action: #selector(goBack))
        let forwardButton = UIBarButtonItem(image: UIImage(systemName: "chevron.right"), style: .plain, target: self, action: #selector(goForward))
        let refreshButton = UIBarButtonItem(image: UIImage(systemName: "arrow.clockwise"), style: .plain, target: self, action: #selector(refresh))
        let homeButton = UIBarButtonItem(image: UIImage(systemName: "house"), style: .plain, target: self, action: #selector(goHome))
        let flexSpace = UIBarButtonItem(barButtonSystemItem: .flexibleSpace, target: nil, action: nil)

        toolbar.items = [backButton, flexSpace, forwardButton, flexSpace, refreshButton, flexSpace, homeButton]

        NSLayoutConstraint.activate([
            toolbar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            toolbar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            toolbar.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
            toolbar.heightAnchor.constraint(equalToConstant: 44)
        ])
    }

    // MARK: - Navigation

    private func loadHomePage() {
        webView.load(URLRequest(url: homeURL))
    }

    private func loadURL(_ urlString: String) {
        var url = urlString.trimmingCharacters(in: .whitespacesAndNewlines)

        // Check if it's a search query
        if !url.contains(".") || url.contains(" ") {
            let query = url.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? url
            url = "https://cleanfinding.com/search?q=\(query)"
        } else if !url.hasPrefix("http://") && !url.hasPrefix("https://") {
            url = "https://\(url)"
        }

        guard let requestURL = URL(string: url) else { return }

        // Check for blocked content
        if isBlockedURL(url) {
            showBlockedAlert()
            return
        }

        // Enforce SafeSearch
        let safeURL = enforceSafeSearch(url)
        if let finalURL = URL(string: safeURL) {
            webView.load(URLRequest(url: finalURL))
        }
    }

    // MARK: - Content Blocking

    private func isBlockedURL(_ url: String) -> Bool {
        let lowerURL = url.lowercased()

        // CRITICAL: Whitelist trusted domains - never block these
        for trusted in trustedDomains {
            if lowerURL.contains(trusted) {
                return false
            }
        }

        // Check tracker/ad domains
        for domain in blockedDomains {
            if lowerURL.contains(domain) {
                return true
            }
        }

        // Check adult content - only check the DOMAIN part, not full URL
        // This prevents false positives from video titles, search queries, etc.
        if let urlObj = URL(string: url), let host = urlObj.host?.lowercased() {
            for keyword in adultKeywords {
                if host.contains(keyword) {
                    return true
                }
            }
        } else {
            // Fallback: basic host extraction
            let hostPart = lowerURL.components(separatedBy: "://").last?.components(separatedBy: "/").first ?? ""
            for keyword in adultKeywords {
                if hostPart.contains(keyword) {
                    return true
                }
            }
        }

        return false
    }

    private func enforceSafeSearch(_ url: String) -> String {
        var safeURL = url

        // Google SafeSearch
        if url.contains("google.") && url.contains("/search") {
            if url.contains("safe=") {
                safeURL = url.replacingOccurrences(of: "safe=off", with: "safe=active")
                    .replacingOccurrences(of: "safe=images", with: "safe=active")
            } else {
                safeURL = url.contains("?") ? "\(url)&safe=active" : "\(url)?safe=active"
            }
        }

        // Bing SafeSearch
        if url.contains("bing.com") && url.contains("/search") {
            if !url.contains("safeSearch=") {
                safeURL = url.contains("?") ? "\(url)&safeSearch=Strict" : "\(url)?safeSearch=Strict"
            }
        }

        // DuckDuckGo SafeSearch
        if url.contains("duckduckgo.com") {
            if !url.contains("kp=") {
                safeURL = url.contains("?") ? "\(url)&kp=1" : "\(url)?kp=1"
            }
        }

        return safeURL
    }

    private func createBlockingScript() -> String {
        let domainsJSON = blockedDomains.map { "\"\($0)\"" }.joined(separator: ",")
        return """
        (function() {
            // CRITICAL: Skip ad blocking on CleanFinding.com to prevent breaking search functionality
            if (window.location.hostname.indexOf('cleanfinding.com') !== -1) {
                console.log('CleanFinding: Skipping ad blocking on cleanfinding.com');
                return;
            }

            // CRITICAL: Skip ad blocking on YouTube to prevent video playback issues
            // YouTube's internal classes contain "ad" patterns that our selectors would incorrectly match
            if (window.location.hostname.indexOf('youtube.com') !== -1 ||
                window.location.hostname.indexOf('youtu.be') !== -1) {
                console.log('CleanFinding: Skipping ad blocking on YouTube for proper video playback');
                return;
            }

            var blockedDomains = [\(domainsJSON)];

            // Override fetch
            var originalFetch = window.fetch;
            window.fetch = function(url, options) {
                for (var i = 0; i < blockedDomains.length; i++) {
                    if (url && url.toString().indexOf(blockedDomains[i]) !== -1) {
                        return Promise.reject(new Error('Blocked by CleanFinding'));
                    }
                }
                return originalFetch.apply(this, arguments);
            };

            // Remove ad containers (skip on YouTube)
            function removeAds() {
                var selectors = ['[class*="ad-"]', '[class*="ads-"]', '[id*="ad-"]', 'ins.adsbygoogle'];
                selectors.forEach(function(selector) {
                    document.querySelectorAll(selector).forEach(function(el) {
                        el.style.display = 'none';
                    });
                });
            }
            removeAds();
            setInterval(removeAds, 2000);
        })();
        """
    }

    private func showBlockedAlert() {
        let alert = UIAlertController(
            title: "Content Blocked",
            message: "This content has been blocked by CleanFinding Browser for your safety.",
            preferredStyle: .alert
        )
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        alert.addAction(UIAlertAction(title: "Go Home", style: .default) { [weak self] _ in
            self?.loadHomePage()
        })
        present(alert, animated: true)
    }

    // MARK: - Actions

    @objc private func goBack() {
        if webView.canGoBack {
            webView.goBack()
        }
    }

    @objc private func goForward() {
        if webView.canGoForward {
            webView.goForward()
        }
    }

    @objc private func refresh() {
        webView.reload()
    }

    @objc private func goHome() {
        loadHomePage()
    }

    // MARK: - KVO

    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        if keyPath == "estimatedProgress" {
            progressView.progress = Float(webView.estimatedProgress)
            progressView.isHidden = webView.estimatedProgress >= 1
        } else if keyPath == "url" {
            urlTextField.text = webView.url?.absoluteString
        }
    }
}

// MARK: - UITextFieldDelegate

extension BrowserViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        if let text = textField.text, !text.isEmpty {
            loadURL(text)
        }
        textField.resignFirstResponder()
        return true
    }
}

// MARK: - WKNavigationDelegate

extension BrowserViewController: WKNavigationDelegate {
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        guard let url = navigationAction.request.url?.absoluteString else {
            decisionHandler(.allow)
            return
        }

        if isBlockedURL(url) {
            showBlockedAlert()
            decisionHandler(.cancel)
            return
        }

        // Enforce SafeSearch on navigation
        let safeURL = enforceSafeSearch(url)
        if safeURL != url, let newURL = URL(string: safeURL) {
            decisionHandler(.cancel)
            webView.load(URLRequest(url: newURL))
            return
        }

        decisionHandler(.allow)
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        progressView.isHidden = true
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        progressView.isHidden = true
    }
}

// MARK: - WKUIDelegate

extension BrowserViewController: WKUIDelegate {
    func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration, for navigationAction: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
        // Open links that want to open in new window in same webview
        if navigationAction.targetFrame == nil {
            webView.load(navigationAction.request)
        }
        return nil
    }
}

// MARK: - UIScrollViewDelegate (Chrome-like scrolling toolbar)

extension BrowserViewController: UIScrollViewDelegate {
    func scrollViewDidScroll(_ scrollView: UIScrollView) {
        let currentScrollY = scrollView.contentOffset.y
        let diff = currentScrollY - lastScrollY

        // Only respond to significant scroll changes
        if abs(diff) > scrollThreshold {
            if diff > 0 && isToolbarVisible && currentScrollY > 50 {
                // Scrolling DOWN - hide toolbar
                hideToolbar()
            } else if diff < 0 && !isToolbarVisible {
                // Scrolling UP - show toolbar
                showToolbar()
            }
        }

        // Always show toolbar when at top of page
        if currentScrollY <= 10 && !isToolbarVisible {
            showToolbar()
        }

        lastScrollY = currentScrollY
    }

    private func hideToolbar() {
        guard isToolbarVisible else { return }
        isToolbarVisible = false

        UIView.animate(withDuration: 0.2) {
            // Hide URL bar (slide up)
            self.view.subviews.first?.transform = CGAffineTransform(translationX: 0, y: -56)
            // Hide toolbar (slide down)
            self.toolbar.transform = CGAffineTransform(translationX: 0, y: 44)
        }
    }

    private func showToolbar() {
        guard !isToolbarVisible else { return }
        isToolbarVisible = true

        UIView.animate(withDuration: 0.2) {
            // Show URL bar
            self.view.subviews.first?.transform = .identity
            // Show toolbar
            self.toolbar.transform = .identity
        }
    }
}
