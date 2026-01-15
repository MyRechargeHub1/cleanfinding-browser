package com.cleanfinding.browser

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for downloads
 * Provides methods to interact with the downloads database
 */
@Dao
interface DownloadDao {

    /**
     * Get all downloads ordered by download time (newest first)
     * @return Flow of download list
     */
    @Query("SELECT * FROM downloads ORDER BY download_time DESC")
    fun getAllDownloads(): Flow<List<Download>>

    /**
     * Get downloads by status
     * @param status The download status to filter by
     * @return Flow of downloads with matching status
     */
    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY download_time DESC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<Download>>

    /**
     * Get active downloads (pending or running)
     * @return Flow of active downloads
     */
    @Query("SELECT * FROM downloads WHERE status IN ('PENDING', 'RUNNING') ORDER BY download_time DESC")
    fun getActiveDownloads(): Flow<List<Download>>

    /**
     * Get download by database ID
     * @param id The database ID
     * @return Download or null if not found
     */
    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): Download?

    /**
     * Get download by Android DownloadManager ID
     * @param downloadId The Android DownloadManager ID
     * @return Download or null if not found
     */
    @Query("SELECT * FROM downloads WHERE download_id = :downloadId")
    suspend fun getDownloadByDownloadId(downloadId: Long): Download?

    /**
     * Insert a new download
     * @param download The download to insert
     * @return The ID of the inserted download
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: Download): Long

    /**
     * Update an existing download
     * @param download The download to update
     */
    @Update
    suspend fun updateDownload(download: Download)

    /**
     * Update download progress
     * @param downloadId Android DownloadManager ID
     * @param bytesDownloaded Bytes downloaded so far
     */
    @Query("UPDATE downloads SET bytes_downloaded = :bytesDownloaded WHERE download_id = :downloadId")
    suspend fun updateProgress(downloadId: Long, bytesDownloaded: Long)

    /**
     * Update download status
     * @param downloadId Android DownloadManager ID
     * @param status New status
     */
    @Query("UPDATE downloads SET status = :status WHERE download_id = :downloadId")
    suspend fun updateStatus(downloadId: Long, status: DownloadStatus)

    /**
     * Update download status and file path (when complete)
     * @param downloadId Android DownloadManager ID
     * @param status New status
     * @param filePath Path to downloaded file
     */
    @Query("UPDATE downloads SET status = :status, file_path = :filePath WHERE download_id = :downloadId")
    suspend fun updateStatusAndPath(downloadId: Long, status: DownloadStatus, filePath: String)

    /**
     * Delete a download by ID
     * @param id The database ID
     */
    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: Long)

    /**
     * Delete download by Android DownloadManager ID
     * @param downloadId The Android DownloadManager ID
     */
    @Query("DELETE FROM downloads WHERE download_id = :downloadId")
    suspend fun deleteDownloadByDownloadId(downloadId: Long)

    /**
     * Delete all downloads with a specific status
     * @param status The status to match
     * @return Number of downloads deleted
     */
    @Query("DELETE FROM downloads WHERE status = :status")
    suspend fun deleteDownloadsByStatus(status: DownloadStatus): Int

    /**
     * Clear all completed downloads
     * @return Number of downloads deleted
     */
    @Query("DELETE FROM downloads WHERE status = 'SUCCESSFUL'")
    suspend fun clearCompletedDownloads(): Int

    /**
     * Clear all downloads
     * @return Number of downloads deleted
     */
    @Query("DELETE FROM downloads")
    suspend fun clearAllDownloads(): Int

    /**
     * Get total download count
     * @return Total number of downloads
     */
    @Query("SELECT COUNT(*) FROM downloads")
    suspend fun getDownloadCount(): Int

    /**
     * Get count of active downloads
     * @return Number of active downloads
     */
    @Query("SELECT COUNT(*) FROM downloads WHERE status IN ('PENDING', 'RUNNING')")
    suspend fun getActiveDownloadCount(): Int
}
