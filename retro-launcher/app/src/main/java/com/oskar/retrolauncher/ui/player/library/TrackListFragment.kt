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

/**
 * Tracks of a single album or artist, with a "play all" action. Opened as a
 * top-level overlay from the album/artist tabs; closes via its own back button
 * (the launcher swallows the system back press).
 */
class TrackListFragment : Fragment(R.layout.fragment_track_list) {

    private val vm: LibraryViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = requireArguments()
        view.findViewById<TextView>(R.id.track_list_title).text = args.getString(ARG_TITLE)
        view.findViewById<ImageButton>(R.id.track_list_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val tracks = tracksFor(args)
        val adapter = SongRowAdapter(
            onClick = { index -> vm.play(tracks, index) },
            onToggleLike = { vm.toggleLike(it) },
            onLongClick = { PlaylistDialogs.showAddToPlaylist(this, vm, it) },
        )
        view.findViewById<RecyclerView>(R.id.track_list).apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
        adapter.submitList(tracks)
        view.findViewById<TextView>(R.id.track_list_empty).isVisible = tracks.isEmpty()
        view.findViewById<View>(R.id.track_list_play_all).setOnClickListener { vm.play(tracks, 0) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.likedIds.collect { adapter.setLiked(it) }
            }
        }
    }

    private fun tracksFor(args: Bundle): List<Track> {
        val albumId = args.getLong(ARG_ALBUM, -1L)
        return if (albumId >= 0) vm.tracksForAlbum(albumId)
        else vm.tracksForArtist(args.getString(ARG_ARTIST).orEmpty())
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_ALBUM = "album_id"
        private const val ARG_ARTIST = "artist"

        fun forAlbum(albumId: Long, title: String) = TrackListFragment().apply {
            arguments = bundleOf(ARG_ALBUM to albumId, ARG_TITLE to title)
        }

        fun forArtist(artist: String) = TrackListFragment().apply {
            arguments = bundleOf(ARG_ARTIST to artist, ARG_TITLE to artist)
        }
    }
}
