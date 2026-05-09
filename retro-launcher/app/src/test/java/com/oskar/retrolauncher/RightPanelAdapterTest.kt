package com.oskar.retrolauncher

import com.oskar.retrolauncher.ui.embed.EmbedFragment
import com.oskar.retrolauncher.ui.grid.AppGridFragment
import com.oskar.retrolauncher.ui.home.RightPanelAdapter
import com.oskar.retrolauncher.ui.trips.TripsFragment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T1.8 AC3: RightPanelAdapter extends FragmentStateAdapter and returns
 *           EmbedFragment / AppGridFragment / TripsFragment.
 *
 * Uses the testable companion (`PAGE_COUNT`, `newPage`) so we don't need to wire up
 * a real FragmentManager + parent Fragment to verify the page-creation logic.
 */
class RightPanelAdapterTest {

    @Test
    fun `adapter exposes three pages`() {
        assertEquals(3, RightPanelAdapter.PAGE_COUNT)
    }

    @Test
    fun `page 0 is EmbedFragment`() {
        assertTrue(RightPanelAdapter.newPage(0) is EmbedFragment)
    }

    @Test
    fun `page 1 is AppGridFragment`() {
        assertTrue(RightPanelAdapter.newPage(1) is AppGridFragment)
    }

    @Test
    fun `page 2 is TripsFragment`() {
        assertTrue(RightPanelAdapter.newPage(2) is TripsFragment)
    }

    @Test(expected = IllegalStateException::class)
    fun `out-of-range position errors`() {
        RightPanelAdapter.newPage(3)
    }
}
