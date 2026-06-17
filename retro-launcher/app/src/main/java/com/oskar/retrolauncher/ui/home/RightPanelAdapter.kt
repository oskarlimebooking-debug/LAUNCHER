package com.oskar.retrolauncher.ui.home

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.oskar.retrolauncher.ui.dashboard.DashboardFragment
import com.oskar.retrolauncher.ui.embed.EmbedFragment
import com.oskar.retrolauncher.ui.grid.AppGridFragment
import com.oskar.retrolauncher.ui.trips.TripsFragment

/**
 * Right-panel pager adapter for the launcher home screen. Four pages:
 *   0 — Driving dashboard (the landing page)
 *   1 — App grid
 *   2 — Trip history
 *   3 — Embed (map / embedded app, ships in v0.3)
 *
 * Page-creation logic lives on the companion (`newPage`) so tests can verify it
 * without instantiating a `FragmentManager` (T1.8 AC3).
 */
internal class RightPanelAdapter(host: Fragment) : FragmentStateAdapter(host) {

    override fun getItemCount(): Int = PAGE_COUNT

    override fun createFragment(position: Int): Fragment = newPage(position)

    companion object {
        const val PAGE_COUNT = 4

        /** Index of the embed page (map / embedded app). */
        const val EMBED_PAGE = 3

        fun newPage(position: Int): Fragment = when (position) {
            0 -> DashboardFragment()
            1 -> AppGridFragment()
            2 -> TripsFragment()
            3 -> EmbedFragment()
            else -> error("RightPanelAdapter: invalid position $position")
        }
    }
}
