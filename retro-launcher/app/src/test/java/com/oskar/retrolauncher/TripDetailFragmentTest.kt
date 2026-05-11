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
 * T1.39 — verifies the trip-detail layout has the required map view, polyline
 * container, stats panel, and address text fields. Layout-only walk (mirrors
 * the TripsFragmentTest pattern in T1.27): inflating a `MapView` under
 * Robolectric requires the osmdroid native deps so we only verify IDs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class TripDetailFragmentTest {

    private companion object {
        const val NS_ANDROID = "http://schemas.android.com/apk/res/android"
    }

    @Test
    fun `fragment_trip_detail declares all required IDs`() {
        val ctx = RuntimeEnvironment.getApplication()
        val parser = ctx.resources.getLayout(R.layout.fragment_trip_detail)

        val seen = mutableSetOf<Int>()
        var rootOrientation = -1
        var event = parser.eventType
        var depth = 0
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                if (depth == 0) {
                    rootOrientation = parser.getAttributeIntValue(NS_ANDROID, "orientation", -1)
                }
                val id = parser.getAttributeResourceValue(NS_ANDROID, "id", 0)
                if (id != 0) seen.add(id)
                depth++
            } else if (event == XmlPullParser.END_TAG) {
                depth--
            }
            event = parser.next()
        }

        // Vertical root: map on top, stats panel at bottom.
        assertEquals("root must be vertical (map above stats)", 1, rootOrientation)
        assertTrue("trip_map missing", seen.contains(R.id.trip_map))
        assertTrue("trip_stats panel missing", seen.contains(R.id.trip_stats))
        assertTrue("stat_distance missing", seen.contains(R.id.stat_distance))
        assertTrue("stat_duration missing", seen.contains(R.id.stat_duration))
        assertTrue("stat_max_speed missing", seen.contains(R.id.stat_max_speed))
        assertTrue("stat_avg_speed missing", seen.contains(R.id.stat_avg_speed))
        assertTrue("stat_start_addr missing", seen.contains(R.id.stat_start_addr))
        assertTrue("stat_end_addr missing", seen.contains(R.id.stat_end_addr))
        assertTrue("trip_detail_back missing", seen.contains(R.id.trip_detail_back))
    }
}
