package com.oskar.retrolauncher.data.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Moshi DTO for OpenWeatherMap `/data/2.5/weather` (current weather endpoint).
 *
 * Internal — UI layer consumes [WeatherSnapshot] instead, so the DTO never escapes
 * the data module. Optional fields are nullable with default `null` so that a
 * partial OWM payload (a free-tier response missing `wind.gust`, for example)
 * deserializes cleanly without throwing.
 */
@JsonClass(generateAdapter = true)
internal data class WeatherDto(
    val main: Main,
    val weather: List<Condition>,
    val wind: Wind?,
    val name: String,
    val dt: Long,
) {
    @JsonClass(generateAdapter = true)
    internal data class Main(
        val temp: Double,
        @Json(name = "feels_like") val feelsLike: Double,
        @Json(name = "temp_min") val tempMin: Double? = null,
        @Json(name = "temp_max") val tempMax: Double? = null,
        val pressure: Int? = null,
        val humidity: Int? = null,
    )

    @JsonClass(generateAdapter = true)
    internal data class Condition(
        val id: Int,
        val icon: String,
        val main: String? = null,
        val description: String? = null,
    )

    @JsonClass(generateAdapter = true)
    internal data class Wind(
        val speed: Double,
        val deg: Int? = null,
        val gust: Double? = null,
    )
}
