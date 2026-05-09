package com.oskar.retrolauncher.util

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette

/**
 * Async dominant-colour extraction backed by AndroidX Palette.
 *
 * Palette runs the swatch generation on a background executor; the [onResult]
 * callback fires on the main thread. When extraction fails (`Palette.from`
 * returns no swatches — e.g. a fully transparent or 1×1 bitmap) the [fallback]
 * is delivered verbatim, so callers can plug in the theme's `colorSurface`
 * (T1.18 AC4).
 */
fun Bitmap.dominantColorAsync(
    @ColorInt fallback: Int,
    onResult: (Int) -> Unit,
) {
    Palette.from(this).generate { palette ->
        val swatch = palette?.dominantSwatch
        val out = if (swatch != null) swatch.rgb.darkened(0.4f) else fallback
        onResult(out)
    }
}

fun Int.darkened(factor: Float): Int {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this, hsl)
    hsl[2] = (hsl[2] * (1 - factor)).coerceIn(0f, 1f)
    return ColorUtils.HSLToColor(hsl)
}
