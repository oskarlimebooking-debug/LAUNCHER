package com.oskar.retrolauncher.ui.home

import androidx.constraintlayout.widget.Guideline
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.media.MediaState
import com.oskar.retrolauncher.data.prefs.SettingsStore
import com.oskar.retrolauncher.test.ScreenshotOnFailureRule
import com.oskar.retrolauncher.test.TestData
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T1.36 — instrumented coverage for [HomeFragment].
 *
 *   1. Empty state: defaults render with the spec'd 0.40 panel split and
 *      the right pager opens on page 1 (the app grid).
 *   2. Populated state: changing [SettingsStore.panelRatioPercent] flows
 *      through to the home_split guideline.
 *   3. User action: programmatically swiping the right_pager to a
 *      different page is reflected in `currentItem`.
 */
@RunWith(AndroidJUnit4::class)
class HomeFragmentInstrumentedTest {

    @get:Rule val screenshotRule = ScreenshotOnFailureRule()

    private val app: App get() = ApplicationProvider.getApplicationContext()

    @Before
    fun seedClean() {
        // HomeFragment hosts MediaFragment + WeatherFragment as child slots,
        // and the right pager hosts AppGridFragment which calls
        // appList.refresh() on attach. Pre-seed those repos so no children
        // attempt to render against unbounded real data.
        app.service.media.update(MediaState.empty)
        app.service.weather.setSnapshotForTest(null)
        app.service.appList.setForTest(all = emptyList())
    }

    @Test
    fun emptyState_rendersDefaultSplitAndDefaultPage() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_RetroLauncher,
        )
        Thread.sleep(200)
        scenario.onFragment { f ->
            val v = f.requireView()
            val splitGuideline = v.findViewById<Guideline>(R.id.home_split)
            val pager = v.findViewById<ViewPager2>(R.id.right_pager)
            val params = splitGuideline.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            assertEquals("default panel split is 0.40", 0.40f, params.guidePercent, 1e-3f)
            assertEquals("right pager opens on the grid (page 1)", 1, pager.currentItem)
        }
        onView(withId(R.id.right_pager)).check(matches(isDisplayed()))
    }

    @Test
    fun populatedState_panelRatioSettingFlowsIntoGuideline() {
        // SettingsStore exposes panelRatioPercent read-only — write to the
        // backing prefs key directly. HomeFragment observes
        // settings.changes(KEY_PANEL_RATIO) and re-applies the guideline.
        app.service.prefs.edit()
            .putInt(SettingsStore.KEY_PANEL_RATIO, 60)
            .commit()
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_RetroLauncher,
        )
        Thread.sleep(200)
        scenario.onFragment { f ->
            val splitGuideline = f.requireView().findViewById<Guideline>(R.id.home_split)
            val params = splitGuideline.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            assertEquals(0.60f, params.guidePercent, 1e-3f)
        }
        // Mutate again — the changes() flow should re-bind on the next emission.
        app.service.prefs.edit()
            .putInt(SettingsStore.KEY_PANEL_RATIO, 35)
            .commit()
        Thread.sleep(400)
        scenario.onFragment { f ->
            val splitGuideline = f.requireView().findViewById<Guideline>(R.id.home_split)
            val params = splitGuideline.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            assertEquals(0.35f, params.guidePercent, 1e-3f)
        }
    }

    @Test
    fun rightPager_supportsProgrammaticPageChange() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_RetroLauncher,
        )
        Thread.sleep(200)
        scenario.onFragment { f ->
            val pager = f.requireView().findViewById<ViewPager2>(R.id.right_pager)
            // Drive the pager to "Trips" (page 2) without animation so the
            // assertion can read the new current page synchronously.
            pager.setCurrentItem(2, /* smoothScroll = */ false)
        }
        Thread.sleep(150)
        scenario.onFragment { f ->
            val pager = f.requireView().findViewById<ViewPager2>(R.id.right_pager)
            assertEquals(2, pager.currentItem)
        }
        // Touch a Test fixture so we keep the import alive without warnings.
        TestData.mediaState()
    }
}
