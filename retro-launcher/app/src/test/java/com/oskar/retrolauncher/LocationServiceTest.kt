package com.oskar.retrolauncher

import android.app.Service
import com.oskar.retrolauncher.service.LocationService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPowerManager

/**
 * T1.11 ACs covered:
 *   - AC1: service starts up (onCreate completes without throwing).
 *   - AC3: onStartCommand returns START_STICKY.
 *   - AC4: WakeLock is acquired during onCreate and released during onDestroy
 *          (no battery leak — checked via ShadowPowerManager.getLatestWakeLock).
 *
 * AC2 (1 Hz GPS rate) and AC5 (no FusedLocationProviderClient) are static
 * guarantees: AC2 is the literal `lm.requestLocationUpdates(GPS_PROVIDER,
 * 1000L, 0f, …)` call; AC5 holds because Play Services is not on the
 * dependency graph (`libs.versions.toml`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class LocationServiceTest {

    @Test
    fun `onStartCommand returns START_STICKY (AC3)`() {
        val controller = Robolectric.buildService(LocationService::class.java).create()
        val service = controller.get()
        val result = service.onStartCommand(null, 0, 0)
        assertEquals(Service.START_STICKY, result)
        controller.destroy()
    }

    @Test
    fun `WakeLock is held after onCreate (AC4)`() {
        val controller = Robolectric.buildService(LocationService::class.java).create()
        val wl = ShadowPowerManager.getLatestWakeLock()
        assertNotNull("LocationService must acquire a WakeLock in onCreate", wl)
        assertTrue("WakeLock must be held after onCreate", wl.isHeld)
        controller.destroy()
    }

    @Test
    fun `WakeLock is released after onDestroy (AC4)`() {
        val controller = Robolectric.buildService(LocationService::class.java).create()
        val wl = ShadowPowerManager.getLatestWakeLock()
        assertTrue(wl.isHeld)
        controller.destroy()
        assertFalse(
            "WakeLock must be released by onDestroy — otherwise battery leaks",
            wl.isHeld,
        )
    }

    @Test
    fun `service can be created and destroyed without throwing (AC1)`() {
        val controller = Robolectric.buildService(LocationService::class.java).create()
        // Sanity: foreground service started, notification posted.
        // Runtime visibility is verified on device — here we only check no crash.
        controller.destroy()
    }
}
