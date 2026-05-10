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
 * T1.28 AC4 — App-grid sort order persists across launches via SettingsStore.
 * The store keeps an ordered list of "package/activity" strings; the order is
 * what the grid renders.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class SettingsStoreAppOrderTest {

    private fun freshStore(): SettingsStore {
        val ctx = RuntimeEnvironment.getApplication()
        val prefs = ctx.getSharedPreferences("apporder_test_${System.nanoTime()}", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        return SettingsStore(prefs)
    }

    @Test
    fun `default appOrder is empty`() {
        assertTrue(freshStore().appOrder.isEmpty())
    }

    @Test
    fun `setAppOrder then read returns the same list in order`() {
        val store = freshStore()
        val order = listOf("com.a/A", "com.b/B", "com.c/C")
        store.setAppOrder(order)
        assertEquals(order, store.appOrder)
    }

    @Test
    fun `setAppOrder persists across new SettingsStore instances (same prefs)`() {
        val ctx = RuntimeEnvironment.getApplication()
        val prefs = ctx.getSharedPreferences("apporder_persist_${System.nanoTime()}", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        val a = SettingsStore(prefs)
        a.setAppOrder(listOf("com.x/X", "com.y/Y"))
        val b = SettingsStore(prefs) // simulate re-launch
        assertEquals(listOf("com.x/X", "com.y/Y"), b.appOrder)
    }

    @Test
    fun `setAppOrder with empty list clears the saved order`() {
        val store = freshStore()
        store.setAppOrder(listOf("com.a/A"))
        store.setAppOrder(emptyList())
        assertTrue(store.appOrder.isEmpty())
    }
}
