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
 * T1.27 ACs covered by walking the compiled fragment_trips.xml:
 *   - AC1: calendar (`@id/calendar`) and `@id/trip_list` RecyclerView are both present.
 *   - AC4: `@id/empty` TextView with the trips_none string is in the layout, ready
 *          to be toggled visible by the fragment.
 *   - AC5: layout fits the 60% right panel — the root LinearLayout is vertical,
 *          calendar uses wrap_content and the RecyclerView uses weight=1 so the
 *          list fills remaining space at 1024×600.
 *
 * The fragment_trips layout has no FragmentContainerView children so we walk
 * the compiled XML directly — same pattern as HomeFragmentTest / MediaFragmentTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class TripsFragmentTest {

    private companion object {
        const val NS_ANDROID = "http://schemas.android.com/apk/res/android"
    }

    @Test
    fun `fragment_trips has month_label calendar summary trip_list and empty IDs`() {
        val ctx = RuntimeEnvironment.getApplication()
        val parser = ctx.resources.getLayout(R.layout.fragment_trips)

        var sawMonthLabel = false
        var sawCalendar = false
        var sawSummary = false
        var sawTripList = false
        var sawEmpty = false
        var emptyTextResId = 0
        var tripListWeight = Float.NaN
        var rootOrientation = -1

        var event = parser.eventType
        var depth = 0
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                if (depth == 0) {
                    rootOrientation = parser.getAttributeIntValue(NS_ANDROID, "orientation", -1)
                }
                when (parser.getAttributeResourceValue(NS_ANDROID, "id", 0)) {
                    R.id.month_label -> sawMonthLabel = true
                    R.id.calendar -> sawCalendar = true
                    R.id.summary -> sawSummary = true
                    R.id.trip_list -> {
                        sawTripList = true
                        tripListWeight = parser.getAttributeFloatValue(NS_ANDROID, "layout_weight", Float.NaN)
                    }
                    R.id.empty -> {
                        sawEmpty = true
                        emptyTextResId = parser.getAttributeResourceValue(NS_ANDROID, "text", 0)
                    }
                }
                depth++
            } else if (event == XmlPullParser.END_TAG) {
                depth--
            }
            event = parser.next()
        }

        assertTrue("month_label missing", sawMonthLabel)
        assertTrue("calendar missing", sawCalendar)
        assertTrue("summary missing", sawSummary)
        assertTrue("trip_list missing", sawTripList)
        assertTrue("empty missing", sawEmpty)
        assertEquals("empty TextView must use trips_none string", R.string.trips_none, emptyTextResId)
        // Vertical (1) so calendar sits above the RecyclerView, matching the spec.
        assertEquals("root LinearLayout must be vertical", 1, rootOrientation)
        // weight=1 means the RecyclerView grows to fill the remaining height in the panel.
        assertEquals("trip_list must take remaining height (weight=1)", 1.0f, tripListWeight, 0.0001f)
    }

    @Test
    fun `trips_none string resource exists`() {
        val ctx = RuntimeEnvironment.getApplication()
        val s = ctx.getString(R.string.trips_none)
        assertTrue("trips_none must be non-empty", s.isNotBlank())
    }
}
