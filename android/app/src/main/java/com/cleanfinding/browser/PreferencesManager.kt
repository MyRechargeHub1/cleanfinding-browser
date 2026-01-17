package com.cleanfinding.browser

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Manager for browser settings and preferences
 * Uses SharedPreferences to store user settings
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        // Privacy settings
        const val KEY_BLOCK_TRACKERS = "block_trackers"
        const val KEY_BLOCK_ADS = "block_ads"
        const val KEY_BLOCK_ADULT_CONTENT = "block_adult_content"
        const val KEY_FORCE_HTTPS = "force_https"
        const val KEY_FORCE_SAFE_SEARCH = "force_safe_search"
        const val KEY_DUCK_PLAYER = "duck_player"
        const val KEY_EMAIL_PROTECTION = "email_protection"

        // Appearance settings
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_SHOW_IMAGES = "show_images"
        const val KEY_TEXT_SIZE = "text_size"

        // Advanced settings
        const val KEY_JAVASCRIPT = "javascript"
        const val KEY_COOKIES = "cookies"
        const val KEY_DOM_STORAGE = "dom_storage"
        const val KEY_LOCATION_ACCESS = "location_access"
        const val KEY_CACHE_MODE = "cache_mode"

        // Download settings
        const val KEY_AUTO_DOWNLOAD = "auto_download"
        const val KEY_DOWNLOAD_WIFI_ONLY = "download_wifi_only"

        // Security settings
        const val KEY_BIOMETRIC_LOCK_INCOGNITO = "biometric_lock_incognito"

        // Default values
        const val DEFAULT_BLOCK_TRACKERS = true
        const val DEFAULT_BLOCK_ADS = true
        const val DEFAULT_BLOCK_ADULT_CONTENT = true
        const val DEFAULT_FORCE_HTTPS = true
        const val DEFAULT_FORCE_SAFE_SEARCH = true
        // CRITICAL FIX: DuckPlayer disabled by default to allow native YouTube playback
        // When enabled, YouTube redirects to youtube-nocookie.com/embed which causes black screen
        // Users can enable this manually in settings if they prefer privacy over native playback
        const val DEFAULT_DUCK_PLAYER = false
        const val DEFAULT_EMAIL_PROTECTION = true
        const val DEFAULT_DARK_MODE = true
        const val DEFAULT_SHOW_IMAGES = true
        const val DEFAULT_TEXT_SIZE = 100
        const val DEFAULT_JAVASCRIPT = true
        const val DEFAULT_COOKIES = true
        const val DEFAULT_DOM_STORAGE = true
        const val DEFAULT_LOCATION_ACCESS = false
        const val DEFAULT_CACHE_MODE = "normal"
        const val DEFAULT_AUTO_DOWNLOAD = false
        const val DEFAULT_DOWNLOAD_WIFI_ONLY = false
        const val DEFAULT_BIOMETRIC_LOCK_INCOGNITO = false
    }

    // Privacy settings
    fun getBlockTrackers(): Boolean = prefs.getBoolean(KEY_BLOCK_TRACKERS, DEFAULT_BLOCK_TRACKERS)
    fun setBlockTrackers(enabled: Boolean) = prefs.edit().putBoolean(KEY_BLOCK_TRACKERS, enabled).apply()

    fun getBlockAds(): Boolean = prefs.getBoolean(KEY_BLOCK_ADS, DEFAULT_BLOCK_ADS)
    fun setBlockAds(enabled: Boolean) = prefs.edit().putBoolean(KEY_BLOCK_ADS, enabled).apply()

    fun getBlockAdultContent(): Boolean = prefs.getBoolean(KEY_BLOCK_ADULT_CONTENT, DEFAULT_BLOCK_ADULT_CONTENT)
    fun setBlockAdultContent(enabled: Boolean) = prefs.edit().putBoolean(KEY_BLOCK_ADULT_CONTENT, enabled).apply()

    fun getForceHttps(): Boolean = prefs.getBoolean(KEY_FORCE_HTTPS, DEFAULT_FORCE_HTTPS)
    fun setForceHttps(enabled: Boolean) = prefs.edit().putBoolean(KEY_FORCE_HTTPS, enabled).apply()

    fun getForceSafeSearch(): Boolean = prefs.getBoolean(KEY_FORCE_SAFE_SEARCH, DEFAULT_FORCE_SAFE_SEARCH)
    fun setForceSafeSearch(enabled: Boolean) = prefs.edit().putBoolean(KEY_FORCE_SAFE_SEARCH, enabled).apply()

    fun getDuckPlayer(): Boolean = prefs.getBoolean(KEY_DUCK_PLAYER, DEFAULT_DUCK_PLAYER)
    fun setDuckPlayer(enabled: Boolean) = prefs.edit().putBoolean(KEY_DUCK_PLAYER, enabled).apply()

    fun getEmailProtection(): Boolean = prefs.getBoolean(KEY_EMAIL_PROTECTION, DEFAULT_EMAIL_PROTECTION)
    fun setEmailProtection(enabled: Boolean) = prefs.edit().putBoolean(KEY_EMAIL_PROTECTION, enabled).apply()

    // Appearance settings
    fun getDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, DEFAULT_DARK_MODE)
    fun setDarkMode(enabled: Boolean) = prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()

    fun getShowImages(): Boolean = prefs.getBoolean(KEY_SHOW_IMAGES, DEFAULT_SHOW_IMAGES)
    fun setShowImages(enabled: Boolean) = prefs.edit().putBoolean(KEY_SHOW_IMAGES, enabled).apply()

    fun getTextSize(): Int = prefs.getInt(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE)
    fun setTextSize(size: Int) = prefs.edit().putInt(KEY_TEXT_SIZE, size).apply()

    // Advanced settings
    fun getJavaScriptEnabled(): Boolean = prefs.getBoolean(KEY_JAVASCRIPT, DEFAULT_JAVASCRIPT)
    fun setJavaScriptEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_JAVASCRIPT, enabled).apply()

    fun getCookiesEnabled(): Boolean = prefs.getBoolean(KEY_COOKIES, DEFAULT_COOKIES)
    fun setCookiesEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_COOKIES, enabled).apply()

    fun getDomStorageEnabled(): Boolean = prefs.getBoolean(KEY_DOM_STORAGE, DEFAULT_DOM_STORAGE)
    fun setDomStorageEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_DOM_STORAGE, enabled).apply()

    fun getLocationAccessEnabled(): Boolean = prefs.getBoolean(KEY_LOCATION_ACCESS, DEFAULT_LOCATION_ACCESS)
    fun setLocationAccessEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_LOCATION_ACCESS, enabled).apply()

    fun getCacheMode(): String = prefs.getString(KEY_CACHE_MODE, DEFAULT_CACHE_MODE) ?: DEFAULT_CACHE_MODE
    fun setCacheMode(mode: String) = prefs.edit().putString(KEY_CACHE_MODE, mode).apply()

    // Download settings
    fun getAutoDownload(): Boolean = prefs.getBoolean(KEY_AUTO_DOWNLOAD, DEFAULT_AUTO_DOWNLOAD)
    fun setAutoDownload(enabled: Boolean) = prefs.edit().putBoolean(KEY_AUTO_DOWNLOAD, enabled).apply()

    fun getDownloadWifiOnly(): Boolean = prefs.getBoolean(KEY_DOWNLOAD_WIFI_ONLY, DEFAULT_DOWNLOAD_WIFI_ONLY)
    fun setDownloadWifiOnly(enabled: Boolean) = prefs.edit().putBoolean(KEY_DOWNLOAD_WIFI_ONLY, enabled).apply()

    // Security settings
    fun getBiometricLockIncognito(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_LOCK_INCOGNITO, DEFAULT_BIOMETRIC_LOCK_INCOGNITO)
    fun setBiometricLockIncognito(enabled: Boolean) = prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK_INCOGNITO, enabled).apply()

    /**
     * Register a listener for preference changes
     */
    fun registerChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Unregister a preference change listener
     */
    fun unregisterChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Reset all settings to defaults
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
}
