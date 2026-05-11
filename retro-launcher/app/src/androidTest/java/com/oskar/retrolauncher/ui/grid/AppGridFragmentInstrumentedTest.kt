package com.oskar.retrolauncher.ui.grid

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.test.ScreenshotOnFailureRule
import com.oskar.retrolauncher.test.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T1.36 — instrumented coverage for [AppGridFragment].
 *
 *   1. Empty state: appList seeded empty → grid + rail render zero items.
 *   2. Populated state: seeding 4 apps shows them in the grid.
 *   3. User action: tapping an app icon does not crash (the
 *      `startActivity` call is wrapped in `runCatching` so a missing
 *      target activity in the test apk falls through silently).
 */
@RunWith(AndroidJUnit4::class)
class AppGridFragmentInstrumentedTest {

    @get:Rule val screenshotRule = ScreenshotOnFailureRule()

    private val app: App get() = ApplicationProvider.getApplicationContext()

    @Test
    fun emptyState_renderEmptyGridAndRail() {
        app.service.appList.setForTest(all = emptyList(), pinned = emptyList())
        val scenario = launchFragmentInContainer<AppGridFragment>(
            themeResId = R.style.Theme_RetroLauncher,
        )
        Thread.sleep(200)
        scenario.onFragment { f ->
            val grid = f.requireView().findViewById<RecyclerView>(R.id.grid)
            val rail = f.requireView().findViewById<RecyclerView>(R.id.rail)
            assertEquals(0, grid.adapter?.itemCount ?: -1)
            assertEquals(0, rail.adapter?.itemCount ?: -1)
        }
    }

    @Test
    fun populatedState_showsAppsInGrid() {
        val apps = listOf(
            TestData.appEntry(label = "Alpha",  packageName = "test.alpha"),
            TestData.appEntry(label = "Bravo",  packageName = "test.bravo"),
            TestData.appEntry(label = "Charlie",packageName = "test.charlie"),
            TestData.appEntry(label = "Delta",  packageName = "test.delta"),
        )
        app.service.appList.setForTest(all = apps, pinned = listOf("test.alpha"))

        val scenario = launchFragmentInContainer<AppGridFragment>(
            themeResId = R.style.Theme_RetroLauncher,
        )
        Thread.sleep(300)
        scenario.onFragment { f ->
            val grid = f.requireView().findViewById<RecyclerView>(R.id.grid)
            val rail = f.requireView().findViewById<RecyclerView>(R.id.rail)
            assertEquals(4, grid.adapter?.itemCount ?: -1)
            assertEquals(1, rail.adapter?.itemCount ?: -1)
        }
        onView(withId(R.id.grid)).check(matches(isDisplayed()))
    }

    @Test
    fun tapAppIcon_doesNotCrash() {
        val apps = listOf(
            TestData.appEntry(label = "Alpha", packageName = "test.alpha"),
        )
        app.service.appList.setForTest(all = apps)
        launchFragmentInContainer<AppGridFragment>(themeResId = R.style.Theme_RetroLauncher)
        Thread.sleep(300)

        // Click the first cell. AppGridFragment.launch wraps startActivity()
        // in runCatching, so a non-resolvable test.alpha component is fine.
        onView(withId(R.id.grid))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        // Still rendered after the click.
        onView(withId(R.id.grid)).check(matches(isDisplayed()))
        assertTrue(true)
    }
}
