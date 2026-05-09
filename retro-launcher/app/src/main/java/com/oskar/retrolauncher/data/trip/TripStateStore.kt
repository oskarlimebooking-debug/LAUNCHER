package com.oskar.retrolauncher.data.trip

import android.content.SharedPreferences

/**
 * Persisted snapshot of an in-progress trip — used to recover work if the
 * service is killed mid-trip. Persisted on every committed bucket so the
 * worst-case data loss is one bucket window (default 5 s).
 */
data class TripSnapshot(
    val startMs: Long,
    val lastSampleMs: Long,
    val distanceM: Double,
    val maxSpeedMs: Double,
    val points: List<TripPointData>,
)

data class TripPointData(
    val tsMs: Long,
    val lat: Double,
    val lon: Double,
    val speedMs: Float,
)

interface TripStateStore {
    fun save(snapshot: TripSnapshot)
    fun load(): TripSnapshot?
    fun clear()
}

class InMemoryTripStateStore : TripStateStore {
    private var snap: TripSnapshot? = null
    override fun save(snapshot: TripSnapshot) { snap = snapshot }
    override fun load(): TripSnapshot? = snap
    override fun clear() { snap = null }
}

/**
 * SharedPreferences-backed [TripStateStore]. Encodes the points list as a
 * delimited string to avoid pulling in a JSON dependency on a hot write path.
 */
class SharedPrefsTripStateStore(
    private val prefs: SharedPreferences,
    private val keyPrefix: String = DEFAULT_KEY_PREFIX,
) : TripStateStore {

    override fun save(snapshot: TripSnapshot) {
        prefs.edit()
            .putLong(key(KEY_START), snapshot.startMs)
            .putLong(key(KEY_LAST), snapshot.lastSampleMs)
            .putLong(key(KEY_DISTANCE), java.lang.Double.doubleToRawLongBits(snapshot.distanceM))
            .putLong(key(KEY_MAX_SPEED), java.lang.Double.doubleToRawLongBits(snapshot.maxSpeedMs))
            .putString(key(KEY_POINTS), encodePoints(snapshot.points))
            .putBoolean(key(KEY_PRESENT), true)
            .apply()
    }

    override fun load(): TripSnapshot? {
        if (!prefs.getBoolean(key(KEY_PRESENT), false)) return null
        val pointsRaw = prefs.getString(key(KEY_POINTS), null) ?: return null
        return TripSnapshot(
            startMs = prefs.getLong(key(KEY_START), 0L),
            lastSampleMs = prefs.getLong(key(KEY_LAST), 0L),
            distanceM = java.lang.Double.longBitsToDouble(prefs.getLong(key(KEY_DISTANCE), 0L)),
            maxSpeedMs = java.lang.Double.longBitsToDouble(prefs.getLong(key(KEY_MAX_SPEED), 0L)),
            points = decodePoints(pointsRaw),
        )
    }

    override fun clear() {
        prefs.edit()
            .remove(key(KEY_START))
            .remove(key(KEY_LAST))
            .remove(key(KEY_DISTANCE))
            .remove(key(KEY_MAX_SPEED))
            .remove(key(KEY_POINTS))
            .remove(key(KEY_PRESENT))
            .apply()
    }

    private fun key(suffix: String) = "$keyPrefix.$suffix"

    private fun encodePoints(points: List<TripPointData>): String =
        points.joinToString(POINT_SEP) { "${it.tsMs}$FIELD_SEP${it.lat}$FIELD_SEP${it.lon}$FIELD_SEP${it.speedMs}" }

    private fun decodePoints(raw: String): List<TripPointData> {
        if (raw.isEmpty()) return emptyList()
        return raw.split(POINT_SEP).mapNotNull { token ->
            val parts = token.split(FIELD_SEP)
            if (parts.size != 4) return@mapNotNull null
            TripPointData(
                tsMs = parts[0].toLongOrNull() ?: return@mapNotNull null,
                lat = parts[1].toDoubleOrNull() ?: return@mapNotNull null,
                lon = parts[2].toDoubleOrNull() ?: return@mapNotNull null,
                speedMs = parts[3].toFloatOrNull() ?: return@mapNotNull null,
            )
        }
    }

    private companion object {
        const val DEFAULT_KEY_PREFIX = "trip_recovery"
        const val KEY_START = "startMs"
        const val KEY_LAST = "lastMs"
        const val KEY_DISTANCE = "distanceM"
        const val KEY_MAX_SPEED = "maxSpeedMs"
        const val KEY_POINTS = "points"
        const val KEY_PRESENT = "present"
        const val POINT_SEP = ";"
        const val FIELD_SEP = "|"
    }
}
