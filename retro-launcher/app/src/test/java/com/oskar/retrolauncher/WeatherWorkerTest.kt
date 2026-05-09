package com.oskar.retrolauncher

import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.ListenableWorker
import com.oskar.retrolauncher.data.location.LocationRepository
import com.oskar.retrolauncher.data.location.LocationSample
import com.oskar.retrolauncher.data.weather.WeatherRepository
import com.oskar.retrolauncher.service.WeatherWorker
import com.oskar.retrolauncher.service.runWeatherRefresh
import com.oskar.retrolauncher.service.scheduleWeather
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * T1.21 — covers AC1 (scheduling), AC3 (retry/success classification),
 * AC4 (snapshot persistence — verified indirectly via WeatherRepository state),
 * AC5 (no crash + Result.failure when API key blank).
 *
 * AC2 (BOOT_COMPLETED rescheduling) is verified in [BootReceiverTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class WeatherWorkerTest {

    private lateinit var owm: MockWebServer
    private lateinit var nominatim: MockWebServer
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Before
    fun setUp() {
        owm = MockWebServer().apply { start() }
        nominatim = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        owm.shutdown()
        nominatim.shutdown()
    }

    // --- runWeatherRefresh helper (the unit-testable core of the worker) ---

    @Test
    fun `runWeatherRefresh returns failure and skips network when api key is blank`() = runTest {
        owm.enqueue(MockResponse().setResponseCode(200).setBody(OWM_BODY))
        val repo = newRepo(apiKey = "")
        val loc = newLocationRepo().apply { push(SAMPLE) }

        val result = runWeatherRefresh(repo, loc)

        assertEquals(ListenableWorker.Result.failure(), result)
        assertEquals("blank API key must short-circuit before any HTTP call", 0, owm.requestCount)
    }

    @Test
    fun `runWeatherRefresh returns retry when no last known location`() = runTest {
        val repo = newRepo()
        val loc = newLocationRepo() // never pushed

        val result = runWeatherRefresh(repo, loc)

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `runWeatherRefresh returns retry on transient network failure`() = runTest {
        owm.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val fastClient = OkHttpClient.Builder()
            .connectTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .build()
        val repo = newRepo(client = fastClient)
        val loc = newLocationRepo().apply { push(SAMPLE) }

        val result = runWeatherRefresh(repo, loc)

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `runWeatherRefresh returns failure on permanent error like 401`() = runTest {
        owm.enqueue(MockResponse().setResponseCode(401).setBody("""{"cod":401}"""))
        val repo = newRepo()
        val loc = newLocationRepo().apply { push(SAMPLE) }

        val result = runWeatherRefresh(repo, loc)

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `runWeatherRefresh returns success and updates repository state on happy path`() = runTest {
        owm.enqueue(MockResponse().setResponseCode(200).setBody(OWM_BODY))
        nominatim.enqueue(MockResponse().setResponseCode(200).setBody(NOMINATIM_BODY))
        val repo = newRepo()
        val loc = newLocationRepo().apply { push(SAMPLE) }

        val result = runWeatherRefresh(repo, loc)

        assertEquals(ListenableWorker.Result.success(), result)
        assertNotNull("snapshot must be persisted to repository state", repo.state.value)
        assertEquals("Ljubljana", repo.state.value!!.city)
    }

    // --- scheduleWeather: WorkManager constraints + tag (AC1) ---

    @Test
    fun `scheduleWeather enqueues a 15-min periodic worker tagged for inspection`() {
        val app = RuntimeEnvironment.getApplication()
        WorkManagerTestInitHelper.initializeTestWorkManager(app)
        val wm = WorkManager.getInstance(app)

        wm.scheduleWeather()

        val infos = wm.getWorkInfosByTag(WeatherWorker.WORK_TAG).get()
        assertEquals("scheduleWeather must enqueue exactly one work", 1, infos.size)
        val info = infos[0]
        assertTrue(
            "worker must require a network connection",
            info.constraints.requiredNetworkType == NetworkType.CONNECTED,
        )
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
    }

    @Test
    fun `scheduleWeather is idempotent under KEEP policy`() {
        val app = RuntimeEnvironment.getApplication()
        WorkManagerTestInitHelper.initializeTestWorkManager(app)
        val wm = WorkManager.getInstance(app)

        wm.scheduleWeather()
        wm.scheduleWeather()

        val infos = wm.getWorkInfosByTag(WeatherWorker.WORK_TAG).get()
        assertEquals("KEEP policy must not duplicate the periodic work", 1, infos.size)
    }

    private fun newRepo(
        client: OkHttpClient = OkHttpClient(),
        apiKey: String = "test-key",
    ): WeatherRepository = WeatherRepository(
        ctx = RuntimeEnvironment.getApplication(),
        http = client,
        moshi = moshi,
        owmBaseUrl = owm.url("/").toString().trimEnd('/'),
        nominatimBaseUrl = nominatim.url("/").toString().trimEnd('/'),
        apiKey = apiKey,
    )

    private fun newLocationRepo() = LocationRepository()

    private companion object {
        val SAMPLE = LocationSample(
            lat = 46.05,
            lon = 14.51,
            speedMs = 0f,
            bearing = 0f,
            tsMs = 1_715_250_000_000,
            accuracy = 5f,
        )

        const val OWM_BODY = """
            {
              "weather": [{"id": 800, "main": "Clear", "description": "clear sky", "icon": "01d"}],
              "main": {"temp": 18.5, "feels_like": 17.0, "temp_min": 16.0, "temp_max": 21.0},
              "wind": {"speed": 3.4, "deg": 220, "gust": 6.1},
              "name": "Ljubljana",
              "dt": 1715250000
            }
        """

        const val NOMINATIM_BODY = """
            {
              "address": {
                "city": "Ljubljana",
                "country": "Slovenia"
              }
            }
        """
    }
}
