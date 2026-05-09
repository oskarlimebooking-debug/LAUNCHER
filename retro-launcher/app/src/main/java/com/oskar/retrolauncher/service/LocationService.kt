package com.oskar.retrolauncher.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.location.LocationSample
import com.oskar.retrolauncher.data.location.SpeedFilter
import timber.log.Timber

class LocationService : Service(), LocationListener {

    private val lm: LocationManager by lazy {
        getSystemService(LOCATION_SERVICE) as LocationManager
    }
    private lateinit var filter: SpeedFilter
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        filter = SpeedFilter(threshold = App.settings.speedThresholdMs)
        // Partial wake lock keeps the CPU running so GPS callbacks survive Doze on
        // rooted ROMs (spec section 11). Released in onDestroy.
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
            .apply { setReferenceCounted(false); acquire() }
        ensureChannel()
        startForeground(NOTIF_ID, buildNotification())
        if (hasFineLocation()) {
            try {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this)
                lm.lastKnownLocation(LocationManager.GPS_PROVIDER)?.let(::onLocationChanged)
            } catch (t: Throwable) {
                Timber.w(t, "GPS provider not available")
            }
        } else {
            Timber.w("LocationService started without ACCESS_FINE_LOCATION")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onLocationChanged(loc: Location) {
        // Re-read threshold each tick — user may have changed it in settings.
        filter.threshold = App.settings.speedThresholdMs
        val raw = LocationSample(
            lat = loc.latitude,
            lon = loc.longitude,
            speedMs = if (loc.hasSpeed()) loc.speed else 0f,
            bearing = loc.bearing,
            tsMs = loc.time.takeIf { it > 0 } ?: System.currentTimeMillis(),
            accuracy = if (loc.hasAccuracy()) loc.accuracy else Float.NaN,
        )
        App.location.push(raw.copy(speedMs = filter.update(raw)))
    }

    override fun onProviderDisabled(provider: String) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onDestroy() {
        runCatching { lm.removeUpdates(this) }
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        wakeLock = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasFineLocation() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Location",
                        NotificationManager.IMPORTANCE_MIN,
                    ).apply { setShowBadge(false) }
                )
            }
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("GPS active")
            .setSmallIcon(R.drawable.ic_gps)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

    private fun LocationManager.lastKnownLocation(provider: String): Location? =
        runCatching { getLastKnownLocation(provider) }.getOrNull()

    companion object {
        private const val NOTIF_ID = 1001
        private const val CHANNEL_ID = "loc"
        private const val WAKELOCK_TAG = "RetroLauncher::LocationService"
    }
}
