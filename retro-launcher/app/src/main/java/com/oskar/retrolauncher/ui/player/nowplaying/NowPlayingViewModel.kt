package com.oskar.retrolauncher.ui.player.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oskar.retrolauncher.App
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Drives the now-playing screen + queue. Surfaces the player's state flows and
 * polls live position (the controller only emits on discontinuities, so the
 * seek bar needs its own tick).
 */
class NowPlayingViewModel : ViewModel() {

    val nowPlaying = App.player.nowPlaying
    val queue = App.player.queue
    val currentIndex = App.player.currentIndex

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position

    init {
        viewModelScope.launch {
            while (isActive) {
                _position.value = App.player.positionMs()
                delay(POLL_MS)
            }
        }
    }

    fun togglePlay() = App.player.togglePlay()
    fun next() = App.player.next()
    fun prev() = App.player.prev()
    fun seekTo(ms: Long) = App.player.seekTo(ms)
    fun playIndex(index: Int) = App.player.playIndex(index)

    private companion object {
        const val POLL_MS = 500L
    }
}
