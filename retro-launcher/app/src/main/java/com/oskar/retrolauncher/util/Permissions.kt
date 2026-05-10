package com.oskar.retrolauncher.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.oskar.retrolauncher.service.MediaNotificationListener

object Permissions {

    fun hasFineLocation(ctx: Context): Boolean = ContextCompat.checkSelfPermission(
        ctx, Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    fun hasNotificationListener(ctx: Context): Boolean =
        MediaNotificationListener.isEnabled(ctx)

    fun isIgnoringBatteryOptimizations(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }

    /**
     * True when this app is the system's default home/launcher. Resolves
     * the canonical home Intent and compares against our own packageName.
     */
    fun isDefaultLauncher(ctx: Context): Boolean {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = ctx.packageManager.resolveActivity(
            homeIntent,
            PackageManager.MATCH_DEFAULT_ONLY,
        ) ?: return false
        return resolved.activityInfo?.packageName == ctx.packageName
    }

    fun isFirstRunComplete(ctx: Context): Boolean =
        hasFineLocation(ctx) && hasNotificationListener(ctx)
}
