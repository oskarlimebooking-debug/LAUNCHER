package com.oskar.retrolauncher.test

import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.os.SystemClock
import com.oskar.retrolauncher.data.apps.AppEntry
import com.oskar.retrolauncher.data.media.MediaState
import com.oskar.retrolauncher.data.trip.TripEntity
import com.oskar.retrolauncher.data.weather.WeatherSnapshot

/**
 * T1.36 — fixture builders shared across the fragment instrumented tests.
 * Defaults are the "happy populated" case; tests override only the fields
 * they care about (e.g. `mediaState(playing = true)`).
 */
object TestData {

    fun mediaState(
        title: String = "Test Track",
        artist: String = "Test Artist",
        playing: Boolean = false,
        durationMs: Long = 240_000,
        positionMs: Long = 30_000,
    ): MediaState {
        val now = SystemClock.elapsedRealtime()
        return MediaState(
            title = title,
            artist = artist,
            album = "Test Album",
            durationMs = durationMs,
            positionMs = positionMs,
            positionAtMs = now,
            speed = 1f,
            playing = playing,
            art = null,
            packageName = "test.app",
        )
    }

    fun weatherSnapshot(
        tempC: Double = 18.0,
        feelsLikeC: Double = 17.0,
        highC: Double = 20.0,
        lowC: Double = 14.0,
        windMs: Double = 2.5,
        condition: String = "few clouds",
        city: String = "Test City",
    ): WeatherSnapshot = WeatherSnapshot(
        tempC = tempC,
        feelsLikeC = feelsLikeC,
        highC = highC,
        lowC = lowC,
        windMs = windMs,
        iconId = 801,
        iconCode = "02d",
        condition = condition,
        city = city,
        asOfMs = System.currentTimeMillis(),
    )

    fun tripEntity(
        id: Long = 1L,
        startMs: Long = System.currentTimeMillis() - 60 * 60_000L,
        endMs: Long = System.currentTimeMillis() - 30 * 60_000L,
        distanceM: Double = 12_500.0,
        avgSpeedMs: Double = 12.0,
        maxSpeedMs: Double = 22.0,
        startLabel: String? = "Home",
        endLabel: String? = "Office",
    ): TripEntity = TripEntity(
        id = id,
        startMs = startMs,
        endMs = endMs,
        distanceM = distanceM,
        avgSpeedMs = avgSpeedMs,
        maxSpeedMs = maxSpeedMs,
        startLabel = startLabel,
        endLabel = endLabel,
    )

    /**
     * Builds an [AppEntry] anchored on a real package — defaults to the
     * launcher under test which is always present in the test apk's
     * `<queries>` view of the world.
     */
    fun appEntry(
        label: String = "Test App",
        packageName: String = "com.oskar.retrolauncher.debug",
        className: String = "$packageName.MainActivity",
    ): AppEntry = AppEntry(
        label = label,
        packageName = packageName,
        componentName = ComponentName(packageName, className),
        applicationInfo = ApplicationInfo().also { it.packageName = packageName },
    )
}
