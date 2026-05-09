package com.oskar.retrolauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

/**
 * T1.8 ACs covered:
 *   - AC1: fragment_home.xml has a `home_split` Guideline driving the panel split.
 *   - AC5: default split is 0.4 (40%).
 *
 * fragment_home.xml uses `<FragmentContainerView android:name="…">` for the media + weather
 * slots, which can't inflate via plain LayoutInflater (it needs a FragmentManager). We walk
 * the compiled XML attribute set directly instead — same fidelity, no Fragment lifecycle.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class HomeFragmentTest {

    private companion object {
        const val NS_ANDROID = "http://schemas.android.com/apk/res/android"
        const val NS_APP = "http://schemas.android.com/apk/res-auto"
    }

    @Test
    fun `fragment_home has home_split guideline at 0_40 default and a right_pager`() {
        val ctx = RuntimeEnvironment.getApplication()
        val parser = ctx.resources.getLayout(R.layout.fragment_home)

        var sawHomeSplit = false
        var homeSplitPercent = Float.NaN
        var sawRightPager = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                val id = parser.getAttributeResourceValue(NS_ANDROID, "id", 0)
                when (id) {
                    R.id.home_split -> {
                        sawHomeSplit = true
                        homeSplitPercent = parser.getAttributeFloatValue(
                            NS_APP, "layout_constraintGuide_percent", Float.NaN,
                        )
                    }
                    R.id.right_pager -> sawRightPager = true
                }
            }
            event = parser.next()
        }

        assertTrue("home_split guideline must be declared in fragment_home.xml", sawHomeSplit)
        assertTrue("right_pager (ViewPager2) must be declared in fragment_home.xml", sawRightPager)
        assertEquals(
            "default panel split must be 0.40 per spec section 8 (AC5)",
            0.40f, homeSplitPercent, 1e-4f,
        )
    }
}
