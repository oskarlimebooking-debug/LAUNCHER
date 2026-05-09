package com.oskar.retrolauncher

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

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
}
