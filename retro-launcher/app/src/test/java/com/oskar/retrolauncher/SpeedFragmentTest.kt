package com.oskar.retrolauncher

import com.oskar.retrolauncher.ui.speed.SpeedFragment
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

/**
 * T1.14 AC1: the fragment displays a SpeedometerView. We verify by walking the
 * compiled XML — same approach as HomeFragmentTest / StatusBarFragmentTest, no
 * FragmentManager needed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class SpeedFragmentTest {

    @Test
    fun `fragment_speed contains a SpeedometerView`() {
        val ctx = RuntimeEnvironment.getApplication()
        val parser = ctx.resources.getLayout(R.layout.fragment_speed)
        var sawSpeedometer = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name.endsWith(".SpeedometerView")) {
                sawSpeedometer = true
            }
            event = parser.next()
        }
        assertTrue(
            "fragment_speed.xml must declare a com.oskar.retrolauncher.ui.speed.SpeedometerView",
            sawSpeedometer,
        )
    }

    @Test
    fun `SpeedFragment class can be instantiated`() {
        // Default no-args constructor is required by FragmentManager.
        SpeedFragment()
    }
}
