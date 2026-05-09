package com.oskar.retrolauncher.data.media

import com.oskar.retrolauncher.service.MediaNotificationListenerHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MediaRepository {

    private val _state = MutableStateFlow(MediaState())
    val state: StateFlow<MediaState> = _state

    fun update(s: MediaState) { _state.value = s }
    fun clear() { _state.value = MediaState() }

    fun play()  = controller()?.transportControls?.play()
    fun pause() = controller()?.transportControls?.pause()
    fun next()  = controller()?.transportControls?.skipToNext()
    fun prev()  = controller()?.transportControls?.skipToPrevious()
    fun seekTo(positionMs: Long) = controller()?.transportControls?.seekTo(positionMs)

    private fun controller() = MediaNotificationListenerHolder.instance?.controller()
}
