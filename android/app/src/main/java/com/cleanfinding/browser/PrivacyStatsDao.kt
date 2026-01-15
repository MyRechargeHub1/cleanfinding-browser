package com.cleanfinding.browser

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Privacy Statistics
 */
@Dao
interface PrivacyStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: PrivacyStats): Long

    /**
     * Get total trackers blocked all-time
     */
    @Query("SELECT SUM(trackers_blocked) FROM privacy_stats")
    fun getTotalTrackersBlocked(): Flow<Int?>

    /**
     * Get trackers blocked today
     */
    @Query("SELECT SUM(trackers_blocked) FROM privacy_stats WHERE timestamp >= :startOfDay")
    fun getTrackersBlockedToday(startOfDay: Long): Flow<Int?>

    /**
     * Get trackers blocked this week
     */
    @Query("SELECT SUM(trackers_blocked) FROM privacy_stats WHERE timestamp >= :startOfWeek")
    fun getTrackersBlockedThisWeek(startOfWeek: Long): Flow<Int?>

    /**
     * Get most tracked domains
     */
    @Query("""
        SELECT domain, SUM(trackers_blocked) as total
        FROM privacy_stats
        WHERE trackers_blocked > 0
        GROUP BY domain
        ORDER BY total DESC
        LIMIT :limit
    """)
    suspend fun getMostTrackedDomains(limit: Int = 10): List<DomainTrackingStats>

    /**
     * Get privacy grade distribution
     */
    @Query("""
        SELECT privacy_grade, COUNT(*) as count
        FROM privacy_stats
        GROUP BY privacy_grade
        ORDER BY privacy_grade
    """)
    suspend fun getPrivacyGradeDistribution(): List<GradeDistribution>

    /**
     * Get recent privacy stats
     */
    @Query("SELECT * FROM privacy_stats ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentStats(limit: Int = 100): Flow<List<PrivacyStats>>

    /**
     * Clear all privacy stats
     */
    @Query("DELETE FROM privacy_stats")
    suspend fun clearAllStats(): Int

    /**
     * Clear old stats (older than 30 days)
     */
    @Query("DELETE FROM privacy_stats WHERE timestamp < :cutoffTime")
    suspend fun clearOldStats(cutoffTime: Long): Int
}

/**
 * Data class for domain tracking stats
 */
data class DomainTrackingStats(
    val domain: String,
    val total: Int
)

/**
 * Data class for privacy grade distribution
 */
data class GradeDistribution(
    val privacy_grade: String,
    val count: Int
)
