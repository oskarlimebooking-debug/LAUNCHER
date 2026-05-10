package com.oskar.retrolauncher

import android.content.Context
import com.oskar.retrolauncher.data.prefs.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * T1.30 — `SettingsStore.pinnedApps` persists the rail-pinned package names
 * across launches. The list is treated as ordered (oldest first). Order
 * preservation matters because the rail enforces FIFO eviction at max-8.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class SettingsStorePinnedAppsTest {

    private fun freshStore(): SettingsStore {
        val ctx = RuntimeEnvironment.getApplication()
        val prefs = ctx.getSharedPreferences("pinned_test_${System.nanoTime()}", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        return SettingsStore(prefs)
    }

    @Test
    fun `default pinnedApps is empty`() {
        assertTrue(freshStore().pinnedApps.isEmpty())
    }

    @Test
    fun `setPinnedApps then read returns the same list in order`() {
        val store = freshStore()
        val pkgs = listOf("com.a", "com.b", "com.c")
        store.setPinnedApps(pkgs)
        assertEquals(pkgs, store.pinnedApps)
    }

    @Test
    fun `setPinnedApps persists across new SettingsStore instances (same prefs)`() {
        val ctx = RuntimeEnvironment.getApplication()
        val prefs = ctx.getSharedPreferences("pinned_persist_${System.nanoTime()}", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        val a = SettingsStore(prefs)
        a.setPinnedApps(listOf("com.x", "com.y"))
        val b = SettingsStore(prefs) // simulate re-launch
        assertEquals(listOf("com.x", "com.y"), b.pinnedApps)
    }

    @Test
    fun `setPinnedApps with empty list clears the saved list`() {
        val store = freshStore()
        store.setPinnedApps(listOf("com.a"))
        store.setPinnedApps(emptyList())
        assertTrue(store.pinnedApps.isEmpty())
    }
}
