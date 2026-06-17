package com.oskar.retrolauncher.ui.player.library

import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.music.Track

/** Create-playlist and add-to-playlist dialogs, shared across the browser. */
object PlaylistDialogs {

    fun showAddToPlaylist(fragment: Fragment, vm: LibraryViewModel, track: Track) {
        val playlists = vm.playlists.value
        val labels = playlists.map { it.name } + fragment.getString(R.string.music_new_playlist)
        AlertDialog.Builder(fragment.requireContext())
            .setTitle(R.string.music_add_to_playlist)
            .setItems(labels.toTypedArray()) { _, which ->
                if (which < playlists.size) vm.addToPlaylist(playlists[which], track)
                else promptName(fragment) { vm.createPlaylistAndAdd(it, track) }
            }
            .show()
    }

    fun showCreatePlaylist(fragment: Fragment, vm: LibraryViewModel) {
        promptName(fragment) { vm.createPlaylist(it) }
    }

    private fun promptName(fragment: Fragment, onName: (String) -> Unit) {
        val input = EditText(fragment.requireContext()).apply {
            hint = fragment.getString(R.string.music_playlist_name_hint)
        }
        AlertDialog.Builder(fragment.requireContext())
            .setTitle(R.string.music_new_playlist)
            .setView(input)
            .setPositiveButton(R.string.music_create) { _, _ -> onName(input.text.toString()) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
