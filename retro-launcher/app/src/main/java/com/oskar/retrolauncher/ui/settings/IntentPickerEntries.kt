package com.oskar.retrolauncher.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognizerIntent

/**
 * Resolves installed apps that can handle a given launch intent into label/package
 * pairs suitable for a `ListPreference`. Used by [SettingsFragment] to populate the
 * "Default map" and "Default voice" pickers (T1.32 AC3).
 *
 * Returns an immutable list of entries sorted alphabetically by label. The list is
 * prefixed with a "(Ask every time)" choice mapped to an empty package value so the
 * user can clear a previously set default without uninstalling anything.
 */
data class AppChoice(val label: String, val packageName: String)

object IntentPickerEntries {

    /** Intent that any map app responds to. */
    fun mapIntent(): Intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"))

    /** Intent that any voice / speech-recognition app responds to. */
    fun voiceIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

    /**
     * Query [PackageManager] for activities matching [intent] and return them as
     * `AppChoice`s. Errors are swallowed — returning an empty list is acceptable.
     */
    fun resolve(context: Context, intent: Intent): List<AppChoice> {
        val pm = context.packageManager
        val infos = runCatching {
            pm.queryIntentActivities(intent, 0)
        }.getOrElse { emptyList() }

        return infos
            .asSequence()
            .filter { it.activityInfo?.packageName != null }
            .map { info ->
                val label = info.loadLabel(pm)?.toString().orEmpty().ifEmpty {
                    info.activityInfo.packageName
                }
                AppChoice(label = label, packageName = info.activityInfo.packageName)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** Builds the `entries` / `entryValues` arrays for a `ListPreference`. */
    fun toListPreferenceArrays(
        choices: List<AppChoice>,
        askEveryTimeLabel: String,
    ): Pair<Array<CharSequence>, Array<CharSequence>> {
        val entries = ArrayList<CharSequence>(choices.size + 1)
        val values = ArrayList<CharSequence>(choices.size + 1)
        entries += askEveryTimeLabel
        values += ""
        for (c in choices) {
            entries += c.label
            values += c.packageName
        }
        return entries.toTypedArray() to values.toTypedArray()
    }
}
