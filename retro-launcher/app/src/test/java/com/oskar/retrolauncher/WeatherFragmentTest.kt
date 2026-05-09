package com.oskar.retrolauncher

import com.oskar.retrolauncher.ui.weather.WeatherFragment
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

/**
 * T1.22 ACs verified by walking the compiled fragment_weather.xml:
 *   - AC2: placeholder TextView with id `temp` exists for the "—" fallback
 *   - AC4: a `wind` TextView is present so wind speed can be rendered
 *   - feels-like + condition text views exist (per task body / spec 10.6)
 *
 * Same pattern as MediaFragmentTest — walk the compiled XML directly because the
 * fragment_weather inflation needs no FragmentManager and we just want
 * structural assertions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class WeatherFragmentTest {

    private companion object {
        const val NS_ANDROID = "http://schemas.android.com/apk/res/android"
    }

    @Test
    fun `fragment_weather declares all required IDs`() {
        val ctx = RuntimeEnvironment.getApplication()
        val parser = ctx.resources.getLayout(R.layout.fragment_weather)

        var sawIcon = false
        var sawTemp = false
        var sawFeelsLike = false
        var sawCondition = false
        var sawWind = false
        var sawCity = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.getAttributeResourceValue(NS_ANDROID, "id", 0)) {
                    R.id.icon -> sawIcon = true
                    R.id.temp -> sawTemp = true
                    R.id.feels_like -> sawFeelsLike = true
                    R.id.condition -> sawCondition = true
                    R.id.wind -> sawWind = true
                    R.id.city -> sawCity = true
                }
            }
            event = parser.next()
        }

        assertTrue("icon ImageView required", sawIcon)
        assertTrue("temp TextView required", sawTemp)
        assertTrue("feels_like TextView required (spec 10.6)", sawFeelsLike)
        assertTrue("condition TextView required (spec 10.6)", sawCondition)
        assertTrue("wind TextView required (AC4)", sawWind)
        assertTrue("city TextView required", sawCity)
    }

    @Test
    fun `WeatherFragment can be instantiated via no-args ctor`() {
        // FragmentManager requires the no-args constructor.
        WeatherFragment()
    }
}
