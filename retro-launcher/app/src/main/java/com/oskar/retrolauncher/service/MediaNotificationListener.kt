package com.oskar.retrolauncher.service

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.data.media.MediaState
import timber.log.Timber

class MediaNotificationListener : NotificationListenerService() {

    private val sessionManager: MediaSessionManager by lazy {
        getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    }
    private val componentName by lazy {
        ComponentName(this, MediaNotificationListener::class.java)
    }

    private var activeController: MediaController? = null

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(md: MediaMetadata?) = pushState()
        override fun onPlaybackStateChanged(state: PlaybackState?) = pushState()
        override fun onSessionDestroyed() {
            activeController = null
            App.media.clear()
        }
    }

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { list ->
            rebind(list ?: emptyList())
        }

    override fun onCreate() {
        super.onCreate()
        MediaNotificationListenerHolder.instance = this
    }

    override fun onListenerConnected() {
        try {
            // T2.7-followup — onListenerConnected runs on a Binder thread with no
            // Looper. The 2-arg addOnActiveSessionsChangedListener overload tries
            // to derive a Handler from the current thread and crashes with
            // "Can't create handler inside thread that has not called
            // Looper.prepare()" on API 23. Pass an explicit main-looper Handler.
            sessionManager.addOnActiveSessionsChangedListener(
                sessionsListener, componentName, Handler(Looper.getMainLooper()),
            )
            rebind(sessionManager.getActiveSessions(componentName))
        } catch (t: Throwable) {
            Timber.w(t, "addOnActiveSessionsChangedListener failed")
        }
    }

    override fun onListenerDisconnected() {
        runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsListener) }
        activeController?.unregisterCallback(callback)
        activeController = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (MediaNotificationListenerHolder.instance === this) {
            MediaNotificationListenerHolder.instance = null
        }
    }

    private fun rebind(sessions: List<MediaController>) {
        // When the built-in player is enabled, a playing session owned by the
        // launcher itself wins the tiebreak so local playback reliably binds —
        // otherwise the original "first playing, else first" policy applies.
        val preferOwn = runCatching { App.settings.fullPlayerMode }.getOrDefault(false)
        val newCtrl = pickSession(
            sessions = sessions,
            isPlaying = { it.playbackState?.state == PlaybackState.STATE_PLAYING },
            isOwn = { it.packageName == packageName },
            preferOwn = preferOwn,
        )
        if (newCtrl?.sessionToken == activeController?.sessionToken) {
            pushState()
            return
        }
        activeController?.unregisterCallback(callback)
        activeController = newCtrl
        activeController?.registerCallback(callback, Handler(Looper.getMainLooper()))
        pushState()
    }

    private fun pushState() {
        val ctrl = activeController ?: run {
            App.media.clear()
            return
        }
        App.media.update(metadataToMediaState(ctrl.metadata, ctrl.playbackState, ctrl.packageName))
    }

    fun controller(): MediaController? = activeController

    companion object {
        fun isEnabled(ctx: Context): Boolean {
            val flat = Settings.Secure.getString(
                ctx.contentResolver,
                "enabled_notification_listeners",
            )
            return flat?.contains(ctx.packageName) == true
        }
    }
}

/** Singleton holder so the repo can reach the listener for transport calls. */
object MediaNotificationListenerHolder {
    @Volatile var instance: MediaNotificationListener? = null
}

/**
 * Pure transform from `(MediaMetadata?, PlaybackState?, packageName)` → [MediaState].
 *
 * Extracted so the listener's metadata/playback-state mapping can be unit-tested
 * without spinning up a real `NotificationListenerService`. Every accessor is
 * null-safe — both arguments may be null mid-callback when a notification is
 * removed before the controller delivers its final state (T1.15 AC5).
 */
fun metadataToMediaState(
    metadata: MediaMetadata?,
    playbackState: PlaybackState?,
    packageName: String?,
): MediaState = MediaState(
    title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
    artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
    album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM),
    durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
    positionMs = playbackState?.position ?: 0L,
    positionAtMs = playbackState?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
    speed = playbackState?.playbackSpeed ?: 1f,
    playing = playbackState?.state == PlaybackState.STATE_PLAYING,
    art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART),
    packageName = packageName,
)
