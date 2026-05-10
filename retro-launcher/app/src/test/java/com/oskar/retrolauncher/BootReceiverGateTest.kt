package com.oskar.retrolauncher

import android.content.ComponentName
import android.content.pm.PackageManager
import com.oskar.retrolauncher.service.BootReceiver
import com.oskar.retrolauncher.service.BootReceiverGate
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * T1.34 AC4 — the BootReceiver must be disabled in the manifest by default
 * and only flipped to ENABLED once the user has completed the first-run
 * wizard. Otherwise a freshly sideloaded APK could fire LocationService /
 * WeatherWorker on every reboot before the user has granted permissions or
 * entered their OWM key — wasting GPS, network, and battery.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class BootReceiverGateTest {

    private fun componentState(): Int {
        val ctx = RuntimeEnvironment.getApplication()
        val name = ComponentName(ctx, BootReceiver::class.java)
        return ctx.packageManager.getComponentEnabledSetting(name)
    }

    @Test
    fun `enable flips component to ENABLED`() {
        val ctx = RuntimeEnvironment.getApplication()
        BootReceiverGate.enable(ctx)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, componentState())
    }

    @Test
    fun `disable flips component to DISABLED`() {
        val ctx = RuntimeEnvironment.getApplication()
        BootReceiverGate.disable(ctx)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED, componentState())
    }

    @Test
    fun `syncWithFirstRun true enables`() {
        val ctx = RuntimeEnvironment.getApplication()
        BootReceiverGate.disable(ctx)
        BootReceiverGate.syncWithFirstRun(ctx, firstRunDone = true)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, componentState())
    }

    @Test
    fun `syncWithFirstRun false disables`() {
        val ctx = RuntimeEnvironment.getApplication()
        BootReceiverGate.enable(ctx)
        BootReceiverGate.syncWithFirstRun(ctx, firstRunDone = false)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED, componentState())
    }
}
