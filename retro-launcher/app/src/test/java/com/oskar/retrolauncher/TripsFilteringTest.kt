package com.oskar.retrolauncher

import com.oskar.retrolauncher.data.trip.TripEntity
import com.oskar.retrolauncher.ui.trips.TripsFiltering
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * T1.27 ACs covered:
 *   - AC1: Selecting a day filters the trip list to that day.
 *   - AC4: Empty state — when no trips fall on the selected day, the
 *          filter returns an empty list.
 */
class TripsFilteringTest {

    private val tz: TimeZone = TimeZone.getTimeZone("UTC")

    private fun startOfDay(year: Int, month0: Int, day: Int): Long =
        Calendar.getInstance(tz).apply {
            clear()
            set(year, month0, day, 0, 0, 0)
        }.timeInMillis

    private fun trip(id: Long, startMs: Long, distanceM: Double = 1_000.0): TripEntity =
        TripEntity(
            id = id,
            startMs = startMs,
            endMs = startMs + TimeUnit.MINUTES.toMillis(20),
            distanceM = distanceM,
            avgSpeedMs = 5.0,
            maxSpeedMs = 10.0,
            startLabel = null,
            endLabel = null,
        )

    @Test
    fun `filterTripsForDay returns only trips that started on the selected day`() {
        val day1 = startOfDay(2026, Calendar.MAY, 7)
        val day2 = startOfDay(2026, Calendar.MAY, 8)
        val day3 = startOfDay(2026, Calendar.MAY, 9)
        val trips = listOf(
            trip(1, day1 + TimeUnit.HOURS.toMillis(8)),
            trip(2, day2 + TimeUnit.HOURS.toMillis(9)),
            trip(3, day2 + TimeUnit.HOURS.toMillis(18)),
            trip(4, day3 + TimeUnit.HOURS.toMillis(7)),
        )

        val out = TripsFiltering.filterTripsForDay(trips, day2, tz)
        assertEquals(listOf<Long>(2, 3), out.map { it.id })
    }

    @Test
    fun `filterTripsForDay returns empty list when no trips match the selected day`() {
        val day = startOfDay(2026, Calendar.MAY, 9)
        val other = startOfDay(2026, Calendar.MAY, 8)
        val trips = listOf(trip(1, other + TimeUnit.HOURS.toMillis(7)))
        assertTrue(TripsFiltering.filterTripsForDay(trips, day, tz).isEmpty())
    }

    @Test
    fun `filterTripsForDay handles trips at the start-of-day boundary`() {
        val day = startOfDay(2026, Calendar.MAY, 9)
        val trips = listOf(
            trip(1, day),                                  // exactly midnight — included
            trip(2, day - 1L),                             // one ms before — excluded
            trip(3, day + TimeUnit.DAYS.toMillis(1)),      // exactly next midnight — excluded
        )
        val out = TripsFiltering.filterTripsForDay(trips, day, tz)
        assertEquals(listOf<Long>(1), out.map { it.id })
    }

    @Test
    fun `groupDistanceByDay sums distance per local day`() {
        val day = startOfDay(2026, Calendar.MAY, 9)
        val trips = listOf(
            trip(1, day + TimeUnit.HOURS.toMillis(7), distanceM = 1_000.0),
            trip(2, day + TimeUnit.HOURS.toMillis(18), distanceM = 2_500.0),
            trip(3, day + TimeUnit.DAYS.toMillis(1) + TimeUnit.HOURS.toMillis(9), distanceM = 7_500.0),
        )
        val out = TripsFiltering.groupDistanceByDay(trips, tz)
        assertEquals(2, out.size)
        assertEquals(3_500.0, out[day]!!, 0.0001)
        assertEquals(7_500.0, out[day + TimeUnit.DAYS.toMillis(1)]!!, 0.0001)
    }

    @Test
    fun `groupDistanceByDay returns empty map for empty input`() {
        assertTrue(TripsFiltering.groupDistanceByDay(emptyList(), tz).isEmpty())
    }
}
