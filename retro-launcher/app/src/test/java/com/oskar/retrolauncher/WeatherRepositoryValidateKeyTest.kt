package com.oskar.retrolauncher

import com.oskar.retrolauncher.data.weather.WeatherRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * T1.33 — Wizard step 5 must validate the user-entered OWM key by hitting the
 * OpenWeatherMap API. WeatherRepository.validateApiKey() returns true on
 * HTTP 200, false on 401 (invalid key) or other failures (network, etc).
 *
 * Uses MockWebServer following the pattern in WeatherRepositoryTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class WeatherRepositoryValidateKeyTest {

    private lateinit var owm: MockWebServer
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Before
    fun setUp() {
        owm = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        owm.shutdown()
    }

    private fun newRepo(): WeatherRepository = WeatherRepository(
        ctx = RuntimeEnvironment.getApplication(),
        http = OkHttpClient(),
        moshi = moshi,
        owmBaseUrl = owm.url("/").toString().trimEnd('/'),
        nominatimBaseUrl = "http://unused.invalid",
        apiKey = "ignored-default",
    )

    @Test
    fun `validateApiKey returns true on 200 response`() = runTest {
        owm.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val ok = newRepo().validateApiKey("VALID_KEY")

        assertTrue("expected validateApiKey to be true on 200", ok)
    }

    @Test
    fun `validateApiKey returns false on 401 unauthorized`() = runTest {
        owm.enqueue(MockResponse().setResponseCode(401).setBody("""{"cod":401,"message":"Invalid API key"}"""))

        val ok = newRepo().validateApiKey("BOGUS_KEY")

        assertFalse("expected validateApiKey to be false on 401", ok)
    }

    @Test
    fun `validateApiKey returns false on blank key without hitting network`() = runTest {
        // Don't enqueue anything — if the request hit the server, the test would hang.
        val ok = newRepo().validateApiKey("")
        assertFalse(ok)
    }

    @Test
    fun `validateApiKey sends the supplied key as appid query param`() = runTest {
        owm.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        newRepo().validateApiKey("SENT_KEY")

        val req = owm.takeRequest(2, TimeUnit.SECONDS)!!
        assertTrue(
            "OWM validate request must include appid=SENT_KEY (path=${req.path})",
            req.path!!.contains("appid=SENT_KEY"),
        )
    }
}
