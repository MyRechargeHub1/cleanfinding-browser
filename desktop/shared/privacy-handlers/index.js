/**
 * CleanFinding Browser - Privacy Handlers
 * Cross-platform privacy protection modules
 *
 * @version 1.4.0
 * @platform Android, Windows, macOS, Linux, iOS
 */

const DuckPlayerHandler = require('./DuckPlayerHandler');

// Export all handlers
module.exports = {
    DuckPlayerHandler
    // More handlers will be added:
    // EmailProtectionHandler (coming soon)
    // CookieConsentHandler (coming soon)
    // TrackerBlocker (coming soon)
    // PrivacyGradeCalculator (coming soon)
};

// Browser global exports
if (typeof window !== 'undefined') {
    window.CleanFindingPrivacy = module.exports;
}
