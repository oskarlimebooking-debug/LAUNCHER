package com.oskar.retrolauncher.ui.player

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.ui.player.library.LibraryFragment
import com.oskar.retrolauncher.ui.player.nowplaying.NowPlayingFragment
import com.oskar.retrolauncher.util.Permissions

/**
 * Full-screen player overlay. Hosts the library browser (M2; the now-playing
 * screen is added in M3). Gates on the audio-read permission, requesting it the
 * first time the player is opened. Closes via the explicit X — the launcher
 * deliberately swallows the system back press.
 */
class PlayerHostFragment : Fragment(R.layout.fragment_player_host) {

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) showLibrary() else showPermissionPrompt()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ImageButton>(R.id.player_close).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        view.findViewById<View>(R.id.player_grant).setOnClickListener {
            requestPermission.launch(Permissions.audioReadPermission())
        }
        if (Permissions.hasAudioRead(requireContext())) {
            showLibrary()
        } else {
            showPermissionPrompt()
            requestPermission.launch(Permissions.audioReadPermission())
        }
    }

    private fun showLibrary() {
        view?.findViewById<View>(R.id.player_permission)?.visibility = View.GONE
        if (childFragmentManager.findFragmentById(R.id.player_content) == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.player_now_playing, NowPlayingFragment())
                .replace(R.id.player_content, LibraryFragment())
                .commit()
        }
    }

    private fun showPermissionPrompt() {
        view?.findViewById<View>(R.id.player_permission)?.visibility = View.VISIBLE
    }
}
