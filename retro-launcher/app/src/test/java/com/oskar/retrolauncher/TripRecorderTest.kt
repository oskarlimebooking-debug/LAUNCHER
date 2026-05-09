package com.oskar.retrolauncher

import com.oskar.retrolauncher.data.location.LocationSample
import com.oskar.retrolauncher.data.trip.InMemoryTripStateStore
import com.oskar.retrolauncher.data.trip.TripDao
import com.oskar.retrolauncher.data.trip.TripEntity
import com.oskar.retrolauncher.data.trip.TripPoint
import com.oskar.retrolauncher.data.trip.TripRecorder
import com.oskar.retrolauncher.data.trip.TripStateStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.random.Random

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
        override suspend fun deleteById(id: Long): Int {
            val before = trips.size
            trips.removeAll { it.id == id }
            points.removeAll { it.tripId == id }
            return before - trips.size
        }
        override suspend fun insertPoints(points: List<TripPoint>) { this.points.addAll(points) }
        override suspend fun points(id: Long): List<TripPoint> = points.filter { it.tripId == id }
        override fun pointsFlow(id: Long) = flow<List<TripPoint>> {
            emit(points.filter { it.tripId == id })
        }
    }

    private fun sample(t: Long, lat: Double = 0.0, lon: Double = 0.0, mps: Float = 0f) =
        LocationSample(lat = lat, lon = lon, speedMs = mps, bearing = 0f, tsMs = t, accuracy = 5f)

    private suspend fun TestScope.feed(
        flow: MutableSharedFlow<LocationSample>,
        samples: Sequence<LocationSample>,
    ) {
        runCurrent()
        for (s in samples) {
            flow.emit(s)
            runCurrent()
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
    fun `activeTrip is null when idle and populated when recording then null after stop`() = runTest {
        val flow = MutableSharedFlow<LocationSample>(replay = 0, extraBufferCapacity = 64)
        val dao = FakeDao()
        val rec = TripRecorder(backgroundScope, dao, flow)
        assertNull("idle → null", rec.activeTrip.value)

        // Sustained motion for 12 s → RECORDING.
        feed(flow, (0..12).asSequence().map { sample(it * 1000L, lat = it * 0.0001, mps = 10f) })
        assertEquals(TripRecorder.State.RECORDING, rec.state.value)
        val active = rec.activeTrip.value
        assertNotNull("recording → populated", active)
        assertEquals(0L, active!!.id)
        assertEquals(0L, active.startMs)

        // 70 s stop → trip ends, recorder returns to IDLE.
        feed(flow, (0 until 70).asSequence().map { sample(13_000L + it * 1000L, lat = 12 * 0.0001, mps = 0f) })
        assertEquals(TripRecorder.State.IDLE, rec.state.value)
        assertNull("post-trip → null", rec.activeTrip.value)
    }

    @Test
    fun `starts recording after sustained motion above start threshold`() = runTest {
        // 10 m/s = 36 km/h, above default 10 km/h start threshold.
        // 12 samples at 1 Hz = 11 s elapsed, exceeds 10 s detect window.
        val flow = MutableSharedFlow<LocationSample>(replay = 0, extraBufferCapacity = 64)
        val dao = FakeDao()
        val rec = TripRecorder(backgroundScope, dao, flow)
        feed(flow, (0..12).asSequence().map { sample(it * 1000L, lat = it * 0.0001, mps = 10f) })
        assertEquals(TripRecorder.State.RECORDING, rec.state.value)
        assertNotNull(rec.live.value)
    }

    @Test
    fun `transitions IDLE then DETECTING then RECORDING`() = runTest {
        val flow = MutableSharedFlow<LocationSample>(replay = 0, extraBufferCapacity = 64)
        val dao = FakeDao()
        val rec = TripRecorder(backgroundScope, dao, flow)
        // First high-speed sample: enter DETECTING.
        feed(flow, sequenceOf(sample(0L, lat = 0.0, mps = 10f)))
        assertEquals(TripRecorder.State.DETECTING, rec.state.value)
        // Brief sustained motion (<10 s): still DETECTING.
        feed(flow, (1..5).asSequence().map { sample(it * 1000L, lat = it * 0.0001, mps = 10f) })
        assertEquals(TripRecorder.State.DETECTING, rec.state.value)
        // Cross 10 s threshold: RECORDING.
        feed(flow, (6..11).asSequence().map { sample(it * 1000L, lat = it * 0.0001, mps = 10f) })
        assertEquals(TripRecorder.State.RECORDING, rec.state.value)
    }

    @Test
    fun `DETECTING returns to IDLE when motion drops before sustained window`() = runTest {
        val flow = MutableSharedFlow<LocationSample>(replay = 0, extraBufferCapacity = 64)
        val dao = FakeDao()
        val rec = TripRecorder(backgroundScope, dao, flow)
        feed(flow, (0..3).asSequence().map { sample(it * 1000L, lat = it * 0.0001, mps = 10f) })
        assertEquals(TripRecorder.State.DETECTING, rec.state.value)
        feed(flow, sequenceOf(sample(4_000L, mps = 0f)))
        assertEquals(TripRecorder.State.IDLE, rec.state.value)
        assertTrue(dao.trips.isEmpty())
    }

    @Test
    fun `ends trip after sustained stop and persists`() = runTest {
        val flow = MutableSharedFlow<LocationSample>(replay = 0, extraBufferCapacity = 64)
        val dao = FakeDao()
        val rec = TripRecorder(backgroundScope, dao, flow)

        feed(flow, (0..30).asSequence().map { sample(it * 1000L, lat = it * 0.0001, mps = 10f) })
        assertEquals(TripRecorder.State.RECORDING, rec.state.value)

        // 70 s of zero speed exceeds 60 s stop window.
        feed(flow, (0 until 70).asSequence().map { sample(31_000L + it * 1000L, lat = 30 * 0.0001, mps = 0f) })

        assertEquals(TripRecorder.State.IDLE, rec.state.value)
        assertEquals(1, dao.trips.size)
        val trip = dao.trips.first()
        assertTrue("distance should be > 50m, was ${trip.distanceM}", trip.distanceM > 50.0)
        assertTrue(trip.maxSpeedMs >= 9.5)
        assertTrue("at least 2 points persisted", dao.points.size >= 2)
    }

    @Test
    fun `30s traffic-light stop does not split trip`() = runTest {
        val flow = MutableSharedFlow<LocationSample>(replay = 0, extraBufferCapacity = 64)
        val dao = FakeDao()
        val rec = TripRecorder(backgroundScope, dao, flow)

        // 30 s of motion → enter RECORDING.
        feed(flow, (0..30).asSequence().map { sample(it * 1000L, lat = it * 0.0001, mps = 10f) })
        assertEquals(TripRecorder.State.RECORDING, rec.state.value)

        // 30 s stopped (well under 60 s threshold).
        feed(flow, (0 until 30).asSequence().map { sample(31_000L + it * 1000L, lat = 30 * 0.0001, mps = 0f) })
        assertEquals("still RECORDING during traffic light", TripRecorder.State.RECORDING, rec.state.value)

        // Resume motion for 30 s.
        feed(
            flow,
            (0 until 30).asSequence().map {
                sample(61_000L + it * 1000L, lat = (30 + it) * 0.0001, mps = 10f)
            },
        )
        assertEquals(TripRecorder.State.RECORDING, rec.state.value)

        // Now stop for full 65 s to finalize.
        feed(flow, (0 until 65).asSequence().map { sample(91_000L + it * 1000L, lat = 60 * 0.0001, mps = 0f) })
        assertEquals(TripRecorder.State.IDLE, rec.state.value)
        assertEquals("traffic-light stop must not split into two trips", 1, dao.trips.size)
    }

    @Test
    fun `bucket aggregation reduces DB writes by at least 80 percent`() = runTest {
        val flow = MutableSharedFlow<LocationSample>(replay = 0, extraBufferCapacity = 64)
        val dao = FakeDao()
        TripRecorder(backgroundScope, dao, flow)

        // 600 raw 1 Hz samples (10 min) of constant motion, then sustained stop to flush.
        val rawCount = 600
        feed(
            flow,
            (0 until rawCount).asSequence().map {
                sample(it * 1000L, lat = it * 0.0001, mps = 10f)
            },
        )
        feed(
            flow,
            (0 until 65).asSequence().map {
                sample((rawCount + it) * 1000L, lat = (rawCount - 1) * 0.0001, mps = 0f)
            },
        )

        assertEquals(1, dao.trips.size)
        val written = dao.points.size
        val reduction = 1.0 - (written.toDouble() / rawCount.toDouble())
        assertTrue(
            "bucket reduction was ${"%.2f".format(reduction * 100)}% — expected >= 80%",
            reduction >= 0.80,
        )
    }

    @Test
    fun `10-minute synthetic GPS trace produces one trip with correct distance`() = runTest {
        val flow = MutableSharedFlow<LocationSample>(replay = 0, extraBufferCapacity = 64)
        val dao = FakeDao()
        TripRecorder(backgroundScope, dao, flow)

        // Drive 10 minutes due north at 20 m/s. 1 deg lat ≈ 111_320 m.
        // Step per second: 20 / 111_320 deg.
        val mps = 20f
        val degPerSec = mps / 111_320.0
        val durSec = 600
        feed(
            flow,
            (0 until durSec).asSequence().map {
                sample(it * 1000L, lat = it * degPerSec, lon = 0.0, mps = mps)
            },
        )
        feed(
            flow,
            (0 until 65).asSequence().map {
                sample((durSec + it) * 1000L, lat = (durSec - 1) * degPerSec, mps = 0f)
            },
        )

        assertEquals(1, dao.trips.size)
        val trip = dao.trips.first()
        val expected = mps * durSec.toDouble() // 12 000 m
        val rel = abs(trip.distanceM - expected) / expected
        assertTrue(
            "distance was ${trip.distanceM}, expected ~$expected (rel error ${"%.3f".format(rel)})",
            rel <= 0.05,
        )
    }

    @Test
    fun `state machine never enters an invalid transition under random input`() = runTest {
        val flow = MutableSharedFlow<LocationSample>(replay = 0, extraBufferCapacity = 256)
        val dao = FakeDao()
        val rec = TripRecorder(backgroundScope, dao, flow)
        val seen = mutableListOf<TripRecorder.State>()
        seen += rec.state.value
        val rng = Random(42)
        var t = 0L
        var lat = 0.0
        repeat(2000) {
            t += 1000
            val mps = if (rng.nextBoolean()) rng.nextFloat() * 25f else rng.nextFloat() * 0.5f
            lat += mps / 111_320.0
            flow.emit(sample(t, lat = lat, mps = mps))
            runCurrent()
            val s = rec.state.value
            if (s != seen.last()) seen += s
        }
        // Verify each transition is valid per the spec graph:
        //   IDLE -> DETECTING
        //   DETECTING -> {RECORDING, IDLE}
        //   RECORDING -> {STOPPING, IDLE}   (STOPPING is internal; observers may see direct -> IDLE)
        //   STOPPING -> IDLE
        val allowed = mapOf(
            TripRecorder.State.IDLE to setOf(TripRecorder.State.DETECTING),
            TripRecorder.State.DETECTING to setOf(TripRecorder.State.RECORDING, TripRecorder.State.IDLE),
            TripRecorder.State.RECORDING to setOf(TripRecorder.State.STOPPING, TripRecorder.State.IDLE),
            TripRecorder.State.STOPPING to setOf(TripRecorder.State.IDLE),
        )
        for (i in 1 until seen.size) {
            val from = seen[i - 1]
            val to = seen[i]
            assertTrue(
                "invalid transition $from -> $to (full sequence: $seen)",
                to in allowed.getValue(from),
            )
        }
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

    @Test
    fun `partial trip is recovered from store on next recorder start`() = runTest {
        val store: TripStateStore = InMemoryTripStateStore()
        val flow1 = MutableSharedFlow<LocationSample>(replay = 0, extraBufferCapacity = 64)
        val dao = FakeDao()
        val rec1 = TripRecorder(backgroundScope, dao, flow1, store = store)

        // Drive long enough to enter RECORDING and accumulate several buckets, but DO NOT stop.
        feed(
            flow1,
            (0..120).asSequence().map { sample(it * 1000L, lat = it * 0.0001, mps = 10f) },
        )
        assertEquals(TripRecorder.State.RECORDING, rec1.state.value)
        assertTrue("recovery snapshot must be persisted", store.load() != null)
        assertTrue("no trip persisted yet — still in progress", dao.trips.isEmpty())

        // Simulate process kill: the previous recorder's scope is cancelled when this test
        // suite restarts a new recorder against the same store + dao.
        val flow2 = MutableSharedFlow<LocationSample>(replay = 0, extraBufferCapacity = 64)
        TripRecorder(backgroundScope, dao, flow2, store = store)
        runCurrent()

        // The partial trip must have been persisted and the recovery snapshot cleared.
        assertEquals(1, dao.trips.size)
        assertTrue("recovered trip must include points", dao.points.isNotEmpty())
        assertNull("recovery store must be cleared after recovery", store.load())
    }
}
