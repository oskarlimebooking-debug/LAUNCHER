package com.oskar.retrolauncher.data.trip

import com.oskar.retrolauncher.data.location.LocationSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Auto-detects trips from a stream of [LocationSample]s and persists them via [dao].
 *
 * Start: speed > 5 km/h sustained 10 s.
 * End:   speed < 3 km/h sustained 2 min.
 */
class TripRecorder(
    private val scope: CoroutineScope,
    private val dao: TripDao,
    private val locationFlow: SharedFlow<LocationSample>,
    private val geocoder: suspend (Double, Double) -> String? = { _, _ -> null },
) {
    enum class State { IDLE, RECORDING }

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state

    private val _live = MutableStateFlow<LiveStats?>(null)
    val live: StateFlow<LiveStats?> = _live

    private var startMs = 0L
    private var lastMoveMs = 0L
    private val points = ArrayDeque<LocationSample>()
    private var maxSpeed = 0f
    private var distance = 0.0
    private var enabled = true

    init {
        scope.launch { locationFlow.collect(::onSample) }
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value && _state.value == State.RECORDING) {
            // discard the in-progress trip
            reset()
        }
    }

    private suspend fun onSample(s: LocationSample) {
        if (!enabled) return
        val kmh = s.speedMs * 3.6f
        when (_state.value) {
            State.IDLE -> {
                if (kmh > 5f) {
                    if (lastMoveMs == 0L) lastMoveMs = s.tsMs
                    if (s.tsMs - lastMoveMs > 10_000) start(s)
                } else lastMoveMs = 0L
            }
            State.RECORDING -> {
                accumulate(s)
                if (kmh < 3f) {
                    if (lastMoveMs == 0L) lastMoveMs = s.tsMs
                    if (s.tsMs - lastMoveMs > 120_000) end(s)
                } else lastMoveMs = 0L
            }
        }
    }

    private fun start(s: LocationSample) {
        _state.value = State.RECORDING
        startMs = s.tsMs
        maxSpeed = 0f
        distance = 0.0
        lastMoveMs = 0L
        points.clear()
        points += s
        emitLive(s.tsMs)
    }

    private fun accumulate(s: LocationSample) {
        val prev = points.lastOrNull() ?: run { points += s; emitLive(s.tsMs); return }
        val dist = haversine(prev.lat, prev.lon, s.lat, s.lon)
        if (dist > 5.0) {
            distance += dist
            points += s
        }
        if (s.speedMs > maxSpeed) maxSpeed = s.speedMs
        emitLive(s.tsMs)
    }

    private fun emitLive(tsMs: Long) {
        val durMs = (tsMs - startMs).coerceAtLeast(0)
        val avg = if (durMs > 0) distance / (durMs / 1000.0) else 0.0
        _live.value = LiveStats(
            startMs = startMs,
            durationMs = durMs,
            distanceM = distance,
            avgSpeedMs = avg,
            maxSpeedMs = maxSpeed.toDouble(),
        )
    }

    private suspend fun end(s: LocationSample) {
        if (points.size < 2 || distance < 50) {
            // Discard noise — < 50 m drives are GPS jitter.
            reset()
            return
        }
        val first = points.first()
        val last = points.last()
        val durSec = ((s.tsMs - startMs) / 1000.0).coerceAtLeast(1.0)
        val avg = distance / durSec
        val tripId = dao.insert(
            TripEntity(
                startMs = startMs,
                endMs = s.tsMs,
                distanceM = distance,
                avgSpeedMs = avg,
                maxSpeedMs = maxSpeed.toDouble(),
                startLabel = geocoder(first.lat, first.lon),
                endLabel = geocoder(last.lat, last.lon),
            )
        )
        dao.insertPoints(points.map {
            TripPoint(tripId = tripId, tsMs = it.tsMs, lat = it.lat, lon = it.lon, speedMs = it.speedMs)
        })
        reset()
    }

    private fun reset() {
        _state.value = State.IDLE
        _live.value = null
        startMs = 0L
        lastMoveMs = 0L
        points.clear()
        maxSpeed = 0f
        distance = 0.0
    }

    private fun haversine(la1: Double, lo1: Double, la2: Double, lo2: Double): Double {
        val r = 6_371_000.0
        val dLa = Math.toRadians(la2 - la1)
        val dLo = Math.toRadians(lo2 - lo1)
        val a = sin(dLa / 2).pow(2) +
            cos(Math.toRadians(la1)) * cos(Math.toRadians(la2)) * sin(dLo / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    data class LiveStats(
        val startMs: Long,
        val durationMs: Long,
        val distanceM: Double,
        val avgSpeedMs: Double,
        val maxSpeedMs: Double,
    )
}
