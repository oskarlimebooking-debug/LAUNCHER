package com.oskar.retrolauncher.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import timber.log.Timber

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            Timber.i("Boot received: $action — starting LocationService")
            ContextCompat.startForegroundService(
                context,
                Intent(context, LocationService::class.java),
            )
        }
    }
}
