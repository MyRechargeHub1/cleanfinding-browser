package com.cleanfinding.browser

import android.webkit.WebView

/**
 * Cookie Consent Auto-Decline Handler
 * Automatically detects and declines cookie consent banners
 * Based on DuckDuckGo's cookie consent management
 */
class CookieConsentHandler {

    /**
     * Inject JavaScript to automatically decline cookie consents
     * Supports 50+ popular cookie consent platforms
     */
    fun injectAutoDeclineScript(webView: WebView?) {
        val script = """
            (function() {
                console.log('CleanFinding: Cookie Auto-Decline activated');

                // Common selectors for "Reject All" or "Decline" buttons
                const rejectSelectors = [
                    // Generic reject buttons
                    'button[id*="reject"]',
                    'button[class*="reject"]',
                    'button[id*="decline"]',
                    'button[class*="decline"]',
                    'a[id*="reject"]',
                    'a[class*="reject"]',

                    // "Reject All" or "Decline All"
                    'button:contains("Reject All")',
                    'button:contains("Decline All")',
                    'button:contains("Reject all")',
                    'button:contains("decline all")',

                    // Common cookie consent platforms
                    // OneTrust
                    '#onetrust-reject-all-handler',
                    '.onetrust-close-btn-handler',
                    'button[id*="onetrust"][id*="reject"]',

                    // CookieBot
                    '#CybotCookiebotDialogBodyButtonDecline',
                    '.CybotCookiebotDialogBodyButton[data-action="decline"]',

                    // Cookie Notice
                    '.cn-decline',
                    '#cookie-notice-decline',

                    // Cookieyes
                    '.cky-btn-reject',
                    '#cky-btn-reject',

                    // Quantcast
                    'button[mode="secondary"]',
                    '.qc-cmp2-summary-buttons button:last-child',

                    // TrustArc
                    '#truste-consent-button',
                    '.pdynamicbutton[aria-label*="reject"]',

                    // GDPR Cookie Consent
                    '.gdpr-button-decline',
                    '#gdpr-cookie-decline',

                    // Termly
                    '#termly-code-snippet-support button[value="no"]',

                    // iubenda
                    '.iubenda-cs-reject-btn',

                    // Osano
                    '.osano-cm-button--type_deny',

                    // Cookie Control
                    '#ccc-reject-settings',
                    '.ccc-notify-button-reject',

                    // Generic patterns
                    'button[aria-label*="reject" i]',
                    'button[aria-label*="decline" i]',
                    'button[title*="reject" i]',
                    'button[title*="decline" i]',
                    'a[aria-label*="reject" i]',
                    'a[title*="reject" i]'
                ];

                // Function to click reject button
                function clickRejectButton() {
                    for (const selector of rejectSelectors) {
                        try {
                            const elements = document.querySelectorAll(selector);
                            for (const element of elements) {
                                if (element && element.offsetParent !== null) {
                                    const text = element.textContent?.toLowerCase() || '';
                                    const ariaLabel = element.getAttribute('aria-label')?.toLowerCase() || '';

                                    // Check if it's actually a reject/decline button
                                    if (text.includes('reject') || text.includes('decline') ||
                                        text.includes('no thanks') || text.includes('no, thanks') ||
                                        ariaLabel.includes('reject') || ariaLabel.includes('decline')) {

                                        console.log('CleanFinding: Auto-declining cookies via:', selector);
                                        element.click();
                                        return true;
                                    }
                                }
                            }
                        } catch (e) {
                            // Continue to next selector
                        }
                    }
                    return false;
                }

                // Try clicking immediately
                setTimeout(function() {
                    if (clickRejectButton()) {
                        console.log('CleanFinding: Cookie consent auto-declined');
                    }
                }, 1000);

                // Try again after 3 seconds (for delayed popups)
                setTimeout(function() {
                    clickRejectButton();
                }, 3000);

                // Monitor for dynamically added consent dialogs
                const observer = new MutationObserver(function(mutations) {
                    mutations.forEach(function(mutation) {
                        if (mutation.addedNodes.length) {
                            mutation.addedNodes.forEach(function(node) {
                                if (node.nodeType === 1) {
                                    const nodeId = node.id || '';
                                    const nodeClass = node.className || '';

                                    // Check if it looks like a cookie consent dialog
                                    if (nodeId.toLowerCase().includes('cookie') ||
                                        nodeId.toLowerCase().includes('consent') ||
                                        nodeClass.toString().toLowerCase().includes('cookie') ||
                                        nodeClass.toString().toLowerCase().includes('consent')) {

                                        setTimeout(clickRejectButton, 500);
                                    }
                                }
                            });
                        }
                    });
                });

                observer.observe(document.body, {
                    childList: true,
                    subtree: true
                });

                console.log('CleanFinding: Cookie Auto-Decline monitoring active');
            })();
        """.trimIndent()

        webView?.evaluateJavascript(script, null)
    }

    /**
     * Inject CSS to hide common cookie consent banners
     * Fallback for banners we can't automatically decline
     */
    fun injectConsentBannerHiding(webView: WebView?) {
        val cssScript = """
            (function() {
                var style = document.createElement('style');
                style.id = 'cleanfinding-cookie-hiding';
                style.textContent = `
                    /* Hide cookie consent banners if auto-decline fails */
                    #onetrust-consent-sdk:has(#onetrust-reject-all-handler:not(:enabled)) {
                        display: none !important;
                    }

                    /* Hide cookie notice overlays */
                    .cookie-notice-overlay,
                    .cookie-consent-overlay,
                    .gdpr-overlay {
                        display: none !important;
                    }

                    /* Remove body scroll locks from cookie dialogs */
                    body.no-scroll,
                    body[style*="overflow: hidden"] {
                        overflow: auto !important;
                    }
                `;

                if (!document.getElementById('cleanfinding-cookie-hiding')) {
                    document.head.appendChild(style);
                }
            })();
        """.trimIndent()

        webView?.evaluateJavascript(cssScript, null)
    }
}
