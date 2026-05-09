package com.oskar.retrolauncher

import com.oskar.retrolauncher.data.weather.WeatherDto
import com.oskar.retrolauncher.data.weather.WeatherSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherSnapshotTest {

    @Test
    fun `from picks current temp_max temp_min and condition`() {
        val dto = WeatherDto(
            main = WeatherDto.Main(
                temp = 18.5,
                feelsLike = 17.0,
                tempMin = 12.0,
                tempMax = 22.0,
            ),
            weather = listOf(
                WeatherDto.Condition(id = 803, icon = "04d", main = "Clouds", description = "broken"),
            ),
            wind = WeatherDto.Wind(speed = 3.4),
            name = "Ljubljana",
            dt = 1715250000L,
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
    fun `from falls back to current temp when temp_min temp_max missing`() {
        val dto = WeatherDto(
            main = WeatherDto.Main(temp = 5.0, feelsLike = 4.0),
            weather = listOf(
                WeatherDto.Condition(id = 800, icon = "01d", description = "clear"),
            ),
            wind = null,
            name = "—",
            dt = 0L,
        )

        val snap = WeatherSnapshot.from(dto, city = "—", asOf = 0L)

        assertEquals(5.0, snap.highC, 0.0001)
        assertEquals(5.0, snap.lowC, 0.0001)
        assertEquals(800, snap.iconId)
    }

    @Test
    fun `from defaults iconId 800 when no weather conditions`() {
        val dto = WeatherDto(
            main = WeatherDto.Main(temp = 0.0, feelsLike = 0.0),
            weather = emptyList(),
            wind = null,
            name = "x",
            dt = 0L,
        )
        val snap = WeatherSnapshot.from(dto, city = "x", asOf = 0L)
        assertEquals(800, snap.iconId)
        assertEquals("", snap.condition)
    }
}
