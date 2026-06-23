package com.example.meteonode.ui.fragments

import android.content.Context

object SettingsRepository {

    private const val PREF_NAME = "meteonode_settings"

    private const val KEY_MAX_TEMP = "max_temp"
    private const val KEY_MIN_TEMP = "min_temp"

    private const val KEY_MAX_HUM = "max_hum"
    private const val KEY_MIN_HUM = "min_hum"

    private const val KEY_MAX_AQI = "max_aqi"

    fun saveMaxTemp(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_MAX_TEMP, value).apply()
    }

    fun saveMinTemp(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_MIN_TEMP, value).apply()
    }

    fun saveMaxHumidity(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_MAX_HUM, value).apply()
    }

    fun saveMinHumidity(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_MIN_HUM, value).apply()
    }

    fun saveMaxAqi(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_MAX_AQI, value).apply()
    }

    fun getMaxTemp(context: Context): Float {
        return prefs(context).getFloat(KEY_MAX_TEMP, 28f)
    }

    fun getMinTemp(context: Context): Float {
        return prefs(context).getFloat(KEY_MIN_TEMP, 16f)
    }

    fun getMaxHumidity(context: Context): Float {
        return prefs(context).getFloat(KEY_MAX_HUM, 70f)
    }

    fun getMinHumidity(context: Context): Float {
        return prefs(context).getFloat(KEY_MIN_HUM, 30f)
    }

    fun getMaxAqi(context: Context): Int {
        return prefs(context).getInt(KEY_MAX_AQI, 4)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )
}