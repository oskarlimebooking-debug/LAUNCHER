package com.oskar.retrolauncher

import android.app.Application
import android.content.SharedPreferences
import com.oskar.retrolauncher.data.apps.AppListRepository
import com.oskar.retrolauncher.data.location.LocationRepository
import com.oskar.retrolauncher.data.media.MediaRepository
import com.oskar.retrolauncher.data.prefs.SettingsStore
import com.oskar.retrolauncher.data.trip.AppDb
import com.oskar.retrolauncher.data.trip.TripRecorder
import com.oskar.retrolauncher.data.trip.TripRepository
import com.oskar.retrolauncher.data.weather.WeatherRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import timber.log.Timber

class App : Application() {

    val service: ServiceLocator by lazy { ServiceLocator(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        service.startup()
    }

    companion object {
        lateinit var instance: App
            private set

        // Convenience accessors. Each delegates to the lazy ServiceLocator so the
        // 14 existing call sites that reach for `App.media`, `App.settings`, etc.
        // continue to work unchanged. The repos themselves are still `by lazy`,
        // satisfying T1.6 AC4.
        val http: OkHttpClient get() = instance.service.http
        val moshi: Moshi get() = instance.service.moshi
        val db: AppDb get() = instance.service.db
        val prefs: SharedPreferences get() = instance.service.prefs
        val settings: SettingsStore get() = instance.service.settings
        val media: MediaRepository get() = instance.service.media
        val weather: WeatherRepository get() = instance.service.weather
        val location: LocationRepository get() = instance.service.location
        val appList: AppListRepository get() = instance.service.appList
        val trips: TripRepository get() = instance.service.trips
        val tripRecorder: TripRecorder get() = instance.service.tripRecorder
        val appScope: CoroutineScope get() = instance.service.appScope
    }
}
