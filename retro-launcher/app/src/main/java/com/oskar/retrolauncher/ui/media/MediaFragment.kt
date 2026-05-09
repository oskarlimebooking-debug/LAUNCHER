package com.oskar.retrolauncher.ui.media

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.util.dominantColorAsync

/**
 * T1.17 — binds the active media session to the layout per spec section 9.6.
 *
 * The position seek bar is driven by `MediaRepository`'s 250 ms ticker (T1.16);
 * this fragment never runs its own loop. Title and artist use marquee animation
 * (configured in `fragment_media.xml`) — `setSelected(true)` activates it once
 * the fragment is visible.
 */
class MediaFragment : Fragment(R.layout.fragment_media) {

    private val vm: MediaViewModel by viewModels()
    private var lastArtRef: android.graphics.Bitmap? = null

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
                    newArt.dominantColorAsync { c -> applyTint(view, c) }
                } else {
                    Glide.with(this).clear(art)
                    art.setImageResource(R.drawable.bg_album)
                }
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

    private fun applyTint(root: View, color: Int) {
        val bg = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(color, 0xFF111111.toInt()),
        ).apply {
            cornerRadius = resources.getDimension(R.dimen.tile_corner)
        }
        root.background = bg
    }
}
