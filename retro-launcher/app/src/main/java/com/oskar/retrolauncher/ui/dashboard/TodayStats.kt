package com.oskar.retrolauncher.ui.dashboard

import com.oskar.retrolauncher.data.trip.TripEntity

/** Aggregated driving for a single day. */
data class TodayDriveStats(
    val distanceM: Double = 0.0,
    val durationMs: Long = 0L,
    val tripCount: Int = 0,
)

/**
 * Pure day-window aggregation over recorded trips. Kept Android-free so the
 * day-boundary maths is unit-testable; the caller supplies the window
 * (a `Calendar` lives in the view-model, not here).
 */
object TodayStats {
    fun forDay(trips: List<TripEntity>, dayStartMs: Long, dayEndMs: Long): TodayDriveStats {
        val today = trips.filter { it.startMs in dayStartMs until dayEndMs }
        return TodayDriveStats(
            distanceM = today.sumOf { it.distanceM },
            durationMs = today.sumOf { (it.endMs - it.startMs).coerceAtLeast(0L) },
            tripCount = today.size,
        )
    }
}
