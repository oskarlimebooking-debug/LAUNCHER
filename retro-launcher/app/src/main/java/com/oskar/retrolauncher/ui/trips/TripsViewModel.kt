package com.oskar.retrolauncher.ui.trips

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.data.trip.TripEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class TripsViewModel : ViewModel() {

    data class MonthSelection(val year: Int, val month: Int) {
        fun rangeMs(): Pair<Long, Long> {
            val from = Calendar.getInstance().apply {
                clear(); set(year, month, 1, 0, 0)
            }.timeInMillis
            val to = Calendar.getInstance().apply {
                timeInMillis = from
                add(Calendar.MONTH, 1)
            }.timeInMillis
            return from to to
        }
    }

    private val now = Calendar.getInstance()
    private val _month = MutableStateFlow(MonthSelection(now.get(Calendar.YEAR), now.get(Calendar.MONTH)))
    private val _selectedDay = MutableStateFlow(now.get(Calendar.DAY_OF_MONTH))

    private val _monthTrips = MutableLiveData<List<TripEntity>>(emptyList())
    val monthTrips: LiveData<List<TripEntity>> = _monthTrips

    private val _dayTrips = MutableLiveData<List<TripEntity>>(emptyList())
    val dayTrips: LiveData<List<TripEntity>> = _dayTrips

    val month: kotlinx.coroutines.flow.StateFlow<MonthSelection> = _month
    val selectedDay: kotlinx.coroutines.flow.StateFlow<Int> = _selectedDay

    init {
        // Stream of trips for the currently selected month
        val monthFlow = _month.flatMapLatest { sel ->
            val (from, to) = sel.rangeMs()
            App.trips.byRange(from, to)
        }
        viewModelScope.launch(Dispatchers.Default) {
            // Combine month-trips + selected day so day list updates whenever either changes.
            monthFlow.combine(_selectedDay) { trips, day -> trips to day }
                .combine(_month) { (trips, day), sel -> Triple(trips, day, sel) }
                .collect { (trips, day, sel) ->
                    _monthTrips.postValue(trips)
                    _dayTrips.postValue(trips.filter { trip ->
                        val cal = Calendar.getInstance().apply { timeInMillis = trip.startMs }
                        cal.get(Calendar.YEAR) == sel.year &&
                            cal.get(Calendar.MONTH) == sel.month &&
                            cal.get(Calendar.DAY_OF_MONTH) == day
                    })
                }
        }
    }

    fun selectDay(day: Int) { _selectedDay.value = day }

    fun delete(trip: TripEntity) {
        viewModelScope.launch(Dispatchers.IO) { App.trips.delete(trip) }
    }
}
