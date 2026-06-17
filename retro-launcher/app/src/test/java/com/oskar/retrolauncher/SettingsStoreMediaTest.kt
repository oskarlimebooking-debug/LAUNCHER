package com.oskar.retrolauncher

import android.content.Context
import android.content.SharedPreferences
import com.oskar.retrolauncher.data.prefs.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * M0 — media/player settings: defaults (full player ON by default, the layout
 * toggles OFF) and that each property exposes a reactive `Flow<Boolean>`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class SettingsStoreMediaTest {

    private fun freshPrefs(): SharedPreferences {
        val ctx = RuntimeEnvironment.getApplication()
        val prefs = ctx.getSharedPreferences("media_${System.nanoTime()}", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        return prefs
    }

    @Test
    fun `defaults — full player on, layout toggles off`() {
        val s = SettingsStore(freshPrefs())
        assertTrue("full player defaults on", s.fullPlayerMode)
        assertFalse(s.mediaCardEnlarged)
        assertFalse(s.mediaReplacesWeather)
    }

    @Test
    fun `fullPlayerMode reflects writes and flow emits current value`() = runTest {
        val prefs = freshPrefs()
        val s = SettingsStore(prefs)
        assertTrue(s.fullPlayerModeFlow.first())

        prefs.edit().putBoolean(SettingsStore.KEY_FULL_PLAYER, false).commit()
        assertFalse(s.fullPlayerMode)
        assertFalse(s.fullPlayerModeFlow.first())
    }

    @Test
    fun `layout toggles round-trip through prefs`() = runTest {
        val prefs = freshPrefs()
        val s = SettingsStore(prefs)
        prefs.edit()
            .putBoolean(SettingsStore.KEY_MEDIA_CARD_SIZE, true)
            .putBoolean(SettingsStore.KEY_MEDIA_REPLACES_WEATHER, true)
            .commit()
        assertTrue(s.mediaCardEnlarged)
        assertTrue(s.mediaReplacesWeather)
        assertEquals(true, s.mediaCardEnlargedFlow.first())
        assertEquals(true, s.mediaReplacesWeatherFlow.first())
    }
}
