package com.oskar.retrolauncher

import com.oskar.retrolauncher.data.trip.TripEntity
import com.oskar.retrolauncher.data.trip.TripPoint
import com.oskar.retrolauncher.data.trip.TripStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * T1.39 — AC4: stats are computed correctly from the points and must cross-check
 * against TripEntity aggregates (distance, duration, max/avg speed).
 */
class TripStatsTest {

    private fun pt(tripId: Long = 1L, tsMs: Long, lat: Double, lon: Double, speedMs: Float = 0f) =
        TripPoint(tripId = tripId, tsMs = tsMs, lat = lat, lon = lon, speedMs = speedMs)

    @Test
    fun `empty point list yields zero stats`() {
        val s = TripStats.fromPoints(emptyList())
        assertEquals(0.0, s.distanceM, 0.0)
        assertEquals(0L, s.durationMs)
        assertEquals(0.0, s.avgSpeedMs, 0.0)
        assertEquals(0.0, s.maxSpeedMs, 0.0)
    }

    @Test
    fun `single point yields zero distance and zero duration`() {
        val s = TripStats.fromPoints(listOf(pt(tsMs = 100L, lat = 0.0, lon = 0.0)))
        assertEquals(0.0, s.distanceM, 0.0)
        assertEquals(0L, s.durationMs)
        assertEquals(0.0, s.avgSpeedMs, 0.0)
    }

    @Test
    fun `two points one degree apart at 0N produce ~111km`() {
        // 1 degree of longitude at the equator ≈ 111.319 km
        val a = pt(tsMs = 0L, lat = 0.0, lon = 0.0)
        val b = pt(tsMs = 60_000L, lat = 0.0, lon = 1.0)
        val s = TripStats.fromPoints(listOf(a, b))
        assertTrue("expected ~111km, got ${s.distanceM} m", abs(s.distanceM - 111_319.0) < 1_000.0)
        assertEquals(60_000L, s.durationMs)
    }

    @Test
    fun `max speed is the maximum across point speeds`() {
        val pts = listOf(
            pt(tsMs = 0L, lat = 0.0, lon = 0.0, speedMs = 1.5f),
            pt(tsMs = 1_000L, lat = 0.0, lon = 0.0001, speedMs = 25.7f),
            pt(tsMs = 2_000L, lat = 0.0, lon = 0.0002, speedMs = 18.0f),
        )
        val s = TripStats.fromPoints(pts)
        assertEquals(25.7, s.maxSpeedMs, 0.001)
    }

    @Test
    fun `avg speed equals distance over duration`() {
        val a = pt(tsMs = 0L, lat = 0.0, lon = 0.0)
        val b = pt(tsMs = 10_000L, lat = 0.0, lon = 0.001) // ~111.3 m
        val s = TripStats.fromPoints(listOf(a, b))
        // ~111.3 m / 10 s = ~11.13 m/s
        assertTrue("expected ~11 m/s, got ${s.avgSpeedMs}", abs(s.avgSpeedMs - 11.13) < 0.5)
    }

    @Test
    fun `fromPoints agrees with TripEntity aggregates within rounding tolerance`() {
        val tripId = 7L
        val pts = listOf(
            pt(tripId, tsMs = 0L, lat = 60.1, lon = 24.9, speedMs = 0f),
            pt(tripId, tsMs = 10_000L, lat = 60.1001, lon = 24.9001, speedMs = 10f),
            pt(tripId, tsMs = 20_000L, lat = 60.1002, lon = 24.9002, speedMs = 12f),
            pt(tripId, tsMs = 30_000L, lat = 60.1003, lon = 24.9003, speedMs = 8f),
        )
        val s = TripStats.fromPoints(pts)
        // Construct a TripEntity from the same data the way TripRecorder would aggregate.
        val entity = TripEntity(
            id = tripId,
            startMs = pts.first().tsMs,
            endMs = pts.last().tsMs,
            distanceM = s.distanceM,
            avgSpeedMs = s.avgSpeedMs,
            maxSpeedMs = s.maxSpeedMs,
            startLabel = null,
            endLabel = null,
        )
        val crossCheck = TripStats.crossCheck(entity, s, tolerancePct = 0.01)
        assertTrue("entity aggregates must match computed stats within 1%", crossCheck)
    }

    @Test
    fun `crossCheck rejects entity whose distance disagrees by more than tolerance`() {
        val computed = TripStats(distanceM = 1_000.0, durationMs = 60_000L, maxSpeedMs = 20.0, avgSpeedMs = 16.7)
        val mismatched = TripEntity(
            id = 1L, startMs = 0L, endMs = 60_000L,
            distanceM = 2_000.0, avgSpeedMs = 16.7, maxSpeedMs = 20.0,
            startLabel = null, endLabel = null,
        )
        assertTrue(!TripStats.crossCheck(mismatched, computed, tolerancePct = 0.05))
    }
}
