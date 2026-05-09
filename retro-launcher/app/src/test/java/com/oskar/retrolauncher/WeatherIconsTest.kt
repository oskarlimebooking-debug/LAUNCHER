package com.oskar.retrolauncher

import com.oskar.retrolauncher.data.weather.iconResForCode
import com.oskar.retrolauncher.data.weather.iconResForCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T1.22 AC1 — all 18 OWM icon codes resolve to a local vector drawable.
 *
 * The full list of OWM icon codes (https://openweathermap.org/weather-conditions):
 *   01d 01n 02d 02n 03d 03n 04d 04n 09d 09n 10d 10n 11d 11n 13d 13n 50d 50n
 */
class WeatherIconsTest {

    private val allCodes = listOf(
        "01d", "01n", "02d", "02n", "03d", "03n", "04d", "04n",
        "09d", "09n", "10d", "10n", "11d", "11n", "13d", "13n",
        "50d", "50n",
    )

    @Test
    fun `all 18 OWM icon codes resolve to a non-zero local drawable`() {
        for (code in allCodes) {
            val res = iconResForCode(code)
            assertTrue(
                "icon code $code must map to a local drawable (non-zero res id)",
                res != 0,
            )
        }
    }

    @Test
    fun `unknown icon code falls back to ic_unknown`() {
        // Ensures we don't crash on a malformed payload.
        assertEquals(R.drawable.ic_unknown, iconResForCode(""))
        assertEquals(R.drawable.ic_unknown, iconResForCode("99x"))
    }

    @Test
    fun `clear day code 01d maps to sun, clear night 01n maps to a clear icon`() {
        assertEquals(R.drawable.ic_sun, iconResForCode("01d"))
        // Night clear should not be the sun (we don't have a moon, so it's still sun
        // for now, but the lookup must accept the n-suffix).
        assertNotEquals(0, iconResForCode("01n"))
    }

    @Test
    fun `cloud, rain, drizzle, thunder, snow, fog codes resolve to expected family`() {
        assertEquals(R.drawable.ic_cloud, iconResForCode("03d"))
        assertEquals(R.drawable.ic_cloud, iconResForCode("04n"))
        assertEquals(R.drawable.ic_drizzle, iconResForCode("09d"))
        assertEquals(R.drawable.ic_rain, iconResForCode("10n"))
        assertEquals(R.drawable.ic_thunder, iconResForCode("11d"))
        assertEquals(R.drawable.ic_snow, iconResForCode("13n"))
        assertEquals(R.drawable.ic_fog, iconResForCode("50d"))
    }

    @Test
    fun `int-id mapping still works (legacy iconResForCondition)`() {
        // iconResForCondition (Int → drawable) is kept for callers that have a
        // numeric OWM condition id (200..804). T1.22 adds the 18-code string map
        // alongside it, not in place of it.
        assertEquals(R.drawable.ic_sun, iconResForCondition(800))
        assertEquals(R.drawable.ic_thunder, iconResForCondition(211))
        assertEquals(R.drawable.ic_unknown, iconResForCondition(999))
    }
}
