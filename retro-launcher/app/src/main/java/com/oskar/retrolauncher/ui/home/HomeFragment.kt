package com.oskar.retrolauncher.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Guideline
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.prefs.SettingsStore
import com.oskar.retrolauncher.ui.embed.EmbedFragment
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * Home screen: media + weather tiles on the left, swipeable embed/grid/trips on the right.
 * Owns the panel-ratio guideline so MainActivity doesn't have to reach across fragments.
 */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private var currentPage = 0 // start on the dashboard
    private var embedFragment: EmbedFragment? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val pager = view.findViewById<ViewPager2>(R.id.right_pager)
        val adapter = RightPanelAdapter(this)
        pager.adapter = adapter
        pager.offscreenPageLimit = 1
        pager.setCurrentItem(0, false) // Open on the driving dashboard by default

        val tabs = view.findViewById<PanelTabStrip>(R.id.panel_tabs)
        tabs.attachTo(
            pager,
            listOf(
                getString(R.string.tab_dashboard),
                getString(R.string.tab_apps),
                getString(R.string.tab_trips),
                getString(R.string.tab_map),
            ),
        )

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val prevPage = currentPage
                currentPage = position
                if (prevPage == RightPanelAdapter.EMBED_PAGE && position != RightPanelAdapter.EMBED_PAGE) {
                    // Leaving embed page — send embedded app to background
                    embedFragment?.pauseEmbeddedApp()
                }
                if (position == RightPanelAdapter.EMBED_PAGE) {
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

        // Media card sizing: enlarge / replace the weather tile (M5).
        val root = view as ConstraintLayout
        applyMediaLayout(root)
        viewLifecycleOwner.lifecycleScope.launch {
            merge(
                App.settings.changes(SettingsStore.KEY_MEDIA_CARD_SIZE),
                App.settings.changes(SettingsStore.KEY_MEDIA_REPLACES_WEATHER),
            ).collect { applyMediaLayout(root) }
        }
    }

    /**
     * Reflect the media-card sizing prefs: when it replaces weather, hide the
     * weather tile and stretch media to the full left column; when only
     * enlarged, give media the larger share of the column.
     */
    private fun applyMediaLayout(root: ConstraintLayout) {
        val replaces = App.settings.mediaReplacesWeather
        val enlarged = App.settings.mediaCardEnlarged
        val set = ConstraintSet().apply { clone(root) }
        if (replaces) {
            set.setVisibility(R.id.weather_slot, View.GONE)
            set.connect(R.id.media_slot, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        } else {
            set.setVisibility(R.id.weather_slot, View.VISIBLE)
            set.connect(R.id.media_slot, ConstraintSet.BOTTOM, R.id.left_split, ConstraintSet.TOP)
            set.setGuidelinePercent(R.id.left_split, if (enlarged) 0.62f else 0.50f)
        }
        set.applyTo(root)
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
