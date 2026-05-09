package com.oskar.retrolauncher

import android.view.View
import android.widget.FrameLayout
import com.oskar.retrolauncher.util.fade
import com.oskar.retrolauncher.util.gone
import com.oskar.retrolauncher.util.visible
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * T1.10 ACs:
 *   - View.gone()/visible()/fade() exist and behave per spec section 17 (AC4 file size,
 *     AC5 ≥ 3 cases per public function including a zero/edge case).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class ViewExtTest {

    private fun newView(): View = FrameLayout(RuntimeEnvironment.getApplication())

    @Test
    fun `gone sets visibility to GONE from VISIBLE`() {
        val v = newView().also { it.visibility = View.VISIBLE }
        v.gone()
        assertEquals(View.GONE, v.visibility)
    }

    @Test
    fun `gone sets visibility to GONE from INVISIBLE`() {
        val v = newView().also { it.visibility = View.INVISIBLE }
        v.gone()
        assertEquals(View.GONE, v.visibility)
    }

    @Test
    fun `gone is idempotent when already GONE`() {
        val v = newView().also { it.visibility = View.GONE }
        v.gone()
        assertEquals(View.GONE, v.visibility)
    }

    @Test
    fun `visible sets visibility to VISIBLE from GONE`() {
        val v = newView().also { it.visibility = View.GONE }
        v.visible()
        assertEquals(View.VISIBLE, v.visibility)
    }

    @Test
    fun `visible sets visibility to VISIBLE from INVISIBLE`() {
        val v = newView().also { it.visibility = View.INVISIBLE }
        v.visible()
        assertEquals(View.VISIBLE, v.visibility)
    }

    @Test
    fun `visible is idempotent when already VISIBLE`() {
        val v = newView().also { it.visibility = View.VISIBLE }
        v.visible()
        assertEquals(View.VISIBLE, v.visibility)
    }

    @Test
    fun `fade(true) starting GONE sets visibility VISIBLE and alpha 0 to animate up`() {
        val v = newView().also { it.visibility = View.GONE; it.alpha = 1f }
        v.fade(toVisible = true)
        assertEquals(View.VISIBLE, v.visibility)
        assertEquals(0f, v.alpha, 1e-6f)
    }

    @Test
    fun `fade(true) starting INVISIBLE sets visibility VISIBLE and alpha 0`() {
        val v = newView().also { it.visibility = View.INVISIBLE; it.alpha = 1f }
        v.fade(toVisible = true)
        assertEquals(View.VISIBLE, v.visibility)
        assertEquals(0f, v.alpha, 1e-6f)
    }

    @Test
    fun `fade(true) on already VISIBLE leaves alpha unchanged on entry (animation finishes it to 1)`() {
        val v = newView().also { it.visibility = View.VISIBLE; it.alpha = 0.3f }
        v.fade(toVisible = true)
        // Already visible — fade() does NOT reset alpha to 0; it just animates to 1 from current.
        assertEquals(View.VISIBLE, v.visibility)
        assertEquals(0.3f, v.alpha, 1e-6f)
    }

    @Test
    fun `fade(false) keeps view visible until animation completes`() {
        val v = newView().also { it.visibility = View.VISIBLE; it.alpha = 1f }
        v.fade(toVisible = false)
        // The view stays VISIBLE while the fade-out animation runs (visibility flips to GONE
        // in withEndAction).
        assertEquals(View.VISIBLE, v.visibility)
    }
}
