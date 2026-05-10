package com.oskar.retrolauncher

import com.oskar.retrolauncher.data.location.LocationSample
import com.oskar.retrolauncher.data.trip.TripDao
import com.oskar.retrolauncher.data.trip.TripEntity
import com.oskar.retrolauncher.data.trip.TripPoint
import com.oskar.retrolauncher.data.trip.TripRecorder
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong

// JUnit 4 requires @Test methods to return void — wrapper to drop the
// PropertyContext that checkAll returns.

/**
 * T1.35 — property-based state-machine tests for [TripRecorder] (kotest-property).
 *
 * Where the existing [TripRecorderTest] verifies hand-picked scenarios, this
 * suite generates random sample streams and asserts the state-machine
 * invariants that must hold *for all inputs*:
 *
 *   - INV1: every transition is allowed by the spec graph
 *   - INV2: persisted trips always have distance >= 50 m (the trip-floor)
 *   - INV3: persisted trips have endMs > startMs (no zero-length trips)
 *   - INV4: persisted trips have avgSpeedMs >= 0 and maxSpeedMs >= avgSpeedMs
 *
 * Each scenario is small (≤ 200 samples) and we bound iterations so the whole
 * property suite still runs in under a second.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TripRecorderPropertyTest {

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

    private val allowed = mapOf(
        TripRecorder.State.IDLE to setOf(TripRecorder.State.DETECTING),
        TripRecorder.State.DETECTING to setOf(TripRecorder.State.RECORDING, TripRecorder.State.IDLE),
        TripRecorder.State.RECORDING to setOf(TripRecorder.State.STOPPING, TripRecorder.State.IDLE),
        TripRecorder.State.STOPPING to setOf(TripRecorder.State.IDLE),
    )

    @Test
    fun `state-machine transitions respect the spec graph under random input`(): Unit = runBlocking {
        // kotest-property: 20 iterations of random m/s streams. Each stream advances
        // 1 s per step and oscillates between motion / stop to push the state machine
        // through the full transition graph.
        @OptIn(io.kotest.common.ExperimentalKotest::class)
        val cfg = PropTestConfig(iterations = 20, seed = 0xC0FFEEL)
        checkAll(cfg, Arb.list(Arb.float(0f, 25f), 30..150)) { speeds ->
            val scope = TestScope(UnconfinedTestDispatcher())
            val flow = MutableSharedFlow<LocationSample>(replay = 0, extraBufferCapacity = 256)
            val rec = TripRecorder(scope.backgroundScope, FakeDao(), flow)
            val seen = mutableListOf(rec.state.value)
            var t = 0L
            var lat = 0.0
            for (mps in speeds) {
                t += 1_000
                lat += mps.toDouble() / 111_320.0
                flow.emit(LocationSample(lat, 0.0, mps, 0f, t, 5f))
                scope.runCurrent()
                val s = rec.state.value
                if (s != seen.last()) seen += s
            }
            for (i in 1 until seen.size) {
                val from = seen[i - 1]
                val to = seen[i]
                assertTrue(
                    "INV1 — invalid $from -> $to in sequence $seen",
                    to in allowed.getValue(from),
                )
            }
        }
    }

    @Test
    fun `persisted trips obey distance, duration, and speed invariants`(): Unit = runBlocking {
        @OptIn(io.kotest.common.ExperimentalKotest::class)
        val cfg = PropTestConfig(iterations = 10, seed = 0xBEEFL)
        checkAll(cfg, Arb.list(Arb.float(0f, 25f), 60..200)) { speeds ->
            val scope = TestScope(UnconfinedTestDispatcher())
            val flow = MutableSharedFlow<LocationSample>(replay = 0, extraBufferCapacity = 256)
            val dao = FakeDao()
            TripRecorder(scope.backgroundScope, dao, flow)
            var t = 0L
            var lat = 0.0
            for (mps in speeds) {
                t += 1_000
                lat += mps.toDouble() / 111_320.0
                flow.emit(LocationSample(lat, 0.0, mps, 0f, t, 5f))
                scope.runCurrent()
            }
            // Drive a sustained stop to flush any in-progress trip.
            repeat(70) {
                t += 1_000
                flow.emit(LocationSample(lat, 0.0, 0f, 0f, t, 5f))
                scope.runCurrent()
            }
            for (trip in dao.trips) {
                assertTrue("INV2 distance >= 50m, was ${trip.distanceM}", trip.distanceM >= 50.0)
                assertTrue("INV3 endMs > startMs, was ${trip.startMs}..${trip.endMs}", trip.endMs > trip.startMs)
                assertTrue("INV4 avgSpeedMs >= 0, was ${trip.avgSpeedMs}", trip.avgSpeedMs >= 0.0)
                assertTrue(
                    "INV4 maxSpeedMs (${trip.maxSpeedMs}) >= avgSpeedMs (${trip.avgSpeedMs})",
                    trip.maxSpeedMs + 1e-6 >= trip.avgSpeedMs,
                )
            }
        }
    }
}
