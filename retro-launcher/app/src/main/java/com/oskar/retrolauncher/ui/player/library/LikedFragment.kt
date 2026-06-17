package com.oskar.retrolauncher.ui.player.library

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.oskar.retrolauncher.R
import kotlinx.coroutines.launch

/** Liked-songs tab. Reuses the browse layout; hearts unlike in place. */
class LikedFragment : Fragment(R.layout.fragment_music_browse) {

    private val vm: LibraryViewModel by viewModels({ requireParentFragment() })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val empty = view.findViewById<TextView>(R.id.music_empty).apply {
            setText(R.string.music_no_liked)
        }
        val adapter = SongRowAdapter(
            onClick = { index -> vm.play(vm.likedTracks.value, index) },
            onToggleLike = { vm.toggleLike(it) },
            onLongClick = { PlaylistDialogs.showAddToPlaylist(this, vm, it) },
        )
        view.findViewById<RecyclerView>(R.id.music_list).apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.likedTracks.collect { adapter.submitList(it); empty.isVisible = it.isEmpty() }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.likedIds.collect { adapter.setLiked(it) }
            }
        }
    }
}
