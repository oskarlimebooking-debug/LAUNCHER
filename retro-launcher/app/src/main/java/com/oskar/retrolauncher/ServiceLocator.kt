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
import com.oskar.retrolauncher.data.music.LocalMusicRepository
import com.oskar.retrolauncher.data.music.MIGRATION_1_2
import com.oskar.retrolauncher.data.music.MusicLibraryRepository
import com.oskar.retrolauncher.data.music.PlayerController
import com.oskar.retrolauncher.data.prefs.SettingsStore
import com.oskar.retrolauncher.data.trip.AppDb
import com.oskar.retrolauncher.data.trip.SharedPrefsTripStateStore
import com.oskar.retrolauncher.data.trip.TripRecorder
import com.oskar.retrolauncher.data.trip.TripRepository
import com.oskar.retrolauncher.data.weather.WeatherRepository
import com.oskar.retrolauncher.service.BootReceiverGate
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
import timber.log.Timber
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
open class ServiceLocator(protected val app: Application) {

    open val http: OkHttpClient by lazy {
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

    open val moshi: Moshi by lazy {
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    }

    open val db: AppDb by lazy {
        Room.databaseBuilder(app, AppDb::class.java, "retro")
            // 1 → 2 adds the music tables (likes/playlists) without touching
            // trips; a real migration keeps recorded trips across the upgrade.
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    open val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(app)
    }

    open val settings: SettingsStore by lazy { SettingsStore(prefs) }

    open val media: MediaRepository by lazy { MediaRepository() }

    /** Built-in local music player (Media3 controller bridge to MusicService). */
    open val player: PlayerController by lazy { PlayerController(app) }

    /** On-device music library scanned from MediaStore. */
    open val localMusic: LocalMusicRepository by lazy { LocalMusicRepository(app) }

    /** Likes + playlists (Room-backed). */
    open val musicLibrary: MusicLibraryRepository by lazy { MusicLibraryRepository(db.music()) }

    open val weather: WeatherRepository by lazy {
        // T1.33 — prefer the user-entered key from the first-run wizard,
        // fall back to BuildConfig (typically empty in CI / unsigned builds).
        val key = settings.owmApiKey ?: BuildConfig.OWM_API_KEY
        WeatherRepository(app, http, moshi, apiKey = key)
    }

    open val location: LocationRepository by lazy { LocationRepository() }

    open val appList: AppListRepository by lazy { AppListRepository(app, app.packageManager, settings) }

    open val trips: TripRepository by lazy {
        TripRepository(dao = db.trips(), activeTripFlow = tripRecorder.activeTrip)
    }

    open val appScope: CoroutineScope by lazy {
        CoroutineScope(Dispatchers.Default + SupervisorJob())
    }

    open val tripRecorder: TripRecorder by lazy {
        TripRecorder(
            scope = appScope,
            dao = db.trips(),
            locationFlow = location.samples,
            geocoder = { lat, lon -> weather.reverseGeocodeBlocking(lat, lon) },
            store = SharedPrefsTripStateStore(prefs),
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
    open fun startup() {
        Timber.d("ServiceLocator.startup begin")
        runCatching {
            appScope.launch {
                settings.changes(SettingsStore.KEY_RECORD_TRIPS).collect {
                    tripRecorder.setEnabled(settings.recordTrips)
                }
            }
        }.onFailure { Timber.w(it, "startup: trip-recorder collect failed") }

        runCatching { media.startPositionTicker(appScope) }
            .onFailure { Timber.w(it, "startup: media ticker failed") }

        runCatching {
            ContextCompat.startForegroundService(
                app,
                Intent(app, LocationService::class.java),
            )
        }.onFailure { Timber.w(it, "startup: startForegroundService failed") }

        // WorkManager auto-initialization can be absent in tests or pared-down ROMs;
        // if it is, just skip scheduling — the launcher must still come up.
        runCatching { WorkManager.getInstance(app).scheduleWeather() }
            .onFailure { Timber.w(it, "startup: scheduleWeather failed") }

        // T1.34 AC4 — enforce the boot-receiver gate every launch so installs
        // that predate the new manifest default get realigned with firstRunDone.
        runCatching { BootReceiverGate.syncWithFirstRun(app, settings.firstRunDone) }
            .onFailure { Timber.w(it, "startup: BootReceiverGate sync failed") }

        runCatching { appList.start() }
            .onFailure { Timber.w(it, "startup: appList.start failed") }
        appScope.launch {
            runCatching { appList.refresh() }
                .onFailure { Timber.w(it, "startup: appList.refresh failed") }
        }
        Timber.d("ServiceLocator.startup end")
    }
}
