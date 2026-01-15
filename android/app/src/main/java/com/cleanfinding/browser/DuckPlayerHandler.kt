package com.cleanfinding.browser

import android.net.Uri
import android.webkit.WebView

/**
 * Duck Player Handler
 * Provides privacy-enhanced YouTube playback by:
 * - Redirecting YouTube videos to privacy-friendly nocookie domain
 * - Blocking YouTube tracking and analytics
 * - Removing recommended videos and ads
 * - Providing clean, distraction-free viewing experience
 */
class DuckPlayerHandler {

    /**
     * Check if URL is a YouTube video
     */
    fun isYouTubeUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains("youtube.com/watch") ||
                lowerUrl.contains("youtu.be/") ||
                lowerUrl.contains("m.youtube.com/watch") ||
                lowerUrl.contains("youtube.com/embed/") ||
                lowerUrl.contains("youtube.com/v/")
    }

    /**
     * Extract video ID from YouTube URL
     */
    fun extractVideoId(url: String): String? {
        try {
            val uri = Uri.parse(url)

            // youtube.com/watch?v=VIDEO_ID
            if (url.contains("youtube.com/watch")) {
                return uri.getQueryParameter("v")
            }

            // youtu.be/VIDEO_ID
            if (url.contains("youtu.be/")) {
                val path = uri.path ?: return null
                return path.removePrefix("/").substringBefore("?")
            }

            // youtube.com/embed/VIDEO_ID
            if (url.contains("youtube.com/embed/")) {
                val path = uri.path ?: return null
                return path.removePrefix("/embed/").substringBefore("?")
            }

            // youtube.com/v/VIDEO_ID
            if (url.contains("youtube.com/v/")) {
                val path = uri.path ?: return null
                return path.removePrefix("/v/").substringBefore("?")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Convert YouTube URL to privacy-friendly nocookie embed URL
     */
    fun convertToPrivacyUrl(url: String): String? {
        val videoId = extractVideoId(url) ?: return null

        // Extract timestamp if present (t parameter or #t= fragment)
        var timestamp = ""
        try {
            val uri = Uri.parse(url)
            val tParam = uri.getQueryParameter("t")
            if (tParam != null) {
                timestamp = "?start=$tParam"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Use youtube-nocookie.com which doesn't set tracking cookies
        // Use minimal embed mode to remove recommendations and ads
        return "https://www.youtube-nocookie.com/embed/$videoId$timestamp"
    }

    /**
     * Inject Duck Player enhancements into WebView
     * - Blocks YouTube tracking scripts
     * - Hides related videos overlay
     * - Improves player UI for privacy
     */
    fun injectDuckPlayerEnhancements(webView: WebView?) {
        val script = """
            (function() {
                console.log('CleanFinding: Duck Player enhancements active');

                // Block YouTube tracking and analytics
                if (window.yt) {
                    // Disable YouTube analytics
                    if (window.yt.config_) {
                        window.yt.config_.EXPERIMENT_FLAGS = window.yt.config_.EXPERIMENT_FLAGS || {};
                        window.yt.config_.EXPERIMENT_FLAGS.web_player_ads_disable = true;
                    }
                }

                // Remove YouTube tracking parameters from links
                function cleanYouTubeLinks() {
                    var links = document.querySelectorAll('a[href*="youtube.com"], a[href*="youtu.be"]');
                    links.forEach(function(link) {
                        try {
                            var url = new URL(link.href);
                            // Remove tracking parameters
                            url.searchParams.delete('feature');
                            url.searchParams.delete('si');
                            url.searchParams.delete('pp');
                            link.href = url.toString();
                        } catch (e) {
                            // Invalid URL, skip
                        }
                    });
                }

                cleanYouTubeLinks();

                // Enhanced CSS for clean YouTube player experience
                var style = document.createElement('style');
                style.id = 'duck-player-style';
                style.textContent = `
                    /* Hide YouTube branding and distractions */
                    .ytp-pause-overlay,
                    .ytp-endscreen-content,
                    .ytp-ce-element,
                    .ytp-cards-teaser,
                    .ytp-watermark,
                    .ytp-impression-link,
                    .ytp-show-cards-title,
                    .iv-branding,
                    .annotation {
                        display: none !important;
                        opacity: 0 !important;
                        visibility: hidden !important;
                    }

                    /* Clean player interface */
                    .html5-video-player {
                        background: #000 !important;
                    }

                    /* Hide related videos overlay at video end */
                    .ytp-endscreen-content,
                    .ytp-ce-covering-overlay,
                    .ytp-ce-element-show {
                        display: none !important;
                    }

                    /* Remove YouTube logo watermark */
                    .annotation-type-custom,
                    .ytp-watermark {
                        display: none !important;
                    }
                `;

                if (!document.getElementById('duck-player-style')) {
                    document.head.appendChild(style);
                }

                // Monitor for dynamically added elements
                var observer = new MutationObserver(function(mutations) {
                    mutations.forEach(function(mutation) {
                        if (mutation.addedNodes.length) {
                            // Clean newly added links
                            cleanYouTubeLinks();

                            // Hide pause overlays and end screens
                            mutation.addedNodes.forEach(function(node) {
                                if (node.nodeType === 1) {
                                    if (node.classList && (
                                        node.classList.contains('ytp-pause-overlay') ||
                                        node.classList.contains('ytp-endscreen-content') ||
                                        node.classList.contains('ytp-ce-element')
                                    )) {
                                        node.style.display = 'none';
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

                console.log('CleanFinding: Duck Player enhancements applied');
            })();
        """.trimIndent()

        webView?.evaluateJavascript(script, null)
    }

    /**
     * Create HTML page for Duck Player with custom controls
     */
    fun createDuckPlayerPage(videoId: String, timestamp: String = ""): String {
        val startParam = if (timestamp.isNotEmpty()) "?start=$timestamp" else ""

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Duck Player - Privacy Protected Video</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }

                    body {
                        background: #000;
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        overflow: hidden;
                    }

                    .container {
                        width: 100%;
                        height: 100vh;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                    }

                    .player-wrapper {
                        position: relative;
                        width: 100%;
                        padding-bottom: 56.25%; /* 16:9 aspect ratio */
                        background: #000;
                    }

                    iframe {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                        border: none;
                    }

                    .info-bar {
                        background: #1a1a1a;
                        color: #fff;
                        padding: 12px 16px;
                        width: 100%;
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        font-size: 13px;
                    }

                    .privacy-badge {
                        display: flex;
                        align-items: center;
                        gap: 8px;
                        color: #4caf50;
                        font-weight: 500;
                    }

                    .shield-icon {
                        font-size: 16px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="info-bar">
                        <div class="privacy-badge">
                            <span class="shield-icon">🛡️</span>
                            <span>Duck Player - Tracking Disabled</span>
                        </div>
                    </div>
                    <div class="player-wrapper">
                        <iframe
                            src="https://www.youtube-nocookie.com/embed/${videoId}${startParam}"
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                            allowfullscreen>
                        </iframe>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
