package com.cleanfinding.browser

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Privacy Statistics Entity
 * Tracks blocking events for privacy dashboard
 */
@Entity(tableName = "privacy_stats")
data class PrivacyStats(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "domain") val domain: String,
    @ColumnInfo(name = "trackers_blocked") val trackersBlocked: Int,
    @ColumnInfo(name = "blocked_domains") val blockedDomains: String, // JSON array of blocked domains
    @ColumnInfo(name = "privacy_grade") val privacyGrade: String,
    @ColumnInfo(name = "is_https") val isHttps: Boolean,
    @ColumnInfo(name = "timestamp") val timestamp: Long
) {
    companion object {
        fun extractDomain(url: String): String {
            return try {
                val uri = android.net.Uri.parse(url)
                uri.host ?: url
            } catch (e: Exception) {
                url
            }
        }
    }
}
