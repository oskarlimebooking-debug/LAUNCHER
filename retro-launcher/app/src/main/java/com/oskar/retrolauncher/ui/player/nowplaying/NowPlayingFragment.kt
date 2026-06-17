package com.oskar.retrolauncher.ui.player.nowplaying

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.music.NowPlaying
import com.oskar.retrolauncher.ui.player.formatTime
import com.oskar.retrolauncher.ui.player.loadAlbumArt
import com.oskar.retrolauncher.ui.player.queue.QueueFragment
import kotlinx.coroutines.launch

/** Left pane of the player: big art, metadata, seek bar and transport. */
class NowPlayingFragment : Fragment(R.layout.fragment_now_playing) {

    private val vm: NowPlayingViewModel by viewModels()
    private var userSeeking = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        wireTransport(view)
        wireSeek(view)
        observeNowPlaying(view)
        observePosition(view)
    }

    private fun wireTransport(view: View) {
        view.findViewById<ImageButton>(R.id.np_prev).setOnClickListener { vm.prev() }
        view.findViewById<ImageButton>(R.id.np_play_pause).setOnClickListener { vm.togglePlay() }
        view.findViewById<ImageButton>(R.id.np_next).setOnClickListener { vm.next() }
        view.findViewById<ImageButton>(R.id.np_queue).setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, QueueFragment())
                .addToBackStack("queue")
                .commit()
        }
    }

    private fun wireSeek(view: View) {
        val seek = view.findViewById<SeekBar>(R.id.np_seek)
        val position = view.findViewById<TextView>(R.id.np_position)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) position.text = formatTime(progress.toLong())
            }

            override fun onStartTrackingTouch(sb: SeekBar) { userSeeking = true }

            override fun onStopTrackingTouch(sb: SeekBar) {
                userSeeking = false
                vm.seekTo(sb.progress.toLong())
            }
        })
    }

    private fun observeNowPlaying(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.nowPlaying.collect { bindNowPlaying(view, it) }
            }
        }
    }

    private fun observePosition(view: View) {
        val seek = view.findViewById<SeekBar>(R.id.np_seek)
        val position = view.findViewById<TextView>(R.id.np_position)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.position.collect { ms ->
                    if (!userSeeking) {
                        seek.progress = ms.toInt()
                        position.text = formatTime(ms)
                    }
                }
            }
        }
    }

    private fun bindNowPlaying(view: View, np: NowPlaying?) {
        view.findViewById<View>(R.id.np_content).isVisible = np != null
        view.findViewById<View>(R.id.np_empty).isVisible = np == null
        if (np == null) return
        view.findViewById<ImageView>(R.id.np_art).loadAlbumArt(np.artworkUri)
        view.findViewById<TextView>(R.id.np_title).text = np.title
        view.findViewById<TextView>(R.id.np_artist).text = np.artist
        view.findViewById<TextView>(R.id.np_duration).text = formatTime(np.durationMs)
        view.findViewById<SeekBar>(R.id.np_seek).max = np.durationMs.toInt()
        view.findViewById<ImageButton>(R.id.np_play_pause)
            .setImageResource(if (np.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
    }
}
