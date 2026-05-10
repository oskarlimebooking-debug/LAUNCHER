package com.oskar.retrolauncher

import android.content.Context
import android.content.SharedPreferences
import com.oskar.retrolauncher.data.prefs.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * T1.33 — SettingsStore.owmApiKey: user-supplied OpenWeatherMap key collected
 * by the first-run wizard. Nullable so an unset key falls back to
 * BuildConfig.OWM_API_KEY in WeatherRepository (T1.20).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class SettingsStoreT133Test {

    private fun freshPrefs(name: String = "t133_${System.nanoTime()}"): SharedPreferences {
        val ctx = RuntimeEnvironment.getApplication()
        val prefs = ctx.getSharedPreferences(name, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        return prefs
    }

    private fun freshStore(): SettingsStore = SettingsStore(freshPrefs())

    @Test
    fun `default owmApiKey is null`() {
        assertNull(freshStore().owmApiKey)
    }

    @Test
    fun `setOwmApiKey roundtrips`() {
        val prefs = freshPrefs()
        SettingsStore(prefs).setOwmApiKey("abc123")
        assertEquals("abc123", SettingsStore(prefs).owmApiKey)
    }

    @Test
    fun `setOwmApiKey with null clears the value`() {
        val prefs = freshPrefs()
        val s = SettingsStore(prefs)
        s.setOwmApiKey("abc123")
        s.setOwmApiKey(null)
        assertNull(SettingsStore(prefs).owmApiKey)
    }

    @Test
    fun `setOwmApiKey with blank clears the value`() {
        val prefs = freshPrefs()
        val s = SettingsStore(prefs)
        s.setOwmApiKey("abc123")
        s.setOwmApiKey("")
        assertNull(SettingsStore(prefs).owmApiKey)
    }

    @Test
    fun `owmApiKeyFlow first emit reflects current value`() = runTest {
        val s = freshStore()
        s.setOwmApiKey("key-from-wizard")
        assertEquals("key-from-wizard", s.owmApiKeyFlow.first())
    }
}
