package com.cleanfinding.browser

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Activity for displaying and managing browsing history
 * Features:
 * - Search history by URL or title
 * - Delete individual items
 * - Clear all history
 * - Click to navigate to URL
 */
class HistoryActivity : AppCompatActivity() {

    private lateinit var historyManager: HistoryManager
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var clearSearchButton: ImageButton
    private lateinit var clearAllButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var emptyStateLayout: LinearLayout
    private var historyCollectionJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        historyManager = HistoryManager(this)

        initViews()
        setupRecyclerView()
        setupSearch()
        setupButtons()
        observeHistory("")
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.historyRecyclerView)
        searchEditText = findViewById(R.id.searchEditText)
        clearSearchButton = findViewById(R.id.clearSearchButton)
        clearAllButton = findViewById(R.id.clearAllButton)
        backButton = findViewById(R.id.backButton)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            onItemClick = { historyItem ->
                // Navigate to URL
                val intent = Intent().apply {
                    putExtra("url", historyItem.url)
                }
                setResult(RESULT_OK, intent)
                finish()
            },
            onDeleteClick = { historyItem ->
                // Delete single item
                showDeleteConfirmDialog(
                    title = "Delete History Item?",
                    message = "Delete ${historyItem.title.ifBlank { historyItem.url }}?",
                    onConfirm = {
                        historyManager.deleteHistoryItem(historyItem.id)
                        Toast.makeText(this, "History item deleted", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""

                // Show/hide clear search button
                clearSearchButton.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE

                // Search history
                observeHistory(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        clearSearchButton.setOnClickListener {
            searchEditText.text.clear()
        }
    }

    private fun setupButtons() {
        backButton.setOnClickListener {
            finish()
        }

        clearAllButton.setOnClickListener {
            showClearHistoryDialog()
        }
    }

    private fun observeHistory(query: String) {
        historyCollectionJob?.cancel()
        historyCollectionJob = lifecycleScope.launch {
            val historyFlow = if (query.isBlank()) {
                historyManager.getRecentHistory(500)
            } else {
                historyManager.searchHistory(query)
            }

            historyFlow.collectLatest { historyList ->
                historyAdapter.submitList(historyList)
                updateEmptyState(historyList.isEmpty(), query)
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean, searchQuery: String = "") {
        if (isEmpty) {
            emptyStateLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE

            val emptyText = emptyStateLayout.findViewById<android.widget.TextView>(R.id.emptyStateText)
            emptyText.text = if (searchQuery.isNotEmpty()) {
                "No results for \"$searchQuery\""
            } else {
                "No browsing history"
            }
        } else {
            emptyStateLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showClearHistoryDialog() {
        val options = arrayOf(
            "Last hour",
            "Last 24 hours",
            "Last 7 days",
            "Last 30 days",
            "All time"
        )

        AlertDialog.Builder(this)
            .setTitle("Clear Browsing History")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> clearHistory(HistoryManager.TimePeriod.LAST_HOUR, "Last hour")
                    1 -> clearHistory(HistoryManager.TimePeriod.LAST_DAY, "Last 24 hours")
                    2 -> clearHistory(HistoryManager.TimePeriod.LAST_WEEK, "Last 7 days")
                    3 -> clearHistory(HistoryManager.TimePeriod.LAST_MONTH, "Last 30 days")
                    4 -> clearAllHistory()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearHistory(period: Long, periodName: String) {
        showDeleteConfirmDialog(
            title = "Clear History?",
            message = "Clear all history from $periodName?",
            onConfirm = {
                val beforeTime = System.currentTimeMillis() - period
                historyManager.deleteHistoryBefore(beforeTime) { count ->
                    Toast.makeText(
                        this,
                        "Cleared $count item${if (count != 1) "s" else ""}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun clearAllHistory() {
        showDeleteConfirmDialog(
            title = "Clear All History?",
            message = "This will permanently delete all your browsing history. This action cannot be undone.",
            onConfirm = {
                historyManager.clearAllHistory { count ->
                    Toast.makeText(
                        this,
                        "Cleared all history ($count items)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        historyCollectionJob?.cancel()
        historyManager.cleanup()
    }

    private fun showDeleteConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Delete") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
