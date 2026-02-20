package com.cleanfinding.browser

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat

/**
 * Settings Activity for CleanFinding Browser
 * Provides comprehensive configuration options
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Setup back button
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Load settings fragment
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settingsContainer, SettingsFragment())
                .commit()
        }
    }

    /**
     * Settings Fragment using PreferenceFragmentCompat
     */
    class SettingsFragment : PreferenceFragmentCompat() {

        private lateinit var prefsManager: PreferencesManager

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            prefsManager = PreferencesManager(requireContext())

            // Setup preference click listeners
            setupPrivacyPolicy()
            setupClearData()
            setupCacheModeListener()
        }

        private fun setupPrivacyPolicy() {
            findPreference<Preference>("privacy_policy")?.setOnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cleanfinding.com/privacy"))
                startActivity(intent)
                true
            }
        }

        private fun setupClearData() {
            findPreference<Preference>("clear_data")?.setOnPreferenceClickListener {
                showClearDataDialog()
                true
            }
        }

        private fun setupCacheModeListener() {
            findPreference<ListPreference>("cache_mode")?.setOnPreferenceChangeListener { _, newValue ->
                val mode = newValue as String
                Toast.makeText(
                    requireContext(),
                    "Cache mode changed to: ${getCacheModeLabel(mode)}",
                    Toast.LENGTH_SHORT
                ).show()
                true
            }
        }

        private fun getCacheModeLabel(mode: String): String {
            return when (mode) {
                "normal" -> "Normal"
                "prefer_cache" -> "Prefer cache"
                "no_cache" -> "No cache"
                "cache_only" -> "Cache only"
                else -> "Normal"
            }
        }

        private fun showClearDataDialog() {
            AlertDialog.Builder(requireContext())
                .setTitle("Clear All Data?")
                .setMessage("This will clear all browsing history, cookies, cache, downloads, and reset all settings to defaults. This action cannot be undone.")
                .setPositiveButton("Clear All") { _, _ ->
                    clearAllData()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun clearAllData() {
            val context = requireContext()

            // Clear history
            val historyManager = HistoryManager(context)
            historyManager.clearAllHistory {
                historyManager.cleanup()
            }

            // Clear downloads
            val downloadManager = DownloadManagerHelper(context)
            downloadManager.clearAllDownloads {
                downloadManager.cleanup()
            }

            // Clear cookies
            CookieManager.getInstance().removeAllCookies { success ->
                if (success) {
                    CookieManager.getInstance().flush()
                }
            }

            // Clear cache
            val cacheDir = context.cacheDir
            cacheDir.deleteRecursively()

            // Clear preferences
            prefsManager.resetToDefaults()

            // Reload preferences to show defaults
            preferenceScreen = null
            addPreferencesFromResource(R.xml.preferences)
            setupPrivacyPolicy()
            setupClearData()
            setupCacheModeListener()

            Toast.makeText(context, "All data cleared and settings reset", Toast.LENGTH_LONG).show()

            // Return to main activity
            requireActivity().setResult(RESULT_OK)
            requireActivity().finish()
        }
    }
}
