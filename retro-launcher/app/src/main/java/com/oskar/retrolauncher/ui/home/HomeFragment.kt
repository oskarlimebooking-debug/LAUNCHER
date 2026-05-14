package com.oskar.retrolauncher.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.Guideline
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.prefs.SettingsStore
import com.oskar.retrolauncher.ui.embed.EmbedFragment
import kotlinx.coroutines.launch

/**
 * Home screen: media + weather tiles on the left, swipeable embed/grid/trips on the right.
 * Owns the panel-ratio guideline so MainActivity doesn't have to reach across fragments.
 */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private var currentPage = 1 // start on app grid
    private var embedFragment: EmbedFragment? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val pager = view.findViewById<ViewPager2>(R.id.right_pager)
        val adapter = RightPanelAdapter(this)
        pager.adapter = adapter
        pager.offscreenPageLimit = 1
        pager.setCurrentItem(1, false) // Open on the app grid by default

        val dots = view.findViewById<DotsIndicator>(R.id.dots)
        dots.attachTo(pager)

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val prevPage = currentPage
                currentPage = position
                if (prevPage == 0 && position != 0) {
                    // Leaving embed page — send embedded app to background
                    embedFragment?.pauseEmbeddedApp()
                }
                if (position == 0) {
                    launchEmbedApp()
                }
            }
        })

        // T1.8 AC1/AC4/AC5: panel split is read from settings and reflected via the
        // Guideline's LayoutParams. setGuidelinePercent updates LayoutParams.guidePercent
        // and triggers a single layout pass — no Activity recreation.
        val homeSplit = view.findViewById<Guideline>(R.id.home_split)
        applyPanelRatio(homeSplit, App.settings.panelRatioPercent)
        viewLifecycleOwner.lifecycleScope.launch {
            App.settings.changes(SettingsStore.KEY_PANEL_RATIO).collect {
                applyPanelRatio(homeSplit, App.settings.panelRatioPercent)
            }
        }
    }

    /** Supply the cached fragment so [onPageSelected] can reach it. */
    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        if (childFragment is EmbedFragment) {
            embedFragment = childFragment
        }
    }

    private fun launchEmbedApp() {
        val pkg = App.settings.embedApp ?: return
        val intent = requireContext().packageManager.getLaunchIntentForPackage(pkg)
            ?: return
        embedFragment?.launchEmbedded(intent)
    }

    private fun applyPanelRatio(guideline: Guideline, leftPct: Int) {
        guideline.setGuidelinePercent(leftPct.coerceIn(30, 70) / 100f)
    }
}
