package com.oskar.retrolauncher

import com.oskar.retrolauncher.ui.weather.WeatherViewModel
import com.oskar.retrolauncher.ui.weather.formatWindSpeed
import com.oskar.retrolauncher.ui.weather.formatTemp
import com.oskar.retrolauncher.data.prefs.SpeedUnit
import com.oskar.retrolauncher.data.prefs.TempUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * T1.22 — pure formatter assertions for AC3 (temp unit) and AC4 (speed unit).
 * The ViewModel itself is a thin wrapper around App.weather.state, so we exercise
 * the formatters that drive the binding instead of spinning up a full Robolectric
 * service locator for trivial state.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class WeatherViewModelTest {

    @Test
    fun `formatTemp celsius returns whole degrees with C suffix`() {
        assertEquals("18°C", formatTemp(18.4, TempUnit.CELSIUS))
        assertEquals("-3°C", formatTemp(-3.2, TempUnit.CELSIUS))
    }

    @Test
    fun `formatTemp fahrenheit converts and uses F suffix`() {
        // 0C → 32F, 100C → 212F
        assertEquals("32°F", formatTemp(0.0, TempUnit.FAHRENHEIT))
        assertEquals("212°F", formatTemp(100.0, TempUnit.FAHRENHEIT))
    }

    @Test
    fun `formatWindSpeed handles all three speed units`() {
        // 10 m/s = 36 km/h = 22.37 mph
        assertEquals("10 m/s", formatWindSpeed(10.0, SpeedUnit.METERS_PER_SECOND))
        assertEquals("36 km/h", formatWindSpeed(10.0, SpeedUnit.KILOMETERS_PER_HOUR))
        assertEquals("22 mph", formatWindSpeed(10.0, SpeedUnit.MILES_PER_HOUR))
    }

    @Test
    fun `WeatherViewModel can be instantiated`() {
        // The VM hooks App.weather.state — Robolectric's App provides a real
        // WeatherRepository with a null cached snapshot.
        WeatherViewModel()
    }
}
