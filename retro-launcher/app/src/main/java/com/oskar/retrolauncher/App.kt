package com.oskar.retrolauncher

import android.app.Application
import android.content.SharedPreferences
import android.os.Build
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import com.oskar.retrolauncher.data.apps.AppListRepository
import com.oskar.retrolauncher.data.location.LocationRepository
import com.oskar.retrolauncher.data.media.MediaRepository
import com.oskar.retrolauncher.data.music.LocalMusicRepository
import com.oskar.retrolauncher.data.music.MusicLibraryRepository
import com.oskar.retrolauncher.data.music.PlayerController
import com.oskar.retrolauncher.data.prefs.SettingsStore
import com.oskar.retrolauncher.data.trip.AppDb
import com.oskar.retrolauncher.data.trip.TripRecorder
import com.oskar.retrolauncher.data.trip.TripRepository
import com.oskar.retrolauncher.data.weather.WeatherRepository
import com.oskar.retrolauncher.diag.CrashHandler
import com.oskar.retrolauncher.diag.FileLogger
import com.oskar.retrolauncher.diag.LogPaths
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import timber.log.Timber

class App : Application() {

    /**
     * Lazily constructed in [onCreate]. Instrumented tests pre-install a fake
     * subclass via [com.oskar.retrolauncher] test-runner hooks — see T1.36.
     * Setting it before `onCreate` runs causes the default construction in
     * [onCreate] to be skipped, satisfying the AC that no real network or
     * sensor work runs during fragment instrumented tests.
     */
    @set:VisibleForTesting
    lateinit var service: ServiceLocator

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Diagnostics first — every subsequent crash should end up on disk.
        installDiagnostics()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        installCrashBarrier()

        Timber.i(
            "App.onCreate sdk=%d device=%s/%s app=%s %s (%s)",
            Build.VERSION.SDK_INT,
            Build.MANUFACTURER,
            Build.MODEL,
            BuildConfig.APPLICATION_ID,
            BuildConfig.VERSION_NAME,
            if (BuildConfig.DEBUG) "debug" else "release",
        )

        if (!::service.isInitialized) {
            service = ServiceLocator(this)
        }
        runCatching { service.startup() }.onFailure {
            Timber.e(it, "ServiceLocator.startup() failed")
        }
    }

    /** File logger + crash dumper. Always-on (debug AND release) so head-unit crashes leave a trail. */
    private fun installDiagnostics() {
        runCatching {
            val logDir = LogPaths.forContext(this)
            Timber.plant(FileLogger(logDir))
            CrashHandler.install(
                logDir = logDir,
                info = CrashHandler.DeviceInfo(
                    sdk = Build.VERSION.SDK_INT,
                    manufacturer = Build.MANUFACTURER ?: "unknown",
                    model = Build.MODEL ?: "unknown",
                    fingerprint = Build.FINGERPRINT ?: "unknown",
                    versionName = BuildConfig.VERSION_NAME,
                    applicationId = BuildConfig.APPLICATION_ID,
                    buildType = if (BuildConfig.DEBUG) "debug" else "release",
                ),
            )
        }
    }

    /**
     * Safety net for API 23 compatibility regressions. Runs in every build
     * type — the head unit ships release APKs and that's exactly where we
     * need the diagnostic trail to survive a crash.
     */
    private fun installCrashBarrier() {
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "Uncaught exception on API ${Build.VERSION.SDK_INT}")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O &&
                (throwable is NoSuchMethodError || throwable is NoClassDefFoundError)
            ) {
                try {
                    Toast.makeText(
                        this, "Crash — compatibility issue: ${throwable.javaClass.simpleName}",
                        Toast.LENGTH_LONG,
                    ).show()
                } catch (_: Exception) {
                    // Toast may fail if no Looper is ready yet.
                }
            }
            prev?.uncaughtException(thread, throwable)
            if (prev == null) throw throwable
        }
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
        val player: PlayerController get() = instance.service.player
        val localMusic: LocalMusicRepository get() = instance.service.localMusic
        val musicLibrary: MusicLibraryRepository get() = instance.service.musicLibrary
        val weather: WeatherRepository get() = instance.service.weather
        val location: LocationRepository get() = instance.service.location
        val appList: AppListRepository get() = instance.service.appList
        val trips: TripRepository get() = instance.service.trips
        val tripRecorder: TripRecorder get() = instance.service.tripRecorder
        val appScope: CoroutineScope get() = instance.service.appScope
    }
}
