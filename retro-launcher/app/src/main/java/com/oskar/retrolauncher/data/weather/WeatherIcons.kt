package com.oskar.retrolauncher.data.weather

import androidx.annotation.DrawableRes
import com.oskar.retrolauncher.R

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
