package com.oskar.retrolauncher.ui.status

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.data.prefs.Units
import com.oskar.retrolauncher.data.trip.TripRecorder
import com.oskar.retrolauncher.util.formatDurationShort
import com.oskar.retrolauncher.util.metersPerSecondToDisplay
import com.oskar.retrolauncher.util.metersToDisplay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class StatusTripViewModel : ViewModel() {

    private val _statText = MutableLiveData<String?>(null)
    val statText: LiveData<String?> = _statText

    init {
        viewModelScope.launch {
            App.tripRecorder.state.combine(App.tripRecorder.live) { state, live ->
                if (state != TripRecorder.State.RECORDING || live == null) null else {
                    val units = App.settings.units
                    val avgUnitLabel = if (units == Units.METRIC) "kph" else "mph"
                    "Trip ${live.durationMs.formatDurationShort()} · " +
                        live.distanceM.metersToDisplay(units) + " · " +
                        "%.0f $avgUnitLabel avg".format(live.avgSpeedMs.metersPerSecondToDisplay(units))
                }
            }.collect { _statText.value = it }
        }
    }
}
