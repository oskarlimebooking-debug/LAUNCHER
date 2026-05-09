package com.oskar.retrolauncher.ui.trips

import com.oskar.retrolauncher.data.trip.TripEntity
import java.util.Calendar
import java.util.TimeZone

/**
 * Pure helpers for TripsViewModel — extracted so they can be unit-tested
 * without standing up the App service locator.
 */
internal object TripsFiltering {

    /**
     * Returns trips whose [TripEntity.startMs] falls on the same local
     * calendar day as [dayStartMs]. The end of the day is computed using
     * [Calendar] (not a fixed 86_400_000 ms offset) so DST boundaries are
     * respected correctly.
     */
    fun filterTripsForDay(
        trips: List<TripEntity>,
        dayStartMs: Long,
        tz: TimeZone = TimeZone.getDefault(),
    ): List<TripEntity> {
        val cal = Calendar.getInstance(tz).apply {
            timeInMillis = dayStartMs
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val nextDayStartMs = cal.timeInMillis
        return trips.filter { it.startMs in dayStartMs until nextDayStartMs }
    }

    /**
     * Per-day total distance in meters, keyed by start-of-local-day millis.
     * Drives the heatmap.
     */
    fun groupDistanceByDay(
        trips: List<TripEntity>,
        tz: TimeZone = TimeZone.getDefault(),
    ): Map<Long, Double> {
        if (trips.isEmpty()) return emptyMap()
        val cal = Calendar.getInstance(tz)
        val out = HashMap<Long, Double>()
        for (t in trips) {
            cal.timeInMillis = t.startMs
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            val key = cal.timeInMillis
            out[key] = (out[key] ?: 0.0) + t.distanceM
        }
        return out
    }
}
