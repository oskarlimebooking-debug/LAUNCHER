package com.oskar.retrolauncher.ui.media

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.data.media.MediaState
import kotlinx.coroutines.launch

/**
 * Observes the [com.oskar.retrolauncher.data.media.MediaRepository] state flow and projects
 * each emission into a [MediaUiState] for the fragment to bind. Position-tick advancement
 * lives in `MediaRepository.startPositionTicker(...)` (T1.16) — every 250 ms the repo
 * republishes a state with an updated `positionMs`, which flows through this VM. The
 * fragment never runs its own ticker (AC3 — no jitter).
 */
class MediaViewModel : ViewModel() {

    private val _ui = MutableLiveData<MediaUiState>()
    val uiState: LiveData<MediaUiState> = _ui

    init {
        viewModelScope.launch {
            App.media.state.collect { _ui.value = MediaUiState.from(it) }
        }
    }

    fun togglePlay() {
        val s = App.media.state.value
        if (s.playing) App.media.pause() else App.media.play()
    }

    fun prev() = App.media.prev()
    fun next() = App.media.next()
}

data class MediaUiState(
    val title: String?,
    val artist: String?,
    val art: Bitmap?,
    val playing: Boolean,
    val durationMs: Long,
    val positionMs: Long,
    val isEmpty: Boolean,
) {
    companion object {
        fun from(s: MediaState) = MediaUiState(
            title = s.title,
            artist = s.artist,
            art = s.art,
            playing = s.playing,
            durationMs = s.durationMs,
            positionMs = s.livePosition(),
            isEmpty = s.isEmpty,
        )
    }
}
