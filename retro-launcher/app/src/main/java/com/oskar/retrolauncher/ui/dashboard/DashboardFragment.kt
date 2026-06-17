package com.oskar.retrolauncher.ui.dashboard

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.ui.grid.RailAdapter
import com.oskar.retrolauncher.ui.player.PlayerHostFragment
import com.oskar.retrolauncher.ui.speed.SpeedViewModel
import com.oskar.retrolauncher.ui.speed.SpeedometerView
import com.oskar.retrolauncher.util.launchApp
import kotlinx.coroutines.launch

/** Landing dashboard: clock, driving readout, today's stats, now-playing, favourites. */
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val vm: DashboardViewModel by viewModels()
    private val speedVm: SpeedViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupSpeed(view)
        setupFavorites(view)
        view.findViewById<View>(R.id.dash_now_playing).setOnClickListener { openPlayer() }
        observeToday(view)
        observeNowPlaying(view)
    }

    private fun setupSpeed(view: View) {
        val gauge = view.findViewById<SpeedometerView>(R.id.dash_speed)
        gauge.displayUnits = App.settings.units
        speedVm.state.observe(viewLifecycleOwner) { gauge.setSpeed(it.kmh, animated = true) }
    }

    private fun setupFavorites(view: View) {
        val adapter = RailAdapter(onClick = { requireContext().launchApp(it) })
        view.findViewById<RecyclerView>(R.id.dash_favorites).apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            this.adapter = adapter
        }
        collect { vm.favorites.collect { adapter.submitList(it) } }
    }

    private fun observeToday(view: View) {
        val distance = view.findViewById<TextView>(R.id.dash_distance)
        val duration = view.findViewById<TextView>(R.id.dash_duration)
        collect {
            vm.today.collect {
                distance.text = "%.1f km".format(it.distanceM / 1000.0)
                duration.text = formatDriveTime(it.durationMs)
            }
        }
    }

    private fun observeNowPlaying(view: View) {
        val title = view.findViewById<TextView>(R.id.dash_np_title)
        val artist = view.findViewById<TextView>(R.id.dash_np_artist)
        collect {
            vm.nowPlaying.collect { s ->
                title.text = if (s.isEmpty) getString(R.string.music_nothing_playing) else s.title
                artist.text = s.artist.orEmpty()
            }
        }
    }

    private fun collect(block: suspend () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { block() }
        }
    }

    private fun openPlayer() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, PlayerHostFragment())
            .addToBackStack("player")
            .commit()
    }
}

private fun formatDriveTime(ms: Long): String {
    val totalMin = ms / 60000
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
