package com.cleanfinding.browser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView Adapter for displaying browsing history
 */
class HistoryAdapter(
    private val onItemClick: (HistoryItem) -> Unit,
    private val onDeleteClick: (HistoryItem) -> Unit
) : ListAdapter<HistoryItem, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view, onItemClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(
        itemView: View,
        private val onItemClick: (HistoryItem) -> Unit,
        private val onDeleteClick: (HistoryItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val faviconText: TextView = itemView.findViewById(R.id.faviconText)
        private val titleText: TextView = itemView.findViewById(R.id.historyTitleText)
        private val urlText: TextView = itemView.findViewById(R.id.historyUrlText)
        private val timeText: TextView = itemView.findViewById(R.id.historyTimeText)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(item: HistoryItem) {
            // Set title
            titleText.text = if (item.title.isNotBlank()) {
                item.title
            } else {
                item.getDomain()
            }

            // Set URL
            urlText.text = item.url

            // Set time
            timeText.text = item.getFormattedDate()

            // Set favicon (use first letter of domain as simple favicon)
            val domain = item.getDomain()
            val faviconChar = if (domain.isNotEmpty()) {
                domain.first().uppercaseChar().toString()
            } else {
                "🌐"
            }
            faviconText.text = faviconChar

            // Set click listeners
            itemView.setOnClickListener {
                onItemClick(item)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    class HistoryDiffCallback : DiffUtil.ItemCallback<HistoryItem>() {
        override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            return oldItem == newItem
        }
    }
}
