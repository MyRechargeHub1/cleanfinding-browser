package com.cleanfinding.browser

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Download status enum
 */
enum class DownloadStatus {
    PENDING,    // Download queued
    RUNNING,    // Currently downloading
    PAUSED,     // Download paused by user
    SUCCESSFUL, // Download completed successfully
    FAILED,     // Download failed
    CANCELLED   // Download cancelled by user
}

/**
 * Data class representing a download entry
 * Stored in Room database for persistent download tracking
 */
@Entity(tableName = "downloads")
data class Download(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "download_id")
    val downloadId: Long = 0, // Android DownloadManager ID

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "filename")
    val filename: String,

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    @ColumnInfo(name = "file_size")
    val fileSize: Long = 0, // Bytes

    @ColumnInfo(name = "bytes_downloaded")
    val bytesDownloaded: Long = 0,

    @ColumnInfo(name = "download_time")
    val downloadTime: Long,

    @ColumnInfo(name = "file_path")
    val filePath: String? = null,

    @ColumnInfo(name = "status")
    val status: DownloadStatus = DownloadStatus.PENDING
) {
    /**
     * Returns download progress as percentage (0-100)
     */
    fun getProgress(): Int {
        return if (fileSize > 0) {
            ((bytesDownloaded.toFloat() / fileSize.toFloat()) * 100).toInt()
        } else {
            0
        }
    }

    /**
     * Returns human-readable file size
     */
    fun getFormattedSize(): String {
        return formatBytes(fileSize)
    }

    /**
     * Returns human-readable downloaded size
     */
    fun getFormattedDownloaded(): String {
        return formatBytes(bytesDownloaded)
    }

    /**
     * Returns formatted date string for display
     */
    fun getFormattedDate(): String {
        val date = java.text.SimpleDateFormat("MMM d, yyyy HH:mm", java.util.Locale.getDefault())
        return date.format(java.util.Date(downloadTime))
    }

    /**
     * Check if download is in progress
     */
    fun isInProgress(): Boolean {
        return status == DownloadStatus.RUNNING || status == DownloadStatus.PENDING
    }

    /**
     * Check if download is complete
     */
    fun isComplete(): Boolean {
        return status == DownloadStatus.SUCCESSFUL
    }

    /**
     * Check if download can be retried
     */
    fun canRetry(): Boolean {
        return status == DownloadStatus.FAILED || status == DownloadStatus.CANCELLED
    }

    companion object {
        /**
         * Format bytes to human-readable string
         */
        fun formatBytes(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
                bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
                else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            }
        }
    }
}
