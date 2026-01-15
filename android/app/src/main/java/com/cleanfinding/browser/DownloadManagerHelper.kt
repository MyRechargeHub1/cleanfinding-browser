package com.cleanfinding.browser

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manager class for handling download operations
 * Uses Android DownloadManager API + Room database for tracking
 */
class DownloadManagerHelper(private val context: Context) {

    private val database = BrowserDatabase.getDatabase(context)
    private val downloadDao = database.downloadDao()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    /**
     * BroadcastReceiver to listen for download completion
     */
    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val downloadId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: return
            if (downloadId != -1L) {
                updateDownloadStatus(downloadId)
            }
        }
    }

    init {
        // Register broadcast receiver for download completion
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            context,
            downloadReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    /**
     * Start a new download
     * @param url Download URL
     * @param filename Suggested filename
     * @param mimeType MIME type of the file
     * @param userAgent User agent string
     * @param contentDisposition Content disposition header
     * @return Download ID from Android DownloadManager
     */
    fun startDownload(
        url: String,
        filename: String? = null,
        mimeType: String? = null,
        userAgent: String? = null,
        contentDisposition: String? = null
    ): Long {
        val fileName = filename ?: URLUtil.guessFileName(url, contentDisposition, mimeType)

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(fileName)
            setDescription("Downloading...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)

            userAgent?.let { addRequestHeader("User-Agent", it) }
        }

        val downloadId = downloadManager.enqueue(request)

        // Save to database
        scope.launch {
            val download = Download(
                downloadId = downloadId,
                url = url,
                filename = fileName,
                mimeType = mimeType ?: "application/octet-stream",
                downloadTime = System.currentTimeMillis(),
                status = DownloadStatus.PENDING
            )
            downloadDao.insertDownload(download)
        }

        return downloadId
    }

    /**
     * Update download status from DownloadManager
     * @param downloadId Android DownloadManager ID
     */
    private fun updateDownloadStatus(downloadId: Long) {
        scope.launch {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor: Cursor? = downloadManager.query(query)

            cursor?.use {
                if (it.moveToFirst()) {
                    val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val reasonIndex = it.getColumnIndex(DownloadManager.COLUMN_REASON)
                    val uriIndex = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    val bytesDownloadedIndex = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalBytesIndex = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                    val status = it.getInt(statusIndex)
                    val bytesDownloaded = it.getLong(bytesDownloadedIndex)
                    val totalBytes = it.getLong(totalBytesIndex)
                    val localUri = it.getString(uriIndex)

                    val downloadStatus = when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            // Update with file path
                            downloadDao.updateStatusAndPath(
                                downloadId,
                                DownloadStatus.SUCCESSFUL,
                                localUri ?: ""
                            )
                            // Update file size
                            downloadDao.getDownloadByDownloadId(downloadId)?.let { download ->
                                downloadDao.updateDownload(
                                    download.copy(
                                        fileSize = totalBytes,
                                        bytesDownloaded = totalBytes
                                    )
                                )
                            }
                            return@launch
                        }
                        DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                        DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
                        DownloadManager.STATUS_PENDING -> DownloadStatus.PENDING
                        DownloadManager.STATUS_RUNNING -> {
                            // Update progress
                            downloadDao.updateProgress(downloadId, bytesDownloaded)
                            // Update file size if available
                            if (totalBytes > 0) {
                                downloadDao.getDownloadByDownloadId(downloadId)?.let { download ->
                                    downloadDao.updateDownload(download.copy(fileSize = totalBytes))
                                }
                            }
                            DownloadStatus.RUNNING
                        }
                        else -> DownloadStatus.PENDING
                    }

                    if (status != DownloadManager.STATUS_RUNNING) {
                        downloadDao.updateStatus(downloadId, downloadStatus)
                    }
                }
            }
        }
    }

    /**
     * Get all downloads
     * @return Flow of all downloads
     */
    fun getAllDownloads(): Flow<List<Download>> {
        return downloadDao.getAllDownloads()
    }

    /**
     * Get active downloads
     * @return Flow of active downloads
     */
    fun getActiveDownloads(): Flow<List<Download>> {
        return downloadDao.getActiveDownloads()
    }

    /**
     * Get downloads by status
     * @param status Download status
     * @return Flow of downloads with matching status
     */
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<Download>> {
        return downloadDao.getDownloadsByStatus(status)
    }

    /**
     * Pause a download
     * @param downloadId Android DownloadManager ID
     */
    fun pauseDownload(downloadId: Long) {
        // Note: DownloadManager doesn't support pause/resume directly
        // This would require implementing custom download logic
        scope.launch {
            downloadDao.updateStatus(downloadId, DownloadStatus.PAUSED)
        }
    }

    /**
     * Resume a download
     * @param downloadId Android DownloadManager ID
     */
    fun resumeDownload(downloadId: Long) {
        // Note: DownloadManager doesn't support pause/resume directly
        scope.launch {
            downloadDao.updateStatus(downloadId, DownloadStatus.RUNNING)
        }
    }

    /**
     * Cancel a download
     * @param downloadId Android DownloadManager ID
     */
    fun cancelDownload(downloadId: Long) {
        downloadManager.remove(downloadId)
        scope.launch {
            downloadDao.updateStatus(downloadId, DownloadStatus.CANCELLED)
        }
    }

    /**
     * Delete a download record (and file if exists)
     * @param download Download to delete
     * @param deleteFile Whether to delete the actual file
     */
    fun deleteDownload(download: Download, deleteFile: Boolean = false) {
        scope.launch {
            // Remove from DownloadManager
            if (download.downloadId > 0) {
                downloadManager.remove(download.downloadId)
            }

            // Delete file if requested
            if (deleteFile && download.filePath != null) {
                try {
                    val uri = Uri.parse(download.filePath)
                    context.contentResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Remove from database
            downloadDao.deleteDownload(download.id)
        }
    }

    /**
     * Clear completed downloads
     * @param onComplete Callback with number of items cleared
     */
    fun clearCompletedDownloads(onComplete: ((Int) -> Unit)? = null) {
        scope.launch {
            val count = downloadDao.clearCompletedDownloads()
            withContext(Dispatchers.Main) {
                onComplete?.invoke(count)
            }
        }
    }

    /**
     * Clear all downloads
     * @param onComplete Callback with number of items cleared
     */
    fun clearAllDownloads(onComplete: ((Int) -> Unit)? = null) {
        scope.launch {
            val count = downloadDao.clearAllDownloads()
            withContext(Dispatchers.Main) {
                onComplete?.invoke(count)
            }
        }
    }

    /**
     * Open a downloaded file
     * @param download Download to open
     * @return True if file was opened successfully
     */
    fun openDownload(download: Download): Boolean {
        if (download.filePath == null || download.status != DownloadStatus.SUCCESSFUL) {
            return false
        }

        return try {
            val uri = Uri.parse(download.filePath)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, download.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Share a downloaded file
     * @param download Download to share
     * @return True if share intent was created successfully
     */
    fun shareDownload(download: Download): Boolean {
        if (download.filePath == null || download.status != DownloadStatus.SUCCESSFUL) {
            return false
        }

        return try {
            val uri = Uri.parse(download.filePath)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = download.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share file"))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get download progress
     * @param downloadId Android DownloadManager ID
     * @return Download progress object or null if not found
     */
    suspend fun getDownloadProgress(downloadId: Long): DownloadProgress? {
        return withContext(Dispatchers.IO) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor: Cursor? = downloadManager.query(query)

            cursor?.use {
                if (it.moveToFirst()) {
                    val bytesDownloadedIndex = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalBytesIndex = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                    val bytesDownloaded = it.getLong(bytesDownloadedIndex)
                    val totalBytes = it.getLong(totalBytesIndex)

                    return@withContext DownloadProgress(bytesDownloaded, totalBytes)
                }
            }
            null
        }
    }

    /**
     * Unregister broadcast receiver (call when no longer needed)
     */
    fun cleanup() {
        try {
            context.unregisterReceiver(downloadReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }

    /**
     * Data class for download progress
     */
    data class DownloadProgress(
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) {
        val progress: Int
            get() = if (totalBytes > 0) {
                ((bytesDownloaded.toFloat() / totalBytes.toFloat()) * 100).toInt()
            } else {
                0
            }
    }
}
