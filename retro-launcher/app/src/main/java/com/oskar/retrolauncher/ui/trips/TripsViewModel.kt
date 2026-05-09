package com.oskar.retrolauncher.ui.trips

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.data.trip.TripEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * View-model for the heatmap-driven Trips screen. Surfaces:
 *   - [recentTrips]: last 91 days of trips (drives the heatmap).
 *   - [dayTrips]: trips that began on [selectedDayMs] (drives the list).
 *   - [distanceByDay]: per-day total distance, keyed by start-of-day millis.
 */
class TripsViewModel : ViewModel() {

    private val _selectedDayMs = MutableStateFlow(startOfTodayMs())
    val selectedDayMs: kotlinx.coroutines.flow.StateFlow<Long> = _selectedDayMs

    private val _recentTrips = MutableLiveData<List<TripEntity>>(emptyList())
    val recentTrips: LiveData<List<TripEntity>> = _recentTrips

    private val _dayTrips = MutableLiveData<List<TripEntity>>(emptyList())
    val dayTrips: LiveData<List<TripEntity>> = _dayTrips

    private val _distanceByDay = MutableLiveData<Map<Long, Double>>(emptyMap())
    val distanceByDay: LiveData<Map<Long, Double>> = _distanceByDay

    init {
        viewModelScope.launch(Dispatchers.Default) {
            App.trips.recentTrips.combine(_selectedDayMs) { trips, day -> trips to day }
                .collect { (trips, day) ->
                    _recentTrips.postValue(trips)
                    _distanceByDay.postValue(buildDistanceByDay(trips))
                    _dayTrips.postValue(trips.filter { sameLocalDay(it.startMs, day) })
                }
        }
    }

    fun selectDay(dayStartMs: Long) {
        _selectedDayMs.value = dayStartMs
    }

    fun delete(trip: TripEntity) {
        viewModelScope.launch(Dispatchers.IO) { App.trips.delete(trip) }
    }

    private fun buildDistanceByDay(trips: List<TripEntity>): Map<Long, Double> {
        if (trips.isEmpty()) return emptyMap()
        val out = HashMap<Long, Double>()
        for (t in trips) {
            val key = startOfDayMs(t.startMs)
            out[key] = (out[key] ?: 0.0) + t.distanceM
        }
        return out
    }

    private fun sameLocalDay(ms: Long, dayStartMs: Long): Boolean {
        val end = dayStartMs + TimeUnit.DAYS.toMillis(1)
        return ms in dayStartMs until end
    }

    private companion object {
        fun startOfTodayMs(): Long = startOfDayMs(System.currentTimeMillis())

        fun startOfDayMs(ms: Long): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = ms
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }
    }
}
