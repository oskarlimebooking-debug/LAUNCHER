package com.oskar.retrolauncher.ui.player.library

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.music.Track
import kotlinx.coroutines.launch

/** A playlist's tracks (resolved from the library), with play-all. */
class PlaylistDetailFragment : Fragment(R.layout.fragment_track_list) {

    private val vm: LibraryViewModel by viewModels()
    private var currentTracks: List<Track> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = requireArguments()
        view.findViewById<TextView>(R.id.track_list_title).text = args.getString(ARG_NAME)
        view.findViewById<ImageButton>(R.id.track_list_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        val empty = view.findViewById<TextView>(R.id.track_list_empty)
        val adapter = SongRowAdapter(
            onClick = { index -> vm.play(currentTracks, index) },
            onToggleLike = { vm.toggleLike(it) },
            onLongClick = { PlaylistDialogs.showAddToPlaylist(this, vm, it) },
        )
        view.findViewById<RecyclerView>(R.id.track_list).apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
        view.findViewById<View>(R.id.track_list_play_all).setOnClickListener { vm.play(currentTracks, 0) }

        val id = args.getLong(ARG_ID)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.playlistItems(id).collect { items ->
                    currentTracks = vm.resolveTracks(items)
                    adapter.submitList(currentTracks)
                    empty.isVisible = currentTracks.isEmpty()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.likedIds.collect { adapter.setLiked(it) }
            }
        }
    }

    companion object {
        private const val ARG_ID = "playlist_id"
        private const val ARG_NAME = "playlist_name"

        fun create(id: Long, name: String) = PlaylistDetailFragment().apply {
            arguments = bundleOf(ARG_ID to id, ARG_NAME to name)
        }
    }
}
