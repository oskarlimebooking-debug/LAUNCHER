package com.oskar.retrolauncher.data.apps

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import com.oskar.retrolauncher.data.prefs.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray

/**
 * Tracks every launchable app on the device and the user-pinned rail subset.
 *
 * - `refresh()` enumerates launchable activities via PackageManager (T1.28 AC1).
 * - `start()` registers a PACKAGE_ADDED/REMOVED broadcast receiver so the
 *   list re-scans when apps are installed or uninstalled (T1.28 AC2).
 * - The launcher's own package is always excluded (T1.28 AC5).
 * - Order is taken from SettingsStore.appOrder, falling back to alphabetical
 *   (T1.28 AC4).
 *
 * Icons are NOT loaded synchronously — the adapter loads them lazily through
 * Glide using the custom ApplicationInfo model loader (T1.28 AC3).
 */
class AppListRepository(
    private val ctx: Context,
    private val pm: PackageManager,
    private val settings: SettingsStore? = null,
) {
    private val prefs: SharedPreferences =
        ctx.getSharedPreferences("rail", Context.MODE_PRIVATE)

    private val _all = MutableStateFlow<List<AppEntry>>(emptyList())
    val all: StateFlow<List<AppEntry>> = _all

    private val _rail = MutableStateFlow<List<ComponentName>>(loadRail())
    val rail: StateFlow<List<ComponentName>> = _rail

    private var receiver: BroadcastReceiver? = null

    fun refresh() {
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val raw = pm.queryIntentActivities(main, 0)
            .asSequence()
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map { ri ->
                AppEntry(
                    label = ri.loadLabel(pm).toString(),
                    packageName = ri.activityInfo.packageName,
                    componentName = ComponentName(ri.activityInfo.packageName, ri.activityInfo.name),
                    applicationInfo = ri.activityInfo.applicationInfo,
                )
            }
            .distinctBy { it.componentName.flattenToShortString() }
            .toList()
        _all.value = applyUserOrder(raw)
    }

    private fun applyUserOrder(entries: List<AppEntry>): List<AppEntry> {
        val order = settings?.appOrder.orEmpty()
        if (order.isEmpty()) return entries.sortedBy { it.label.lowercase() }
        val byKey = entries.associateBy { it.componentName.flattenToShortString() }
        val ordered = order.mapNotNull { byKey[it] }
        val placed = ordered.map { it.componentName.flattenToShortString() }.toHashSet()
        val rest = entries
            .filter { it.componentName.flattenToShortString() !in placed }
            .sortedBy { it.label.lowercase() }
        return ordered + rest
    }

    /** Register the install/uninstall broadcast receiver. Idempotent. */
    fun start() {
        if (receiver != null) return
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action in PACKAGE_ACTIONS) refresh()
            }
        }
        val filter = IntentFilter().apply {
            PACKAGE_ACTIONS.forEach { addAction(it) }
            addDataScheme("package")
        }
        ctx.registerReceiver(r, filter)
        receiver = r
    }

    /** Unregister the broadcast receiver. Safe to call when not started. */
    fun stop() {
        val r = receiver ?: return
        runCatching { ctx.unregisterReceiver(r) }
        receiver = null
    }

    fun pin(component: ComponentName) {
        val current = _rail.value.toMutableList()
        if (current.any { it == component }) return
        if (current.size >= MAX_RAIL) current.removeAt(current.lastIndex)
        current.add(component)
        saveRail(current)
    }

    fun unpin(component: ComponentName) {
        val current = _rail.value.filterNot { it == component }
        saveRail(current)
    }

    fun isPinned(component: ComponentName): Boolean =
        _rail.value.any { it == component }

    private fun saveRail(list: List<ComponentName>) {
        val json = JSONArray().apply { list.forEach { put(it.flattenToShortString()) } }
        prefs.edit().putString("pinned", json.toString()).apply()
        _rail.value = list
    }

    private fun loadRail(): List<ComponentName> {
        val raw = prefs.getString("pinned", null) ?: return defaultRail()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                ComponentName.unflattenFromString(arr.getString(i))
            }
        }.getOrElse { defaultRail() }
    }

    private fun defaultRail(): List<ComponentName> = emptyList()

    fun resolveAppEntry(component: ComponentName): AppEntry? =
        _all.value.firstOrNull { it.componentName == component }

    companion object {
        const val MAX_RAIL = 6
        private val PACKAGE_ACTIONS = setOf(
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_CHANGED,
        )
    }
}
