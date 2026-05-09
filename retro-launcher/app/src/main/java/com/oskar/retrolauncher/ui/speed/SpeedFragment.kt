package com.oskar.retrolauncher.ui.speed

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.oskar.retrolauncher.R

/**
 * Speedometer page. Binds [SpeedViewModel.state] to a [SpeedometerView].
 *
 * Subscribes via `viewLifecycleOwner` so detach/attach cycles drop the
 * observer cleanly — no LiveData reference held by a destroyed view (AC5).
 */
class SpeedFragment : Fragment(R.layout.fragment_speed) {

    private val vm: SpeedViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val gauge = view.findViewById<SpeedometerView>(R.id.speedometer)
        vm.state.observe(viewLifecycleOwner) { state ->
            gauge.thresholdKmh = state.thresholdKmh
            gauge.displayUnits = state.units
            gauge.setSpeed(state.kmh, animated = true)
        }
    }
}
