package com.oskar.retrolauncher.data.prefs

import android.content.SharedPreferences
import com.oskar.retrolauncher.R
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class Units { METRIC, IMPERIAL }

enum class TempUnit { CELSIUS, FAHRENHEIT }

enum class SpeedUnit { METERS_PER_SECOND, KILOMETERS_PER_HOUR, MILES_PER_HOUR }

enum class Theme(val styleRes: Int) {
    SYSTEM(R.style.Theme_RetroLauncher),
    MOCHA(R.style.Theme_RetroLauncher_Mocha),
    OCEAN(R.style.Theme_RetroLauncher_Ocean),
    FOREST(R.style.Theme_RetroLauncher_Forest),
    LIGHT(R.style.Theme_RetroLauncher),
    DARK(R.style.Theme_RetroLauncher);
}

/**
 * Typed wrapper around the app's default [SharedPreferences]. Per T1.31 every
 * surfaced property also exposes a `Flow<T>` so UI layers can react without
 * manual listener bookkeeping.
 *
 * Writes use [SharedPreferences.Editor.apply] — non-blocking disk writes that
 * are safe to call from any thread, satisfying T1.31 AC4.
 */
class SettingsStore(
    private val prefs: SharedPreferences,
    moshi: Moshi = defaultMoshi,
) {

    private val stringListAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    // ───────── Units / derived ─────────

    val units: Units
        get() = if (prefs.getString(KEY_UNITS, "metric") == "metric") Units.METRIC else Units.IMPERIAL

    val tempUnit: TempUnit
        get() = if (units == Units.METRIC) TempUnit.CELSIUS else TempUnit.FAHRENHEIT

    val speedUnit: SpeedUnit
        get() = if (units == Units.METRIC) SpeedUnit.KILOMETERS_PER_HOUR else SpeedUnit.MILES_PER_HOUR

    // ───────── Layout ─────────

    /** Right panel width as percent (30-70). Retained for existing call sites. */
    val panelRatioPercent: Int get() = prefs.getInt(KEY_PANEL_RATIO, 40).coerceIn(30, 70)

    /** Spec 14.2 — panel ratio as Float (0.3-0.7). 0.4 default = 40% width. */
    val panelRatio: Float get() = panelRatioPercent / 100f

    val gridCols: Int get() = prefs.getString(KEY_GRID_COLS, "4")?.toIntOrNull() ?: 4

    /** Spec 14.2 alias — Int gridColumns. */
    val gridColumns: Int get() = gridCols.coerceIn(3, 5)

    val gridRows: Int get() = prefs.getString(KEY_GRID_ROWS, "3")?.toIntOrNull() ?: 3

    // ───────── Trip recording ─────────

    val recordTrips: Boolean get() = prefs.getBoolean(KEY_RECORD_TRIPS, true)

    val speedThresholdKmh: Float
        get() = prefs.getInt(KEY_SPEED_THRESHOLD, 3).toFloat()

    val speedThresholdMs: Float get() = speedThresholdKmh / 3.6f

    /** Spec 14.2 — speed threshold at which a trip auto-starts (km/h, Int). */
    val tripStartSpeedKmh: Int get() = prefs.getInt(KEY_SPEED_THRESHOLD, 3)

    val showLocationOnStatus: Boolean
        get() = prefs.getBoolean(KEY_SHOW_LOC_STATUS, false)

    // ───────── App lists ─────────

    val appOrder: List<String>
        get() = decodeStringList(prefs.getString(KEY_APP_ORDER, null))

    fun setAppOrder(order: List<String>) {
        writeStringList(KEY_APP_ORDER, order)
    }

    /**
     * Ordered list of package names pinned to the rail (T1.30 / spec 14.2).
     * Persisted as a Moshi-encoded JSON array (T1.31 AC2).
     */
    val pinnedApps: List<String>
        get() = decodeStringList(prefs.getString(KEY_PINNED_APPS, null))

    fun setPinnedApps(packages: List<String>) {
        writeStringList(KEY_PINNED_APPS, packages)
    }

    // ───────── Preferred apps + first-run ─────────

    val mapApp: String? get() = prefs.getString(KEY_MAP_APP, null)
    fun setMapApp(pkg: String?) = writeNullableString(KEY_MAP_APP, pkg)

    val voiceApp: String? get() = prefs.getString(KEY_VOICE_APP, null)
    fun setVoiceApp(pkg: String?) = writeNullableString(KEY_VOICE_APP, pkg)

    val weatherLat: Float? get() = readNullableFloat(KEY_WEATHER_LAT)
    val weatherLon: Float? get() = readNullableFloat(KEY_WEATHER_LON)

    /** Effective weather coords: user override if set, else the Ljubljana default. */
    val effectiveWeatherLat: Float get() = weatherLat ?: DEFAULT_WEATHER_LAT
    val effectiveWeatherLon: Float get() = weatherLon ?: DEFAULT_WEATHER_LON

    fun setWeatherLocation(lat: Float?, lon: Float?) {
        prefs.edit().apply {
            if (lat == null) remove(KEY_WEATHER_LAT) else putFloat(KEY_WEATHER_LAT, lat)
            if (lon == null) remove(KEY_WEATHER_LON) else putFloat(KEY_WEATHER_LON, lon)
        }.apply()
    }

    val firstRunDone: Boolean get() = prefs.getBoolean(KEY_FIRST_RUN_DONE, false)
    fun setFirstRunDone(done: Boolean) {
        prefs.edit().putBoolean(KEY_FIRST_RUN_DONE, done).apply()
    }

    /** OWM API key collected by the T1.33 wizard. Blank → null → WeatherRepository falls back to BuildConfig. */
    val owmApiKey: String? get() = prefs.getString(KEY_OWM_API_KEY, null)?.takeIf { it.isNotBlank() }
    fun setOwmApiKey(key: String?) {
        val clean = key?.trim().orEmpty()
        writeNullableString(KEY_OWM_API_KEY, clean.takeIf { it.isNotEmpty() })
    }

    // ───────── Theme / trip export / weather refresh (T1.32) ─────────

    val theme: Theme
        get() = when (prefs.getString(KEY_THEME, "system")) {
            "light" -> Theme.LIGHT
            "dark" -> Theme.DARK
            "mocha" -> Theme.MOCHA
            "ocean" -> Theme.OCEAN
            "forest" -> Theme.FOREST
            else -> Theme.SYSTEM
        }

    fun setTheme(theme: Theme) {
        val value = when (theme) {
            Theme.SYSTEM -> "system"
            Theme.LIGHT -> "light"
            Theme.DARK -> "dark"
            Theme.MOCHA -> "mocha"
            Theme.OCEAN -> "ocean"
            Theme.FOREST -> "forest"
        }
        prefs.edit().putString(KEY_THEME, value).apply()
    }

    val gpxExport: Boolean get() = prefs.getBoolean(KEY_GPX_EXPORT, false)
    fun setGpxExport(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GPX_EXPORT, enabled).apply()
    }

    val weatherRefreshIntervalMin: Int
        get() = prefs.getInt(KEY_WEATHER_REFRESH_MIN, 30).coerceAtLeast(5)

    fun setWeatherRefreshIntervalMin(minutes: Int) {
        prefs.edit().putInt(KEY_WEATHER_REFRESH_MIN, minutes.coerceAtLeast(5)).apply()
    }

    // ───────── Per-property Flows (T1.31 AC3) ─────────

    val unitsFlow: Flow<Units> = flowOfKey(KEY_UNITS) { units }
    val tempUnitFlow: Flow<TempUnit> = flowOfKey(KEY_UNITS) { tempUnit }
    val speedUnitFlow: Flow<SpeedUnit> = flowOfKey(KEY_UNITS) { speedUnit }
    val panelRatioFlow: Flow<Float> = flowOfKey(KEY_PANEL_RATIO) { panelRatio }
    val gridColumnsFlow: Flow<Int> = flowOfKey(KEY_GRID_COLS) { gridColumns }
    val recordTripsFlow: Flow<Boolean> = flowOfKey(KEY_RECORD_TRIPS) { recordTrips }
    val tripStartSpeedKmhFlow: Flow<Int> = flowOfKey(KEY_SPEED_THRESHOLD) { tripStartSpeedKmh }
    val pinnedAppsFlow: Flow<List<String>> = flowOfKey(KEY_PINNED_APPS) { pinnedApps }
    val appOrderFlow: Flow<List<String>> = flowOfKey(KEY_APP_ORDER) { appOrder }
    val mapAppFlow: Flow<String?> = flowOfKey(KEY_MAP_APP) { mapApp }
    val voiceAppFlow: Flow<String?> = flowOfKey(KEY_VOICE_APP) { voiceApp }
    val weatherLatFlow: Flow<Float?> = flowOfKey(KEY_WEATHER_LAT) { weatherLat }
    val weatherLonFlow: Flow<Float?> = flowOfKey(KEY_WEATHER_LON) { weatherLon }
    val firstRunDoneFlow: Flow<Boolean> = flowOfKey(KEY_FIRST_RUN_DONE) { firstRunDone }
    val owmApiKeyFlow: Flow<String?> = flowOfKey(KEY_OWM_API_KEY) { owmApiKey }
    val themeFlow: Flow<Theme> = flowOfKey(KEY_THEME) { theme }
    val gpxExportFlow: Flow<Boolean> = flowOfKey(KEY_GPX_EXPORT) { gpxExport }
    val weatherRefreshIntervalMinFlow: Flow<Int> =
        flowOfKey(KEY_WEATHER_REFRESH_MIN) { weatherRefreshIntervalMin }

    // ───────── Listener flows (legacy, still used) ─────────

    fun changes(key: String): Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, k ->
            if (k == key) trySend(k)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(key)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun anyChange(): Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, k -> if (k != null) trySend(k) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // ───────── Private helpers ─────────

    private fun <T> flowOfKey(key: String, read: () -> T): Flow<T> =
        changes(key).map { read() }.distinctUntilChanged()

    private fun decodeStringList(raw: String?): List<String> {
        if (raw.isNullOrEmpty()) return emptyList()
        return runCatching { stringListAdapter.fromJson(raw) ?: emptyList() }
            .getOrElse { emptyList() }
    }

    private fun writeStringList(key: String, list: List<String>) {
        val edit = prefs.edit()
        if (list.isEmpty()) edit.remove(key) else edit.putString(key, stringListAdapter.toJson(list))
        edit.apply()
    }

    private fun writeNullableString(key: String, value: String?) {
        val edit = prefs.edit()
        if (value == null) edit.remove(key) else edit.putString(key, value)
        edit.apply()
    }

    private fun readNullableFloat(key: String): Float? =
        if (prefs.contains(key)) prefs.getFloat(key, 0f) else null

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
        const val KEY_MAP_APP = "map_app"
        const val KEY_VOICE_APP = "voice_app"
        const val KEY_WEATHER_LAT = "weather_lat"
        const val KEY_WEATHER_LON = "weather_lon"
        const val KEY_FIRST_RUN_DONE = "first_run_done"
        const val KEY_OWM_API_KEY = "owm_api_key"
        const val KEY_THEME = "theme"
        const val KEY_GPX_EXPORT = "gpx_export"
        const val KEY_WEATHER_REFRESH_MIN = "weather_refresh_min"

        /** Hard cap on rail entries (T1.30 AC1 / AC3). */
        const val MAX_PINNED = 8

        /** Default weather coordinates when no override and no GPS fix — Ljubljana, Slovenia. */
        const val DEFAULT_WEATHER_LAT = 46.0569f
        const val DEFAULT_WEATHER_LON = 14.5058f

        private val defaultMoshi: Moshi by lazy {
            Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        }
    }
}
