package com.cleanfinding.browser

import android.webkit.WebView

/**
 * Email Protection Handler
 * Blocks email tracking pixels, open tracking, and link tracking in webmail interfaces
 * Protects privacy when using Gmail, Yahoo Mail, Outlook, and other web-based email services
 */
class EmailProtectionHandler {

    // Common email tracking domains
    private val emailTrackingDomains = listOf(
        // Email analytics and tracking services
        "mailtrack.io",
        "mailchimp.com/track",
        "mailgun.net/track",
        "sendgrid.net/track",
        "mandrillapp.com/track",
        "postmarkapp.com/track",
        "sparkpostmail.com/track",

        // Email marketing platforms
        "constantcontact.com/track",
        "aweber.com/track",
        "getresponse.com/track",
        "activecampaign.com/track",
        "campaignmonitor.com/track",
        "hubspot.com/track",
        "convertkit.com/track",

        // Read receipt and tracking pixels
        "yesware.com/t",
        "streak.com/track",
        "cirrusinsight.com/track",
        "outreach.io/track",
        "salesforce.com/track",
        "salesloft.com/track",

        // Generic tracking
        "pixel.email",
        "tracking.email",
        "open.email",
        "click.email",
        "track.customer.io",
        "link.mixpanel.com",
        "click.pstmrk.it",
        "links.aweber.com"
    )

    // URL parameters used for email tracking
    private val emailTrackingParams = listOf(
        "utm_source",
        "utm_medium",
        "utm_campaign",
        "utm_content",
        "utm_term",
        "mc_cid",
        "mc_eid",
        "_hsenc",
        "_hsmi",
        "mkt_tok",
        "trk",
        "trkEmail",
        "sc_uid",
        "sc_lid",
        "elqTrack",
        "elqTrackId",
        "assetType",
        "assetId",
        "recipientId",
        "campaignId",
        "batchId"
    )

    /**
     * Check if URL is likely an email tracking pixel
     */
    fun isTrackingPixel(url: String): Boolean {
        val lowerUrl = url.lowercase()

        // Check for common tracking pixel patterns
        if (lowerUrl.contains("/track/") ||
            lowerUrl.contains("/pixel") ||
            lowerUrl.contains("/open") ||
            lowerUrl.contains("/beacon") ||
            lowerUrl.contains("/img/track") ||
            lowerUrl.contains("1x1.gif") ||
            lowerUrl.contains("pixel.gif") ||
            lowerUrl.contains("transparent.gif")) {
            return true
        }

        // Check against known tracking domains
        for (domain in emailTrackingDomains) {
            if (lowerUrl.contains(domain)) {
                return true
            }
        }

        return false
    }

    /**
     * Remove tracking parameters from URL
     */
    fun removeTrackingParams(url: String): String {
        try {
            val uri = android.net.Uri.parse(url)
            val builder = uri.buildUpon()
            builder.clearQuery()

            // Re-add only non-tracking parameters
            for (paramName in uri.queryParameterNames) {
                if (!emailTrackingParams.contains(paramName.lowercase())) {
                    val paramValue = uri.getQueryParameter(paramName)
                    if (paramValue != null) {
                        builder.appendQueryParameter(paramName, paramValue)
                    }
                }
            }

            return builder.build().toString()
        } catch (e: Exception) {
            return url
        }
    }

