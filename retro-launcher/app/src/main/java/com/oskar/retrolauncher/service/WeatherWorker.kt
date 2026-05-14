package com.oskar.retrolauncher.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.data.location.LocationRepository
import com.oskar.retrolauncher.data.weather.WeatherRepository
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Periodic refresh of the weather tile. Spec §10.5: every 15 minutes when a
 * network is available — well under OWM's 1000-call/day free tier (96/day).
 *
 * Classification:
 *   - blank API key  → [Result.failure] (permanent; nothing for WorkManager to retry)
 *   - no last fix    → [Result.retry]   (LocationService just hasn't reported yet)
 *   - IOException    → [Result.retry]   (transient: airplane mode, captive portal, DNS)
 *   - any other      → [Result.failure] (parse error, 4xx — retrying won't help)
 */
class WeatherWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        if (!App.weather.isConfigured) {
            Timber.w("WeatherWorker: OWM_API_KEY missing — returning failure")
            return Result.failure()
        }
        val settings = App.settings
        val loc = App.location.lastKnown()
        val lat = loc?.latitude ?: settings.effectiveWeatherLat.toDouble()
        val lon = loc?.longitude ?: settings.effectiveWeatherLon.toDouble()
        val outcome = App.weather.refresh(lat, lon)
        return when {
            outcome.isSuccess -> Result.success()
            outcome.exceptionOrNull() is IOException -> Result.retry()
            else -> Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "weather"
        const val WORK_TAG = "weather-refresh"
    }
}

/**
 * Pure helper extracted so it can be unit-tested without WorkManager. The
 * Worker class itself is a thin shell that just wires App-singletons in.
 */
internal suspend fun runWeatherRefresh(
    weather: WeatherRepository,
    location: LocationRepository,
): androidx.work.ListenableWorker.Result {
    if (!weather.isConfigured) {
        Timber.w("WeatherWorker: OWM_API_KEY missing — returning failure")
        return androidx.work.ListenableWorker.Result.failure()
    }
    val loc = location.lastKnown()
        ?: return androidx.work.ListenableWorker.Result.retry()
    val outcome = weather.refresh(loc.latitude, loc.longitude)
    return when {
        outcome.isSuccess -> androidx.work.ListenableWorker.Result.success()
        outcome.exceptionOrNull() is IOException ->
            androidx.work.ListenableWorker.Result.retry()
        else -> androidx.work.ListenableWorker.Result.failure()
    }
}

fun WorkManager.scheduleWeather() {
    val req = PeriodicWorkRequestBuilder<WeatherWorker>(15, TimeUnit.MINUTES)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .addTag(WeatherWorker.WORK_TAG)
        .build()
    enqueueUniquePeriodicWork(
        WeatherWorker.WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        req,
    )
}
