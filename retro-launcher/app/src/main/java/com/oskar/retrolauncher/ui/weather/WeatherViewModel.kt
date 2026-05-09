package com.oskar.retrolauncher.ui.weather

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.data.weather.WeatherSnapshot
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {

    private val _state = MutableLiveData<WeatherSnapshot?>()
    val state: LiveData<WeatherSnapshot?> = _state

    init {
        viewModelScope.launch {
            App.weather.state.collect { _state.value = it }
        }
        // Trigger an immediate refresh on first GPS fix
        viewModelScope.launch {
            App.location.last.combine(App.weather.state) { loc, _ -> loc }.collect { loc ->
                if (loc != null && shouldRefresh(_state.value)) {
                    App.weather.refresh(loc.lat, loc.lon)
                }
            }
        }
    }

    private fun shouldRefresh(snap: WeatherSnapshot?): Boolean {
        if (snap == null) return true
        val ageMin = (System.currentTimeMillis() - snap.asOfMs) / 60_000
        return ageMin >= 5
    }
}
