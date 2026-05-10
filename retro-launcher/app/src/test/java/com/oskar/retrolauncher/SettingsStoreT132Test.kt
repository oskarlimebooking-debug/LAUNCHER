package com.oskar.retrolauncher

import android.content.Context
import android.content.SharedPreferences
import com.oskar.retrolauncher.data.prefs.SettingsStore
import com.oskar.retrolauncher.data.prefs.Theme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * T1.32 — SettingsStore fields added for SettingsFragment: theme, gpxExport,
 * and weatherRefreshIntervalMin. Each field has a default, a setter, and a
 * `Flow<T>` mirror following the T1.31 pattern.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class SettingsStoreT132Test {

    private fun freshPrefs(name: String = "t132_${System.nanoTime()}"): SharedPreferences {
        val ctx = RuntimeEnvironment.getApplication()
        val prefs = ctx.getSharedPreferences(name, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        return prefs
    }

    private fun freshStore(): SettingsStore = SettingsStore(freshPrefs())

    // -------------------- theme --------------------

    @Test
    fun `default theme is SYSTEM`() {
        assertEquals(Theme.SYSTEM, freshStore().theme)
    }

    @Test
    fun `setTheme roundtrips`() {
        val prefs = freshPrefs()
        SettingsStore(prefs).setTheme(Theme.DARK)
        assertEquals(Theme.DARK, SettingsStore(prefs).theme)
    }

    @Test
    fun `themeFlow first emit reflects current value`() = runTest {
        val s = freshStore()
        s.setTheme(Theme.LIGHT)
        assertEquals(Theme.LIGHT, s.themeFlow.first())
    }

    // -------------------- gpxExport --------------------

    @Test
    fun `default gpxExport is false`() {
        assertEquals(false, freshStore().gpxExport)
    }

    @Test
    fun `setGpxExport roundtrips`() {
        val prefs = freshPrefs()
        SettingsStore(prefs).setGpxExport(true)
        assertEquals(true, SettingsStore(prefs).gpxExport)
    }

    @Test
    fun `gpxExportFlow first emit reflects current value`() = runTest {
        val s = freshStore()
        s.setGpxExport(true)
        assertEquals(true, s.gpxExportFlow.first())
    }

    // -------------------- weatherRefreshIntervalMin --------------------

    @Test
    fun `default weatherRefreshIntervalMin is 30`() {
        assertEquals(30, freshStore().weatherRefreshIntervalMin)
    }

    @Test
    fun `setWeatherRefreshIntervalMin roundtrips`() {
        val prefs = freshPrefs()
        SettingsStore(prefs).setWeatherRefreshIntervalMin(60)
        assertEquals(60, SettingsStore(prefs).weatherRefreshIntervalMin)
    }

    @Test
    fun `weatherRefreshIntervalMinFlow first emit reflects current value`() = runTest {
        val s = freshStore()
        s.setWeatherRefreshIntervalMin(15)
        assertEquals(15, s.weatherRefreshIntervalMinFlow.first())
    }
}
