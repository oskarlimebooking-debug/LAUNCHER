package com.oskar.retrolauncher

import com.oskar.retrolauncher.ui.media.MediaFragment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

/**
 * T1.17 ACs verified by walking the compiled fragment_media.xml:
 *   - AC1: art/title/artist/play_pause/prev/next/progress IDs all declared.
 *   - AC2: title + artist TextViews are configured for marquee animation
 *     (ellipsize=marquee, marqueeRepeatLimit=marquee_forever).
 *
 * We walk the XML directly — the same pattern as HomeFragmentTest /
 * SpeedFragmentTest — because fragment_media.xml has no FragmentContainerView
 * children and we just need to assert structural attributes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class MediaFragmentTest {

    private companion object {
        const val NS_ANDROID = "http://schemas.android.com/apk/res/android"
        // android:ellipsize compiled-XML enum (from frameworks/base/core/res/res/values/attrs.xml):
        // none=0, start=1, middle=2, end=3, marquee=4
        const val ELLIPSIZE_MARQUEE = 4
    }

    @Test
    fun `fragment_media has required IDs`() {
        val ctx = RuntimeEnvironment.getApplication()
        val parser = ctx.resources.getLayout(R.layout.fragment_media)

        var sawArt = false
        var sawTitle = false
        var sawArtist = false
        var sawPlayPause = false
        var sawPrev = false
        var sawNext = false
        var sawProgress = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.getAttributeResourceValue(NS_ANDROID, "id", 0)) {
                    R.id.art -> sawArt = true
                    R.id.title -> sawTitle = true
                    R.id.artist -> sawArtist = true
                    R.id.play_pause -> sawPlayPause = true
                    R.id.prev -> sawPrev = true
                    R.id.next -> sawNext = true
                    R.id.progress -> sawProgress = true
                }
            }
            event = parser.next()
        }

        assertTrue("art ImageView required", sawArt)
        assertTrue("title TextView required", sawTitle)
        assertTrue("artist TextView required", sawArtist)
        assertTrue("play_pause ImageButton required", sawPlayPause)
        assertTrue("prev ImageButton required", sawPrev)
        assertTrue("next ImageButton required", sawNext)
        assertTrue("progress (seek bar) required", sawProgress)
    }

    @Test
    fun `title and artist TextViews are configured for marquee (AC2)`() {
        val ctx = RuntimeEnvironment.getApplication()
        val parser = ctx.resources.getLayout(R.layout.fragment_media)

        var titleEllipsize = -1
        var artistEllipsize = -1
        var titleMarqueeLimit = 0
        var artistMarqueeLimit = 0

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.getAttributeResourceValue(NS_ANDROID, "id", 0)) {
                    R.id.title -> {
                        titleEllipsize = parser.getAttributeIntValue(
                            NS_ANDROID, "ellipsize", -1,
                        )
                        titleMarqueeLimit = parser.getAttributeIntValue(
                            NS_ANDROID, "marqueeRepeatLimit", 0,
                        )
                    }
                    R.id.artist -> {
                        artistEllipsize = parser.getAttributeIntValue(
                            NS_ANDROID, "ellipsize", -1,
                        )
                        artistMarqueeLimit = parser.getAttributeIntValue(
                            NS_ANDROID, "marqueeRepeatLimit", 0,
                        )
                    }
                }
            }
            event = parser.next()
        }

        assertEquals(
            "title TextView must use ellipsize=marquee for AC2",
            ELLIPSIZE_MARQUEE, titleEllipsize,
        )
        assertEquals(
            "artist TextView must use ellipsize=marquee for AC2",
            ELLIPSIZE_MARQUEE, artistEllipsize,
        )
        // marqueeRepeatLimit="marquee_forever" compiles to -1 in the compiled binary XML.
        assertEquals(
            "title TextView must repeat marquee forever",
            -1, titleMarqueeLimit,
        )
        assertEquals(
            "artist TextView must repeat marquee forever",
            -1, artistMarqueeLimit,
        )
    }

    @Test
    fun `MediaFragment class can be instantiated via no-args ctor`() {
        // FragmentManager requires the no-args constructor.
        MediaFragment()
    }
}
