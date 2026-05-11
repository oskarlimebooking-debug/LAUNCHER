package com.oskar.retrolauncher.ui.speed

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.location.LocationSample
import com.oskar.retrolauncher.test.ScreenshotOnFailureRule
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T1.36 — instrumented coverage for [SpeedFragment].
 *
 *   1. Empty state: no GPS sample yet → speedometer reads 0 km/h.
 *   2. Populated state: pushing a 50 km/h sample drives the gauge above 0.
 *   3. Speedometer view is rendered and laid out (non-zero size).
 */
@RunWith(AndroidJUnit4::class)
class SpeedFragmentInstrumentedTest {

    @get:Rule val screenshotRule = ScreenshotOnFailureRule()

    private val app: App get() = ApplicationProvider.getApplicationContext()

    @Test
    fun emptyState_speedometerReadsZero() {
        // No location samples pushed — VM seeds with sample=null → 0 km/h.
        val scenario = launchFragmentInContainer<SpeedFragment>(
            themeResId = R.style.Theme_RetroLauncher,
        )
        onView(withId(R.id.speedometer)).check(matches(isDisplayed()))
        scenario.onFragment { f ->
            val gauge = f.requireView().findViewById<SpeedometerView>(R.id.speedometer)
            assertEquals(0f, gauge.speedKmh, 0.01f)
        }
    }

    @Test
    fun populatedState_pushingFifteenMsAdvancesGauge() {
        val scenario = launchFragmentInContainer<SpeedFragment>(
            themeResId = R.style.Theme_RetroLauncher,
        )
        // Push a sample on the instrumentation thread; LocationRepository's
        // SharedFlow emits on subscribe via replay=1, but new pushes need
        // to be routed back through the fragment's collector — wait briefly.
        app.service.location.push(
            LocationSample(
                tsMs = System.currentTimeMillis(),
                lat = 0.0, lon = 0.0,
                accuracy = 5f,
                speedMs = 15f,
                bearing = 0f,
            ),
        )
        // The SpeedometerView animates over 250 ms. Wait for at least one
        // animator frame to land before reading speedKmh.
        Thread.sleep(400)
        scenario.onFragment { f ->
            val gauge = f.requireView().findViewById<SpeedometerView>(R.id.speedometer)
            assertGreaterThan(gauge.speedKmh, 0f)
        }
    }

    @Test
    fun speedometer_isLaidOutAfterAttach() {
        val scenario = launchFragmentInContainer<SpeedFragment>(
            themeResId = R.style.Theme_RetroLauncher,
        )
        onView(withId(R.id.speedometer)).check(matches(hasNonZeroSize()))
        scenario.onFragment { f ->
            val gauge = f.requireView().findViewById<SpeedometerView>(R.id.speedometer)
            // Default threshold from SpeedViewModel seed is 50 km/h.
            assertEquals(50f, gauge.thresholdKmh, 0.01f)
        }
    }

    private fun assertGreaterThan(actual: Float, lowerBound: Float) {
        if (actual <= lowerBound) {
            throw AssertionError("expected > $lowerBound but was $actual")
        }
    }

    private fun hasNonZeroSize() = object : TypeSafeMatcher<android.view.View>() {
        override fun describeTo(description: Description) {
            description.appendText("has non-zero width and height")
        }
        override fun matchesSafely(item: android.view.View): Boolean =
            item.width > 0 && item.height > 0
    }

}
