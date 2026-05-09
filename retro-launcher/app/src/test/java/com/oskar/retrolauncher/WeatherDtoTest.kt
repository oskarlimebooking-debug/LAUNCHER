package com.oskar.retrolauncher

import com.oskar.retrolauncher.data.weather.WeatherDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * T1.19 — verifies the Moshi DTO for OWM `/data/2.5/weather`.
 *
 * The DTO is internal (AC4) — the test sits in the same module so the visibility holds.
 */
class WeatherDtoTest {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(WeatherDto::class.java)

    @Test
    fun `kapt-generated adapter class exists for WeatherDto`() {
        // moshi-codegen produces a sibling class named "<TypeName>JsonAdapter" — its
        // presence on the classpath is the smoking-gun proof that kapt ran (AC1).
        val cls = Class.forName("com.oskar.retrolauncher.data.weather.WeatherDtoJsonAdapter")
        assertNotNull(cls)
    }

    @Test
    fun `adapter parses full OWM 2_5 weather fixture`() {
        val dto = adapter.fromJson(FULL_FIXTURE)!!

        assertEquals(18.5, dto.main.temp, 0.0001)
        assertEquals(17.0, dto.main.feelsLike, 0.0001)
        assertEquals(800, dto.weather[0].id)
        assertEquals("01d", dto.weather[0].icon)
        val wind = dto.wind!!
        assertEquals(3.4, wind.speed, 0.0001)
        assertEquals(6.1, wind.gust!!, 0.0001)
        assertEquals("Ljubljana", dto.name)
        assertEquals(1715250000L, dto.dt)
    }

    @Test
    fun `missing wind gust deserializes as null without throwing`() {
        val dto = adapter.fromJson(MIN_FIXTURE)!!
        val wind = dto.wind!!

        assertEquals(3.4, wind.speed, 0.0001)
        assertNull(wind.gust)
        assertNull(wind.deg)
    }

    @Test
    fun `missing wind block entirely deserializes as null without throwing`() {
        val json = """
            {
              "weather": [{"id": 800, "main": "Clear", "description": "clear sky", "icon": "01d"}],
              "main": {"temp": 18.5, "feels_like": 17.0},
              "name": "Ljubljana",
              "dt": 1715250000
            }
        """.trimIndent()

        val dto = adapter.fromJson(json)!!

        assertNull(dto.wind)
    }

    @Test
    fun `optional temp_min and temp_max are null when missing`() {
        val dto = adapter.fromJson(MIN_FIXTURE)!!

        assertNull(dto.main.tempMin)
        assertNull(dto.main.tempMax)
    }

    @Test
    fun `roundtrip preserves required fields`() {
        val dto = WeatherDto(
            weather = listOf(
                WeatherDto.Condition(id = 800, icon = "01d", main = "Clear", description = "clear"),
            ),
            main = WeatherDto.Main(temp = 5.0, feelsLike = 4.0),
            wind = WeatherDto.Wind(speed = 2.0),
            name = "X",
            dt = 1L,
        )
        val back = adapter.fromJson(adapter.toJson(dto))!!
        assertEquals(dto, back)
    }

    private companion object {
        const val FULL_FIXTURE = """
            {
              "coord": {"lon": 14.51, "lat": 46.05},
              "weather": [{"id": 800, "main": "Clear", "description": "clear sky", "icon": "01d"}],
              "base": "stations",
              "main": {
                "temp": 18.5,
                "feels_like": 17.0,
                "temp_min": 16.0,
                "temp_max": 21.0,
                "pressure": 1013,
                "humidity": 60
              },
              "visibility": 10000,
              "wind": {"speed": 3.4, "deg": 220, "gust": 6.1},
              "clouds": {"all": 0},
              "dt": 1715250000,
              "sys": {"country": "SI", "sunrise": 1715225000, "sunset": 1715275000},
              "timezone": 7200,
              "id": 3196359,
              "name": "Ljubljana",
              "cod": 200
            }
        """

        const val MIN_FIXTURE = """
            {
              "weather": [{"id": 800, "main": "Clear", "description": "clear sky", "icon": "01d"}],
              "main": {"temp": 18.5, "feels_like": 17.0},
              "wind": {"speed": 3.4},
              "name": "Ljubljana",
              "dt": 1715250000
            }
        """
    }
}
