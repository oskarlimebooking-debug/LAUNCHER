package com.oskar.retrolauncher.data.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherDto(
    val current: Current,
    val daily: List<Daily>,
    val timezone: String?,
) {
    @JsonClass(generateAdapter = true)
    data class Current(
        val temp: Double,
        @Json(name = "feels_like") val feelsLike: Double,
        val weather: List<Condition>,
    )

    @JsonClass(generateAdapter = true)
    data class Daily(
        val temp: Temp,
        val weather: List<Condition>,
    ) {
        @JsonClass(generateAdapter = true)
        data class Temp(val min: Double, val max: Double)
    }

    @JsonClass(generateAdapter = true)
    data class Condition(
        val id: Int,
        val main: String,
        val description: String,
        val icon: String,
    )
}
