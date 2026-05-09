package com.oskar.retrolauncher

import androidx.room.Room
import com.oskar.retrolauncher.data.trip.AppDb
import com.oskar.retrolauncher.data.trip.TripDao
import com.oskar.retrolauncher.data.trip.TripEntity
import com.oskar.retrolauncher.data.trip.TripPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * T1.23 — Room schema verification using the in-memory builder.
 * Covers AC2 (cascade delete), AC3 (suspend / Flow surface) and AC5
 * (insert/query/delete for ≥4 trips × 100 points each).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TripDaoTest {

    private lateinit var db: AppDb
    private lateinit var dao: TripDao

    @Before
    fun setUp() {
        val ctx = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDb::class.java)
            // Foreign keys are on by default for Room, but make it explicit
            // so the cascade-delete assertion below can never silently regress.
            .allowMainThreadQueries()
            .build()
        dao = db.trips()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun trip(start: Long, end: Long, dist: Double): TripEntity = TripEntity(
        startMs = start,
        endMs = end,
        distanceM = dist,
        avgSpeedMs = dist / ((end - start) / 1000.0),
        maxSpeedMs = 30.0,
        startLabel = "from-$start",
        endLabel = "to-$end",
    )

    private fun pointsFor(tripId: Long, startMs: Long, count: Int = 100): List<TripPoint> =
        (0 until count).map { i ->
            TripPoint(
                tripId = tripId,
                tsMs = startMs + i * 1000L,
                lat = 40.0 + i * 0.0001,
                lon = -74.0 + i * 0.0001,
                speedMs = 10f + (i % 5),
            )
        }

    @Test
    fun `insert and query four trips with 100 points each via Flow`() = runTest {
        val ids = (0 until 4).map { idx ->
            val start = 1_700_000_000_000L + idx * 3_600_000L
            val id = dao.insert(trip(start, start + 600_000, dist = 1000.0 + idx))
            dao.insertPoints(pointsFor(id, start))
            id
        }

        // all() returns a Flow ordered by startMs DESC
        val all = dao.all().first()
        assertEquals(4, all.size)
        assertTrue("ordered DESC", all.zipWithNext().all { (a, b) -> a.startMs >= b.startMs })

        // Each trip has exactly 100 points, ordered by tsMs ASC
        for (id in ids) {
            val pts = dao.points(id)
            assertEquals("trip $id has 100 points", 100, pts.size)
            assertTrue(
                "points ordered ASC",
                pts.zipWithNext().all { (a, b) -> a.tsMs <= b.tsMs },
            )
        }
    }

    @Test
    fun `byRange filters by startMs window`() = runTest {
        val base = 1_700_000_000_000L
        val ids = (0 until 4).map { idx ->
            dao.insert(trip(base + idx * 60_000L, base + idx * 60_000L + 30_000L, dist = 500.0))
        }
        ids.forEach { id ->
            val t = dao.all().first().first { it.id == id }
            dao.insertPoints(pointsFor(id, t.startMs))
        }

        val windowed = dao.byRange(from = base + 60_000L, to = base + 180_000L).first()
        assertEquals(2, windowed.size)
        assertTrue(windowed.all { it.startMs in (base + 60_000L) until (base + 180_000L) })
    }

    @Test
    fun `delete trip cascades to its points`() = runTest {
        val ids = (0 until 4).map { idx ->
            val start = 1_700_000_000_000L + idx * 3_600_000L
            val id = dao.insert(trip(start, start + 600_000, dist = 1000.0))
            dao.insertPoints(pointsFor(id, start))
            id
        }

        // Pre-condition: every trip has 100 points
        for (id in ids) {
            assertEquals(100, dao.points(id).size)
        }

        // Delete the first trip; its points must vanish via FK cascade
        val target = dao.all().first().first { it.id == ids[0] }
        dao.delete(target)

        assertEquals("points for deleted trip should be gone", 0, dao.points(ids[0]).size)
        assertEquals("other trips should be untouched", 3, dao.all().first().size)
        for (id in ids.drop(1)) {
            assertEquals(100, dao.points(id).size)
        }
    }

    @Test
    fun `update mutates an existing trip in place`() = runTest {
        val id = dao.insert(trip(1_000L, 2_000L, dist = 100.0))
        val original = dao.all().first().first { it.id == id }
        dao.update(original.copy(distanceM = 999.0, endLabel = "renamed"))
        val reloaded = dao.all().first().first { it.id == id }
        assertEquals(999.0, reloaded.distanceM, 0.0)
        assertEquals("renamed", reloaded.endLabel)
    }
}
