package com.cleanfinding.browser

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Calendar

/**
 * Privacy Statistics Manager
 * Tracks and manages privacy statistics for the dashboard
 */
class PrivacyStatsManager(context: Context) {

    private val database = BrowserDatabase.getDatabase(context)
    private val privacyStatsDao = database.privacyStatsDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Record privacy stats for a page visit
     */
    fun recordPageStats(
        url: String,
        trackersBlocked: Int,
        blockedDomains: List<String>,
        privacyGrade: String,
        isHttps: Boolean
    ) {
        scope.launch {
            val stats = PrivacyStats(
                url = url,
                domain = PrivacyStats.extractDomain(url),
                trackersBlocked = trackersBlocked,
                blockedDomains = JSONArray(blockedDomains).toString(),
                privacyGrade = privacyGrade,
                isHttps = isHttps,
                timestamp = System.currentTimeMillis()
            )
            privacyStatsDao.insertStats(stats)
        }
    }

    /**
     * Get total trackers blocked all-time
     */
    fun getTotalTrackersBlocked(): Flow<Int?> {
        return privacyStatsDao.getTotalTrackersBlocked()
    }

    /**
     * Get trackers blocked today
     */
    fun getTrackersBlockedToday(): Flow<Int?> {
        val startOfDay = getStartOfDay()
        return privacyStatsDao.getTrackersBlockedToday(startOfDay)
    }

    /**
     * Get trackers blocked this week
     */
    fun getTrackersBlockedThisWeek(): Flow<Int?> {
        val startOfWeek = getStartOfWeek()
        return privacyStatsDao.getTrackersBlockedThisWeek(startOfWeek)
    }

    /**
     * Get most tracked domains
     */
    suspend fun getMostTrackedDomains(limit: Int = 10): List<DomainTrackingStats> {
        return privacyStatsDao.getMostTrackedDomains(limit)
    }

    /**
     * Get privacy grade distribution
     */
    suspend fun getPrivacyGradeDistribution(): List<GradeDistribution> {
        return privacyStatsDao.getPrivacyGradeDistribution()
    }

    /**
     * Get recent privacy stats
     */
    fun getRecentStats(limit: Int = 100): Flow<List<PrivacyStats>> {
        return privacyStatsDao.getRecentStats(limit)
    }

    /**
     * Clear all privacy stats
     */
    fun clearAllStats(onComplete: ((Int) -> Unit)? = null) {
        scope.launch {
            val count = privacyStatsDao.clearAllStats()
            onComplete?.invoke(count)
        }
    }

    /**
     * Clear old stats (older than 30 days)
     */
    fun clearOldStats(onComplete: ((Int) -> Unit)? = null) {
        scope.launch {
            val cutoffTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000) // 30 days ago
            val count = privacyStatsDao.clearOldStats(cutoffTime)
            onComplete?.invoke(count)
        }
    }

    /**
     * Get start of current day (midnight)
     */
    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Get start of current week (Monday)
     */
    private fun getStartOfWeek(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Dashboard summary data
     */
    data class DashboardSummary(
        val totalTrackersBlocked: Int,
        val trackersBlockedToday: Int,
        val trackersBlockedThisWeek: Int,
        val mostTrackedDomains: List<DomainTrackingStats>,
        val gradeDistribution: List<GradeDistribution>
    )

    /**
     * Get complete dashboard summary
     */
    suspend fun getDashboardSummary(): DashboardSummary {
        val mostTrackedDomains = getMostTrackedDomains(10)
        val gradeDistribution = getPrivacyGradeDistribution()

        return DashboardSummary(
            totalTrackersBlocked = 0, // Will be updated from Flow
            trackersBlockedToday = 0, // Will be updated from Flow
            trackersBlockedThisWeek = 0, // Will be updated from Flow
            mostTrackedDomains = mostTrackedDomains,
            gradeDistribution = gradeDistribution
        )
    }
}
