package com.oskar.retrolauncher.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Programmatic enable/disable for [BootReceiver]. The manifest declares the
 * receiver as `android:enabled="false"` so a freshly installed APK does not
 * react to BOOT_COMPLETED until the user has finished the first-run wizard
 * (T1.34 AC4 — avoid running LocationService / WeatherWorker against an
 * unconfigured app).
 *
 * Uses [PackageManager.setComponentEnabledSetting] — no reflection or hidden
 * APIs (T1.34 AC5). [PackageManager.DONT_KILL_APP] keeps the current process
 * alive while toggling.
 */
object BootReceiverGate {

    fun enable(context: Context) {
        setState(context, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
    }

    fun disable(context: Context) {
        setState(context, PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
    }

    fun syncWithFirstRun(context: Context, firstRunDone: Boolean) {
        if (firstRunDone) enable(context) else disable(context)
    }

    private fun setState(context: Context, state: Int) {
        val component = ComponentName(context.applicationContext, BootReceiver::class.java)
        context.applicationContext.packageManager.setComponentEnabledSetting(
            component,
            state,
            PackageManager.DONT_KILL_APP,
        )
    }
}
