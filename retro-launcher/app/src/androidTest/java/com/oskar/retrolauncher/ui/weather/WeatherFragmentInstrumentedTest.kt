package com.oskar.retrolauncher.ui.weather

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.test.ScreenshotOnFailureRule
import com.oskar.retrolauncher.test.TestData
import org.hamcrest.CoreMatchers.containsString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T1.36 — instrumented coverage for [WeatherFragment].
 *
 *   1. Empty state: no snapshot → "—" temp + "Updating…" copy.
 *   2. Populated state: pushing a snapshot renders the city + condition.
 *   3. Snapshot churn: clearing the snapshot after a populated render falls
 *      back to the placeholder, exercising both branches of the LiveData
 *      observer.
 */
@RunWith(AndroidJUnit4::class)
class WeatherFragmentInstrumentedTest {

    @get:Rule val screenshotRule = ScreenshotOnFailureRule()

    private val app: App
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as App

    @Test
    fun emptyState_showsPlaceholderCopy() {
        app.service.weather.setSnapshotForTest(null)
        launchFragmentInContainer<WeatherFragment>(themeResId = R.style.Theme_RetroLauncher)

        onView(withId(R.id.temp))
            .check(matches(isDisplayed()))
            .check(matches(withText("—")))
        onView(withId(R.id.condition))
            .check(matches(withText(R.string.weather_loading)))
    }

    @Test
    fun populatedState_rendersCityAndCondition() {
        app.service.weather.setSnapshotForTest(
            TestData.weatherSnapshot(condition = "few clouds", city = "Tallinn"),
        )
        launchFragmentInContainer<WeatherFragment>(themeResId = R.style.Theme_RetroLauncher)

        // Condition is title-cased per WeatherFragment.renderSnapshot.
        onView(withId(R.id.condition)).check(matches(withText("Few clouds")))
        onView(withId(R.id.city)).check(matches(withText("Tallinn")))
        onView(withId(R.id.temp)).check(matches(withText(containsString("°"))))
    }

    @Test
    fun snapshotChurn_revertsToPlaceholderWhenCleared() {
        app.service.weather.setSnapshotForTest(TestData.weatherSnapshot(city = "Tallinn"))
        launchFragmentInContainer<WeatherFragment>(themeResId = R.style.Theme_RetroLauncher)
        onView(withId(R.id.city)).check(matches(withText("Tallinn")))

        app.service.weather.setSnapshotForTest(null)
        // The fragment observer re-runs on the next state emission.
        onView(withId(R.id.temp)).check(matches(withText("—")))
        onView(withId(R.id.condition)).check(matches(withText(R.string.weather_loading)))
    }
}
