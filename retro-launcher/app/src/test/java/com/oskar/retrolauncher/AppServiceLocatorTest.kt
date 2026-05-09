package com.oskar.retrolauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * T1.6 ACs covered:
 *   - AC4: each repo is `by lazy { ... }` and constructed with the application context
 *   - AC5: no service-locator dependency on Activity context (constructor takes only Application)
 *
 * AC1/AC2/AC3 are verified statically (manifest, App.service property, BuildConfig.DEBUG guard).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class AppServiceLocatorTest {

    @Test
    fun `app exposes service property accessible from application context`() {
        val app = RuntimeEnvironment.getApplication() as App
        assertNotNull("App.service must be non-null", app.service)
    }

    @Test
    fun `service repo accessors return same instance on repeated access (lazy)`() {
        val app = RuntimeEnvironment.getApplication() as App
        val s = app.service
        assertSame("media must be a singleton via by-lazy", s.media, s.media)
        assertSame("settings must be a singleton via by-lazy", s.settings, s.settings)
        assertSame("location must be a singleton via by-lazy", s.location, s.location)
        assertSame("appList must be a singleton via by-lazy", s.appList, s.appList)
        assertSame("http must be a singleton via by-lazy", s.http, s.http)
        assertSame("moshi must be a singleton via by-lazy", s.moshi, s.moshi)
    }

    @Test
    fun `service locator constructs with application context only`() {
        // Compile-time guarantee: ServiceLocator(app) takes Application; passing an
        // Activity would not compile. Smoke-test the runtime contract.
        val app = RuntimeEnvironment.getApplication() as App
        val locator = ServiceLocator(app)
        assertNotNull(locator.media)
        assertNotNull(locator.settings)
    }

    /**
     * T1.20 AC2 — OkHttpClient is configured with the spec'd 10 s timeouts and a
     * 5 MB on-disk cache rooted at `cacheDir/weather/`. Verified here (not in
     * WeatherRepositoryTest) because the cache lives on the shared client owned
     * by ServiceLocator.
     */
    @Test
    fun `http client has 10s timeouts and 5MB on-disk cache at cacheDir weather`() {
        val app = RuntimeEnvironment.getApplication() as App
        val http = app.service.http

        assertEquals(10_000, http.connectTimeoutMillis)
        assertEquals(10_000, http.readTimeoutMillis)

        val cache = http.cache
        assertNotNull("OkHttpClient must have a disk cache configured", cache)
        assertEquals(5L * 1024 * 1024, cache!!.maxSize())

        val expectedDir = java.io.File(app.cacheDir, "weather").canonicalPath
        assertEquals(expectedDir, cache.directory.canonicalPath)
        assertTrue("cache directory must exist", cache.directory.exists())
        assertEquals(TimeUnit.SECONDS.toMillis(10).toInt(), http.connectTimeoutMillis)
    }
}
