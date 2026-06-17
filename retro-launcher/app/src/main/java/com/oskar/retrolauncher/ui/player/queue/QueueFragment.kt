package com.oskar.retrolauncher.ui.player.queue

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.ui.player.nowplaying.NowPlayingViewModel
import kotlinx.coroutines.launch

/** The current playback queue, opened as an overlay from the now-playing screen. */
class QueueFragment : Fragment(R.layout.fragment_queue) {

    private val vm: NowPlayingViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ImageButton>(R.id.queue_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        val empty = view.findViewById<TextView>(R.id.queue_empty)
        val adapter = QueueAdapter(onClick = { index -> vm.playIndex(index) })
        view.findViewById<RecyclerView>(R.id.queue_list).apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.queue.collect { adapter.submitList(it); empty.isVisible = it.isEmpty() }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.currentIndex.collect { adapter.setCurrent(it) }
            }
        }
    }
}
