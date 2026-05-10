package com.oskar.retrolauncher

import android.content.Context
import android.content.SharedPreferences
import com.oskar.retrolauncher.data.prefs.SettingsStore
import com.oskar.retrolauncher.data.prefs.SpeedUnit
import com.oskar.retrolauncher.data.prefs.TempUnit
import com.oskar.retrolauncher.data.prefs.Units
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * T1.31 — SettingsStore per-property `Flow<T>` + new fields (mapApp, voiceApp,
 * weatherLat/lon, firstRunDone, tripStartSpeedKmh, panelRatio Float, Moshi-backed
 * pinnedApps).
 *
 * The flow contract validated here is the conservative one: `first()` returns
 * the value currently stored in prefs. Listener-driven re-emit during an active
 * collection is exercised indirectly through `changes(key)` which the existing
 * `SettingsStoreUnitsTest` and live UI already depend on.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class SettingsStoreT131Test {

    private fun freshPrefs(name: String = "t131_${System.nanoTime()}"): SharedPreferences {
        val ctx = RuntimeEnvironment.getApplication()
        val prefs = ctx.getSharedPreferences(name, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        return prefs
    }

    private fun freshStore(): SettingsStore = SettingsStore(freshPrefs())

    // -------------------- Defaults --------------------

    @Test
    fun `defaults — mapApp, voiceApp, weatherLat, weatherLon are null`() {
        val s = freshStore()
        assertNull(s.mapApp)
        assertNull(s.voiceApp)
        assertNull(s.weatherLat)
        assertNull(s.weatherLon)
    }

    @Test
    fun `default firstRunDone is false`() {
        assertEquals(false, freshStore().firstRunDone)
    }

    @Test
    fun `default tripStartSpeedKmh is 3`() {
        assertEquals(3, freshStore().tripStartSpeedKmh)
    }

    @Test
    fun `default panelRatio Float is 0_4 (40 percent)`() {
        assertEquals(0.4f, freshStore().panelRatio, 0.0001f)
    }

    @Test
    fun `default gridColumns is 4`() {
        assertEquals(4, freshStore().gridColumns)
    }

    // -------------------- Round-trip --------------------

    @Test
    fun `setMapApp roundtrips across new SettingsStore instances`() {
        val prefs = freshPrefs("map_persist_${System.nanoTime()}")
        SettingsStore(prefs).setMapApp("com.example.maps")
        val b = SettingsStore(prefs)
        assertEquals("com.example.maps", b.mapApp)
    }

    @Test
    fun `setMapApp(null) clears the stored value`() {
        val s = freshStore()
        s.setMapApp("com.x")
        s.setMapApp(null)
        assertNull(s.mapApp)
    }

    @Test
    fun `setVoiceApp roundtrips and clears`() {
        val s = freshStore()
        s.setVoiceApp("com.assistant")
        assertEquals("com.assistant", s.voiceApp)
        s.setVoiceApp(null)
        assertNull(s.voiceApp)
    }

    @Test
    fun `setWeatherLocation roundtrips lat and lon`() {
        val prefs = freshPrefs("wx_persist_${System.nanoTime()}")
        SettingsStore(prefs).setWeatherLocation(60.17f, 24.94f)
        val b = SettingsStore(prefs)
        assertEquals(60.17f, b.weatherLat!!, 0.0001f)
        assertEquals(24.94f, b.weatherLon!!, 0.0001f)
    }

    @Test
    fun `setWeatherLocation(null, null) clears both`() {
        val s = freshStore()
        s.setWeatherLocation(1.0f, 2.0f)
        s.setWeatherLocation(null, null)
        assertNull(s.weatherLat)
        assertNull(s.weatherLon)
    }

    @Test
    fun `setFirstRunDone roundtrips`() {
        val prefs = freshPrefs("frd_${System.nanoTime()}")
        SettingsStore(prefs).setFirstRunDone(true)
        val b = SettingsStore(prefs)
        assertEquals(true, b.firstRunDone)
    }

    // -------------------- Moshi pinnedApps serialization --------------------

    @Test
    fun `pinnedApps Moshi-encodes and decodes a normal list`() {
        val s = freshStore()
        val packages = listOf("com.a", "com.b", "com.c")
        s.setPinnedApps(packages)
        assertEquals(packages, s.pinnedApps)
    }

    @Test
    fun `pinnedApps falls back to empty list on malformed JSON`() {
        val prefs = freshPrefs("pinned_bad_${System.nanoTime()}")
        prefs.edit().putString(SettingsStore.KEY_PINNED_APPS, "not-json-at-all").commit()
        val s = SettingsStore(prefs)
        assertTrue(s.pinnedApps.isEmpty())
    }

    @Test
    fun `pinnedApps handles entries with special chars (quote, slash, unicode)`() {
        val s = freshStore()
        val pkgs = listOf("com.a/b\"x", "héllo.世界", "x\\y")
        s.setPinnedApps(pkgs)
        assertEquals(pkgs, s.pinnedApps)
    }

    @Test
    fun `pinnedApps stored payload is valid JSON array of strings`() {
        val prefs = freshPrefs("pinned_payload_${System.nanoTime()}")
        SettingsStore(prefs).setPinnedApps(listOf("com.a", "com.b"))
        val raw = prefs.getString(SettingsStore.KEY_PINNED_APPS, null)!!
        assertEquals("""["com.a","com.b"]""", raw)
    }

    // -------------------- Per-property Flow<T> first-emit --------------------

    @Test
    fun `unitsFlow first emit is METRIC by default`() = runTest {
        assertEquals(Units.METRIC, freshStore().unitsFlow.first())
    }

    @Test
    fun `unitsFlow first emit reflects current pref (imperial)`() = runTest {
        val prefs = freshPrefs("uf_${System.nanoTime()}")
        prefs.edit().putString(SettingsStore.KEY_UNITS, "imperial").commit()
        assertEquals(Units.IMPERIAL, SettingsStore(prefs).unitsFlow.first())
    }

    @Test
    fun `speedUnitFlow first emit reflects current pref`() = runTest {
        val prefs = freshPrefs("suf_${System.nanoTime()}")
        prefs.edit().putString(SettingsStore.KEY_UNITS, "imperial").commit()
        assertEquals(SpeedUnit.MILES_PER_HOUR, SettingsStore(prefs).speedUnitFlow.first())
    }

    @Test
    fun `tempUnitFlow first emit reflects current pref`() = runTest {
        val prefs = freshPrefs("tuf_${System.nanoTime()}")
        prefs.edit().putString(SettingsStore.KEY_UNITS, "imperial").commit()
        assertEquals(TempUnit.FAHRENHEIT, SettingsStore(prefs).tempUnitFlow.first())
    }

    @Test
    fun `panelRatioFlow first emit reflects current pref`() = runTest {
        val prefs = freshPrefs("pr_${System.nanoTime()}")
        prefs.edit().putInt(SettingsStore.KEY_PANEL_RATIO, 50).commit()
        assertEquals(0.5f, SettingsStore(prefs).panelRatioFlow.first(), 0.0001f)
    }

    @Test
    fun `gridColumnsFlow first emit reflects current pref`() = runTest {
        val prefs = freshPrefs("gc_${System.nanoTime()}")
        prefs.edit().putString(SettingsStore.KEY_GRID_COLS, "5").commit()
        assertEquals(5, SettingsStore(prefs).gridColumnsFlow.first())
    }

    @Test
    fun `tripStartSpeedKmhFlow first emit is default 3`() = runTest {
        assertEquals(3, freshStore().tripStartSpeedKmhFlow.first())
    }

    @Test
    fun `pinnedAppsFlow first emit reflects current value`() = runTest {
        val s = freshStore()
        s.setPinnedApps(listOf("com.x"))
        assertEquals(listOf("com.x"), s.pinnedAppsFlow.first())
    }

    @Test
    fun `mapAppFlow first emit reflects current value (null default, then set)`() = runTest {
        val s = freshStore()
        assertNull(s.mapAppFlow.first())
        s.setMapApp("com.m")
        assertEquals("com.m", s.mapAppFlow.first())
    }

    @Test
    fun `voiceAppFlow first emit reflects current value`() = runTest {
        val s = freshStore()
        s.setVoiceApp("com.v")
        assertEquals("com.v", s.voiceAppFlow.first())
    }

    @Test
    fun `weatherLatFlow and weatherLonFlow emit current value or null`() = runTest {
        val s = freshStore()
        assertNull(s.weatherLatFlow.first())
        assertNull(s.weatherLonFlow.first())
        s.setWeatherLocation(10.5f, -20.25f)
        assertEquals(10.5f, s.weatherLatFlow.first()!!, 0.0001f)
        assertEquals(-20.25f, s.weatherLonFlow.first()!!, 0.0001f)
    }

    @Test
    fun `firstRunDoneFlow first emit reflects current value`() = runTest {
        val s = freshStore()
        assertEquals(false, s.firstRunDoneFlow.first())
        s.setFirstRunDone(true)
        assertEquals(true, s.firstRunDoneFlow.first())
    }
}
