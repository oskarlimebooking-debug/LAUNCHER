package com.oskar.retrolauncher

import com.oskar.retrolauncher.data.trip.TripEntity
import com.oskar.retrolauncher.ui.dashboard.TodayStats
import org.junit.Assert.assertEquals
import org.junit.Test

/** A.1 — pure day-window aggregation for the dashboard's "today" stats. */
class TodayStatsTest {

    private fun trip(start: Long, end: Long, dist: Double) =
        TripEntity(startMs = start, endMs = end, distanceM = dist, avgSpeedMs = 0.0, maxSpeedMs = 0.0, startLabel = null, endLabel = null)

    private val dayStart = 1_000_000L
    private val dayEnd = dayStart + 24L * 60 * 60 * 1000

    @Test
    fun `sums distance and duration for trips within the day`() {
        val stats = TodayStats.forDay(
            listOf(
                trip(dayStart + 1000, dayStart + 61_000, 1500.0), // 60s, 1.5km
                trip(dayStart + 200_000, dayStart + 500_000, 3000.0), // 300s, 3km
            ),
            dayStart, dayEnd,
        )
        assertEquals(4500.0, stats.distanceM, 0.001)
        assertEquals(360_000L, stats.durationMs)
        assertEquals(2, stats.tripCount)
    }

    @Test
    fun `excludes trips before and after the window`() {
        val stats = TodayStats.forDay(
            listOf(
                trip(dayStart - 5000, dayStart - 1000, 999.0), // yesterday
                trip(dayEnd + 1000, dayEnd + 5000, 999.0), // tomorrow
                trip(dayStart + 10, dayStart + 10_010, 800.0), // today
            ),
            dayStart, dayEnd,
        )
        assertEquals(800.0, stats.distanceM, 0.001)
        assertEquals(1, stats.tripCount)
    }

    @Test
    fun `empty when no trips fall in the window`() {
        val stats = TodayStats.forDay(emptyList(), dayStart, dayEnd)
        assertEquals(0.0, stats.distanceM, 0.0)
        assertEquals(0L, stats.durationMs)
        assertEquals(0, stats.tripCount)
    }
}
