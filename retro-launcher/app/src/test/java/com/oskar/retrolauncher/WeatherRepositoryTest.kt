package com.oskar.retrolauncher

import com.oskar.retrolauncher.data.weather.WeatherRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * T1.20 — covers AC1, AC3, AC4, AC5.
 *
 * AC2 (5 MB on-disk cache at `cacheDir/weather/`) is verified in
 * [AppServiceLocatorTest], because the cache is owned by ServiceLocator's
 * shared OkHttpClient, not by the repository itself.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class WeatherRepositoryTest {

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

    @Test
    fun `fetch success returns Result success with parsed snapshot`() = runTest {
        owm.enqueue(MockResponse().setResponseCode(200).setBody(OWM_BODY))
        nominatim.enqueue(MockResponse().setResponseCode(200).setBody(NOMINATIM_BODY))

        val repo = newRepo()

        val result = repo.fetch(46.05, 14.51)

        assertTrue("expected success: ${result.exceptionOrNull()}", result.isSuccess)
        val snap = result.getOrNull()!!
        assertEquals(18.5, snap.tempC, 0.0001)
        assertEquals(800, snap.iconId)
        assertEquals("Ljubljana", snap.city)
    }

    @Test
    fun `fetch sends OWM key from BuildConfig and Nominatim user-agent header`() = runTest {
        owm.enqueue(MockResponse().setResponseCode(200).setBody(OWM_BODY))
        nominatim.enqueue(MockResponse().setResponseCode(200).setBody(NOMINATIM_BODY))

        val repo = newRepo(apiKey = "TEST_KEY_FROM_BUILDCONFIG")
        repo.fetch(46.05, 14.51)

        val owmReq = owm.takeRequest(2, TimeUnit.SECONDS)!!
        assertTrue(
            "OWM request must hit /data/2.5/weather, was ${owmReq.path}",
            owmReq.path!!.startsWith("/data/2.5/weather"),
        )
        assertTrue(
            "OWM request must include appid=<key>, was ${owmReq.path}",
            owmReq.path!!.contains("appid=TEST_KEY_FROM_BUILDCONFIG"),
        )

        val nomReq = nominatim.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals(
            "Nominatim TOS requires retro-launcher/0.1 User-Agent",
            "retro-launcher/0.1",
            nomReq.getHeader("User-Agent"),
        )
    }

    @Test
    fun `fetch on 401 returns Result failure without crashing`() = runTest {
        owm.enqueue(MockResponse().setResponseCode(401).setBody("""{"cod":401,"message":"Invalid API key"}"""))

        val repo = newRepo()
        val result = repo.fetch(46.05, 14.51)

        assertTrue("401 must surface as Result.failure", result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `fetch on read timeout returns Result failure without crashing`() = runTest {
        // Hold the socket open without responding so the client's read timeout fires.
        owm.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        // Use a fast-timeout client so the test runs in milliseconds, not seconds.
        val fastClient = OkHttpClient.Builder()
            .connectTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .build()

        val repo = newRepo(client = fastClient)
        val result = repo.fetch(46.05, 14.51)

        assertTrue("timeout must surface as Result.failure", result.isFailure)
    }

    @Test
    fun `fetch on malformed JSON returns Result failure without crashing`() = runTest {
        owm.enqueue(MockResponse().setResponseCode(200).setBody("not-json{{"))

        val repo = newRepo()
        val result = repo.fetch(46.05, 14.51)

        assertTrue("malformed JSON must surface as Result.failure", result.isFailure)
    }

    @Test
    fun `fetch with blank api key fails fast without hitting network`() = runTest {
        val repo = newRepo(apiKey = "")
        val result = repo.fetch(46.05, 14.51)

        assertTrue("blank API key must fail without throwing", result.isFailure)
        assertEquals("no OWM request should be issued when key is blank", 0, owm.requestCount)
    }

    @Test
    fun `refresh on success updates state flow`() = runTest {
        owm.enqueue(MockResponse().setResponseCode(200).setBody(OWM_BODY))
        nominatim.enqueue(MockResponse().setResponseCode(200).setBody(NOMINATIM_BODY))

        val repo = newRepo()
        assertNull(repo.state.value)

        repo.refresh(46.05, 14.51)

        assertNotNull("state must be populated after a successful refresh", repo.state.value)
        assertEquals("Ljubljana", repo.state.value!!.city)
    }

    @Test
    fun `refresh on failure does not throw and leaves state unchanged`() = runTest {
        owm.enqueue(MockResponse().setResponseCode(500))

        val repo = newRepo()
        repo.refresh(46.05, 14.51)

        assertNull("failed refresh must not populate state", repo.state.value)
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

    private companion object {
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
