package com.oskar.retrolauncher.ui.status

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.prefs.Units
import com.oskar.retrolauncher.ui.settings.SettingsActivity
import com.oskar.retrolauncher.ui.speed.SpeedViewModel
import com.oskar.retrolauncher.util.ClockTicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatusBarFragment : Fragment(R.layout.fragment_status) {

    private val speedVm: SpeedViewModel by viewModels()
    private val tripVm: StatusTripViewModel by viewModels()

    private lateinit var ticker: ClockTicker
    private val timeFmt by lazy { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    private val dateFmt by lazy { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val drawer = view.findViewById<ImageButton>(R.id.drawer_btn)
        val clock = view.findViewById<TextView>(R.id.clock)
        val date = view.findViewById<TextView>(R.id.date)
        val speedNum = view.findViewById<TextView>(R.id.speed_num)
        val speedBar = view.findViewById<ProgressBar>(R.id.speed_bar)
        val tripChip = view.findViewById<View>(R.id.trip_chip_slot) as android.widget.FrameLayout

        drawer.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        speedNum.setOnLongClickListener {
            val current = App.settings.units
            val next = if (current == Units.METRIC) "imperial" else "metric"
            App.prefs.edit().putString("units", next).apply()
            true
        }

        speedVm.state.observe(viewLifecycleOwner) { snap ->
            speedNum.text = "%.0f".format(snap.display)
            speedBar.max = 200
            speedBar.progress = snap.display.toInt().coerceAtMost(200)
        }

        val tripText = TextView(requireContext()).apply {
            setTextColor(resources.getColor(R.color.accent, null))
            textSize = 13f
            setPadding(8, 0, 8, 0)
        }
        tripChip.addView(tripText)

        tripVm.statText.observe(viewLifecycleOwner) { text ->
            if (text.isNullOrEmpty()) {
                tripChip.visibility = View.GONE
            } else {
                tripChip.visibility = View.VISIBLE
                tripText.text = text
            }
        }

        // Handler-based 1Hz ticker (T1.9 AC1).
        ticker = ClockTicker(
            handler = Handler(Looper.getMainLooper()),
            onTick = { nowMs ->
                val d = Date(nowMs)
                clock.text = timeFmt.format(d)
                date.text = dateFmt.format(d)
            },
        )
    }

    override fun onResume() {
        super.onResume()
        ticker.start()
    }

    override fun onPause() {
        ticker.stop()
        super.onPause()
    }
}
