package com.oskar.retrolauncher.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette

private val DEFAULT_FALLBACK = Color.parseColor("#1F1F1F")

/**
 * Async dominant-color extraction. The bitmap is sampled on a background thread
 * by Palette; the callback fires on the main thread.
 */
fun Bitmap.dominantColorAsync(onResult: (Int) -> Unit) {
    Palette.from(this).generate { p ->
        val raw = p?.getDominantColor(DEFAULT_FALLBACK) ?: DEFAULT_FALLBACK
        onResult(raw.darkened(0.4f))
    }
}

fun Int.darkened(factor: Float): Int {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this, hsl)
    hsl[2] = (hsl[2] * (1 - factor)).coerceIn(0f, 1f)
    return ColorUtils.HSLToColor(hsl)
}
