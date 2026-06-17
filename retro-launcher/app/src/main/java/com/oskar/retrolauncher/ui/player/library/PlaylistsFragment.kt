package com.oskar.retrolauncher.ui.player.library

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.music.Playlist
import kotlinx.coroutines.launch

/** Playlists tab: create, open, delete. */
class PlaylistsFragment : Fragment(R.layout.fragment_playlists) {

    private val vm: LibraryViewModel by viewModels({ requireParentFragment() })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.playlists_create).setOnClickListener {
            PlaylistDialogs.showCreatePlaylist(this, vm)
        }
        val empty = view.findViewById<TextView>(R.id.playlists_empty)
        val adapter = PlaylistListAdapter(onClick = ::openDetail, onLongClick = ::confirmDelete)
        view.findViewById<RecyclerView>(R.id.playlists_list).apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.playlists.collect { adapter.submitList(it); empty.isVisible = it.isEmpty() }
            }
        }
    }

    private fun openDetail(playlist: Playlist) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, PlaylistDetailFragment.create(playlist.id, playlist.name))
            .addToBackStack(null)
            .commit()
    }

    private fun confirmDelete(playlist: Playlist) {
        AlertDialog.Builder(requireContext())
            .setTitle(playlist.name)
            .setMessage(R.string.music_delete_playlist_confirm)
            .setPositiveButton(R.string.music_delete) { _, _ -> vm.deletePlaylist(playlist.id) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
