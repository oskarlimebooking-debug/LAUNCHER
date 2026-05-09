package com.oskar.retrolauncher

import com.oskar.retrolauncher.data.location.LocationSample
import com.oskar.retrolauncher.data.trip.TripDao
import com.oskar.retrolauncher.data.trip.TripEntity
import com.oskar.retrolauncher.data.trip.TripPoint
import com.oskar.retrolauncher.data.trip.TripRecorder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong

@OptIn(ExperimentalCoroutinesApi::class)
class TripRecorderTest {

    private class FakeDao : TripDao {
        val trips = mutableListOf<TripEntity>()
        val points = mutableListOf<TripPoint>()
        private val nextId = AtomicLong(1)

        override fun all() = flow<List<TripEntity>> { emit(trips.toList()) }
        override fun byRange(from: Long, to: Long) = flow<List<TripEntity>> {
            emit(trips.filter { it.startMs >= from && it.startMs < to })
        }
        override suspend fun insert(t: TripEntity): Long {
            val id = nextId.getAndIncrement()
            trips += t.copy(id = id)
            return id
        }
        override suspend fun update(t: TripEntity) {
            val idx = trips.indexOfFirst { it.id == t.id }
            if (idx >= 0) trips[idx] = t
        }
        override suspend fun delete(t: TripEntity) { trips.removeAll { it.id == t.id } }
        override suspend fun insertPoints(points: List<TripPoint>) { this.points.addAll(points) }
        override suspend fun points(id: Long): List<TripPoint> = points.filter { it.tripId == id }
    }

    private fun sample(t: Long, lat: Double = 0.0, lon: Double = 0.0, mps: Float = 0f) =
        LocationSample(lat = lat, lon = lon, speedMs = mps, bearing = 0f, tsMs = t, accuracy = 5f)

    private suspend fun TestScope.feed(
        flow: MutableSharedFlow<LocationSample>,
        samples: Sequence<LocationSample>,
    ) {
        runCurrent() // ensure the recorder's collect is subscribed before we emit
        for (s in samples) {
            flow.emit(s)
            runCurrent() // let the collector consume each emission before the next
        }
    }

    @Test
    fun `does not start trip when speed stays low`() = runTest {
        val flow = MutableSharedFlow<LocationSample>(replay = 0, extraBufferCapacity = 64)
        val dao = FakeDao()
        val rec = TripRecorder(backgroundScope, dao, flow)
        feed(flow, (0..30).asSequence().map { sample(it * 1000L, mps = 0.5f) })
        assertEquals(TripRecorder.State.IDLE, rec.state.value)
        assertTrue(dao.trips.isEmpty())
    }

    @Test
    fun `starts recording after sustained motion`() = runTest {
        val flow = MutableSharedFlow<LocationSample>(replay = 0, extraBufferCapacity = 64)
        val dao = FakeDao()
        val rec = TripRecorder(backgroundScope, dao, flow)
        feed(flow, (0..12).asSequence().map { sample(it * 1000L, lat = it * 0.0001, mps = 10f) })
        assertEquals(TripRecorder.State.RECORDING, rec.state.value)
        assertNotNull(rec.live.value)
    }

    @Test
    fun `ends trip after sustained stop and persists`() = runTest {
        val flow = MutableSharedFlow<LocationSample>(replay = 0, extraBufferCapacity = 64)
        val dao = FakeDao()
        val rec = TripRecorder(backgroundScope, dao, flow)

        feed(flow, (0..30).asSequence().map { sample(it * 1000L, lat = it * 0.0001, mps = 10f) })
        assertEquals(TripRecorder.State.RECORDING, rec.state.value)

        feed(flow, (0 until 140).asSequence().map { sample(31_000L + it * 1000L, lat = 30 * 0.0001, mps = 0f) })

        assertEquals(TripRecorder.State.IDLE, rec.state.value)
        assertEquals(1, dao.trips.size)
        val trip = dao.trips.first()
        assertTrue("distance should be > 50m, was ${trip.distanceM}", trip.distanceM > 50.0)
        assertTrue(trip.maxSpeedMs >= 9.5)
        assertTrue("at least 2 points persisted", dao.points.size >= 2)
    }

    @Test
    fun `disabled recorder ignores samples`() = runTest {
        val flow = MutableSharedFlow<LocationSample>(replay = 0, extraBufferCapacity = 64)
        val dao = FakeDao()
        val rec = TripRecorder(backgroundScope, dao, flow)
        rec.setEnabled(false)
        feed(flow, (0..30).asSequence().map { sample(it * 1000L, mps = 10f) })
        assertEquals(TripRecorder.State.IDLE, rec.state.value)
    }
}
