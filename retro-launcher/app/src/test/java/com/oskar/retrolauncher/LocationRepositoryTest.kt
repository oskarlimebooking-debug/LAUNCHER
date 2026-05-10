package com.oskar.retrolauncher

import app.cash.turbine.test
import com.oskar.retrolauncher.data.location.LocationRepository
import com.oskar.retrolauncher.data.location.LocationSample
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * T1.35 — LocationRepository unit tests.
 *
 * Robolectric is needed because `lastKnown()` allocates an `android.location.Location`,
 * which is a stubbed class on the JVM without an Android runtime.
 *
 * Covers the contract for a hot pub-sub of [LocationSample]:
 *   - happy path: pushed samples reach `samples` and `last`
 *   - failure / edge: zero pushes leaves `last` null and `lastKnown()` null
 *   - edge: replay = 1, so late subscribers see only the most-recent sample
 *   - edge: `lastKnown()` round-trips coords/time/bearing/speed/accuracy faithfully
 *   - flow: `samples` re-emits every push observed by a hot collector (Turbine)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class LocationRepositoryTest {

    private fun sample(
        lat: Double = 46.0,
        lon: Double = 14.5,
        speedMs: Float = 12.5f,
        bearing: Float = 90f,
        tsMs: Long = 1_700_000_000_000L,
        accuracy: Float = 4.0f,
    ) = LocationSample(lat, lon, speedMs, bearing, tsMs, accuracy)

    @Test
    fun `cold start last is null and lastKnown returns null`() {
        val repo = LocationRepository()
        assertNull(repo.last.value)
        assertNull(repo.lastKnown())
    }

    @Test
    fun `push updates last StateFlow`() = runTest {
        val repo = LocationRepository()
        val s = sample()
        repo.push(s)
        assertEquals(s, repo.last.value)
    }

    @Test
    fun `push emits onto samples shared flow`() = runTest {
        val repo = LocationRepository()
        val s = sample(lat = 1.0)
        repo.push(s)
        // replay=1 means a fresh collector sees the most recent value.
        val seen = repo.samples.first()
        assertEquals(s, seen)
    }

    @Test
    fun `lastKnown returns a Location mirroring the latest sample`() {
        val repo = LocationRepository()
        val s = sample(
            lat = 46.0569,
            lon = 14.5058,
            speedMs = 22.2f,
            bearing = 270f,
            tsMs = 1_700_000_001_000L,
            accuracy = 3.5f,
        )
        repo.push(s)

        val loc = repo.lastKnown()
        assertNotNull(loc)
        assertEquals(s.lat, loc!!.latitude, 0.0)
        assertEquals(s.lon, loc.longitude, 0.0)
        assertEquals(s.speedMs, loc.speed, 1e-6f)
        assertEquals(s.bearing, loc.bearing, 1e-6f)
        assertEquals(s.accuracy, loc.accuracy, 1e-6f)
        assertEquals(s.tsMs, loc.time)
    }

    @Test
    fun `push replaces last with the most recent sample`() = runTest {
        val repo = LocationRepository()
        val first = sample(lat = 1.0)
        val second = sample(lat = 2.0)
        repo.push(first)
        repo.push(second)
        assertEquals(second, repo.last.value)
        assertNotEquals(first, repo.last.value)
    }

    @Test
    fun `multiple pushes are observed by a hot collector via Turbine`() = runTest {
        val repo = LocationRepository()
        val s1 = sample(lat = 1.0)
        val s2 = sample(lat = 2.0)
        val s3 = sample(lat = 3.0)

        repo.samples.test {
            repo.push(s1)
            assertEquals(s1, awaitItem())
            repo.push(s2)
            assertEquals(s2, awaitItem())
            repo.push(s3)
            assertEquals(s3, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `late subscriber sees only the most recent sample (replay equals 1)`() = runTest {
        val repo = LocationRepository()
        repo.push(sample(lat = 10.0))
        repo.push(sample(lat = 20.0))
        repo.push(sample(lat = 30.0))

        val replayed = repo.samples.first()
        assertEquals(30.0, replayed.lat, 0.0)
    }

    @Test
    fun `burst of pushes is observed in-order by a hot Turbine collector`() = runTest {
        val repo = LocationRepository()
        repo.samples.test {
            // extraBufferCapacity is 64, so 32 emissions can land without blocking.
            for (i in 0 until 32) {
                repo.push(sample(lat = i.toDouble()))
                assertEquals(i.toDouble(), awaitItem().lat, 0.0)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
