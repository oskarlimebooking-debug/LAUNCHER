package com.oskar.retrolauncher

import androidx.recyclerview.widget.DiffUtil
import com.oskar.retrolauncher.data.trip.TripEntity
import com.oskar.retrolauncher.ui.trips.TripsAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * T1.27 AC3: DiffUtil prevents full re-binding on filter changes.
 *
 * Verifies the DiffUtil callback identifies same-ID trips as the same item
 * (so a re-emit with an unchanged trip avoids a re-bind) and detects content
 * changes within a same-ID trip (so distance / time updates do trigger a
 * re-bind).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class TripsAdapterTest {

    private fun trip(
        id: Long,
        startMs: Long = 0L,
        distanceM: Double = 1_000.0,
        avgSpeedMs: Double = 5.0,
        startLabel: String? = null,
        endLabel: String? = null,
    ) = TripEntity(
        id = id,
        startMs = startMs,
        endMs = startMs + 60_000L,
        distanceM = distanceM,
        avgSpeedMs = avgSpeedMs,
        maxSpeedMs = avgSpeedMs * 2,
        startLabel = startLabel,
        endLabel = endLabel,
    )

    private val callback: DiffUtil.ItemCallback<TripEntity> by lazy {
        // The companion DIFF is private; reach it via the adapter for parity.
        val adapter = TripsAdapter()
        val field = androidx.recyclerview.widget.ListAdapter::class.java
            .getDeclaredField("mDiffer").apply { isAccessible = true }
        val differ = field.get(adapter) as androidx.recyclerview.widget.AsyncListDiffer<*>
        val cfgField = androidx.recyclerview.widget.AsyncListDiffer::class.java
            .getDeclaredField("mConfig").apply { isAccessible = true }
        val cfg = cfgField.get(differ) as androidx.recyclerview.widget.AsyncDifferConfig<*>
        @Suppress("UNCHECKED_CAST")
        cfg.diffCallback as DiffUtil.ItemCallback<TripEntity>
    }

    @Test
    fun `areItemsTheSame matches by trip id`() {
        val a = trip(id = 42, distanceM = 1_000.0)
        val b = trip(id = 42, distanceM = 5_000.0)
        assertTrue(callback.areItemsTheSame(a, b))
    }

    @Test
    fun `areItemsTheSame distinguishes different ids`() {
        assertFalse(callback.areItemsTheSame(trip(id = 1), trip(id = 2)))
    }

    @Test
    fun `areContentsTheSame is true for fully equal entities`() {
        val a = trip(id = 1, distanceM = 1_000.0, startLabel = "Home")
        val b = trip(id = 1, distanceM = 1_000.0, startLabel = "Home")
        assertTrue(callback.areContentsTheSame(a, b))
    }

    @Test
    fun `areContentsTheSame is false when distance changes`() {
        val a = trip(id = 1, distanceM = 1_000.0)
        val b = trip(id = 1, distanceM = 1_500.0)
        assertFalse(callback.areContentsTheSame(a, b))
    }

    @Test
    fun `submitList reports current size after diff applies`() {
        val adapter = TripsAdapter()
        adapter.submitList(listOf(trip(id = 1), trip(id = 2), trip(id = 3)))
        assertEquals(3, adapter.itemCount)
    }
}
