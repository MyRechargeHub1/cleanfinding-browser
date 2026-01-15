package com.cleanfinding.browser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView Adapter for displaying downloads
 */
class DownloadAdapter(
    private val onItemClick: (Download) -> Unit,
    private val onDeleteClick: (Download) -> Unit,
    private val onCancelClick: (Download) -> Unit,
    private val onRetryClick: (Download) -> Unit
) : ListAdapter<Download, DownloadAdapter.DownloadViewHolder>(DownloadDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_download, parent, false)
        return DownloadViewHolder(view, onItemClick, onDeleteClick, onCancelClick, onRetryClick)
    }

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DownloadViewHolder(
        itemView: View,
        private val onItemClick: (Download) -> Unit,
        private val onDeleteClick: (Download) -> Unit,
        private val onCancelClick: (Download) -> Unit,
        private val onRetryClick: (Download) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val fileIconText: TextView = itemView.findViewById(R.id.fileIconText)
        private val filenameText: TextView = itemView.findViewById(R.id.filenameText)
        private val fileSizeText: TextView = itemView.findViewById(R.id.fileSizeText)
        private val downloadDateText: TextView = itemView.findViewById(R.id.downloadDateText)
        private val statusText: TextView = itemView.findViewById(R.id.statusText)
        private val progressLayout: LinearLayout = itemView.findViewById(R.id.progressLayout)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val progressText: TextView = itemView.findViewById(R.id.progressText)
        private val actionButton: ImageButton = itemView.findViewById(R.id.actionButton)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(download: Download) {
            // Set filename
            filenameText.text = download.filename

            // Set file size
            fileSizeText.text = download.getFormattedSize()

            // Set download date
            downloadDateText.text = download.getFormattedDate()

            // Set file icon based on mime type
            fileIconText.text = getFileIcon(download.mimeType)

            // Handle different download statuses
            when (download.status) {
                DownloadStatus.SUCCESSFUL -> {
                    statusText.visibility = View.VISIBLE
                    statusText.text = "Downloaded"
                    statusText.setTextColor(0xFF10b981.toInt()) // Green
                    progressLayout.visibility = View.GONE
                    actionButton.visibility = View.GONE
                    deleteButton.visibility = View.VISIBLE

                    // Click to open file
                    itemView.setOnClickListener { onItemClick(download) }
                }

                DownloadStatus.RUNNING -> {
                    statusText.visibility = View.GONE
                    progressLayout.visibility = View.VISIBLE
                    actionButton.visibility = View.VISIBLE
                    actionButton.setImageResource(R.drawable.ic_close)
                    actionButton.contentDescription = "Cancel"
                    deleteButton.visibility = View.GONE

                    val progress = download.getProgress()
                    progressBar.progress = progress
                    progressText.text = "$progress%"

                    // Cancel download
                    actionButton.setOnClickListener { onCancelClick(download) }
                    itemView.setOnClickListener(null)
                }

                DownloadStatus.PENDING -> {
                    statusText.visibility = View.VISIBLE
                    statusText.text = "Pending..."
                    statusText.setTextColor(0xFF888888.toInt()) // Gray
                    progressLayout.visibility = View.GONE
                    actionButton.visibility = View.VISIBLE
                    actionButton.setImageResource(R.drawable.ic_close)
                    actionButton.contentDescription = "Cancel"
                    deleteButton.visibility = View.GONE

                    // Cancel download
                    actionButton.setOnClickListener { onCancelClick(download) }
                    itemView.setOnClickListener(null)
                }

                DownloadStatus.PAUSED -> {
                    statusText.visibility = View.VISIBLE
                    statusText.text = "Paused"
                    statusText.setTextColor(0xFFfbbf24.toInt()) // Yellow
                    progressLayout.visibility = View.VISIBLE
                    actionButton.visibility = View.VISIBLE
                    actionButton.setImageResource(R.drawable.ic_refresh)
                    actionButton.contentDescription = "Resume"
                    deleteButton.visibility = View.VISIBLE

                    val progress = download.getProgress()
                    progressBar.progress = progress
                    progressText.text = "$progress%"

                    // Resume download
                    actionButton.setOnClickListener { onRetryClick(download) }
                    itemView.setOnClickListener(null)
                }

                DownloadStatus.FAILED -> {
                    statusText.visibility = View.VISIBLE
                    statusText.text = "Failed"
                    statusText.setTextColor(0xFFef4444.toInt()) // Red
                    progressLayout.visibility = View.GONE
                    actionButton.visibility = View.VISIBLE
                    actionButton.setImageResource(R.drawable.ic_refresh)
                    actionButton.contentDescription = "Retry"
                    deleteButton.visibility = View.VISIBLE

                    // Retry download
                    actionButton.setOnClickListener { onRetryClick(download) }
                    itemView.setOnClickListener(null)
                }

                DownloadStatus.CANCELLED -> {
                    statusText.visibility = View.VISIBLE
                    statusText.text = "Cancelled"
                    statusText.setTextColor(0xFF888888.toInt()) // Gray
                    progressLayout.visibility = View.GONE
                    actionButton.visibility = View.VISIBLE
                    actionButton.setImageResource(R.drawable.ic_refresh)
                    actionButton.contentDescription = "Retry"
                    deleteButton.visibility = View.VISIBLE

                    // Retry download
                    actionButton.setOnClickListener { onRetryClick(download) }
                    itemView.setOnClickListener(null)
                }
            }

            // Delete button always available
            deleteButton.setOnClickListener { onDeleteClick(download) }
        }

        private fun getFileIcon(mimeType: String): String {
            return when {
                mimeType.startsWith("image/") -> "🖼️"
                mimeType.startsWith("video/") -> "🎥"
                mimeType.startsWith("audio/") -> "🎵"
                mimeType.startsWith("text/") -> "📄"
                mimeType.contains("pdf") -> "📕"
                mimeType.contains("zip") || mimeType.contains("rar") || mimeType.contains("archive") -> "📦"
                mimeType.contains("word") || mimeType.contains("document") -> "📝"
                mimeType.contains("excel") || mimeType.contains("spreadsheet") -> "📊"
                mimeType.contains("powerpoint") || mimeType.contains("presentation") -> "📈"
                else -> "📥"
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    class DownloadDiffCallback : DiffUtil.ItemCallback<Download>() {
        override fun areItemsTheSame(oldItem: Download, newItem: Download): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Download, newItem: Download): Boolean {
            return oldItem == newItem
        }
    }
}
