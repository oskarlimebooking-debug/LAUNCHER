package com.oskar.retrolauncher.util

import com.oskar.retrolauncher.data.prefs.Units
import java.util.Locale

/**
 * T1.10 spec section-17 format helpers. Output strings are stable across
 * locales (Locale.US for decimal separator) so screenshots and tests don't
 * shift on the head unit's locale.
 *
 * Unit-conversion helpers (m/s ↔ km/h or mph, °C ↔ °F, m ↔ miles/ft) live in
 * `UnitsFormatExt.kt`.
 */

fun formatDistance(meters: Double, unit: Units): String = when (unit) {
    Units.METRIC -> {
        if (meters >= 1000.0) String.format(Locale.US, "%.1f km", meters / 1000.0)
        else String.format(Locale.US, "%.0f m", meters)
    }
    Units.IMPERIAL -> {
        val miles = meters / 1609.344
        if (miles >= 0.1) String.format(Locale.US, "%.1f mi", miles)
        else String.format(Locale.US, "%.0f ft", meters * 3.28084)
    }
}

fun formatDuration(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    val h = safe / 3600
    val m = (safe % 3600) / 60
    val s = safe % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

fun formatSpeed(metersPerSec: Double, unit: Units): String {
    val (value, label) = when (unit) {
        Units.METRIC -> metersPerSec * 3.6 to "km/h"
        Units.IMPERIAL -> metersPerSec * 2.23694 to "mph"
    }
    return String.format(Locale.US, "%.1f %s", value, label)
}
