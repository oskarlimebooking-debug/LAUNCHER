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
 * State machine (spec section 12.2):
 *   `IDLE` → `DETECTING` when speed > [tripStartSpeedKmh].
 *   `DETECTING` → `RECORDING` when sustained for [detectDurationMs].
 *   `DETECTING` → `IDLE` when speed drops before the detect window closes.
 *   `RECORDING` → `STOPPING` when speed < [tripStopSpeedKmh] sustained for [stopDurationMs].
 *   `STOPPING` → `IDLE` after the final [TripEntity] insert completes.
 *
 * Samples are aggregated into [bucketDurationMs] buckets so the persisted point list
 * is at most one row per bucket — at 1 Hz GPS this is an 80% write reduction.
 *
 * Partial trips are persisted to [store] on every bucket commit so a process kill
 * mid-trip can be recovered on next construction.
 */
class TripRecorder(
    private val scope: CoroutineScope,
    private val dao: TripDao,
    private val locationFlow: SharedFlow<LocationSample>,
    private val geocoder: suspend (Double, Double) -> String? = { _, _ -> null },
    private val store: TripStateStore = InMemoryTripStateStore(),
    private val tripStartSpeedKmh: Float = DEFAULT_START_KMH,
    private val tripStopSpeedKmh: Float = DEFAULT_STOP_KMH,
    private val detectDurationMs: Long = DEFAULT_DETECT_MS,
    private val stopDurationMs: Long = DEFAULT_STOP_MS,
    private val bucketDurationMs: Long = DEFAULT_BUCKET_MS,
) {
    enum class State { IDLE, DETECTING, RECORDING, STOPPING }

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state

    private val _live = MutableStateFlow<LiveStats?>(null)
    val live: StateFlow<LiveStats?> = _live

    private val _activeTrip = MutableStateFlow<TripEntity?>(null)
    val activeTrip: StateFlow<TripEntity?> = _activeTrip

    private var enabled = true
    private var startMs = 0L
    private var detectStartMs = 0L
    private var stopStartMs = 0L
    private var distance = 0.0
    private var maxSpeed = 0f
    private val buckets = ArrayDeque<TripPointData>()
    private val bucketSamples = ArrayDeque<LocationSample>()
    private var bucketStartMs = 0L
    private var firstSample: LocationSample? = null
    private var lastSampleMs = 0L

    init {
        scope.launch { recoverPendingTrip() }
        scope.launch { locationFlow.collect(::onSample) }
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value && _state.value != State.IDLE) reset()
    }

    private suspend fun onSample(s: LocationSample) {
        if (!enabled) return
        val kmh = s.speedMs * 3.6f
        when (_state.value) {
            State.IDLE -> if (kmh > tripStartSpeedKmh) startDetecting(s)
            State.DETECTING -> handleDetecting(s, kmh)
            State.RECORDING -> handleRecording(s, kmh)
            State.STOPPING -> Unit
        }
    }

    private fun startDetecting(s: LocationSample) {
        _state.value = State.DETECTING
        startMs = s.tsMs
        detectStartMs = s.tsMs
        stopStartMs = 0L
        distance = 0.0
        maxSpeed = s.speedMs
        buckets.clear()
        bucketSamples.clear()
        bucketSamples += s
        bucketStartMs = s.tsMs
        firstSample = s
        lastSampleMs = s.tsMs
    }

    private fun handleDetecting(s: LocationSample, kmh: Float) {
        if (kmh <= tripStartSpeedKmh) {
            reset()
            return
        }
        accumulate(s)
        if (s.tsMs - detectStartMs >= detectDurationMs) {
            _state.value = State.RECORDING
            emitLive(s.tsMs)
            snapshot()
        }
    }

    private suspend fun handleRecording(s: LocationSample, kmh: Float) {
        accumulate(s)
        if (kmh < tripStopSpeedKmh) {
            if (stopStartMs == 0L) stopStartMs = s.tsMs
            if (s.tsMs - stopStartMs >= stopDurationMs) {
                end(s)
                return
            }
        } else {
            stopStartMs = 0L
        }
        emitLive(s.tsMs)
    }

    private fun accumulate(s: LocationSample) {
        if (s.tsMs - bucketStartMs >= bucketDurationMs) {
            commitBucket()
            bucketStartMs = s.tsMs
        }
        bucketSamples += s
        if (s.speedMs > maxSpeed) maxSpeed = s.speedMs
        lastSampleMs = s.tsMs
    }

    private fun commitBucket() {
        val rep = bucketSamples.lastOrNull() ?: return
        val pt = TripPointData(rep.tsMs, rep.lat, rep.lon, rep.speedMs)
        val prev = buckets.lastOrNull()
        if (prev == null) {
            buckets += pt
        } else {
            val d = haversine(prev.lat, prev.lon, pt.lat, pt.lon)
            // GPS jitter floor: ignore sub-5 m bucket-to-bucket movement.
            if (d > 5.0) {
                distance += d
                buckets += pt
            }
        }
        bucketSamples.clear()
        if (_state.value != State.IDLE) snapshot()
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
        _activeTrip.value = if (_state.value == State.RECORDING) {
            TripEntity(
                id = 0L,
                startMs = startMs,
                endMs = tsMs,
                distanceM = distance,
                avgSpeedMs = avg,
                maxSpeedMs = maxSpeed.toDouble(),
                startLabel = null,
                endLabel = null,
            )
        } else {
            null
        }
    }

    private suspend fun end(s: LocationSample) {
        _state.value = State.STOPPING
        commitBucket()
        if (buckets.size < 2 || distance < 50.0) {
            reset()
            return
        }
        persistTrip(
            startMs = startMs,
            endMs = s.tsMs,
            distanceM = distance,
            maxSpeedMs = maxSpeed.toDouble(),
            points = buckets.toList(),
        )
        reset()
    }

    private suspend fun persistTrip(
        startMs: Long,
        endMs: Long,
        distanceM: Double,
        maxSpeedMs: Double,
        points: List<TripPointData>,
    ) {
        val durSec = ((endMs - startMs) / 1000.0).coerceAtLeast(1.0)
        val avg = distanceM / durSec
        val first = points.first()
        val last = points.last()
        val tripId = dao.insert(
            TripEntity(
                startMs = startMs,
                endMs = endMs,
                distanceM = distanceM,
                avgSpeedMs = avg,
                maxSpeedMs = maxSpeedMs,
                startLabel = geocoder(first.lat, first.lon),
                endLabel = geocoder(last.lat, last.lon),
            )
        )
        dao.insertPoints(
            points.map { TripPoint(tripId = tripId, tsMs = it.tsMs, lat = it.lat, lon = it.lon, speedMs = it.speedMs) }
        )
    }

    private suspend fun recoverPendingTrip() {
        val snap = store.load() ?: return
        store.clear()
        if (snap.points.size < 2 || snap.distanceM < 50.0) return
        persistTrip(
            startMs = snap.startMs,
            endMs = snap.lastSampleMs.coerceAtLeast(snap.startMs + 1),
            distanceM = snap.distanceM,
            maxSpeedMs = snap.maxSpeedMs,
            points = snap.points,
        )
    }

    private fun snapshot() {
        val pts = buckets.toList() +
            (bucketSamples.lastOrNull()?.let {
                listOf(TripPointData(it.tsMs, it.lat, it.lon, it.speedMs))
            } ?: emptyList())
        store.save(
            TripSnapshot(
                startMs = startMs,
                lastSampleMs = lastSampleMs,
                distanceM = distance,
                maxSpeedMs = maxSpeed.toDouble(),
                points = pts,
            )
        )
    }

    private fun reset() {
        _state.value = State.IDLE
        _live.value = null
        _activeTrip.value = null
        startMs = 0L
        detectStartMs = 0L
        stopStartMs = 0L
        distance = 0.0
        maxSpeed = 0f
        buckets.clear()
        bucketSamples.clear()
        bucketStartMs = 0L
        firstSample = null
        lastSampleMs = 0L
        store.clear()
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

    private companion object {
        const val DEFAULT_START_KMH = 10f
        const val DEFAULT_STOP_KMH = 2f
        const val DEFAULT_DETECT_MS = 10_000L
        const val DEFAULT_STOP_MS = 60_000L
        const val DEFAULT_BUCKET_MS = 5_000L
    }
}
