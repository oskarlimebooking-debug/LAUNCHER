package com.oskar.retrolauncher

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.room.Room
import androidx.work.WorkManager
import com.oskar.retrolauncher.data.apps.AppListRepository
import com.oskar.retrolauncher.data.location.LocationRepository
import com.oskar.retrolauncher.data.media.MediaRepository
import com.oskar.retrolauncher.data.prefs.SettingsStore
import com.oskar.retrolauncher.data.trip.AppDb
import com.oskar.retrolauncher.data.trip.TripRecorder
import com.oskar.retrolauncher.data.trip.TripRepository
import com.oskar.retrolauncher.data.weather.WeatherRepository
import com.oskar.retrolauncher.service.LocationService
import com.oskar.retrolauncher.service.scheduleWeather
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Manual service locator. Constructed once per Application from `App.kt`. Every repo and
 * shared singleton lives behind `by lazy { ... }` so we don't pay startup cost for things
 * we may not touch this session.
 *
 * Constructor takes Application context only — never an Activity — so retained references
 * cannot leak a destroyed Activity (T1.6 AC5).
 */
class ServiceLocator(private val app: Application) {

    val http: OkHttpClient by lazy {
        // T1.20 AC2 — 5 MB on-disk LRU cache rooted at cacheDir/weather/.
        // OkHttp's Cache implementation is itself an LRU evictor that respects
        // server cache-control headers, so a single Cache instance satisfies
        // both the size cap and the eviction policy.
        val cacheDir = File(app.cacheDir, "weather").apply { mkdirs() }
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .cache(Cache(cacheDir, CACHE_MAX_BYTES))
            .build()
    }

    val moshi: Moshi by lazy {
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    }

    val db: AppDb by lazy {
        Room.databaseBuilder(app, AppDb::class.java, "retro").build()
    }

    val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(app)
    }

    val settings: SettingsStore by lazy { SettingsStore(prefs) }

    val media: MediaRepository by lazy { MediaRepository() }

    val weather: WeatherRepository by lazy { WeatherRepository(app, http, moshi) }

    val location: LocationRepository by lazy { LocationRepository() }

    val appList: AppListRepository by lazy { AppListRepository(app, app.packageManager) }

    val trips: TripRepository by lazy { TripRepository(db.trips()) }

    val appScope: CoroutineScope by lazy {
        CoroutineScope(Dispatchers.Default + SupervisorJob())
    }

    val tripRecorder: TripRecorder by lazy {
        TripRecorder(
            scope = appScope,
            dao = db.trips(),
            locationFlow = location.samples,
            geocoder = { lat, lon -> weather.reverseGeocodeBlocking(lat, lon) },
        )
    }

    private companion object {
        const val CACHE_MAX_BYTES = 5L * 1024 * 1024
    }

    /**
     * Side-effecting startup work: launching the GPS foreground service, scheduling the
     * periodic weather refresh, eagerly populating the app list. Idempotent — safe to call
     * exactly once from `App.onCreate`.
     */
    fun startup() {
        appScope.launch {
            settings.changes(SettingsStore.KEY_RECORD_TRIPS).collect {
                tripRecorder.setEnabled(settings.recordTrips)
            }
        }

        media.startPositionTicker(appScope)

        runCatching {
            ContextCompat.startForegroundService(
                app,
                Intent(app, LocationService::class.java),
            )
        }

        // WorkManager auto-initialization can be absent in tests or pared-down ROMs;
        // if it is, just skip scheduling — the launcher must still come up.
        runCatching { WorkManager.getInstance(app).scheduleWeather() }

        appScope.launch { runCatching { appList.refresh() } }
    }
}
