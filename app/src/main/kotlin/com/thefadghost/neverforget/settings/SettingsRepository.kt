package com.thefadghost.neverforget.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.thefadghost.neverforget.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("never_forget_settings")

data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val ukEnabled: Boolean = true,
    val bulgariaEnabled: Boolean = true,
    val primaryCountry: String = "GB",
    val englandWalesEnabled: Boolean = true,
    val scotlandEnabled: Boolean = false,
    val northernIrelandEnabled: Boolean = false,
    val theme: ThemePreference = ThemePreference.EMBER,
    val defaultReminderHour: Int = 9,
    val quietStartHour: Int = 21,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val onboarding = booleanPreferencesKey("onboarding_completed")
        val uk = booleanPreferencesKey("uk_enabled")
        val bg = booleanPreferencesKey("bg_enabled")
        val primaryCountry = stringPreferencesKey("primary_country")
        val eaw = booleanPreferencesKey("england_wales_enabled")
        val sct = booleanPreferencesKey("scotland_enabled")
        val nir = booleanPreferencesKey("northern_ireland_enabled")
        val theme = stringPreferencesKey("theme")
        val reminderHour = intPreferencesKey("reminder_hour")
        val quietStart = intPreferencesKey("quiet_start")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            onboardingCompleted = preferences[Keys.onboarding] ?: false,
            ukEnabled = preferences[Keys.uk] ?: true,
            bulgariaEnabled = preferences[Keys.bg] ?: true,
            primaryCountry = preferences[Keys.primaryCountry] ?: "GB",
            englandWalesEnabled = preferences[Keys.eaw] ?: true,
            scotlandEnabled = preferences[Keys.sct] ?: false,
            northernIrelandEnabled = preferences[Keys.nir] ?: false,
            theme = preferences[Keys.theme]
                ?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
                ?: ThemePreference.EMBER,
            defaultReminderHour = preferences[Keys.reminderHour] ?: 9,
            quietStartHour = preferences[Keys.quietStart] ?: 21,
        )
    }

    suspend fun completeOnboarding(
        ukEnabled: Boolean,
        bulgariaEnabled: Boolean,
        primaryCountry: String,
    ) {
        context.settingsDataStore.edit {
            it[Keys.uk] = ukEnabled
            it[Keys.bg] = bulgariaEnabled
            it[Keys.primaryCountry] = normalizePrimary(
                requested = primaryCountry,
                ukEnabled = ukEnabled,
                bulgariaEnabled = bulgariaEnabled,
            )
            it[Keys.onboarding] = true
        }
    }

    suspend fun setTheme(theme: ThemePreference) {
        context.settingsDataStore.edit { it[Keys.theme] = theme.name }
    }

    suspend fun setCountry(country: String, enabled: Boolean) {
        context.settingsDataStore.edit {
            val ukEnabled = it[Keys.uk] ?: true
            val bulgariaEnabled = it[Keys.bg] ?: true
            when (country) {
                "GB" -> {
                    if (!enabled && !bulgariaEnabled) return@edit
                    it[Keys.uk] = enabled
                    if (!enabled) it[Keys.primaryCountry] = "BG"
                }
                "BG" -> {
                    if (!enabled && !ukEnabled) return@edit
                    it[Keys.bg] = enabled
                    if (!enabled) it[Keys.primaryCountry] = "GB"
                }
            }
        }
    }

    suspend fun setPrimaryCountry(country: String) {
        context.settingsDataStore.edit {
            val enabled = when (country) {
                "GB" -> it[Keys.uk] ?: true
                "BG" -> it[Keys.bg] ?: true
                else -> false
            }
            if (enabled) it[Keys.primaryCountry] = country
        }
    }

    suspend fun setUkRegion(region: String, enabled: Boolean) {
        context.settingsDataStore.edit {
            when (region) {
                "GB-EAW" -> it[Keys.eaw] = enabled
                "GB-SCT" -> it[Keys.sct] = enabled
                "GB-NIR" -> it[Keys.nir] = enabled
            }
        }
    }

    private fun normalizePrimary(
        requested: String,
        ukEnabled: Boolean,
        bulgariaEnabled: Boolean,
    ): String = when {
        requested == "GB" && ukEnabled -> "GB"
        requested == "BG" && bulgariaEnabled -> "BG"
        ukEnabled -> "GB"
        else -> "BG"
    }
}
