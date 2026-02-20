package com.cleanfinding.browser

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Activity for displaying and managing downloads
 * Features:
 * - View all downloads
 * - Open downloaded files
 * - Cancel active downloads
 * - Retry failed downloads
 * - Delete downloads
 * - Clear all downloads
 */
class DownloadsActivity : AppCompatActivity() {

    private lateinit var downloadManager: DownloadManagerHelper
    private lateinit var downloadAdapter: DownloadAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var clearButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var emptyStateLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloads)

        downloadManager = DownloadManagerHelper(this)

        initViews()
        setupRecyclerView()
        setupButtons()
        loadDownloads()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.downloadsRecyclerView)
        clearButton = findViewById(R.id.clearButton)
        backButton = findViewById(R.id.backButton)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
    }

    private fun setupRecyclerView() {
        downloadAdapter = DownloadAdapter(
            onItemClick = { download ->
                // Open downloaded file
                if (download.status == DownloadStatus.SUCCESSFUL) {
                    val opened = downloadManager.openDownload(download)
                    if (!opened) {
                        Toast.makeText(
                            this,
                            "Cannot open file. No app available.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onDeleteClick = { download ->
                showDeleteDialog(download)
            },
            onCancelClick = { download ->
                // Cancel download
                downloadManager.cancelDownload(download.downloadId)
                Toast.makeText(this, "Download cancelled", Toast.LENGTH_SHORT).show()
            },
            onRetryClick = { download ->
                downloadManager.retryDownload(download) { success, errorMessage ->
                    val message = if (success) {
                        "Download restarted"
                    } else {
                        "Retry failed${errorMessage?.let { ": $it" } ?: ""}"
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@DownloadsActivity)
            adapter = downloadAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupButtons() {
        backButton.setOnClickListener {
            finish()
        }

        clearButton.setOnClickListener {
            showClearDialog()
        }
    }

    private fun loadDownloads() {
        lifecycleScope.launch {
            downloadManager.getAllDownloads().collectLatest { downloads ->
                downloadAdapter.submitList(downloads)
                updateEmptyState(downloads.isEmpty())
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            emptyStateLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showDeleteDialog(download: Download) {
        val message = if (download.status == DownloadStatus.SUCCESSFUL) {
            "Delete \"${download.filename}\"? This will also delete the downloaded file."
        } else {
            "Delete \"${download.filename}\" from download list?"
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Download?")
            .setMessage(message)
            .setPositiveButton("Delete") { _, _ ->
                val deleteFile = download.status == DownloadStatus.SUCCESSFUL
                downloadManager.deleteDownload(download, deleteFile)
                Toast.makeText(this, "Download deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showClearDialog() {
        val options = arrayOf(
            "Clear completed downloads",
            "Clear all downloads"
        )

        AlertDialog.Builder(this)
            .setTitle("Clear Downloads")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> clearCompletedDownloads()
                    1 -> clearAllDownloads()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearCompletedDownloads() {
        AlertDialog.Builder(this)
            .setTitle("Clear Completed?")
            .setMessage("Clear all completed downloads from the list?")
            .setPositiveButton("Clear") { _, _ ->
                downloadManager.clearCompletedDownloads { count ->
                    Toast.makeText(
                        this,
                        "Cleared $count completed download${if (count != 1) "s" else ""}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAllDownloads() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Downloads?")
            .setMessage("This will remove all downloads from the list. Downloaded files will remain on your device.")
            .setPositiveButton("Clear All") { _, _ ->
                downloadManager.clearAllDownloads { count ->
                    Toast.makeText(
                        this,
                        "Cleared $count download${if (count != 1) "s" else ""}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't cleanup downloadManager here as it might be used elsewhere
        // downloadManager.cleanup()
    }
}