    /**
     * Inject email protection script into webmail pages
     */
    fun injectEmailProtection(webView: WebView?, url: String) {
        // Only inject on known webmail services
        if (!isWebmailService(url)) {
            return
        }

        val script = """
            (function() {
                console.log('CleanFinding: Email Protection active');

                // Block tracking pixels (1x1 images)
                function blockTrackingPixels() {
                    var images = document.querySelectorAll('img');
                    var blocked = 0;

                    images.forEach(function(img) {
                        // Check if image is likely a tracking pixel
                        if (img.width <= 1 || img.height <= 1 ||
                            img.src.indexOf('/track') !== -1 ||
                            img.src.indexOf('/pixel') !== -1 ||
                            img.src.indexOf('/open') !== -1 ||
                            img.src.indexOf('1x1.gif') !== -1) {

                            img.src = '';
                            img.style.display = 'none';
                            blocked++;
                        }
                    });

                    if (blocked > 0) {
                        console.log('CleanFinding: Blocked ' + blocked + ' tracking pixels');
                    }
                }

                // Clean tracking parameters from links
                function cleanEmailLinks() {
                    var trackingParams = ${emailTrackingParams.joinToString(",", "[", "]") { "\"$it\"" }};
                    var links = document.querySelectorAll('a[href]');
                    var cleaned = 0;

                    links.forEach(function(link) {
                        try {
                            var url = new URL(link.href);
                            var modified = false;

                            trackingParams.forEach(function(param) {
                                if (url.searchParams.has(param)) {
                                    url.searchParams.delete(param);
                                    modified = true;
                                }
                            });

                            if (modified) {
                                link.href = url.toString();
                                cleaned++;
                            }
                        } catch (e) {
                            // Invalid URL, skip
                        }
                    });

                    if (cleaned > 0) {
                        console.log('CleanFinding: Cleaned ' + cleaned + ' email links');
                    }
                }

                // Block email open tracking
                function blockOpenTracking() {
                    // Prevent beacon API tracking
                    if (navigator.sendBeacon) {
                        var originalSendBeacon = navigator.sendBeacon;
                        navigator.sendBeacon = function(url, data) {
                            var urlLower = url.toLowerCase();
                            if (urlLower.indexOf('track') !== -1 ||
                                urlLower.indexOf('open') !== -1 ||
                                urlLower.indexOf('pixel') !== -1) {
                                console.log('CleanFinding: Blocked sendBeacon tracking');
                                return true;
                            }
                            return originalSendBeacon.apply(this, arguments);
                        };
                    }
                }

                // Apply protections immediately
                blockTrackingPixels();
                cleanEmailLinks();
                blockOpenTracking();

                // Re-apply when content changes (for dynamic email loading)
                var observer = new MutationObserver(function(mutations) {
                    var shouldCheck = false;
                    mutations.forEach(function(mutation) {
                        if (mutation.addedNodes.length > 0) {
                            shouldCheck = true;
                        }
                    });

                    if (shouldCheck) {
                        setTimeout(function() {
                            blockTrackingPixels();
                            cleanEmailLinks();
                        }, 100);
                    }
                });

                observer.observe(document.body, {
                    childList: true,
                    subtree: true
                });

                // CSS to hide tracking elements
                var style = document.createElement('style');
                style.id = 'cleanfinding-email-protection';
                style.textContent = `
                    /* Hide tracking pixels */
                    img[width="1"],
                    img[height="1"],
                    img[src*="/track"],
                    img[src*="/pixel"],
                    img[src*="/open"],
                    img[src*="1x1.gif"] {
                        display: none !important;
                        opacity: 0 !important;
                        visibility: hidden !important;
                    }

                    /* Hide tracking iframes */
                    iframe[width="1"],
                    iframe[height="1"],
                    iframe[src*="/track"],
                    iframe[src*="/pixel"] {
                        display: none !important;
                    }
                `;

                if (!document.getElementById('cleanfinding-email-protection')) {
                    document.head.appendChild(style);
                }

                console.log('CleanFinding: Email Protection enhancements applied');
            })();
        """.trimIndent()

        webView?.evaluateJavascript(script, null)
    }

    /**
     * Check if URL is a webmail service
     */
    private fun isWebmailService(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains("mail.google.com") ||
                lowerUrl.contains("gmail.com") ||
                lowerUrl.contains("mail.yahoo.com") ||
                lowerUrl.contains("outlook.live.com") ||
                lowerUrl.contains("outlook.office.com") ||
                lowerUrl.contains("mail.aol.com") ||
                lowerUrl.contains("protonmail.com") ||
                lowerUrl.contains("tutanota.com") ||
                lowerUrl.contains("mail.zoho.com") ||
                lowerUrl.contains("fastmail.com") ||
                lowerUrl.contains("webmail")
    }

    /**
     * Get list of email tracking domains for blocking
     */
    fun getTrackingDomains(): List<String> {
        return emailTrackingDomains
    }
}
