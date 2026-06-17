package com.oskar.retrolauncher

import com.oskar.retrolauncher.ui.dashboard.DashboardFragment
import com.oskar.retrolauncher.ui.embed.EmbedFragment
import com.oskar.retrolauncher.ui.grid.AppGridFragment
import com.oskar.retrolauncher.ui.home.RightPanelAdapter
import com.oskar.retrolauncher.ui.trips.TripsFragment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the right-panel page order/count via the testable companion
 * (`PAGE_COUNT`, `newPage`) — no FragmentManager needed. A.1 reordered the pages
 * so the driving dashboard is the landing page and embed moved to the end.
 */
class RightPanelAdapterTest {

    @Test
    fun `adapter exposes four pages with embed last`() {
        assertEquals(4, RightPanelAdapter.PAGE_COUNT)
        assertEquals(3, RightPanelAdapter.EMBED_PAGE)
    }

    @Test
    fun `page 0 is the dashboard landing page`() {
        assertTrue(RightPanelAdapter.newPage(0) is DashboardFragment)
    }

    @Test
    fun `page 1 is AppGridFragment`() {
        assertTrue(RightPanelAdapter.newPage(1) is AppGridFragment)
    }

    @Test
    fun `page 2 is TripsFragment`() {
        assertTrue(RightPanelAdapter.newPage(2) is TripsFragment)
    }

    @Test
    fun `page 3 is EmbedFragment`() {
        assertTrue(RightPanelAdapter.newPage(3) is EmbedFragment)
    }

    @Test(expected = IllegalStateException::class)
    fun `out-of-range position errors`() {
        RightPanelAdapter.newPage(4)
    }
}
