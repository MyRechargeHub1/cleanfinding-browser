package com.cleanfinding.browser

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class representing a browsing history entry
 * Stored in Room database for persistent history tracking
 */
@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "visit_time")
    val visitTime: Long,

    @ColumnInfo(name = "visit_count")
    val visitCount: Int = 1,

    @ColumnInfo(name = "favicon")
    val favicon: String? = null,

    @ColumnInfo(name = "is_incognito")
    val isIncognito: Boolean = false
) {
    /**
     * Returns a formatted date string for display
     */
    fun getFormattedDate(): String {
        val now = System.currentTimeMillis()
        val diff = now - visitTime

        return when {
            diff < 60_000 -> "Just now" // Less than 1 minute
            diff < 3600_000 -> "${diff / 60_000} minutes ago" // Less than 1 hour
            diff < 86400_000 -> "${diff / 3600_000} hours ago" // Less than 1 day
            diff < 604800_000 -> "${diff / 86400_000} days ago" // Less than 1 week
            else -> {
                val date = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                date.format(java.util.Date(visitTime))
            }
        }
    }

    /**
     * Returns a short domain name for display
     */
    fun getDomain(): String {
        return try {
            val uri = android.net.Uri.parse(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }
}
