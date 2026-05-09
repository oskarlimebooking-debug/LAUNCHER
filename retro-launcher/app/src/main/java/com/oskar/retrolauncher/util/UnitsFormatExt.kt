package com.oskar.retrolauncher.util

import com.oskar.retrolauncher.data.prefs.Units
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun Float.metersPerSecondToDisplay(units: Units): Float = when (units) {
    Units.METRIC   -> this * 3.6f
    Units.IMPERIAL -> this * 2.23694f
}

fun Double.metersPerSecondToDisplay(units: Units): Double = when (units) {
    Units.METRIC   -> this * 3.6
    Units.IMPERIAL -> this * 2.23694
}

fun Double.tempCToDisplay(units: Units): Double = when (units) {
    Units.METRIC   -> this
    Units.IMPERIAL -> this * 9.0 / 5.0 + 32.0
}

fun Double.metersToDisplay(units: Units): String = when (units) {
    Units.METRIC -> {
        if (this >= 1000.0) String.format(Locale.US, "%.1fkm", this / 1000.0)
        else String.format(Locale.US, "%.0fm", this)
    }
    Units.IMPERIAL -> {
        val miles = this / 1609.344
        if (miles >= 0.1) String.format(Locale.US, "%.1fmi", miles)
        else String.format(Locale.US, "%.0fft", this * 3.28084)
    }
}

fun Long.formatDurationShort(): String {
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(this)
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return when {
        hours > 0 -> String.format(Locale.US, "%dh%02dm", hours, minutes)
        minutes > 0 -> String.format(Locale.US, "%dm%02ds", minutes, seconds)
        else -> String.format(Locale.US, "%ds", seconds)
    }
}

fun Long.formatHM(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(this))

fun Long.formatHMHM(endMs: Long): String =
    "%s - %s".format(formatHM(), endMs.formatHM())

fun Long.formatYMD(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(this))
