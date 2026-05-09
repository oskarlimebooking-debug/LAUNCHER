package com.oskar.retrolauncher.data.media

import android.graphics.Bitmap
import android.os.SystemClock

data class MediaState(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long = 0,
    val positionMs: Long = 0,
    val positionAtMs: Long = 0,
    val speed: Float = 1f,
    val playing: Boolean = false,
    val art: Bitmap? = null,
    val packageName: String? = null,
) {
    val isEmpty: Boolean get() = title == null

    fun livePosition(now: Long = SystemClock.elapsedRealtime()): Long =
        if (playing) positionMs + ((now - positionAtMs) * speed).toLong()
        else positionMs

    companion object {
        val empty = MediaState()
    }
}
