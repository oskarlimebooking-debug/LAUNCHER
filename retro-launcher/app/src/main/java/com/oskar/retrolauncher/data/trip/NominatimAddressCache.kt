package com.oskar.retrolauncher.data.trip

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

/**
 * T1.39 — small SharedPreferences-backed cache of reverse-geocoded labels keyed by
 * a rounded (lat, lon) pair. Used by the trip-detail screen to show start/end
 * city names without re-hitting Nominatim on every render. When the cache is cold
 * (or Nominatim has not yet warmed it via the weather repository), falls back to
 * a "lat, lon" formatted string per AC5.
 *
 * Rounding precision is 4 decimal places (~11 m at the equator), enough to keep
 * two stops at the same parking spot sharing one entry without merging stops in
 * different cities.
 */
class NominatimAddressCache(
    private val prefs: SharedPreferences,
) {
    constructor(ctx: Context) : this(
        ctx.getSharedPreferences("nominatim_addr", Context.MODE_PRIVATE),
    )

    fun lookup(lat: Double, lon: Double): String? = prefs.getString(key(lat, lon), null)

    fun put(lat: Double, lon: Double, label: String) {
        if (label.isBlank()) return
        prefs.edit().putString(key(lat, lon), label).apply()
    }

    /**
     * Resolve a display label using the priority chain:
     * 1. [explicit] (e.g. `TripEntity.startLabel`) if non-blank.
     * 2. Cached Nominatim label.
     * 3. Lat/lon formatted at 4 decimal places ("60.1699, 24.9384").
     */
    fun labelOrFallback(lat: Double, lon: Double, explicit: String? = null): String {
        val direct = explicit?.takeIf { it.isNotBlank() }
        if (direct != null) return direct
        val cached = lookup(lat, lon)?.takeIf { it.isNotBlank() }
        if (cached != null) return cached
        return formatLatLon(lat, lon)
    }

    private fun key(lat: Double, lon: Double): String =
        String.format(Locale.US, "%.4f,%.4f", lat, lon)

    private companion object {
        fun formatLatLon(lat: Double, lon: Double): String =
            String.format(Locale.US, "%.4f, %.4f", lat, lon)
    }
}
