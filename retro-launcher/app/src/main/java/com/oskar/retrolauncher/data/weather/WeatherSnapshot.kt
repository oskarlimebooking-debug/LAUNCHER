package com.oskar.retrolauncher.data.weather

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherSnapshot(
    val tempC: Double,
    val highC: Double,
    val lowC: Double,
    val iconId: Int,
    val condition: String,
    val city: String,
    val asOfMs: Long,
) {
    companion object {
        fun from(dto: WeatherDto, city: String, asOf: Long): WeatherSnapshot {
            val today = dto.daily.firstOrNull()
            return WeatherSnapshot(
                tempC = dto.current.temp,
                highC = today?.temp?.max ?: dto.current.temp,
                lowC = today?.temp?.min ?: dto.current.temp,
                iconId = dto.current.weather.firstOrNull()?.id ?: 800,
                condition = dto.current.weather.firstOrNull()?.description.orEmpty(),
                city = city,
                asOfMs = asOf,
            )
        }
    }
}
