package com.oskar.retrolauncher

import android.app.Activity
import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * T1.43 — steering-wheel media key forwarding.
 *
 * Verifies that KEYCODE_MEDIA_PLAY_PAUSE / NEXT / PREVIOUS are intercepted
 * and forwarded to the active MediaController, while non-media keys
 * (including KEYCODE_HOME) fall through to the system.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class MainActivityMediaKeysTest {

    private lateinit var activity: MainActivity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
    }

    // ───────── Media transport keys are intercepted ─────────

    @Test
    fun `KEYCODE_MEDIA_PLAY_PAUSE is consumed`() {
        val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        // AC1: media transport keys are forwarded to the controller and consumed.
        // dispatchKeyEvent returns true when the key was handled.
        val handled = activity.dispatchKeyEvent(event)
        // Even without an active session, the handler should consume the key
        // rather than passing it to the system — this prevents the system
        // from playing its own default media.
        assertTrue("MEDIA_PLAY_PAUSE must be consumed", handled)
    }

    @Test
    fun `KEYCODE_MEDIA_NEXT is consumed`() {
        val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT)
        assertTrue("MEDIA_NEXT must be consumed", activity.dispatchKeyEvent(event))
    }

    @Test
    fun `KEYCODE_MEDIA_PREVIOUS is consumed`() {
        val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        assertTrue("MEDIA_PREVIOUS must be consumed", activity.dispatchKeyEvent(event))
    }

    // ───────── Non-media keys fall through ─────────

    @Test
    fun `KEYCODE_HOME is not consumed`() {
        // AC3: KEYCODE_HOME must NOT be intercepted — would break launcher behavior.
        val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HOME)
        assertFalse("KEYCODE_HOME must fall through to system", activity.dispatchKeyEvent(event))
    }

    @Test
    fun `KEYCODE_BACK is consumed per T1_7 AC3 launcher requirement`() {
        // T1.7 requires the launcher to swallow back presses — onBackPressed
        // is a no-op, so super.dispatchKeyEvent for BACK returns true.
        val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)
        assertTrue("KEYCODE_BACK is consumed by launcher back-swallow", activity.dispatchKeyEvent(event))
    }

    @Test
    fun `KEYCODE_VOLUME_UP falls through when no media`() {
        // AC2: volume keys still control system volume when no media is active.
        val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP)
        assertFalse("VOLUME_UP must fall through when no media", activity.dispatchKeyEvent(event))
    }

    @Test
    fun `KEYCODE_VOLUME_DOWN falls through when no media`() {
        val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN)
        assertFalse("VOLUME_DOWN must fall through when no media", activity.dispatchKeyEvent(event))
    }

    @Test
    fun `KEYCODE_VOLUME_MUTE falls through`() {
        val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE)
        assertFalse("VOLUME_MUTE must fall through", activity.dispatchKeyEvent(event))
    }

    @Test
    fun `random keycode falls through`() {
        val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A)
        assertFalse("KEYCODE_A must fall through", activity.dispatchKeyEvent(event))
    }

    // ───────── Key-up events are not double-processed ─────────

    @Test
    fun `ACTION_UP for media key is consumed to suppress double dispatch`() {
        val event = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        assertTrue("ACTION_UP for media key must be consumed", activity.dispatchKeyEvent(event))
    }

    // ───────── Edge cases ─────────

    @Test
    fun `dispatchKeyEvent does not crash with no controller`() {
        // Pre-condition: MediaNotificationListenerHolder.instance is null at startup.
        repeat(50) {
            val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT)
            activity.dispatchKeyEvent(event) // must not throw
        }
    }
}
