package com.oskar.retrolauncher

import android.content.Intent
import com.oskar.retrolauncher.ui.settings.AppChoice
import com.oskar.retrolauncher.ui.settings.IntentPickerEntries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * T1.32 AC3 — `IntentPickerEntries` populates `ListPreference` arrays from
 * `PackageManager.queryIntentActivities` so the user can pick the default map /
 * voice app from installed handlers.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class IntentPickerEntriesTest {

    @Test
    fun `mapIntent has geo scheme`() {
        val i = IntentPickerEntries.mapIntent()
        assertEquals(Intent.ACTION_VIEW, i.action)
        assertEquals("geo", i.data?.scheme)
    }

    @Test
    fun `voiceIntent has recognize-speech action`() {
        val i = IntentPickerEntries.voiceIntent()
        assertEquals(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH, i.action)
    }

    @Test
    fun `resolve returns empty list for an intent with no handlers`() {
        val ctx = RuntimeEnvironment.getApplication()
        val choices = IntentPickerEntries.resolve(ctx, IntentPickerEntries.mapIntent())
        assertTrue(
            "Robolectric stub PackageManager returns no map handlers by default",
            choices.isEmpty(),
        )
    }

    @Test
    fun `toListPreferenceArrays prepends the (Ask every time) entry with empty value`() {
        val (entries, values) = IntentPickerEntries.toListPreferenceArrays(
            listOf(AppChoice("Maps", "com.google.maps"), AppChoice("Organic", "com.organic")),
            askEveryTimeLabel = "Ask every time",
        )
        assertEquals(3, entries.size)
        assertEquals("Ask every time", entries[0])
        assertEquals("", values[0])
        assertEquals("Maps", entries[1])
        assertEquals("com.google.maps", values[1])
    }

    @Test
    fun `launcherIntent has MAIN action and LAUNCHER category`() {
        val i = IntentPickerEntries.launcherIntent()
        assertEquals(Intent.ACTION_MAIN, i.action)
        assertTrue(i.categories.contains(Intent.CATEGORY_LAUNCHER))
    }

    @Test
    fun `toListPreferenceArrays handles empty choices`() {
        val (entries, values) = IntentPickerEntries.toListPreferenceArrays(
            emptyList(),
            askEveryTimeLabel = "Ask every time",
        )
        assertEquals(1, entries.size)
        assertEquals(1, values.size)
    }
}
