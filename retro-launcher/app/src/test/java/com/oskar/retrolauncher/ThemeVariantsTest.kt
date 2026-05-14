package com.oskar.retrolauncher

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.oskar.retrolauncher.data.prefs.SettingsStore
import com.oskar.retrolauncher.data.prefs.Theme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * T1.42 — Day/night theme variants. Verifies that all palette theme values
 * (MOCHA, OCEAN, FOREST) round-trip through SettingsStore alongside SYSTEM,
 * and that each palette theme maps to a distinct style resource.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class ThemeVariantsTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    private fun freshPrefs(): SharedPreferences =
        ctx.getSharedPreferences("theme_variants_${System.nanoTime()}", Context.MODE_PRIVATE)

    // ───────── Theme enum round-trips (single-prefs per test) ─────────

    @Test
    fun `SYSTEM theme round-trips`() {
        val prefs = freshPrefs()
        SettingsStore(prefs).setTheme(Theme.SYSTEM)
        assertEquals(Theme.SYSTEM, SettingsStore(prefs).theme)
    }

    @Test
    fun `MOCHA theme round-trips`() {
        val prefs = freshPrefs()
        SettingsStore(prefs).setTheme(Theme.MOCHA)
        assertEquals(Theme.MOCHA, SettingsStore(prefs).theme)
    }

    @Test
    fun `OCEAN theme round-trips`() {
        val prefs = freshPrefs()
        SettingsStore(prefs).setTheme(Theme.OCEAN)
        assertEquals(Theme.OCEAN, SettingsStore(prefs).theme)
    }

    @Test
    fun `FOREST theme round-trips`() {
        val prefs = freshPrefs()
        SettingsStore(prefs).setTheme(Theme.FOREST)
        assertEquals(Theme.FOREST, SettingsStore(prefs).theme)
    }

    @Test
    fun `LIGHT theme round-trips`() {
        val prefs = freshPrefs()
        SettingsStore(prefs).setTheme(Theme.LIGHT)
        assertEquals(Theme.LIGHT, SettingsStore(prefs).theme)
    }

    @Test
    fun `DARK theme round-trips`() {
        val prefs = freshPrefs()
        SettingsStore(prefs).setTheme(Theme.DARK)
        assertEquals(Theme.DARK, SettingsStore(prefs).theme)
    }

    @Test
    fun `default theme is SYSTEM`() {
        assertEquals(Theme.SYSTEM, SettingsStore(freshPrefs()).theme)
    }

    // ───────── themeFlow ─────────

    @Test
    fun `themeFlow emits on change`() = runTest {
        val s = SettingsStore(freshPrefs())
        s.setTheme(Theme.OCEAN)
        assertEquals(Theme.OCEAN, s.themeFlow.first())
    }

    // ───────── Theme to resource mapping ─────────

    @Test
    fun `every theme has a valid style resource`() {
        for (theme in Theme.entries) {
            assertTrue("Theme $theme should have valid style resource", theme.styleRes != 0)
        }
    }

    @Test
    fun `palette themes have distinct style resources`() {
        val paletteIds = setOf(
            Theme.MOCHA.styleRes,
            Theme.OCEAN.styleRes,
            Theme.FOREST.styleRes,
        )
        assertEquals("MOCHA, OCEAN, FOREST must each map to a unique style", 3, paletteIds.size)
    }

    @Test
    fun `palette themes differ from default`() {
        assertNotEquals(Theme.SYSTEM.styleRes, Theme.MOCHA.styleRes)
        assertNotEquals(Theme.SYSTEM.styleRes, Theme.OCEAN.styleRes)
        assertNotEquals(Theme.SYSTEM.styleRes, Theme.FOREST.styleRes)
    }

    // ───────── Speedometer readability (AC5) ─────────

    @Test
    fun `speedometer threshold colors exist`() {
        val names = listOf("ok", "warn", "err", "bg", "card", "accent", "text_primary", "text_tertiary")
        for (name in names) {
            val resId = ctx.resources.getIdentifier(name, "color", ctx.packageName)
            assertTrue("Missing @color/$name resource", resId != 0)
        }
    }

    @Test
    fun `ok warn err are opaque`() {
        val colorNames = listOf("ok", "warn", "err", "bg")
        for (name in colorNames) {
            val resId = ctx.resources.getIdentifier(name, "color", ctx.packageName)
            val color = ctx.resources.getColor(resId, null)
            val alpha = (color shr 24) and 0xFF
            assertTrue("@color/$name should be opaque (alpha >= 0x80)", alpha >= 0x80)
        }
    }
}
