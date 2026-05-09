package com.oskar.retrolauncher.data.location

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * 1D Kalman filter over GPS speed (spec section 11.2).
 *
 * - Process noise σ = 0.5 m/s² → Q = 0.25 (m/s)²
 * - Measurement variance R scales linearly with the sample's `accuracy` field
 * - Falls back to a 5-sample moving average when `accuracy > 20 m`
 * - Two consecutive raw zeros snap output to 0 (kills GPS jitter when parked)
 * - Below `threshold` the filtered output is clamped to 0
 */
class SpeedFilter(
    var threshold: Float = 0f,
    private val processNoiseStdMs2: Float = 0.5f,
    private val accuracyFallbackM: Float = 20f,
) {

    private val _flow = MutableSharedFlow<Float>(replay = 1, extraBufferCapacity = 1)
    val flow: SharedFlow<Float> = _flow

    private var x: Float = 0f
    private var p: Float = 1f
    private var initialized = false
    private val maBuffer = ArrayDeque<Float>(MA_SIZE)
    private var consecutiveZeros = 0

    fun update(sample: LocationSample): Float {
        val raw = sample.speedMs
        val acc = effectiveAccuracy(sample.accuracy)

        // MA buffer always tracks raw samples so a switch into the fallback
        // path has history to average over.
        pushToMA(raw)

        if (raw == 0f) {
            consecutiveZeros++
            if (consecutiveZeros >= STATIONARY_ZERO_COUNT) {
                hardReset()
                emit(0f)
                return 0f
            }
        } else {
            consecutiveZeros = 0
        }

        val filtered = if (acc > accuracyFallbackM) movingAverage() else kalmanStep(raw, acc)
        val clamped = if (filtered < threshold) 0f else filtered
        emit(clamped)
        return clamped
    }

    fun reset() {
        hardReset()
        consecutiveZeros = 0
    }

    private fun kalmanStep(raw: Float, accuracy: Float): Float {
        val r = (accuracy / R_SCALE).coerceAtLeast(R_MIN)
        if (!initialized) {
            x = raw
            p = r.coerceAtLeast(R_MIN)
            initialized = true
            return x
        }
        // Predict (constant-velocity model).
        p += processNoiseStdMs2 * processNoiseStdMs2
        // Update.
        val k = p / (p + r)
        x += k * (raw - x)
        p = (1f - k) * p
        return x
    }

    private fun movingAverage(): Float {
        var sum = 0f
        for (v in maBuffer) sum += v
        return sum / maBuffer.size
    }

    private fun pushToMA(raw: Float) {
        if (maBuffer.size == MA_SIZE) maBuffer.removeFirst()
        maBuffer.addLast(raw)
    }

    private fun hardReset() {
        x = 0f
        p = 1f
        initialized = false
        maBuffer.clear()
    }

    private fun effectiveAccuracy(accuracy: Float): Float =
        if (accuracy.isNaN() || accuracy < 0f) Float.MAX_VALUE else accuracy

    private fun emit(value: Float) {
        _flow.tryEmit(value)
    }

    companion object {
        private const val MA_SIZE = 5
        private const val R_SCALE = 5f
        private const val R_MIN = 1e-3f
        private const val STATIONARY_ZERO_COUNT = 2
    }
}
