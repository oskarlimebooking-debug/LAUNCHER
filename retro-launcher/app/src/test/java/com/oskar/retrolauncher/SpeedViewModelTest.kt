package com.oskar.retrolauncher

import com.oskar.retrolauncher.data.location.LocationSample
import com.oskar.retrolauncher.data.prefs.Units
import com.oskar.retrolauncher.ui.speed.SpeedUiState
import com.oskar.retrolauncher.ui.speed.produceSpeedUiState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * T1.14 ACs covered (via the pure transform that the VM uses to fold
 * `(LocationSample?, Units, thresholdKmh)` into `SpeedUiState`):
 *
 *   - AC2 (1 Hz): the VM forwards every sample to `produceSpeedUiState` —
 *     verified structurally; the transform is exercised directly here.
 *   - AC3 (unit toggle): metric/imperial branches each yield distinct
 *     display + label.
 *   - AC4 (no fix → zero): null sample yields display=0 and kmh=0.
 */
class SpeedViewModelTest {

    @Test
    fun `null sample yields zero speed and the current unit label`() {
        val s = produceSpeedUiState(sample = null, units = Units.METRIC, thresholdKmh = 50f)
        assertEquals(0f, s.kmh, 0.0001f)
        assertEquals(0f, s.display, 0.0001f)
        assertEquals("km/h", s.unitLabel)
        assertEquals(Units.METRIC, s.units)
        assertEquals(50f, s.thresholdKmh, 0.0001f)
    }

    @Test
    fun `metric converts m_s to km_h for both display and kmh`() {
        val sample = locSample(speedMs = 10f) // 36 km/h
        val s = produceSpeedUiState(sample, Units.METRIC, thresholdKmh = 50f)
        assertEquals(36f, s.kmh, 0.001f)
        assertEquals(36f, s.display, 0.001f)
        assertEquals("km/h", s.unitLabel)
    }

    @Test
    fun `imperial converts m_s to mph for display while kmh stays internal`() {
        val sample = locSample(speedMs = 10f) // 36 km/h ≈ 22.37 mph
        val s = produceSpeedUiState(sample, Units.IMPERIAL, thresholdKmh = 50f)
        assertEquals(36f, s.kmh, 0.001f)
        assertEquals(22.3694f, s.display, 0.01f)
        assertEquals("mph", s.unitLabel)
    }

    @Test
    fun `threshold is passed through unchanged so the speedometer can color-band`() {
        val sample = locSample(speedMs = 5f)
        val s = produceSpeedUiState(sample, Units.METRIC, thresholdKmh = 80f)
        assertEquals(80f, s.thresholdKmh, 0.0001f)
    }

    @Test
    fun `SpeedUiState supports value-equality via data class`() {
        val a = SpeedUiState(display = 36f, kmh = 36f, units = Units.METRIC, unitLabel = "km/h", thresholdKmh = 50f)
        val b = a.copy()
        assertEquals(a, b)
    }

    private fun locSample(speedMs: Float, accuracy: Float = 5f): LocationSample =
        LocationSample(0.0, 0.0, speedMs, 0f, 0L, accuracy)
}
