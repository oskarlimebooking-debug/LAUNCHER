package com.oskar.retrolauncher

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import com.oskar.retrolauncher.data.prefs.SettingsStore
import com.oskar.retrolauncher.ui.wizard.WizardActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * T1.33 — first-run wizard, 6 linear steps per task spec:
 *   1. Welcome
 *   2. Location (ACCESS_FINE_LOCATION)
 *   3. Notification listener (ACTION_NOTIFICATION_LISTENER_SETTINGS)
 *   4. Default launcher (ACTION_MANAGE_DEFAULT_APPS_SETTINGS / ACTION_HOME_SETTINGS)
 *   5. OWM API key entry — validates via network on continue (or skip)
 *   6. Home location entry (lat/lon, or skip to use device GPS)
 *
 * firstRunDone is set true ONLY after step 6 — skipping intermediate steps
 * is allowed but does not bypass the wizard.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class WizardActivityTest {

    @After
    fun resetPrefs() {
        // Clear default-shared-prefs so each test starts with firstRunDone=false.
        val ctx = RuntimeEnvironment.getApplication()
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx)
            .edit().clear().commit()
    }

    private fun inflateLayout(): View {
        val ctx = RuntimeEnvironment.getApplication()
        return LayoutInflater.from(ctx).inflate(R.layout.activity_wizard, null, false)
    }

    @Test
    fun `layout has the six required view ids for wizard rendering`() {
        val root = inflateLayout()
        assertNotNull("step_index", root.findViewById<View>(R.id.step_index))
        assertNotNull("title", root.findViewById<View>(R.id.title))
        assertNotNull("body", root.findViewById<View>(R.id.body))
        assertNotNull("grant_btn", root.findViewById<View>(R.id.grant_btn))
        assertNotNull("skip_btn", root.findViewById<View>(R.id.skip_btn))
        // Inputs for OWM key (step 5) and home location (step 6).
        assertNotNull("input_text", root.findViewById<EditText>(R.id.input_text))
        assertNotNull("input_lat", root.findViewById<EditText>(R.id.input_lat))
        assertNotNull("input_lon", root.findViewById<EditText>(R.id.input_lon))
    }

    @Test
    fun `welcome step is shown first and firstRunDone stays false`() {
        val activity = Robolectric.buildActivity(WizardActivity::class.java).setup().get()
        assertFalse(
            "firstRunDone must remain false until the wizard completes",
            App.settings.firstRunDone,
        )
        val stepIndex = activity.findViewById<android.widget.TextView>(R.id.step_index)
        assertEquals("1 of 6", stepIndex.text.toString())
    }

    @Test
    fun `skip advances through every step until completion`() {
        val activity = Robolectric.buildActivity(WizardActivity::class.java).setup().get()
        val skip = activity.findViewById<View>(R.id.skip_btn)

        // 5 skips advance through steps 1→6.
        repeat(5) { skip.performClick() }

        val stepIndex = activity.findViewById<android.widget.TextView>(R.id.step_index)
        assertEquals("6 of 6", stepIndex.text.toString())
        assertFalse(
            "firstRunDone must still be false at step 6 (not yet finished)",
            App.settings.firstRunDone,
        )

        // Final skip on step 6 finishes the wizard and flips firstRunDone.
        skip.performClick()

        assertTrue(
            "firstRunDone must be true after wizard completes",
            App.settings.firstRunDone,
        )
        assertTrue("wizard activity must finish on completion", activity.isFinishing)
    }

    @Test
    fun `notification step grant button launches notification listener settings`() {
        val activity = Robolectric.buildActivity(WizardActivity::class.java).setup().get()
        val skip = activity.findViewById<View>(R.id.skip_btn)
        // Welcome → Location → Notification.
        repeat(2) { skip.performClick() }

        val grant = activity.findViewById<View>(R.id.grant_btn)
        grant.performClick()

        val app = RuntimeEnvironment.getApplication() as Application
        val next = shadowOf(app).peekNextStartedActivity()
        assertNotNull("grant on notification step must start an activity", next)
        assertEquals(
            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
            next!!.action,
        )
    }

    @Test
    fun `home location step persists lat lon on continue`() {
        val activity = Robolectric.buildActivity(WizardActivity::class.java).setup().get()
        val skip = activity.findViewById<View>(R.id.skip_btn)
        // Step 1→2→3→4→5→6 (5 skips).
        repeat(5) { skip.performClick() }

        // On step 6 the lat/lon inputs must be visible.
        val lat = activity.findViewById<EditText>(R.id.input_lat)
        val lon = activity.findViewById<EditText>(R.id.input_lon)
        assertEquals(View.VISIBLE, lat.visibility)
        assertEquals(View.VISIBLE, lon.visibility)

        lat.setText("46.05")
        lon.setText("14.51")

        val grant = activity.findViewById<View>(R.id.grant_btn)
        grant.performClick()

        assertEquals(46.05f, App.settings.weatherLat!!, 0.001f)
        assertEquals(14.51f, App.settings.weatherLon!!, 0.001f)
        assertTrue("firstRunDone must be set after step 6 completes", App.settings.firstRunDone)
        assertTrue(activity.isFinishing)
    }
}
