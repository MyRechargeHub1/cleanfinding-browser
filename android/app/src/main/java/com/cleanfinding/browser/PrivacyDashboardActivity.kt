package com.cleanfinding.browser

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Privacy Dashboard Activity
 * Displays comprehensive privacy statistics
 */
class PrivacyDashboardActivity : AppCompatActivity() {

    private lateinit var totalTrackersText: TextView
    private lateinit var todayTrackersText: TextView
    private lateinit var weekTrackersText: TextView
    private lateinit var mostTrackedRecyclerView: RecyclerView
    private lateinit var noDataText: TextView
    private lateinit var gradeDistributionContainer: LinearLayout

    private lateinit var privacyStatsManager: PrivacyStatsManager
    private lateinit var domainAdapter: DomainTrackingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_dashboard)

        privacyStatsManager = PrivacyStatsManager(this)

        initViews()
        setupListeners()
        loadStatistics()
    }

    private fun initViews() {
        totalTrackersText = findViewById(R.id.totalTrackersText)
        todayTrackersText = findViewById(R.id.todayTrackersText)
        weekTrackersText = findViewById(R.id.weekTrackersText)
        mostTrackedRecyclerView = findViewById(R.id.mostTrackedRecyclerView)
        noDataText = findViewById(R.id.noDataText)
        gradeDistributionContainer = findViewById(R.id.gradeDistributionContainer)

        // Setup RecyclerView
        mostTrackedRecyclerView.layoutManager = LinearLayoutManager(this)
        domainAdapter = DomainTrackingAdapter()
        mostTrackedRecyclerView.adapter = domainAdapter
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    private fun loadStatistics() {
        // Load total trackers blocked
        lifecycleScope.launch {
            privacyStatsManager.getTotalTrackersBlocked().collectLatest { total ->
                totalTrackersText.text = (total ?: 0).toString()
            }
        }

        // Load trackers blocked today
        lifecycleScope.launch {
            privacyStatsManager.getTrackersBlockedToday().collectLatest { today ->
                todayTrackersText.text = (today ?: 0).toString()
            }
        }

        // Load trackers blocked this week
        lifecycleScope.launch {
            privacyStatsManager.getTrackersBlockedThisWeek().collectLatest { week ->
                weekTrackersText.text = (week ?: 0).toString()
            }
        }

        // Load most tracked domains
        lifecycleScope.launch {
            val mostTracked = privacyStatsManager.getMostTrackedDomains(10)
            if (mostTracked.isNotEmpty()) {
                noDataText.visibility = View.GONE
                mostTrackedRecyclerView.visibility = View.VISIBLE
                domainAdapter.submitList(mostTracked)
            } else {
                noDataText.visibility = View.VISIBLE
                mostTrackedRecyclerView.visibility = View.GONE
            }
        }

        // Load privacy grade distribution
        lifecycleScope.launch {
            val distribution = privacyStatsManager.getPrivacyGradeDistribution()
            displayGradeDistribution(distribution)
        }
    }

    private fun displayGradeDistribution(distribution: List<GradeDistribution>) {
        gradeDistributionContainer.removeAllViews()

        if (distribution.isEmpty()) {
            val noDataText = TextView(this)
            noDataText.text = "No grading data yet"
            noDataText.setTextColor(Color.parseColor("#9CA3AF"))
            noDataText.textSize = 14f
            noDataText.setPadding(20, 20, 20, 20)
            gradeDistributionContainer.addView(noDataText)
            return
        }

        val grades = listOf("A", "B", "C", "D", "F")
        val gradeColors = mapOf(
            "A" to "#4CAF50",
            "B" to "#8BC34A",
            "C" to "#FFC107",
            "D" to "#FF9800",
            "F" to "#F44336"
        )

        val totalCount = distribution.sumOf { it.count }

        grades.forEach { grade ->
            val gradeData = distribution.find { it.privacy_grade == grade }
            val count = gradeData?.count ?: 0
            val percentage = if (totalCount > 0) (count * 100 / totalCount) else 0

            val gradeRow = LayoutInflater.from(this).inflate(
                android.R.layout.simple_list_item_2,
                gradeDistributionContainer,
                false
            ) as LinearLayout

            val gradeView = LinearLayout(this)
            gradeView.orientation = LinearLayout.HORIZONTAL
            gradeView.setPadding(0, 8, 0, 8)

            // Grade badge
            val gradeText = TextView(this)
            gradeText.text = grade
            gradeText.setTextColor(Color.WHITE)
            gradeText.textSize = 14f
            gradeText.setPadding(12, 4, 12, 4)
            gradeText.setBackgroundColor(Color.parseColor(gradeColors[grade] ?: "#9CA3AF"))
            gradeView.addView(gradeText)

            // Count text
            val countText = TextView(this)
            countText.text = "  $count sites ($percentage%)"
            countText.setTextColor(Color.parseColor("#E5E7EB"))
            countText.textSize = 14f
            countText.setPadding(16, 4, 0, 4)
            gradeView.addView(countText)

            gradeDistributionContainer.addView(gradeView)
        }
    }
}

/**
 * Adapter for domain tracking list
 */
class DomainTrackingAdapter : RecyclerView.Adapter<DomainTrackingAdapter.ViewHolder>() {

    private var domains = listOf<DomainTrackingStats>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val domainText: TextView = view.findViewById(R.id.domainText)
        val countText: TextView = view.findViewById(R.id.countText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_domain_tracking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val domain = domains[position]
        holder.domainText.text = domain.domain
        holder.countText.text = domain.total.toString()
    }

    override fun getItemCount() = domains.size

    fun submitList(newDomains: List<DomainTrackingStats>) {
        domains = newDomains
        notifyDataSetChanged()
    }
}
