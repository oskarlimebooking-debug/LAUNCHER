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
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.util.dominantColorAsync
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MediaFragment : Fragment(R.layout.fragment_media) {

    private val vm: MediaViewModel by viewModels()
    private var positionJob: Job? = null
    private var lastArtId: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val art = view.findViewById<ImageView>(R.id.art)
        val title = view.findViewById<TextView>(R.id.title)
        val artist = view.findViewById<TextView>(R.id.artist)
        val playPause = view.findViewById<ImageButton>(R.id.play_pause)
        val prev = view.findViewById<ImageButton>(R.id.prev)
        val next = view.findViewById<ImageButton>(R.id.next)
        val bar = view.findViewById<ProgressBar>(R.id.progress)

        vm.uiState.observe(viewLifecycleOwner) { s ->
            title.text = if (s.isEmpty) getString(R.string.media_no_app) else s.title.orEmpty()
            artist.text = s.artist.orEmpty()
            playPause.setImageResource(
                if (s.playing) R.drawable.ic_pause else R.drawable.ic_play
            )
            // Color extraction + Glide load — only on art change
            val artBitmap = s.art
            val newArtId = artBitmap?.hashCode() ?: 0
            if (artBitmap != null && newArtId != lastArtId) {
                lastArtId = newArtId
                Glide.with(this)
                    .load(artBitmap)
                    .placeholder(R.drawable.bg_album)
                    .into(art)
                artBitmap.dominantColorAsync { c -> applyTint(view, c) }
            } else if (artBitmap == null) {
                lastArtId = 0
                art.setImageResource(R.drawable.bg_album)
            }

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

    override fun onResume() {
        super.onResume()
        positionJob?.cancel()
        positionJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                vm.tick()
                delay(500)
            }
        }
    }

    override fun onPause() {
        positionJob?.cancel(); positionJob = null
        super.onPause()
    }

    private fun applyTint(root: View, color: Int) {
        // Subtle gradient: dominant color on top, fading to base card at bottom.
        val bg = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(color, 0xFF111111.toInt()),
        ).apply {
            cornerRadius = resources.getDimension(R.dimen.tile_corner)
        }
        root.background = bg
    }
}
