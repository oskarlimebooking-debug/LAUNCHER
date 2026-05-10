package com.oskar.retrolauncher.data.prefs

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONArray

enum class Units { METRIC, IMPERIAL }

enum class TempUnit { CELSIUS, FAHRENHEIT }

enum class SpeedUnit { METERS_PER_SECOND, KILOMETERS_PER_HOUR, MILES_PER_HOUR }

class SettingsStore(private val prefs: SharedPreferences) {

    val units: Units
        get() = if (prefs.getString(KEY_UNITS, "metric") == "metric") Units.METRIC else Units.IMPERIAL

    // Derived from `units` for now. T1.31+ may add a separate pref for m/s as a
    // metric speed override; until then the metric → km/h, imperial → mph rule
    // matches the existing speedometer behaviour.
    val tempUnit: TempUnit
        get() = if (units == Units.METRIC) TempUnit.CELSIUS else TempUnit.FAHRENHEIT

    val speedUnit: SpeedUnit
        get() = if (units == Units.METRIC) SpeedUnit.KILOMETERS_PER_HOUR else SpeedUnit.MILES_PER_HOUR

    val panelRatioPercent: Int get() = prefs.getInt(KEY_PANEL_RATIO, 40).coerceIn(30, 70)

    val gridCols: Int get() = prefs.getString(KEY_GRID_COLS, "4")?.toIntOrNull() ?: 4
    val gridRows: Int get() = prefs.getString(KEY_GRID_ROWS, "3")?.toIntOrNull() ?: 3

    val recordTrips: Boolean get() = prefs.getBoolean(KEY_RECORD_TRIPS, true)

    val speedThresholdKmh: Float
        get() = prefs.getInt(KEY_SPEED_THRESHOLD, 3).toFloat()

    val speedThresholdMs: Float get() = speedThresholdKmh / 3.6f

    val showLocationOnStatus: Boolean
        get() = prefs.getBoolean(KEY_SHOW_LOC_STATUS, false)

    /**
     * User-defined order for the app grid as a list of `package/activity` strings
     * (ComponentName#flattenToShortString). Empty list means "use the default
     * alphabetical sort."
     */
    val appOrder: List<String>
        get() {
            val raw = prefs.getString(KEY_APP_ORDER, null) ?: return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { arr.getString(it) }
            }.getOrElse { emptyList() }
        }

    fun setAppOrder(order: List<String>) {
        val edit = prefs.edit()
        if (order.isEmpty()) {
            edit.remove(KEY_APP_ORDER)
        } else {
            val arr = JSONArray().apply { order.forEach { put(it) } }
            edit.putString(KEY_APP_ORDER, arr.toString())
        }
        edit.apply()
    }

    /**
     * Ordered list of package names pinned to the rail (T1.30). The list is
     * the source of truth for rail ordering; order matters because the rail
     * enforces FIFO eviction at [MAX_PINNED]. Empty when nothing is pinned.
     */
    val pinnedApps: List<String>
        get() {
            val raw = prefs.getString(KEY_PINNED_APPS, null) ?: return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { arr.getString(it) }
            }.getOrElse { emptyList() }
        }

    fun setPinnedApps(packages: List<String>) {
        val edit = prefs.edit()
        if (packages.isEmpty()) {
            edit.remove(KEY_PINNED_APPS)
        } else {
            val arr = JSONArray().apply { packages.forEach { put(it) } }
            edit.putString(KEY_PINNED_APPS, arr.toString())
        }
        edit.apply()
    }

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
        const val KEY_APP_ORDER = "app_order"
        const val KEY_PINNED_APPS = "pinned_apps"

        /** Hard cap on rail entries (T1.30 AC1 / AC3). */
        const val MAX_PINNED = 8
    }
}
