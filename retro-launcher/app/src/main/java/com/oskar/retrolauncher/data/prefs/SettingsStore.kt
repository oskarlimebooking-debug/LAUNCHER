package com.oskar.retrolauncher.data.prefs

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class Units { METRIC, IMPERIAL }

class SettingsStore(private val prefs: SharedPreferences) {

    val units: Units
        get() = if (prefs.getString(KEY_UNITS, "metric") == "metric") Units.METRIC else Units.IMPERIAL

    val panelRatioPercent: Int get() = prefs.getInt(KEY_PANEL_RATIO, 40).coerceIn(30, 70)

    val gridCols: Int get() = prefs.getString(KEY_GRID_COLS, "4")?.toIntOrNull() ?: 4
    val gridRows: Int get() = prefs.getString(KEY_GRID_ROWS, "3")?.toIntOrNull() ?: 3

    val recordTrips: Boolean get() = prefs.getBoolean(KEY_RECORD_TRIPS, true)

    val speedThresholdKmh: Float
        get() = prefs.getInt(KEY_SPEED_THRESHOLD, 3).toFloat()

    val speedThresholdMs: Float get() = speedThresholdKmh / 3.6f

    val showLocationOnStatus: Boolean
        get() = prefs.getBoolean(KEY_SHOW_LOC_STATUS, false)

    /** Cold + hot stream of changes for the given key. */
    fun changes(key: String): Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, k ->
            if (k == key) trySend(k)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(key)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    /** Stream of any change. */
    fun anyChange(): Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, k -> if (k != null) trySend(k) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    companion object {
        const val KEY_UNITS = "units"
        const val KEY_PANEL_RATIO = "panel_ratio"
        const val KEY_GRID_COLS = "grid_cols"
        const val KEY_GRID_ROWS = "grid_rows"
        const val KEY_RECORD_TRIPS = "record_trips"
        const val KEY_SPEED_THRESHOLD = "speed_threshold"
        const val KEY_SHOW_LOC_STATUS = "show_loc_status"
    }
}
