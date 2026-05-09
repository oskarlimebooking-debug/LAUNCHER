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
import java.util.concurrent.TimeUnit

class WeatherWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val loc = App.location.lastKnown() ?: return Result.retry()
        App.weather.refresh(loc.latitude, loc.longitude)
        return Result.success()
    }
}

fun WorkManager.scheduleWeather() {
    val req = PeriodicWorkRequestBuilder<WeatherWorker>(15, TimeUnit.MINUTES)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()
    enqueueUniquePeriodicWork(
        "weather",
        ExistingPeriodicWorkPolicy.KEEP,
        req,
    )
}
