package com.cleanfinding.browser

import android.webkit.WebView

/**
 * Global Privacy Control (GPC) Handler
 * Implements GPC signal to tell websites not to sell/share user data
 * Based on https://globalprivacycontrol.org/
 */
class GlobalPrivacyControlHandler {

    /**
     * Inject GPC signal into page
     * Sets navigator.globalPrivacyControl = true
     * Also sends DNT (Do Not Track) signal for broader compatibility
     */
    fun injectGPCSignal(webView: WebView?) {
        val script = """
            (function() {
                console.log('CleanFinding: Enabling Global Privacy Control (GPC)');

                // Set GPC signal (modern standard)
                if (!navigator.globalPrivacyControl) {
                    Object.defineProperty(navigator, 'globalPrivacyControl', {
                        value: true,
                        writable: false,
                        configurable: false
                    });
                }

                // Set DNT (Do Not Track) for broader compatibility
                if (!navigator.doNotTrack) {
                    Object.defineProperty(navigator, 'doNotTrack', {
                        value: '1',
                        writable: false,
                        configurable: false
                    });
                }

                // Also set for msDoNotTrack (Microsoft browsers)
                if (navigator.msDoNotTrack === undefined) {
                    Object.defineProperty(navigator, 'msDoNotTrack', {
                        value: '1',
                        writable: false,
                        configurable: false
                    });
                }

                console.log('CleanFinding: GPC enabled - navigator.globalPrivacyControl =', navigator.globalPrivacyControl);
                console.log('CleanFinding: DNT enabled - navigator.doNotTrack =', navigator.doNotTrack);
            })();
        """.trimIndent()

        webView?.evaluateJavascript(script, null)
    }

    /**
     * Inject JavaScript to monitor for GPC violations
     * Logs when websites try to override the GPC signal
     */
    fun monitorGPCViolations(webView: WebView?) {
        val script = """
            (function() {
                // Monitor attempts to disable GPC
                const originalDefineProperty = Object.defineProperty;
                Object.defineProperty = function(obj, prop, descriptor) {
                    if ((prop === 'globalPrivacyControl' || prop === 'doNotTrack') &&
                        (descriptor.value === false || descriptor.value === '0')) {
                        console.warn('CleanFinding: Blocked attempt to disable ' + prop);
                        return obj;
                    }
                    return originalDefineProperty.apply(this, arguments);
                };
            })();
        """.trimIndent()

        webView?.evaluateJavascript(script, null)
    }
}
