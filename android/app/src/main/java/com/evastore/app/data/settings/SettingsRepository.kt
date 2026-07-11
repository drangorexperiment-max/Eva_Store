package com.evastore.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.evastore.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "eva_settings")

data class EvaSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoScanDownloads: Boolean = true,
    val showNsfw: Boolean = false,
    val animationsEnabled: Boolean = true,
    val wifiOnlyDownloads: Boolean = false,
    val virusTotalApiKey: String = ""
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val AUTO_SCAN = booleanPreferencesKey("auto_scan_downloads")
        val SHOW_NSFW = booleanPreferencesKey("show_nsfw")
        val ANIMATIONS = booleanPreferencesKey("animations_enabled")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only_downloads")
        val VT_API_KEY = stringPreferencesKey("virustotal_api_key")
    }

    val settings: Flow<EvaSettings> = context.dataStore.data.map { prefs ->
        EvaSettings(
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            autoScanDownloads = prefs[Keys.AUTO_SCAN] ?: true,
            showNsfw = prefs[Keys.SHOW_NSFW] ?: false,
            animationsEnabled = prefs[Keys.ANIMATIONS] ?: true,
            wifiOnlyDownloads = prefs[Keys.WIFI_ONLY] ?: false,
            virusTotalApiKey = prefs[Keys.VT_API_KEY] ?: ""
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) =
        context.dataStore.edit { it[Keys.THEME] = mode.name }

    suspend fun setAutoScan(enabled: Boolean) =
        context.dataStore.edit { it[Keys.AUTO_SCAN] = enabled }

    suspend fun setAnimations(enabled: Boolean) =
        context.dataStore.edit { it[Keys.ANIMATIONS] = enabled }

    suspend fun setWifiOnly(enabled: Boolean) =
        context.dataStore.edit { it[Keys.WIFI_ONLY] = enabled }

    suspend fun setVirusTotalApiKey(key: String) =
        context.dataStore.edit { it[Keys.VT_API_KEY] = key.trim() }
}
