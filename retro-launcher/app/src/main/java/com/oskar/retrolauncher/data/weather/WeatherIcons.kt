package com.oskar.retrolauncher.data.weather

import androidx.annotation.DrawableRes
import com.oskar.retrolauncher.R

/**
 * Numeric OWM condition id (200..804) → local vector drawable.
 * Kept for legacy callers and the int-id snapshot path; `iconResForCode` is the
 * preferred 18-code string lookup per spec section 10.6.
 */
@DrawableRes
fun iconResForCondition(owmId: Int): Int = when (owmId) {
    in 200..232 -> R.drawable.ic_thunder
    in 300..321 -> R.drawable.ic_drizzle
    in 500..531 -> R.drawable.ic_rain
    in 600..622 -> R.drawable.ic_snow
    in 700..781 -> R.drawable.ic_fog
    800 -> R.drawable.ic_sun
    in 801..804 -> R.drawable.ic_cloud
    else -> R.drawable.ic_unknown
}

/**
 * OWM `icon` string code (`01d`, `01n`, … 18 total) → local vector drawable.
 * Day/night variants share the same drawable until we ship moon glyphs.
 *
 * Reference: https://openweathermap.org/weather-conditions
 */
@DrawableRes
fun iconResForCode(code: String): Int = when (code) {
    "01d", "01n" -> R.drawable.ic_sun
    "02d", "02n", "03d", "03n", "04d", "04n" -> R.drawable.ic_cloud
    "09d", "09n" -> R.drawable.ic_drizzle
    "10d", "10n" -> R.drawable.ic_rain
    "11d", "11n" -> R.drawable.ic_thunder
    "13d", "13n" -> R.drawable.ic_snow
    "50d", "50n" -> R.drawable.ic_fog
    else -> R.drawable.ic_unknown
}
