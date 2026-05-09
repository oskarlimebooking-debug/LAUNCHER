package com.oskar.retrolauncher.ui.weather

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.prefs.Units
import com.oskar.retrolauncher.data.weather.iconResForCondition
import com.oskar.retrolauncher.util.tempCToDisplay

class WeatherFragment : Fragment(R.layout.fragment_weather) {

    private val vm: WeatherViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val icon = view.findViewById<ImageView>(R.id.icon)
        val temp = view.findViewById<TextView>(R.id.temp)
        val high = view.findViewById<TextView>(R.id.high)
        val low = view.findViewById<TextView>(R.id.low)
        val city = view.findViewById<TextView>(R.id.city)

        vm.state.observe(viewLifecycleOwner) { snap ->
            if (snap == null) {
                temp.text = "—"
                high.text = ""
                low.text = ""
                city.text = getString(R.string.weather_loading)
                icon.setImageResource(R.drawable.ic_unknown)
                return@observe
            }
            val units = App.settings.units
            temp.text = "%.0f".format(snap.tempC.tempCToDisplay(units))
            high.text = "%.0f°".format(snap.highC.tempCToDisplay(units))
            low.text = "%.0f°".format(snap.lowC.tempCToDisplay(units))
            city.text = if (units == Units.METRIC) snap.city else snap.city
            icon.setImageResource(iconResForCondition(snap.iconId))
        }
    }
}
