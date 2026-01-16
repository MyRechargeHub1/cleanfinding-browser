package com.cleanfinding.browser

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel
import java.io.Closeable

/**
 * Manager class for handling browsing history operations
 * Provides a simplified API for history management
 *
 * IMPORTANT: Call cleanup() or close() when done to prevent memory leaks
 */
class HistoryManager(context: Context) : Closeable {

    private val database = BrowserDatabase.getDatabase(context)
    private val historyDao = database.historyDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Record a page visit
     * If URL already exists, updates visit count and timestamp
     * Ignores incognito mode visits
     *
     * @param url The URL visited
     * @param title Page title
     * @param isIncognito Whether this is an incognito visit
     */
    fun recordVisit(url: String, title: String, isIncognito: Boolean = false) {
        // Don't record incognito visits in persistent history
        if (isIncognito) return

        // Don't record about:blank or empty URLs
        if (url.isBlank() || url == "about:blank") return

        scope.launch {
            try {
                // Check if URL already exists
                val existing = historyDao.getHistoryByUrl(url)

                if (existing != null) {
                    // Update existing entry with new visit time and incremented count
                    historyDao.updateVisitCount(
                        url = url,
                        visitTime = System.currentTimeMillis(),
                        visitCount = existing.visitCount + 1
                    )
                } else {
                    // Create new history entry
                    val historyItem = HistoryItem(
                        url = url,
                        title = title.take(200), // Limit title length
                        visitTime = System.currentTimeMillis(),
                        visitCount = 1,
                        isIncognito = false
                    )
                    historyDao.insertHistory(historyItem)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Get recent history items
     * @param limit Maximum number of items to return (default 500)
     * @return Flow of history items
     */
    fun getRecentHistory(limit: Int = 500): Flow<List<HistoryItem>> {
        return historyDao.getRecentHistory(limit)
    }

    /**
     * Search history by query
     * Searches both URL and title fields
     *
     * @param query Search query
     * @return Flow of matching history items
     */
    fun searchHistory(query: String): Flow<List<HistoryItem>> {
        return if (query.isBlank()) {
            getRecentHistory()
        } else {
            historyDao.searchHistory(query)
        }
    }

    /**
     * Get history for a specific date range
     * @param startTime Start timestamp (inclusive)
     * @param endTime End timestamp (exclusive)
     * @return Flow of history items in date range
     */
    fun getHistoryByDateRange(startTime: Long, endTime: Long): Flow<List<HistoryItem>> {
        return historyDao.getHistoryByDateRange(startTime, endTime)
    }

    /**
     * Delete a specific history item
     * @param historyId The history item ID to delete
     */
    fun deleteHistoryItem(historyId: Long) {
        scope.launch {
            historyDao.deleteHistoryItem(historyId)
        }
    }

    /**
     * Delete history older than specified timestamp
     * @param beforeTime Delete history before this timestamp
     * @param onComplete Callback with number of items deleted
     */
    fun deleteHistoryBefore(beforeTime: Long, onComplete: ((Int) -> Unit)? = null) {
        scope.launch {
            val count = historyDao.deleteHistoryBefore(beforeTime)
            withContext(Dispatchers.Main) {
                onComplete?.invoke(count)
            }
        }
    }

    /**
     * Clear all browsing history
     * @param onComplete Callback with number of items deleted
     */
    fun clearAllHistory(onComplete: ((Int) -> Unit)? = null) {
        scope.launch {
            val count = historyDao.clearAllHistory()
            withContext(Dispatchers.Main) {
                onComplete?.invoke(count)
            }
        }
    }

    /**
     * Get total history count
     * @param onResult Callback with total count
     */
    fun getHistoryCount(onResult: (Int) -> Unit) {
        scope.launch {
            val count = historyDao.getHistoryCount()
            withContext(Dispatchers.Main) {
                onResult(count)
            }
        }
    }

    /**
     * Delete history by time period
     * Convenience methods for common time ranges
     */
    object TimePeriod {
        const val LAST_HOUR = 3_600_000L // 1 hour in milliseconds
        const val LAST_DAY = 86_400_000L // 24 hours
        const val LAST_WEEK = 604_800_000L // 7 days
        const val LAST_MONTH = 2_592_000_000L // 30 days

        fun getTimestampBefore(period: Long): Long {
            return System.currentTimeMillis() - period
        }
    }

    /**
     * Delete history from last hour
     */
    fun deleteLastHour(onComplete: ((Int) -> Unit)? = null) {
        deleteHistoryBefore(TimePeriod.getTimestampBefore(TimePeriod.LAST_HOUR), onComplete)
    }

    /**
     * Delete history from last 24 hours
     */
    fun deleteLastDay(onComplete: ((Int) -> Unit)? = null) {
        deleteHistoryBefore(TimePeriod.getTimestampBefore(TimePeriod.LAST_DAY), onComplete)
    }

    /**
     * Delete history from last week
     */
    fun deleteLastWeek(onComplete: ((Int) -> Unit)? = null) {
        deleteHistoryBefore(TimePeriod.getTimestampBefore(TimePeriod.LAST_WEEK), onComplete)
    }

    /**
     * Delete history from last month
     */
    fun deleteLastMonth(onComplete: ((Int) -> Unit)? = null) {
        deleteHistoryBefore(TimePeriod.getTimestampBefore(TimePeriod.LAST_MONTH), onComplete)
    }

    /**
     * Cancel coroutine scope to prevent memory leaks
     * CRITICAL: Must be called when HistoryManager is no longer needed
     */
    fun cleanup() {
        scope.cancel()
    }

    /**
     * Implementation of Closeable interface
     * Calls cleanup() to release resources
     */
    override fun close() {
        cleanup()
    }
}
