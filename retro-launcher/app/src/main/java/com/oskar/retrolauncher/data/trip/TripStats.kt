package com.oskar.retrolauncher.data.trip

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * T1.39 — derived trip metrics computed from raw [TripPoint] samples.
 *
 * Used by `TripDetailFragment` to render the bottom stats panel and to
 * cross-check the values persisted in [TripEntity] (AC4).
 */
data class TripStats(
    val distanceM: Double,
    val durationMs: Long,
    val maxSpeedMs: Double,
    val avgSpeedMs: Double,
) {
    companion object {
        private const val EARTH_RADIUS_M = 6_371_008.8

        fun fromPoints(points: List<TripPoint>): TripStats {
            if (points.size < 2) return TripStats(0.0, 0L, 0.0, 0.0)
            var distance = 0.0
            var maxSpeed = 0.0
            for (i in 1 until points.size) {
                distance += haversine(points[i - 1], points[i])
                val s = points[i].speedMs.toDouble()
                if (s > maxSpeed) maxSpeed = s
            }
            // First sample's speed too (it might be the max for very short trips).
            val firstSpeed = points.first().speedMs.toDouble()
            if (firstSpeed > maxSpeed) maxSpeed = firstSpeed

            val duration = points.last().tsMs - points.first().tsMs
            val avg = if (duration > 0L) distance / (duration / 1000.0) else 0.0
            return TripStats(
                distanceM = distance,
                durationMs = duration,
                maxSpeedMs = maxSpeed,
                avgSpeedMs = avg,
            )
        }

        /**
         * AC4 — confirms the persisted [TripEntity] aggregates match values
         * derived from the raw points within [tolerancePct] (0.0–1.0).
         * Used by tests; also exposed for debug-build sanity checks.
         */
        fun crossCheck(entity: TripEntity, computed: TripStats, tolerancePct: Double = 0.05): Boolean {
            fun within(a: Double, b: Double): Boolean {
                if (a == 0.0 && b == 0.0) return true
                val denom = if (abs(a) > abs(b)) abs(a) else abs(b)
                return abs(a - b) / denom <= tolerancePct
            }
            return within(entity.distanceM, computed.distanceM) &&
                within(entity.maxSpeedMs, computed.maxSpeedMs) &&
                within(entity.avgSpeedMs, computed.avgSpeedMs)
        }

        private fun haversine(a: TripPoint, b: TripPoint): Double {
            val lat1 = Math.toRadians(a.lat)
            val lat2 = Math.toRadians(b.lat)
            val dLat = lat2 - lat1
            val dLon = Math.toRadians(b.lon - a.lon)
            val h = sin(dLat / 2).let { it * it } +
                cos(lat1) * cos(lat2) * sin(dLon / 2).let { it * it }
            return 2 * EARTH_RADIUS_M * atan2(sqrt(h), sqrt(1 - h))
        }
    }
}

/**
 * Axis-aligned lat/lon bounding box for a list of points. Used by the
 * detail map to compute the initial zoomToBoundingBox span.
 */
data class GeoBoundingBox(
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double,
) {
    companion object {
        fun fromPoints(points: List<TripPoint>): GeoBoundingBox? {
            if (points.isEmpty()) return null
            var minLat = Double.POSITIVE_INFINITY
            var minLon = Double.POSITIVE_INFINITY
            var maxLat = Double.NEGATIVE_INFINITY
            var maxLon = Double.NEGATIVE_INFINITY
            for (p in points) {
                if (p.lat < minLat) minLat = p.lat
                if (p.lat > maxLat) maxLat = p.lat
                if (p.lon < minLon) minLon = p.lon
                if (p.lon > maxLon) maxLon = p.lon
            }
            return GeoBoundingBox(minLat, minLon, maxLat, maxLon)
        }
    }
}
