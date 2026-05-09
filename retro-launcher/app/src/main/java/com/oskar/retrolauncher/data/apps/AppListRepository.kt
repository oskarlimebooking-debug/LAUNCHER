package com.oskar.retrolauncher.data.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray

class AppListRepository(
    ctx: Context,
    private val pm: PackageManager,
) {
    private val prefs: SharedPreferences =
        ctx.getSharedPreferences("rail", Context.MODE_PRIVATE)

    private val _all = MutableStateFlow<List<AppEntry>>(emptyList())
    val all: StateFlow<List<AppEntry>> = _all

    private val _rail = MutableStateFlow<List<ComponentName>>(loadRail())
    val rail: StateFlow<List<ComponentName>> = _rail

    fun refresh() {
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val list = pm.queryIntentActivities(main, 0).map {
            AppEntry(
                label = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName,
                componentName = ComponentName(it.activityInfo.packageName, it.activityInfo.name),
                icon = it.loadIcon(pm),
            )
        }.distinctBy { it.componentName.flattenToShortString() }
            .sortedBy { it.label.lowercase() }
        _all.value = list
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
    }
}
