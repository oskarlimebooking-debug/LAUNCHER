package com.oskar.retrolauncher.ui.speed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.data.location.LocationSample
import com.oskar.retrolauncher.data.prefs.SettingsStore
import com.oskar.retrolauncher.data.prefs.Units
import com.oskar.retrolauncher.util.metersPerSecondToDisplay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * UI state for the speedometer.
 *
 * - [kmh] is the canonical speed in km/h, drives the colored arc / sweep.
 * - [display] is the same speed converted to the user's chosen [units].
 * - [unitLabel] mirrors [units] so the view doesn't need a `when`.
 * - [thresholdKmh] flows from [SettingsStore.speedThresholdKmh] and keeps the
 *   gauge's colour-band breakpoint in sync without a fragment recreate.
 */
data class SpeedUiState(
    val kmh: Float,
    val display: Float,
    val units: Units,
    val unitLabel: String,
    val thresholdKmh: Float,
)

/**
 * Pure transform from `(LocationSample?, Units, thresholdKmh)` to [SpeedUiState].
 * Extracted out of the VM so it can be unit-tested without coroutine
 * machinery — the VM just folds the latest values into this every emission.
 */
fun produceSpeedUiState(sample: LocationSample?, units: Units, thresholdKmh: Float): SpeedUiState {
    val ms = sample?.speedMs ?: 0f
    val kmh = ms * 3.6f
    return SpeedUiState(
        kmh = kmh,
        display = ms.metersPerSecondToDisplay(units),
        units = units,
        unitLabel = if (units == Units.METRIC) "km/h" else "mph",
        thresholdKmh = thresholdKmh,
    )
}

class SpeedViewModel : ViewModel() {

    private val _state = MutableLiveData(
        produceSpeedUiState(sample = null, units = Units.METRIC, thresholdKmh = 50f),
    )
    val state: LiveData<SpeedUiState> = _state

    init {
        val settings = App.settings
        // AC2: every LocationRepository sample (1 Hz) flows through.
        // AC3: unit OR threshold change re-emits without fragment recreation.
        val settingsKeys = merge(
            settings.changes(SettingsStore.KEY_UNITS),
            settings.changes(SettingsStore.KEY_SPEED_THRESHOLD),
        )
        viewModelScope.launch {
            App.location.last.combine(settingsKeys) { sample, _ -> sample }.collect { sample ->
                _state.value = produceSpeedUiState(sample, settings.units, settings.speedThresholdKmh)
            }
        }
    }
}
