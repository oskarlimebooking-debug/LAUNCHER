package com.oskar.retrolauncher

import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.oskar.retrolauncher.ui.home.PanelTabStrip
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * A.2 — the labelled tab strip renders one tab per supplied label (upper-cased)
 * and tapping a tab drives the pager. Interaction/highlight on real swipes is
 * covered by the instrumented HomeFragment test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class PanelTabStripTest {

    private fun pager() = ViewPager2(RuntimeEnvironment.getApplication())

    @Test
    fun `builds one upper-cased tab per label`() {
        val strip = PanelTabStrip(RuntimeEnvironment.getApplication())
        strip.attachTo(pager(), listOf("Dashboard", "Apps", "Trips", "Map"))

        assertEquals(4, strip.childCount)
        assertEquals("DASHBOARD", (strip.getChildAt(0) as TextView).text.toString())
        assertEquals("APPS", (strip.getChildAt(1) as TextView).text.toString())
        assertEquals("TRIPS", (strip.getChildAt(2) as TextView).text.toString())
        assertEquals("MAP", (strip.getChildAt(3) as TextView).text.toString())
    }

    @Test
    fun `re-attaching rebuilds the tab set`() {
        val strip = PanelTabStrip(RuntimeEnvironment.getApplication())
        strip.attachTo(pager(), listOf("Map", "Apps", "Trips"))
        strip.attachTo(pager(), listOf("Dashboard", "Apps", "Trips", "Map"))
        assertEquals(4, strip.childCount)
    }
}
