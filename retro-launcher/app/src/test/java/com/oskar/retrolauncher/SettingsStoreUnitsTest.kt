package com.oskar.retrolauncher

import android.content.Context
import com.oskar.retrolauncher.data.prefs.SettingsStore
import com.oskar.retrolauncher.data.prefs.SpeedUnit
import com.oskar.retrolauncher.data.prefs.TempUnit
import com.oskar.retrolauncher.data.prefs.Units
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * T1.22 AC3/AC4 — `SettingsStore` exposes typed `tempUnit` and `speedUnit`
 * properties so the WeatherFragment can branch on them directly without
 * re-implementing the metric/imperial pivot.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class SettingsStoreUnitsTest {

    private fun newStore(): SettingsStore {
        val ctx = RuntimeEnvironment.getApplication()
        val prefs = ctx.getSharedPreferences("settings_test", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        return SettingsStore(prefs)
    }

    @Test
    fun `default tempUnit is Celsius (matches metric default)`() {
        assertEquals(TempUnit.CELSIUS, newStore().tempUnit)
    }

    @Test
    fun `default speedUnit is km per hour (matches metric default)`() {
        assertEquals(SpeedUnit.KILOMETERS_PER_HOUR, newStore().speedUnit)
    }

    @Test
    fun `setting units to imperial flips temp to Fahrenheit and speed to mph`() {
        val ctx = RuntimeEnvironment.getApplication()
        val prefs = ctx.getSharedPreferences("settings_test", Context.MODE_PRIVATE)
        prefs.edit().clear().putString(SettingsStore.KEY_UNITS, "imperial").apply()
        val store = SettingsStore(prefs)
        assertEquals(Units.IMPERIAL, store.units)
        assertEquals(TempUnit.FAHRENHEIT, store.tempUnit)
        assertEquals(SpeedUnit.MILES_PER_HOUR, store.speedUnit)
    }
}
