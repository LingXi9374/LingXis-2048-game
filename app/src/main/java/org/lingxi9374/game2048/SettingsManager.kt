package org.lingxi9374.game2048

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.Locale

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("game_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_SOUND_ENABLED = "sound_enabled"
        const val KEY_SOUND_VOLUME = "sound_volume"
        const val KEY_LANGUAGE = "language"
        const val KEY_COUNTRY = "country"
    }

    fun isSoundEnabled(): Boolean {
        return prefs.getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_SOUND_ENABLED, enabled)
        }
    }

    fun getSoundVolume(): Float {
        return prefs.getFloat(KEY_SOUND_VOLUME, 1.0f)
    }

    fun setSoundVolume(volume: Float) {
        prefs.edit {
            putFloat(KEY_SOUND_VOLUME, volume)
        }
    }

    fun setLocale(locale: Locale) {
        prefs.edit {
            putString(KEY_LANGUAGE, locale.language)
            putString(KEY_COUNTRY, locale.country)
        }
    }

    fun getLocale(): Locale {
        val language = prefs.getString(KEY_LANGUAGE, null)
        val country = prefs.getString(KEY_COUNTRY, null)
        return if (language != null && country != null) {
            Locale(language, country)
        } else {
            Locale.getDefault()
        }
    }
}
