package com.oskar.retrolauncher

import android.content.Intent
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.oskar.retrolauncher.service.BootReceiver
import com.oskar.retrolauncher.service.WeatherWorker
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * T1.21 AC2 — BOOT_COMPLETED reschedules the weather worker. WorkManager
 * persists periodic work across reboots in the happy path, but if the system
 * cleared its DB or the user just sideloaded a new APK, this receiver is the
 * safety net that restores the cadence.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class BootReceiverTest {

    @Test
    fun `BOOT_COMPLETED schedules the weather periodic worker`() {
        val app = RuntimeEnvironment.getApplication()
        WorkManagerTestInitHelper.initializeTestWorkManager(app)

        BootReceiver().onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))

        val infos = WorkManager.getInstance(app)
            .getWorkInfosByTag(WeatherWorker.WORK_TAG)
            .get()
        assertEquals(
            "weather worker must be (re)scheduled by the boot receiver",
            1,
            infos.size,
        )
    }

    @Test
    fun `LOCKED_BOOT_COMPLETED also schedules the weather worker`() {
        val app = RuntimeEnvironment.getApplication()
        WorkManagerTestInitHelper.initializeTestWorkManager(app)

        BootReceiver().onReceive(app, Intent(Intent.ACTION_LOCKED_BOOT_COMPLETED))

        val infos = WorkManager.getInstance(app)
            .getWorkInfosByTag(WeatherWorker.WORK_TAG)
            .get()
        assertEquals(1, infos.size)
    }
}
