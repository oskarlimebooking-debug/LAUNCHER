package com.oskar.retrolauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

/**
 * T1.9 ACs covered:
 *   - AC1: clock element is a TextView (not TextClock) — confirms BroadcastReceiver-based
 *          TextClock has been replaced with a Handler-driven ticker.
 *   - AC3: trip_chip_slot has android:visibility="gone" by default.
 *   - AC4: drawer_btn exists in fragment_status.xml.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class StatusBarFragmentTest {

    private companion object {
        const val NS_ANDROID = "http://schemas.android.com/apk/res/android"
        // android:visibility — encoded values: 0 = visible, 1 = invisible, 2 = gone.
        const val VISIBILITY_GONE = 2
    }

    private fun walkLayout(): Map<Int, Map<String, String>> {
        val ctx = RuntimeEnvironment.getApplication()
        val parser = ctx.resources.getLayout(R.layout.fragment_status)
        val out = mutableMapOf<Int, MutableMap<String, String>>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                val id = parser.getAttributeResourceValue(NS_ANDROID, "id", 0)
                if (id != 0) {
                    val attrs = mutableMapOf<String, String>()
                    attrs["__name__"] = parser.name
                    out[id] = attrs
                }
            }
            event = parser.next()
        }
        return out
    }

    @Test
    fun `clock element is a TextView (Handler-driven, not TextClock)`() {
        val byId = walkLayout()
        val clock = byId[R.id.clock]
        assertTrue("@id/clock must exist in fragment_status.xml", clock != null)
        assertNotEquals(
            "clock must NOT be TextClock (uses BroadcastReceiver — AC1 requires Handler ticker)",
            "TextClock", clock?.get("__name__"),
        )
        assertEquals("clock should be a TextView updated by the ticker", "TextView", clock?.get("__name__"))
    }

    @Test
    fun `date element is a TextView (Handler-driven, not TextClock)`() {
        val byId = walkLayout()
        val date = byId[R.id.date]
        assertTrue("@id/date must exist in fragment_status.xml", date != null)
        assertEquals("TextView", date?.get("__name__"))
    }

    @Test
    fun `trip_chip_slot defaults to visibility=gone (AC3)`() {
        val ctx = RuntimeEnvironment.getApplication()
        val parser = ctx.resources.getLayout(R.layout.fragment_status)
        var foundGone = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                val id = parser.getAttributeResourceValue(NS_ANDROID, "id", 0)
                if (id == R.id.trip_chip_slot) {
                    val visibility = parser.getAttributeIntValue(NS_ANDROID, "visibility", -1)
                    foundGone = visibility == VISIBILITY_GONE
                    break
                }
            }
            event = parser.next()
        }
        assertTrue("trip_chip_slot must default to android:visibility=\"gone\"", foundGone)
    }

    @Test
    fun `drawer button exists for AC4 SettingsActivity entry point`() {
        val byId = walkLayout()
        assertTrue("@id/drawer_btn must exist in fragment_status.xml", byId.containsKey(R.id.drawer_btn))
    }
}
