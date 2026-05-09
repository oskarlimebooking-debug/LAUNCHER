package com.oskar.retrolauncher.data.trip

import kotlinx.coroutines.flow.Flow

class TripRepository(private val dao: TripDao) {

    fun all(): Flow<List<TripEntity>> = dao.all()

    fun byRange(fromMs: Long, untilMs: Long): Flow<List<TripEntity>> =
        dao.byRange(fromMs, untilMs)

    suspend fun delete(t: TripEntity) = dao.delete(t)

    suspend fun pointsFor(tripId: Long): List<TripPoint> = dao.points(tripId)
}
