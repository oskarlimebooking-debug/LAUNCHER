package com.oskar.retrolauncher.ui.trips

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.data.trip.TripEntity
import com.oskar.retrolauncher.data.trip.TripRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Surfaces three things to [TripsFragment]:
 *
 *  - [displayed]: a [Display] payload describing what the top card should show.
 *    Either the in-progress trip (with a 5-minute rolling buffer of live
 *    samples) or the most recent finished trip (with its persisted points
 *    loaded lazily).
 *  - [recentTrips]: 30-day window of finished trips for the bottom strip.
 *  - [weekStats]: total distance + trip count over the last 7 days.
 *
 * The view model also owns a per-trip sparkline cache used by
 * [TripCardAdapter]: [sparkValues] returns whatever is already loaded, or an
 * empty array (in which case [requestSpark] kicks off an IO fetch and
 * publishes [sparkInvalidations] when ready).
 */
class TripsViewModel : ViewModel() {

    enum class Mode { IDLE, RECORDING, LAST }

    data class Display(
        val mode: Mode,
        val trip: TripEntity?,
        val samples: List<Pair<Long, Float>>,
    )

    data class WeekStats(val totalM: Double, val count: Int)

    private val _displayed = MutableStateFlow(Display(Mode.IDLE, null, emptyList()))
    val displayed: StateFlow<Display> = _displayed

    private val _recentTrips = MutableLiveData<List<TripEntity>>(emptyList())
    val recentTrips: LiveData<List<TripEntity>> = _recentTrips

    private val _weekStats = MutableLiveData(WeekStats(0.0, 0))
    val weekStats: LiveData<WeekStats> = _weekStats

    /** Bumped each time the spark cache gains a new entry — observer rebinds the strip. */
    private val _sparkInvalidations = MutableStateFlow(0L)
    val sparkInvalidations: StateFlow<Long> = _sparkInvalidations

    private val sparkCache = HashMap<Long, FloatArray>()
    private val sparkInflight = HashSet<Long>()
    private val liveBuffer = ArrayDeque<Pair<Long, Float>>()

    init {
        // Stream finished trips for the strip + week stats.
        viewModelScope.launch(Dispatchers.Default) {
            App.trips.recentTrips.collect { trips ->
                val sorted = trips.sortedByDescending { it.startMs }
                _recentTrips.postValue(sorted)
                _weekStats.postValue(computeWeekStats(sorted))
                refreshLastTripDisplayIfIdle(sorted)
            }
        }

        // Combine recorder state + active live trip + raw GPS samples.
        viewModelScope.launch(Dispatchers.Default) {
            App.tripRecorder.state.combine(App.tripRecorder.activeTrip) { s, t -> s to t }
                .collect { (state, trip) ->
                    when (state) {
                        TripRecorder.State.RECORDING, TripRecorder.State.DETECTING ->
                            _displayed.value = Display(Mode.RECORDING, trip, liveBuffer.toList())
                        TripRecorder.State.STOPPING, TripRecorder.State.IDLE -> {
                            liveBuffer.clear()
                            refreshLastTripDisplayIfIdle(_recentTrips.value.orEmpty())
                        }
                    }
                }
        }

        // While recording, accumulate live samples into the rolling buffer.
        viewModelScope.launch(Dispatchers.Default) {
            App.location.samples.collect { s ->
                if (App.tripRecorder.state.value != TripRecorder.State.RECORDING &&
                    App.tripRecorder.state.value != TripRecorder.State.DETECTING) return@collect
                liveBuffer.addLast(s.tsMs to s.speedMs)
                trimBuffer(s.tsMs)
                _displayed.value = _displayed.value.copy(samples = liveBuffer.toList())
            }
        }
    }

    /** Latest snapshot of the spark for the given trip, or empty if not yet cached. */
    fun sparkValues(trip: TripEntity): FloatArray =
        sparkCache[trip.id] ?: FloatArray(0)

    /** Schedule a background load of the trip's speed profile for its sparkline. */
    fun requestSpark(trip: TripEntity) {
        if (sparkCache.containsKey(trip.id)) return
        if (!sparkInflight.add(trip.id)) return
        viewModelScope.launch(Dispatchers.IO) {
            val pts = App.trips.pointsFor(trip.id)
            // Downsample to ~24 buckets so the sparkline reads as a shape, not noise.
            val target = 24
            val out = if (pts.size <= target) {
                FloatArray(pts.size) { pts[it].speedMs }
            } else {
                val arr = FloatArray(target)
                val stride = pts.size.toFloat() / target
                for (i in 0 until target) {
                    val idx = (i * stride).toInt().coerceAtMost(pts.size - 1)
                    arr[i] = pts[idx].speedMs
                }
                arr
            }
            sparkCache[trip.id] = out
            sparkInflight.remove(trip.id)
            _sparkInvalidations.value = System.currentTimeMillis()
        }
    }

    fun delete(trip: TripEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            App.trips.delete(trip)
            sparkCache.remove(trip.id)
        }
    }

    // -- internals --------------------------------------------------------

    private fun refreshLastTripDisplayIfIdle(trips: List<TripEntity>) {
        if (App.tripRecorder.state.value == TripRecorder.State.RECORDING) return
        val last = trips.firstOrNull()
        if (last == null) {
            _displayed.value = Display(Mode.IDLE, null, emptyList())
            return
        }
        // Lazy-load the trip's full speed-vs-time profile for the chart.
        viewModelScope.launch(Dispatchers.IO) {
            val pts = App.trips.pointsFor(last.id)
            val samples = pts.map { it.tsMs to it.speedMs }
            _displayed.value = Display(Mode.LAST, last, samples)
        }
    }

    private fun trimBuffer(nowMs: Long) {
        val cutoff = nowMs - LIVE_WINDOW_MS
        while (liveBuffer.isNotEmpty() && liveBuffer.first().first < cutoff) {
            liveBuffer.removeFirst()
        }
    }

    private fun computeWeekStats(trips: List<TripEntity>): WeekStats {
        val cutoff = System.currentTimeMillis() - WEEK_MS
        val week = trips.filter { it.startMs >= cutoff }
        return WeekStats(week.sumOf { it.distanceM }, week.size)
    }

    private companion object {
        const val LIVE_WINDOW_MS = 5L * 60L * 1000L
        const val WEEK_MS = 7L * 24L * 60L * 60L * 1000L
    }
}
