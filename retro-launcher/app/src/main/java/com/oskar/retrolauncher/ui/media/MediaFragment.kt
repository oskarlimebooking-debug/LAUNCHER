package com.oskar.retrolauncher.ui.media

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R

/**
 * T1.17 — binds the active media session to the layout per spec section 9.6.
 * T1.18 — drives the tile background via [MediaTintCache] (cache by track) and
 * [MediaTinter] (400 ms ArgbEvaluator gradient transition).
 *
 * The position seek bar is driven by `MediaRepository`'s 250 ms ticker (T1.16);
 * this fragment never runs its own loop.
 */
class MediaFragment : Fragment(R.layout.fragment_media) {

    private val vm: MediaViewModel by viewModels()
    private var lastArtRef: android.graphics.Bitmap? = null
    private var tinter: MediaTinter? = null
    private var tintCache: MediaTintCache? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val art = view.findViewById<ImageView>(R.id.art)
        val title = view.findViewById<TextView>(R.id.title)
        val artist = view.findViewById<TextView>(R.id.artist)
        val playPause = view.findViewById<ImageButton>(R.id.play_pause)
        val prev = view.findViewById<ImageButton>(R.id.prev)
        val next = view.findViewById<ImageButton>(R.id.next)
        val bar = view.findViewById<ProgressBar>(R.id.progress)

        // AC2: marquee runs only while the TextView is "selected".
        title.isSelected = true
        artist.isSelected = true

        val cornerPx = resources.getDimensionPixelSize(R.dimen.album_art_corner)
        val glideOptions = RequestOptions().transform(RoundedCorners(cornerPx))

        // T1.18 AC4: fall back to the theme's colorSurface when extraction fails
        // or art is null. Resolved once per view-create from the inflated theme.
        val surface = resolveColorSurface(view)
        val tinter = MediaTinter(
            fallback = surface,
            cornerRadiusPx = resources.getDimension(R.dimen.tile_corner),
        ).also { this.tinter = it }
        val tintCache = MediaTintCache(fallback = surface).also { this.tintCache = it }
        view.background = tinter.drawable

        vm.uiState.observe(viewLifecycleOwner) { s ->
            // AC5 empty state: title shows "Nothing playing", artist blank.
            title.text = if (s.isEmpty) getString(R.string.media_no_app) else s.title.orEmpty()
            artist.text = if (s.isEmpty) "" else s.artist.orEmpty()

            playPause.setImageResource(
                if (s.playing) R.drawable.ic_pause else R.drawable.ic_play
            )

            // AC1: rounded-corner album art. Re-load only on bitmap-reference change so
            // the 250 ms position ticker (which re-emits identical art) doesn't re-trigger
            // Glide and cause flicker.
            val newArt = s.art
            if (newArt !== lastArtRef) {
                lastArtRef = newArt
                if (newArt != null) {
                    Glide.with(this)
                        .load(newArt)
                        .apply(glideOptions)
                        .placeholder(R.drawable.bg_album)
                        .into(art)
                } else {
                    Glide.with(this).clear(art)
                    art.setImageResource(R.drawable.bg_album)
                }
                // T1.18: resolve dominant colour (cache hit short-circuits Palette;
                // null art / empty state delivers fallback synchronously) and animate.
                // The StateFlow value carries packageName + album needed for the cache
                // key — LiveData ticks with positionMs only ever lag the StateFlow, so
                // the source-of-truth state at observe time matches `s` track-wise.
                tintCache.resolve(App.media.state.value) { color -> tinter.animateTo(color) }
            }

            // AC3: seek bar bound directly to the StateFlow's position — repo's
            // 250 ms ticker advances it without any view-side timer.
            if (s.durationMs > 0) {
                bar.max = s.durationMs.toInt()
                bar.progress = s.positionMs.coerceIn(0, s.durationMs).toInt()
            } else {
                bar.max = 1
                bar.progress = 0
            }
        }

        playPause.setOnClickListener { vm.togglePlay() }
        prev.setOnClickListener { vm.prev() }
        next.setOnClickListener { vm.next() }
    }

    override fun onDestroyView() {
        tinter?.cancel()
        tinter = null
        tintCache = null
        super.onDestroyView()
    }

    private fun resolveColorSurface(view: View): Int {
        val tv = TypedValue()
        val resolved = view.context.theme.resolveAttribute(
            com.google.android.material.R.attr.colorSurface, tv, true,
        )
        return if (resolved && tv.resourceId != 0) {
            ContextCompat.getColor(view.context, tv.resourceId)
        } else if (resolved) {
            tv.data
        } else {
            ContextCompat.getColor(view.context, R.color.card)
        }
    }
}

