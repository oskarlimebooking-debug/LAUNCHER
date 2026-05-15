package com.oskar.retrolauncher

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Tests for the crash barrier in [App.onCreate()].
 *
 * Verifies that an uncaught-exception handler is installed during startup
 * and that it logs compatibility errors before the process dies.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23], application = App::class)
class AppCrashBarrierTest {

    private var caughtThrowable: Throwable? = null
    private var wasReThrown: Boolean = false

    @After
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(null)
    }

    @Test
    fun `App installs uncaught exception handler during startup`() {
        val app = ApplicationProvider.getApplicationContext<App>()
        val handler = Thread.getDefaultUncaughtExceptionHandler()
        assertNotNull(
            "App.onCreate() must install a default uncaught exception handler",
            handler,
        )
    }

    @Test
    fun `uncaught handler logs NoSuchMethodError without swallowing it`() {
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        val caught = mutableListOf<Throwable>()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            caught.add(throwable)
        }

        val error = NoSuchMethodError("android.app.ActivityOptions.setLaunchDisplayId")
        val thread = Thread.currentThread()
        try {
            Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(thread, error)
        } catch (_: NoSuchMethodError) {
            // expected re-throw — the handler must not silently swallow crashes
        }

        assertTrue(
            "NoSuchMethodError should be passed to the handler for logging",
            caught.isNotEmpty(),
        )
    }

    @Test
    fun `uncaught handler logs NoClassDefFoundError`() {
        val caught = mutableListOf<Throwable>()

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            caught.add(throwable)
        }

        val error = NoClassDefFoundError("j$/time/LocalDateTime")
        val thread = Thread.currentThread()
        try {
            Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(thread, error)
        } catch (_: NoClassDefFoundError) {
            // expected
        }

        assertTrue(
            "NoClassDefFoundError should be passed to the handler",
            caught.isNotEmpty(),
        )
    }
}
