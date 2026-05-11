package com.oskar.retrolauncher

import android.content.Context
import com.oskar.retrolauncher.data.trip.NominatimAddressCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * T1.39 — AC5: addresses fall back to "lat, lon" when the Nominatim cache is cold.
 *
 * The cache lives in a SharedPreferences bucket keyed by a rounded lat/lon pair
 * so two trips ending at nearly the same parking spot share an entry.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class NominatimAddressCacheTest {

    private lateinit var ctx: Context
    private lateinit var cache: NominatimAddressCache

    @Before
    fun setUp() {
        ctx = RuntimeEnvironment.getApplication()
        ctx.getSharedPreferences("nominatim_addr", Context.MODE_PRIVATE).edit().clear().apply()
        cache = NominatimAddressCache(ctx)
    }

    @Test
    fun `cold cache returns null for lookup`() {
        assertNull(cache.lookup(60.1, 24.9))
    }

    @Test
    fun `put then lookup hits same rounded key`() {
        cache.put(60.1234, 24.9876, "Helsinki, Finland")
        assertEquals("Helsinki, Finland", cache.lookup(60.1234, 24.9876))
    }

    @Test
    fun `lookup tolerates small lat lon jitter under rounding precision`() {
        cache.put(60.10001, 24.90001, "Home")
        // A point 5 m away rounds to the same key at 4 decimal places (≈11 m at the equator)
        assertEquals("Home", cache.lookup(60.10004, 24.90004))
    }

    @Test
    fun `labelOrFallback returns cached label`() {
        cache.put(50.0, 10.0, "Frankfurt")
        assertEquals("Frankfurt", cache.labelOrFallback(50.0, 10.0))
    }

    @Test
    fun `labelOrFallback returns formatted lat lon when cold`() {
        val label = cache.labelOrFallback(60.169857, 24.938379)
        // Format: 60.1699, 24.9384 — 4 decimal places, comma separator
        assertEquals("60.1699, 24.9384", label)
    }

    @Test
    fun `labelOrFallback prefers an explicit entity label over the cache`() {
        cache.put(50.0, 10.0, "Cached")
        val label = cache.labelOrFallback(50.0, 10.0, explicit = "Trip End")
        assertEquals("Trip End", label)
    }

    @Test
    fun `labelOrFallback skips blank explicit label`() {
        cache.put(50.0, 10.0, "Cached")
        assertEquals("Cached", cache.labelOrFallback(50.0, 10.0, explicit = " "))
    }

    @Test
    fun `entries survive across cache instances backed by same prefs`() {
        cache.put(1.0, 2.0, "PointA")
        val fresh = NominatimAddressCache(ctx)
        assertNotNull(fresh.lookup(1.0, 2.0))
    }
}
