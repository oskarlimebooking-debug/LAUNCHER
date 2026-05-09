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
        // `internal` — WeatherDto is module-private, so this transform is too.
        // Repository (same module) calls into it; UI never sees a DTO.
        internal fun from(dto: WeatherDto, city: String, asOf: Long): WeatherSnapshot {
            val condition = dto.weather.firstOrNull()
            return WeatherSnapshot(
                tempC = dto.main.temp,
                highC = dto.main.tempMax ?: dto.main.temp,
                lowC = dto.main.tempMin ?: dto.main.temp,
                iconId = condition?.id ?: 800,
                condition = condition?.description.orEmpty(),
                city = city,
                asOfMs = asOf,
            )
        }
    }
}
