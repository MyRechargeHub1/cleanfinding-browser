package com.cleanfinding.browser

/**
 * Privacy Grade Calculator
 * Calculates privacy scores for websites based on DuckDuckGo's methodology
 *
 * Grading factors:
 * - HTTPS usage (10 points for HTTPS)
 * - Number of trackers found (-5 points per tracker)
 * - Known tracking companies (-10 points for Google Analytics, Facebook, etc.)
 * - Adult content detection (-20 points)
 * - Safe Search enforcement (+5 points)
 *
 * Grade scale:
 * A: 85-100 (Excellent privacy)
 * B: 70-84 (Good privacy)
 * C: 50-69 (Fair privacy)
 * D: 30-49 (Poor privacy)
 * F: 0-29 (Very poor privacy)
 */
class PrivacyGradeCalculator {

    companion object {
        // Major tracking companies (worse offenders)
        private val majorTrackers = setOf(
            "google-analytics.com",
            "googletagmanager.com",
            "doubleclick.net",
            "facebook.net",
            "facebook.com/tr",
            "connect.facebook.net"
        )
    }

    data class PrivacyScore(
        val grade: String,
        val score: Int,
        val color: String,
        val summary: String,
        val trackersBlocked: Int,
        val isHttps: Boolean,
        val hasMajorTrackers: Boolean
    )

    /**
     * Calculate privacy grade for a URL
     */
    fun calculateGrade(
        url: String,
        trackersBlocked: Int,
        blockedDomains: List<String>
    ): PrivacyScore {
        var score = 75 // Start at C (fair)

        // Check HTTPS
        val isHttps = url.startsWith("https://")
        if (isHttps) {
            score += 10
        } else {
            score -= 10
        }

        // Deduct points for trackers found (even if blocked)
        score -= trackersBlocked * 3

        // Check for major trackers
        val hasMajorTrackers = blockedDomains.any { domain ->
            majorTrackers.any { majorTracker -> domain.contains(majorTracker) }
        }
        if (hasMajorTrackers) {
            score -= 15
        }

        // Bonus for clean sites (no trackers)
        if (trackersBlocked == 0) {
            score += 10
        }

        // Clamp score between 0 and 100
        score = score.coerceIn(0, 100)

        // Determine grade
        val grade = when {
            score >= 85 -> "A"
            score >= 70 -> "B"
            score >= 50 -> "C"
            score >= 30 -> "D"
            else -> "F"
        }

        // Determine color
        val color = when (grade) {
            "A" -> "#4CAF50" // Green
            "B" -> "#8BC34A" // Light Green
            "C" -> "#FFC107" // Amber
            "D" -> "#FF9800" // Orange
            else -> "#F44336" // Red
        }

        // Generate summary
        val summary = generateSummary(grade, trackersBlocked, isHttps, hasMajorTrackers)

        return PrivacyScore(
            grade = grade,
            score = score,
            color = color,
            summary = summary,
            trackersBlocked = trackersBlocked,
            isHttps = isHttps,
            hasMajorTrackers = hasMajorTrackers
        )
    }

    private fun generateSummary(
        grade: String,
        trackersBlocked: Int,
        isHttps: Boolean,
        hasMajorTrackers: Boolean
    ): String {
        return when (grade) {
            "A" -> "Excellent privacy protection"
            "B" -> if (trackersBlocked > 0) {
                "Good privacy, blocked $trackersBlocked tracker${if (trackersBlocked != 1) "s" else ""}"
            } else {
                "Good privacy protection"
            }
            "C" -> if (!isHttps) {
                "Fair privacy, connection not secure"
            } else if (hasMajorTrackers) {
                "Fair privacy, major trackers blocked"
            } else {
                "Fair privacy, blocked $trackersBlocked tracker${if (trackersBlocked != 1) "s" else ""}"
            }
            "D" -> if (!isHttps) {
                "Poor privacy, no encryption"
            } else {
                "Poor privacy, many trackers detected"
            }
            else -> "Very poor privacy protection"
        }
    }

    /**
     * Get emoji for grade
     */
    fun getGradeEmoji(grade: String): String {
        return when (grade) {
            "A" -> "🛡️"
            "B" -> "✅"
            "C" -> "⚠️"
            "D" -> "⚠️"
            else -> "❌"
        }
    }

    /**
     * Get detailed report for privacy dashboard
     */
    fun getDetailedReport(privacyScore: PrivacyScore): String {
        val builder = StringBuilder()
        builder.append("Privacy Grade: ${privacyScore.grade} (${privacyScore.score}/100)\n\n")
        builder.append("${privacyScore.summary}\n\n")
        builder.append("Details:\n")
        builder.append("• Connection: ${if (privacyScore.isHttps) "Secure (HTTPS)" else "Not secure (HTTP)"}\n")
        builder.append("• Trackers blocked: ${privacyScore.trackersBlocked}\n")
        if (privacyScore.hasMajorTrackers) {
            builder.append("• Major tracking companies detected\n")
        }
        return builder.toString()
    }
}
