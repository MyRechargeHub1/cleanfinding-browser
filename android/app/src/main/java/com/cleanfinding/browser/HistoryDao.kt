package com.cleanfinding.browser

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for browsing history
 * Provides methods to interact with the history database
 */
@Dao
interface HistoryDao {

    /**
     * Get recent history items (non-incognito only)
     * @param limit Maximum number of items to return
     * @return Flow of history items ordered by visit time (newest first)
     */
    @Query("SELECT * FROM history WHERE is_incognito = 0 ORDER BY visit_time DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 500): Flow<List<HistoryItem>>

    /**
     * Search history by URL or title
     * @param query Search query (will be wrapped with % for LIKE matching)
     * @return Flow of matching history items
     */
    @Query("SELECT * FROM history WHERE is_incognito = 0 AND (url LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%') ORDER BY visit_time DESC LIMIT 100")
    fun searchHistory(query: String): Flow<List<HistoryItem>>

    /**
     * Get history items for a specific date range
     * @param startTime Start timestamp (inclusive)
     * @param endTime End timestamp (exclusive)
     * @return Flow of history items in the date range
     */
    @Query("SELECT * FROM history WHERE is_incognito = 0 AND visit_time >= :startTime AND visit_time < :endTime ORDER BY visit_time DESC")
    fun getHistoryByDateRange(startTime: Long, endTime: Long): Flow<List<HistoryItem>>

    /**
     * Get history for a specific URL
     * @param url The URL to look up
     * @return History item if exists, null otherwise
     */
    @Query("SELECT * FROM history WHERE url = :url AND is_incognito = 0 ORDER BY visit_time DESC LIMIT 1")
    suspend fun getHistoryByUrl(url: String): HistoryItem?

    /**
     * Insert or update a history item
     * If URL already exists, update visit count and time
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryItem): Long

    /**
     * Update visit count for existing URL
     * @param url The URL to update
     * @param visitTime New visit timestamp
     * @param visitCount New visit count
     */
    @Query("UPDATE history SET visit_time = :visitTime, visit_count = :visitCount WHERE url = :url")
    suspend fun updateVisitCount(url: String, visitTime: Long, visitCount: Int)

    /**
     * Delete a specific history item by ID
     * @param id The history item ID to delete
     */
    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryItem(id: Long)

    /**
     * Delete history items before a specific timestamp
     * @param beforeTime Delete all items older than this timestamp
     * @return Number of items deleted
     */
    @Query("DELETE FROM history WHERE visit_time < :beforeTime")
    suspend fun deleteHistoryBefore(beforeTime: Long): Int

    /**
     * Delete all history
     * @return Number of items deleted
     */
    @Query("DELETE FROM history WHERE is_incognito = 0")
    suspend fun clearAllHistory(): Int

    /**
     * Get total history count (non-incognito only)
     * @return Total number of history items
     */
    @Query("SELECT COUNT(*) FROM history WHERE is_incognito = 0")
    suspend fun getHistoryCount(): Int

    /**
     * Get history grouped by date (for date headers in UI)
     * Returns distinct dates where history exists
     */
    @Query("""
        SELECT DISTINCT
            date(visit_time / 1000, 'unixepoch', 'localtime') as date
        FROM history
        WHERE is_incognito = 0
        ORDER BY visit_time DESC
    """)
    fun getHistoryDates(): Flow<List<String>>
}
