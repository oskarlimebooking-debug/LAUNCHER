package com.oskar.retrolauncher

import com.oskar.retrolauncher.data.weather.WeatherDto
import com.oskar.retrolauncher.data.weather.WeatherSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherSnapshotTest {

    @Test
    fun `from picks today's high low and current condition`() {
        val dto = WeatherDto(
            current = WeatherDto.Current(
                temp = 18.5,
                feelsLike = 17.0,
                weather = listOf(
                    WeatherDto.Condition(id = 803, main = "Clouds", description = "broken", icon = "04d"),
                ),
            ),
            daily = listOf(
                WeatherDto.Daily(
                    temp = WeatherDto.Daily.Temp(min = 12.0, max = 22.0),
                    weather = emptyList(),
                ),
            ),
            timezone = "Europe/Ljubljana",
        )

        val snap = WeatherSnapshot.from(dto, city = "Ljubljana", asOf = 12345L)

        assertEquals(18.5, snap.tempC, 0.0001)
        assertEquals(22.0, snap.highC, 0.0001)
        assertEquals(12.0, snap.lowC, 0.0001)
        assertEquals(803, snap.iconId)
        assertEquals("broken", snap.condition)
        assertEquals("Ljubljana", snap.city)
        assertEquals(12345L, snap.asOfMs)
    }

    @Test
    fun `from falls back to current temp when daily empty`() {
        val dto = WeatherDto(
            current = WeatherDto.Current(
                temp = 5.0,
                feelsLike = 4.0,
                weather = listOf(
                    WeatherDto.Condition(id = 800, main = "Clear", description = "clear", icon = "01d"),
                ),
            ),
            daily = emptyList(),
            timezone = null,
        )

        val snap = WeatherSnapshot.from(dto, city = "—", asOf = 0L)

        assertEquals(5.0, snap.highC, 0.0001)
        assertEquals(5.0, snap.lowC, 0.0001)
        assertEquals(800, snap.iconId)
    }

    @Test
    fun `from defaults iconId 800 when no weather conditions`() {
        val dto = WeatherDto(
            current = WeatherDto.Current(temp = 0.0, feelsLike = 0.0, weather = emptyList()),
            daily = emptyList(),
            timezone = null,
        )
        val snap = WeatherSnapshot.from(dto, city = "x", asOf = 0L)
        assertEquals(800, snap.iconId)
        assertEquals("", snap.condition)
    }
}
