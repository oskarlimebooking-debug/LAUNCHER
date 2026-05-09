package com.oskar.retrolauncher

import android.graphics.Bitmap
import android.graphics.Color
import com.oskar.retrolauncher.data.media.MediaState
import com.oskar.retrolauncher.ui.media.MediaTintCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * T1.18 ACs covered by [MediaTintCache] pure-logic tests:
 *
 *   - AC2: cache hit short-circuits extraction within the same session
 *     (resolver is never called twice for the same key).
 *   - AC4: null bitmap or empty state → falls back to the supplied surface
 *     color without invoking the async extractor.
 *
 * The resolver is injected so we can count invocations and avoid pulling in
 * Palette's real async executor under Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class MediaTintCacheTest {

    private val fallback = Color.parseColor("#1F1F1F")

    private fun bmp(): Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    private class CountingResolver(private val color: Int) : (Bitmap, (Int) -> Unit) -> Unit {
        var calls = 0
        override fun invoke(b: Bitmap, cb: (Int) -> Unit) {
            calls++
            cb(color)
        }
    }

    @Test
    fun `keyOf empty state is null`() {
        assertNull(MediaTintCache.keyOf(MediaState.empty))
    }

    @Test
    fun `keyOf populated state is deterministic`() {
        val s = MediaState(title = "Title", artist = "A", album = "Alb", packageName = "pkg")
        assertEquals(MediaTintCache.keyOf(s), MediaTintCache.keyOf(s))
    }

    @Test
    fun `keyOf differs across distinct tracks`() {
        val a = MediaState(title = "T1", artist = "A", album = "Alb", packageName = "pkg")
        val b = MediaState(title = "T2", artist = "A", album = "Alb", packageName = "pkg")
        assertNotEquals(MediaTintCache.keyOf(a), MediaTintCache.keyOf(b))
    }

    @Test
    fun `resolve empty state returns fallback without invoking resolver`() {
        val resolver = CountingResolver(Color.RED)
        val cache = MediaTintCache(fallback = fallback, resolver = resolver)
        var got = 0
        cache.resolve(MediaState.empty) { got = it }
        assertEquals(fallback, got)
        assertEquals(0, resolver.calls)
    }

    @Test
    fun `resolve state with null art returns fallback without invoking resolver`() {
        val resolver = CountingResolver(Color.RED)
        val cache = MediaTintCache(fallback = fallback, resolver = resolver)
        var got = 0
        cache.resolve(MediaState(title = "T", art = null)) { got = it }
        assertEquals(fallback, got)
        assertEquals(0, resolver.calls)
    }

    @Test
    fun `resolve cache miss invokes resolver and stores extracted color`() {
        val resolver = CountingResolver(Color.MAGENTA)
        val cache = MediaTintCache(fallback = fallback, resolver = resolver)
        val s = MediaState(title = "T", art = bmp(), packageName = "pkg")
        var got = 0
        cache.resolve(s) { got = it }
        assertEquals(Color.MAGENTA, got)
        assertEquals(1, resolver.calls)
        assertEquals(1, cache.cacheSize())
    }

    @Test
    fun `resolve cache hit short circuits resolver (AC2)`() {
        val resolver = CountingResolver(Color.MAGENTA)
        val cache = MediaTintCache(fallback = fallback, resolver = resolver)
        val s = MediaState(title = "T", art = bmp(), packageName = "pkg")
        cache.resolve(s) { /* prime */ }
        var second = 0
        cache.resolve(s) { second = it }
        assertEquals(Color.MAGENTA, second)
        assertEquals("resolver invoked twice for the same key — cache miss", 1, resolver.calls)
        assertEquals(1, cache.cacheSize())
    }

    @Test
    fun `resolve distinct keys both invoke resolver and populate cache`() {
        val resolver = CountingResolver(Color.MAGENTA)
        val cache = MediaTintCache(fallback = fallback, resolver = resolver)
        val a = MediaState(title = "T1", art = bmp(), packageName = "pkg")
        val b = MediaState(title = "T2", art = bmp(), packageName = "pkg")
        cache.resolve(a) { /* */ }
        cache.resolve(b) { /* */ }
        assertEquals(2, resolver.calls)
        assertEquals(2, cache.cacheSize())
    }
}
