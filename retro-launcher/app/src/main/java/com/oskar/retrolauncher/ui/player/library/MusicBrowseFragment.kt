package com.oskar.retrolauncher.ui.player.library

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.oskar.retrolauncher.R
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * One browser tab — Songs (list), Albums (grid) or Artists (list). Shares the
 * parent [LibraryFragment]'s [LibraryViewModel] so the library is scanned once.
 */
class MusicBrowseFragment : Fragment(R.layout.fragment_music_browse) {

    private val vm: LibraryViewModel by viewModels({ requireParentFragment() })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.music_list)
        val empty = view.findViewById<TextView>(R.id.music_empty)
        when (requireArguments().getString(ARG_SECTION)) {
            SECTION_ALBUMS -> bindAlbums(rv, empty)
            SECTION_ARTISTS -> bindArtists(rv, empty)
            else -> bindSongs(rv, empty)
        }
    }

    private fun bindSongs(rv: RecyclerView, empty: TextView) {
        val adapter = SongRowAdapter(
            onClick = { index -> vm.play(vm.songs.value, index) },
            onToggleLike = { vm.toggleLike(it) },
            onLongClick = { PlaylistDialogs.showAddToPlaylist(this, vm, it) },
        )
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        observe(vm.songs) { adapter.submitList(it); empty.isVisible = it.isEmpty() }
        observe(vm.likedIds) { adapter.setLiked(it) }
    }

    private fun bindAlbums(rv: RecyclerView, empty: TextView) {
        val adapter = AlbumGridAdapter { openDetail(TrackListFragment.forAlbum(it.id, it.name)) }
        rv.layoutManager = GridLayoutManager(requireContext(), ALBUM_COLUMNS)
        rv.adapter = adapter
        observe(vm.albums) { adapter.submitList(it); empty.isVisible = it.isEmpty() }
    }

    private fun bindArtists(rv: RecyclerView, empty: TextView) {
        val adapter = ArtistListAdapter { openDetail(TrackListFragment.forArtist(it.name)) }
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        observe(vm.artists) { adapter.submitList(it); empty.isVisible = it.isEmpty() }
    }

    private fun <T> observe(flow: StateFlow<T>, block: (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { flow.collect(block) }
        }
    }

    private fun openDetail(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        private const val ARG_SECTION = "section"
        const val SECTION_SONGS = "songs"
        const val SECTION_ALBUMS = "albums"
        const val SECTION_ARTISTS = "artists"
        private const val ALBUM_COLUMNS = 3

        fun create(section: String) =
            MusicBrowseFragment().apply { arguments = bundleOf(ARG_SECTION to section) }
    }
}
