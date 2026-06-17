package com.oskar.retrolauncher.ui.media

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.music.NowPlaying
import com.oskar.retrolauncher.ui.player.loadAlbumArt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Binds the rich media card (full-player mode): album art + title + artist +
 * progress + transport, driven by the built-in [com.oskar.retrolauncher.data.music.PlayerController].
 * Tapping the card opens the full player.
 */
class MediaCardBinder(
    private val fragment: Fragment,
    private val root: View,
    private val onOpenPlayer: () -> Unit,
) {

    fun bind() {
        root.setOnClickListener { onOpenPlayer() }
        root.findViewById<ImageButton>(R.id.rich_prev).setOnClickListener { App.player.prev() }
        root.findViewById<ImageButton>(R.id.rich_play_pause).setOnClickListener { App.player.togglePlay() }
        root.findViewById<ImageButton>(R.id.rich_next).setOnClickListener { App.player.next() }
        App.player.initialize()
        observeNowPlaying()
        observePosition()
    }

    private fun observeNowPlaying() {
        val owner = fragment.viewLifecycleOwner
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                App.player.nowPlaying.collect { render(it) }
            }
        }
    }

    private fun render(np: NowPlaying?) {
        root.findViewById<TextView>(R.id.rich_title).text =
            np?.title?.takeIf { it.isNotBlank() } ?: root.context.getString(R.string.media_tap_to_play)
        root.findViewById<TextView>(R.id.rich_artist).text = np?.artist.orEmpty()
        root.findViewById<ImageView>(R.id.rich_art).loadAlbumArt(np?.artworkUri)
        root.findViewById<ImageButton>(R.id.rich_play_pause)
            .setImageResource(if (np?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play)
        root.findViewById<ProgressBar>(R.id.rich_progress).max =
            (np?.durationMs ?: 0L).toInt().coerceAtLeast(1)
    }

    private fun observePosition() {
        val owner = fragment.viewLifecycleOwner
        val progress = root.findViewById<ProgressBar>(R.id.rich_progress)
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    progress.progress = App.player.positionMs().toInt()
                    delay(POLL_MS)
                }
            }
        }
    }

    private companion object {
        const val POLL_MS = 1000L
    }
}
