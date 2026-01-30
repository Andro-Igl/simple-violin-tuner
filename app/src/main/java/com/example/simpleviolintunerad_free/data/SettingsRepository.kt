package com.example.simpleviolintunerad_free.data

import android.content.Context
import android.content.SharedPreferences
import com.example.simpleviolintunerad_free.ui.components.FrequencySettings

/**
 * Repository for persistent storage of app settings.
 *
 * Uses SharedPreferences for simple, reliable storage
 * without external dependencies. Data persists after app restart.
 *
 * USAGE:
 * ```
 * val repo = SettingsRepository(context)
 * repo.saveFrequencySettings(settings)
 * val loaded = repo.loadFrequencySettings()
 * ```
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Saves the frequency settings.
     */
    fun saveFrequencySettings(settings: FrequencySettings) {
        prefs.edit().apply {
            putDouble(KEY_FREQ_G, settings.gFrequency)
            putDouble(KEY_FREQ_D, settings.dFrequency)
            putDouble(KEY_FREQ_A, settings.aFrequency)
            putDouble(KEY_FREQ_E, settings.eFrequency)
            apply()
        }
    }

    /**
     * Loads the saved frequency settings.
     * Returns default values if nothing has been saved yet.
     */
    fun loadFrequencySettings(): FrequencySettings {
        return FrequencySettings(
            gFrequency = prefs.getDouble(KEY_FREQ_G, FrequencySettings.DEFAULT.gFrequency),
            dFrequency = prefs.getDouble(KEY_FREQ_D, FrequencySettings.DEFAULT.dFrequency),
            aFrequency = prefs.getDouble(KEY_FREQ_A, FrequencySettings.DEFAULT.aFrequency),
            eFrequency = prefs.getDouble(KEY_FREQ_E, FrequencySettings.DEFAULT.eFrequency)
        )
    }

    /**
     * Resets all settings to default.
     */
    fun resetToDefaults() {
        saveFrequencySettings(FrequencySettings.DEFAULT)
    }

    companion object {
        private const val PREFS_NAME = "violin_tuner_settings"
        private const val KEY_FREQ_G = "frequency_g"
        private const val KEY_FREQ_D = "frequency_d"
        private const val KEY_FREQ_A = "frequency_a"
        private const val KEY_FREQ_E = "frequency_e"
    }
}

/**
 * Extension function for saving Double values in SharedPreferences.
 * SharedPreferences natively doesn't support Double, so it's stored as Long bits.
 */
private fun SharedPreferences.Editor.putDouble(key: String, value: Double): SharedPreferences.Editor {
    return putLong(key, java.lang.Double.doubleToRawLongBits(value))
}

/**
 * Extension function for reading Double values from SharedPreferences.
 */
private fun SharedPreferences.getDouble(key: String, defaultValue: Double): Double {
    return if (contains(key)) {
        java.lang.Double.longBitsToDouble(getLong(key, 0L))
    } else {
        defaultValue
    }
}
