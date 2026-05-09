package com.oskar.retrolauncher.data.trip

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Read/write surface for persisted trips. Wraps [TripDao] queries and exposes the
 * `activeTrip` StateFlow published by [TripRecorder] so observers can render the
 * in-progress trip without depending on the recorder directly.
 *
 * `recentTrips` is a Flow filtered to a 30-day window, computed at collection time
 * via [nowMs]. The Flow itself stays subscribed to Room's invalidation tracker —
 * any insert/update/delete on the `trips` table re-emits within tens of ms (AC1).
 */
class TripRepository(
    private val dao: TripDao,
    private val activeTripFlow: StateFlow<TripEntity?> = MutableStateFlow(null),
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val recentWindowMs: Long = DEFAULT_WINDOW_MS,
) {

    val recentTrips: Flow<List<TripEntity>>
        get() = dao.byRange(nowMs() - recentWindowMs, Long.MAX_VALUE)

    val activeTrip: StateFlow<TripEntity?> get() = activeTripFlow

    fun all(): Flow<List<TripEntity>> = dao.all()

    fun byRange(fromMs: Long, untilMs: Long): Flow<List<TripEntity>> =
        dao.byRange(fromMs, untilMs)

    fun getPoints(tripId: Long): Flow<List<TripPoint>> = dao.pointsFlow(tripId)

    suspend fun delete(tripId: Long): Int = dao.deleteById(tripId)

    suspend fun delete(t: TripEntity) = dao.delete(t)

    suspend fun pointsFor(tripId: Long): List<TripPoint> = dao.points(tripId)

    private companion object {
        const val DEFAULT_WINDOW_MS = 30L * 24 * 60 * 60 * 1000
    }
}
