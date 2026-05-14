package com.oskar.retrolauncher.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.BuildConfig
import com.oskar.retrolauncher.R

/**
 * Spec §14.3 / T1.32 — top-level settings screen. All preferences read/write
 * the default `SharedPreferences` instance, which is the same one wrapping
 * [com.oskar.retrolauncher.data.prefs.SettingsStore], so every UI mutation
 * propagates to the rest of the app immediately (AC1).
 *
 * Two pieces of UI need extra wiring on top of the static XML:
 *   - Default-map / Default-voice list pickers are populated dynamically from
 *     installed packages that respond to the relevant intent (AC3).
 *   - Latitude / longitude inputs are stored as `Float` by [SettingsStore], but
 *     `EditTextPreference` only knows how to read/write `String`. We bridge by
 *     parsing the input string and calling `setWeatherLocation`.
 */
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        wireNotificationAccess()
        wireIntentPickers()
        wireWeatherLocationBridge()
        wireVersionAndLicenses()
    }

    private fun wireNotificationAccess() {
        findPreference<Preference>("notification_access")?.setOnPreferenceClickListener {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            true
        }
    }

    private fun wireIntentPickers() {
        val ctx = requireContext()
        val askEveryTime = getString(R.string.pref_apps_ask_every_time)

        findPreference<ListPreference>("map_app")?.let { pref ->
            val choices = IntentPickerEntries.resolve(ctx, IntentPickerEntries.mapIntent())
            val (entries, values) =
                IntentPickerEntries.toListPreferenceArrays(choices, askEveryTime)
            pref.entries = entries
            pref.entryValues = values
            if (pref.value == null) pref.value = ""
        }

        findPreference<ListPreference>("voice_app")?.let { pref ->
            val choices = IntentPickerEntries.resolve(ctx, IntentPickerEntries.voiceIntent())
            val (entries, values) =
                IntentPickerEntries.toListPreferenceArrays(choices, askEveryTime)
            pref.entries = entries
            pref.entryValues = values
            if (pref.value == null) pref.value = ""
        }

        findPreference<ListPreference>("embed_app")?.let { pref ->
            val choices = IntentPickerEntries.resolve(ctx, IntentPickerEntries.launcherIntent())
            // Exclude the launcher itself from embeddable app list
            val filtered =
                choices.filter { it.packageName != requireContext().packageName }
            val (entries, values) =
                IntentPickerEntries.toListPreferenceArrays(filtered, askEveryTime)
            pref.entries = entries
            pref.entryValues = values
            if (pref.value == null) pref.value = ""
        }
    }

    private fun wireWeatherLocationBridge() {
        val store = App.settings
        val latPref = findPreference<EditTextPreference>("weather_lat_input")
        val lonPref = findPreference<EditTextPreference>("weather_lon_input")

        latPref?.text = store.weatherLat?.toString().orEmpty()
        lonPref?.text = store.weatherLon?.toString().orEmpty()

        latPref?.setOnPreferenceChangeListener { _, newValue ->
            val lat = (newValue as? String)?.toFloatOrNull()
            store.setWeatherLocation(lat, store.weatherLon)
            true
        }
        lonPref?.setOnPreferenceChangeListener { _, newValue ->
            val lon = (newValue as? String)?.toFloatOrNull()
            store.setWeatherLocation(store.weatherLat, lon)
            true
        }
    }

    private fun wireVersionAndLicenses() {
        findPreference<Preference>("version")?.summary = BuildConfig.VERSION_NAME

        findPreference<Preference>("oss_licenses")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), LicensesActivity::class.java))
            true
        }
    }
}
