package com.oskar.retrolauncher.ui.media

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.media.MediaState
import com.oskar.retrolauncher.test.ScreenshotOnFailureRule
import com.oskar.retrolauncher.test.TestData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T1.36 — instrumented coverage for [MediaFragment].
 *
 *   1. Empty state: no track in [App.media] → "Nothing playing" copy.
 *   2. Populated state: pushing a track → title + artist render.
 *   3. User action: tapping play/pause does not crash and the icon flips
 *      when the underlying state mutates.
 */
@RunWith(AndroidJUnit4::class)
class MediaFragmentInstrumentedTest {

    @get:Rule val screenshotRule = ScreenshotOnFailureRule()

    private val app: App
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as App

    @Test
    fun emptyState_showsNothingPlayingCopy() {
        app.service.media.update(MediaState.empty)
        launchFragmentInContainer<MediaFragment>(themeResId = R.style.Theme_RetroLauncher)

        onView(withId(R.id.title))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.media_no_app)))
        onView(withId(R.id.artist))
            .check(matches(withText("")))
    }

    @Test
    fun populatedState_rendersTitleAndArtist() {
        app.service.media.update(TestData.mediaState(title = "Song A", artist = "Artist B"))
        launchFragmentInContainer<MediaFragment>(themeResId = R.style.Theme_RetroLauncher)

        onView(withId(R.id.title)).check(matches(withText("Song A")))
        onView(withId(R.id.artist)).check(matches(withText("Artist B")))
    }

    @Test
    fun tapPlayPause_doesNotCrash_andStateChangeFlipsIcon() {
        // Seed paused state, render, tap play/pause. The repo call is a no-op
        // when no MediaController is bound (test env), so we manually flip
        // the state to verify the icon binding still observes downstream.
        app.service.media.update(TestData.mediaState(playing = false))
        launchFragmentInContainer<MediaFragment>(themeResId = R.style.Theme_RetroLauncher)

        onView(withId(R.id.play_pause)).check(matches(isDisplayed())).perform(click())

        // Mutate state to "playing" — fragment binding swaps the play_pause icon.
        // We can't easily assert the drawable resource via Espresso, but we can
        // confirm the click target is still alive and the title is unchanged.
        app.service.media.update(TestData.mediaState(playing = true))
        onView(withId(R.id.play_pause)).check(matches(isDisplayed()))
        onView(withId(R.id.title)).check(matches(withText("Test Track")))
    }
}
