package com.oskar.retrolauncher.ui.media

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import com.oskar.retrolauncher.data.media.MediaState
import com.oskar.retrolauncher.util.dominantColorAsync

/**
 * In-memory cache of dominant-colour extractions, keyed by track identity
 * (`packageName | title | album`). Lives for the lifetime of the host
 * fragment so a track skip + skip-back doesn't re-run Palette (T1.18 AC2).
 *
 * The async extractor is injected so tests can drive cache hit / miss paths
 * without spinning up Palette's real background executor.
 */
class MediaTintCache(
    @ColorInt private val fallback: Int,
    private val resolver: (Bitmap, (Int) -> Unit) -> Unit = { bmp, cb ->
        bmp.dominantColorAsync(fallback, cb)
    },
) {

    private val cache = mutableMapOf<String, Int>()

    /**
     * Resolve the tint colour for [state]:
     *  - empty state or null art → [fallback] (AC4), no extraction.
     *  - cache hit → [onColor] fired immediately with the stored colour (AC2).
     *  - cache miss → [resolver] runs (off the main thread for the default
     *    `dominantColorAsync` impl), the result is cached, then [onColor] fires.
     */
    fun resolve(state: MediaState, onColor: (Int) -> Unit) {
        val key = keyOf(state)
        val art = state.art
        if (key == null || art == null) {
            onColor(fallback)
            return
        }
        val hit = cache[key]
        if (hit != null) {
            onColor(hit)
            return
        }
        resolver(art) { color ->
            cache[key] = color
            onColor(color)
        }
    }

    @VisibleForTesting
    fun cacheSize(): Int = cache.size

    companion object {
        fun keyOf(s: MediaState): String? =
            if (s.isEmpty) null
            else "${s.packageName.orEmpty()}|${s.title.orEmpty()}|${s.album.orEmpty()}"
    }
}
