package com.oskar.retrolauncher.test

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.oskar.retrolauncher.ServiceLocator
import com.oskar.retrolauncher.data.apps.AppListRepository
import com.oskar.retrolauncher.data.location.LocationRepository
import com.oskar.retrolauncher.data.media.MediaRepository
import com.oskar.retrolauncher.data.trip.AppDb
import com.oskar.retrolauncher.data.trip.TripRepository
import com.oskar.retrolauncher.data.weather.WeatherRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient

/**
 * T1.36 — drop-in [ServiceLocator] for instrumented tests. Three rules:
 *
 *  1. **No real network.** [WeatherRepository] is built with an [OkHttpClient]
 *     that has no cache and an empty API key, so its `isConfigured` returns
 *     false and observers (e.g. [WeatherWorker]) never fire a real request.
 *  2. **No real sensors.** The location repo is the real one but never has
 *     `push()` called by a [LocationService] (we override [startup] to skip it).
 *     Tests inject samples directly via `App.location.push(...)`.
 *  3. **No persistent DB.** Room runs in-memory so each test process starts
 *     with a clean trips table.
 *
 * [startup] is a no-op so we don't try to bind a foreground service, schedule
 * WorkManager, register a settings collector, or trigger an `appList.refresh()`
 * that would walk PackageManager — every test seeds the data it needs by hand.
 */
class TestServiceLocator(app: Application) : ServiceLocator(app) {

    override val prefs: SharedPreferences by lazy {
        // A throwaway prefs file scoped to instrumented tests so we don't
        // poison the app's real default prefs (e.g. firstRunDone, owmApiKey).
        // Cleared at construction time so each test process starts fresh.
        app.getSharedPreferences("retro-test-prefs", Context.MODE_PRIVATE).also {
            it.edit().clear().apply()
        }
    }

    override val http: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    override val moshi: Moshi by lazy {
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    }

    override val db: AppDb by lazy {
        // In-memory DB: no on-disk file, no cross-test pollution.
        Room.inMemoryDatabaseBuilder(app, AppDb::class.java)
            .allowMainThreadQueries()
            .build()
    }

    override val media: MediaRepository by lazy {
        // Slow ticker — the production 250 ms cadence would re-emit while the
        // test was inside an Espresso idle wait and starve it.
        MediaRepository(tickIntervalMs = 60_000L)
    }

    override val weather: WeatherRepository by lazy {
        WeatherRepository(
            ctx = app,
            http = http,
            moshi = moshi,
            owmBaseUrl = "http://127.0.0.1:0",
            nominatimBaseUrl = "http://127.0.0.1:0",
            apiKey = "",
        )
    }

    override val location: LocationRepository by lazy { LocationRepository() }

    override val appList: AppListRepository by lazy {
        AppListRepository(app, app.packageManager, settings)
    }

    override val trips: TripRepository by lazy {
        TripRepository(dao = db.trips())
    }

    /**
     * No-op: skip the foreground LocationService, WorkManager weather schedule,
     * boot-receiver sync, and the implicit appList.refresh — none of which are
     * relevant to fragment-render tests and all of which are flaky on an
     * emulator with no GPS provider.
     */
    override fun startup() = Unit
}
