package com.oskar.retrolauncher.ui.weather

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.prefs.SpeedUnit
import com.oskar.retrolauncher.data.prefs.TempUnit
import com.oskar.retrolauncher.data.weather.WeatherSnapshot
import com.oskar.retrolauncher.data.weather.iconResForCode
import java.util.Locale
import kotlin.math.roundToInt

private const val PLACEHOLDER = "—"

class WeatherFragment : Fragment(R.layout.fragment_weather) {

    private val vm: WeatherViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val icon = view.findViewById<ImageView>(R.id.icon)
        val temp = view.findViewById<TextView>(R.id.temp)
        val condition = view.findViewById<TextView>(R.id.condition)
        val feelsLike = view.findViewById<TextView>(R.id.feels_like)
        val wind = view.findViewById<TextView>(R.id.wind)
        val high = view.findViewById<TextView>(R.id.high)
        val low = view.findViewById<TextView>(R.id.low)
        val city = view.findViewById<TextView>(R.id.city)

        vm.state.observe(viewLifecycleOwner) { snap ->
            if (snap == null) {
                renderPlaceholder(icon, temp, condition, feelsLike, wind, high, low, city)
                return@observe
            }
            renderSnapshot(snap, icon, temp, condition, feelsLike, wind, high, low, city)
        }
    }

    private fun renderPlaceholder(
        icon: ImageView,
        temp: TextView,
        condition: TextView,
        feelsLike: TextView,
        wind: TextView,
        high: TextView,
        low: TextView,
        city: TextView,
    ) {
        temp.text = PLACEHOLDER
        condition.text = getString(R.string.weather_loading)
        feelsLike.text = ""
        wind.text = ""
        high.text = ""
        low.text = ""
        city.text = ""
        icon.setImageResource(R.drawable.ic_unknown)
    }

    private fun renderSnapshot(
        snap: WeatherSnapshot,
        icon: ImageView,
        temp: TextView,
        condition: TextView,
        feelsLike: TextView,
        wind: TextView,
        high: TextView,
        low: TextView,
        city: TextView,
    ) {
        val tempUnit = App.settings.tempUnit
        val speedUnit = App.settings.speedUnit
        temp.text = formatTemp(snap.tempC, tempUnit)
        high.text = formatTemp(snap.highC, tempUnit)
        low.text = formatTemp(snap.lowC, tempUnit)
        condition.text = snap.condition.replaceFirstChar { it.titlecase(Locale.getDefault()) }
        feelsLike.text = getString(
            R.string.weather_feels_like_fmt,
            formatTemp(snap.feelsLikeC, tempUnit),
        )
        wind.text = formatWindSpeed(snap.windMs, speedUnit)
        city.text = snap.city
        icon.setImageResource(iconResForCode(snap.iconCode))
    }
}

internal fun formatTemp(tempC: Double, unit: TempUnit): String {
    val display = when (unit) {
        TempUnit.CELSIUS -> tempC
        TempUnit.FAHRENHEIT -> tempC * 9.0 / 5.0 + 32.0
    }
    val suffix = if (unit == TempUnit.CELSIUS) "C" else "F"
    return "${display.roundToInt()}°$suffix"
}

internal fun formatWindSpeed(ms: Double, unit: SpeedUnit): String {
    val (display, label) = when (unit) {
        SpeedUnit.METERS_PER_SECOND -> ms to "m/s"
        SpeedUnit.KILOMETERS_PER_HOUR -> ms * 3.6 to "km/h"
        SpeedUnit.MILES_PER_HOUR -> ms * 2.23694 to "mph"
    }
    return "${display.roundToInt()} $label"
}
