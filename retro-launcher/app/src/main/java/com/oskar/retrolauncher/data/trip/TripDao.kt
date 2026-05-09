package com.oskar.retrolauncher.data.trip

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startMs DESC")
    fun all(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE startMs >= :from AND startMs < :to ORDER BY startMs DESC")
    fun byRange(from: Long, to: Long): Flow<List<TripEntity>>

    @Insert
    suspend fun insert(t: TripEntity): Long

    @Update
    suspend fun update(t: TripEntity)

    @Delete
    suspend fun delete(t: TripEntity)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Insert
    suspend fun insertPoints(points: List<TripPoint>)

    @Query("SELECT * FROM trip_points WHERE tripId = :id ORDER BY tsMs ASC")
    suspend fun points(id: Long): List<TripPoint>

    @Query("SELECT * FROM trip_points WHERE tripId = :id ORDER BY tsMs ASC")
    fun pointsFlow(id: Long): Flow<List<TripPoint>>
}
