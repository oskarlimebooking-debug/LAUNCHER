package com.oskar.retrolauncher.data.apps

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.annotation.VisibleForTesting
import com.oskar.retrolauncher.data.prefs.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.content.pm.PackageManager

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
 * Rail (T1.30): pinned package names are persisted in
 * [SettingsStore.pinnedApps]. `pin` enforces a hard cap of [MAX_RAIL] using
 * FIFO eviction (oldest entry shifts out). `rail` resolves package names
 * back to [AppEntry] using the latest scan, dropping entries whose package
 * has been uninstalled.
 */
class AppListRepository(
    private val ctx: Context,
    private val pm: PackageManager,
    private val settings: SettingsStore,
) {
    private val _all = MutableStateFlow<List<AppEntry>>(emptyList())
    val all: StateFlow<List<AppEntry>> = _all

    private val _pinnedPackages = MutableStateFlow(settings.pinnedApps)
    val pinnedPackages: StateFlow<List<String>> = _pinnedPackages

    private val _rail = MutableStateFlow<List<AppEntry>>(resolveRail(_pinnedPackages.value, _all.value))
    val rail: StateFlow<List<AppEntry>> = _rail

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
        _rail.value = resolveRail(_pinnedPackages.value, _all.value)
    }

    private fun applyUserOrder(entries: List<AppEntry>): List<AppEntry> {
        val order = settings.appOrder
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

    fun pin(packageName: String) {
        val current = _pinnedPackages.value
        if (packageName in current) return
        val next = if (current.size >= MAX_RAIL) {
            current.drop(current.size - MAX_RAIL + 1) + packageName
        } else {
            current + packageName
        }
        savePinned(next)
    }

    fun unpin(packageName: String) {
        val next = _pinnedPackages.value.filterNot { it == packageName }
        if (next.size == _pinnedPackages.value.size) return
        savePinned(next)
    }

    fun isPinned(packageName: String): Boolean = packageName in _pinnedPackages.value

    /**
     * T1.41 — commit a user-reordered grid. The list comes from the drag-and-
     * drop callback after a drop; its componentName-keys are persisted to
     * `SettingsStore.appOrder` and the `all` StateFlow is re-emitted in the
     * new order so the adapter's next submitList is a DiffUtil no-op.
     */
    fun commitGridOrder(entries: List<AppEntry>) {
        settings.setAppOrder(entries.map { it.componentName.flattenToShortString() })
        _all.value = entries
        _rail.value = resolveRail(_pinnedPackages.value, entries)
    }

    /**
     * T1.41 — commit a user-reordered rail. Packages are persisted to
     * `SettingsStore.pinnedApps`; `pinnedPackages` and `rail` are refreshed
     * from the latest scan.
     */
    fun commitPinnedOrder(packages: List<String>) {
        savePinned(packages)
    }

    private fun savePinned(packages: List<String>) {
        settings.setPinnedApps(packages)
        _pinnedPackages.value = packages
        _rail.value = resolveRail(packages, _all.value)
    }

    private fun resolveRail(packages: List<String>, all: List<AppEntry>): List<AppEntry> {
        if (packages.isEmpty() || all.isEmpty()) return emptyList()
        val byPkg = HashMap<String, AppEntry>(all.size)
        for (e in all) byPkg.putIfAbsent(e.packageName, e)
        return packages.mapNotNull { byPkg[it] }
    }

    /**
     * T1.36 — instrumented tests seed the in-memory state directly so they
     * never depend on what apps the emulator happens to have installed.
     * Skips the broadcast-receiver and PackageManager scan paths entirely.
     */
    @VisibleForTesting
    fun setForTest(all: List<AppEntry>, pinned: List<String> = emptyList()) {
        _all.value = all
        _pinnedPackages.value = pinned
        _rail.value = resolveRail(pinned, all)
    }

    companion object {
        /** Hard cap on rail entries (T1.30 AC1 / AC3). */
        const val MAX_RAIL = SettingsStore.MAX_PINNED
        private val PACKAGE_ACTIONS = setOf(
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_CHANGED,
        )
    }
}
