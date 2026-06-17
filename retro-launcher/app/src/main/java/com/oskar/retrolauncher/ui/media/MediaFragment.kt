package com.oskar.retrolauncher.ui.media

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.ui.player.PlayerHostFragment

/**
 * The left-column media tile. Two modes:
 *  - Full-player (default): rich card (art + title + artist + progress +
 *    transport) bound to the built-in player via [MediaCardBinder]; tap opens
 *    the full player.
 *  - Simple: marquee title + transport, controlling whatever external session
 *    is active (the original behaviour); used when the built-in player is off.
 */
class MediaFragment : Fragment() {

    private val vm: MediaViewModel by viewModels()
    private var lastEmpty: Boolean = true
    private var lastTitleApplied: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val layout = if (App.settings.fullPlayerMode) R.layout.fragment_media_rich else R.layout.fragment_media
        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (App.settings.fullPlayerMode) {
            MediaCardBinder(this, view) { openFullPlayer() }.bind()
        } else {
            bindSimpleCard(view)
        }
    }

    private fun bindSimpleCard(view: View) {
        val title = view.findViewById<TextView>(R.id.title)
        val playPause = view.findViewById<ImageButton>(R.id.play_pause)
        val prev = view.findViewById<ImageButton>(R.id.prev)
        val next = view.findViewById<ImageButton>(R.id.next)
        title.isSelected = true
        val root = view as ConstraintLayout

        vm.uiState.observe(viewLifecycleOwner) { s ->
            val empty = s.isEmpty
            if (empty != lastEmpty) {
                applyLayout(root, empty)
                lastEmpty = empty
            }
            title.visibility = if (empty) View.GONE else View.VISIBLE
            val desired = if (empty) null else s.title.orEmpty()
            if (desired != lastTitleApplied) {
                lastTitleApplied = desired
                title.text = desired.orEmpty()
                if (!empty) {
                    title.isSelected = false
                    title.isSelected = true
                }
            }
            playPause.setImageResource(if (s.playing) R.drawable.ic_pause else R.drawable.ic_play)
        }

        playPause.setOnClickListener {
            if (lastEmpty) dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) else vm.togglePlay()
        }
        prev.setOnClickListener {
            if (lastEmpty) dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS) else vm.prev()
        }
        next.setOnClickListener {
            if (lastEmpty) dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT) else vm.next()
        }
    }

    private fun applyLayout(root: ConstraintLayout, empty: Boolean) {
        val set = ConstraintSet().apply { clone(root) }
        set.setVerticalBias(R.id.transport, if (empty) 0.5f else 0.6f)
        set.applyTo(root)
    }

    private fun openFullPlayer() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, PlayerHostFragment())
            .addToBackStack("player")
            .commit()
    }

    private fun dispatchMediaKey(keyCode: Int) {
        val am = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val now = SystemClock.uptimeMillis()
        am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
        am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
    }
}
