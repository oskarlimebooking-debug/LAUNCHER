package com.oskar.retrolauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import com.oskar.retrolauncher.data.apps.AppListRepository
import com.oskar.retrolauncher.data.prefs.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * T1.30 — Rail (pinned-apps strip) behavior. The rail is a list of up to
 * `MAX_RAIL` package names backed by `SettingsStore.pinnedApps`. Pinning a
 * 9th app shifts out the oldest entry; unpin removes; persistence is shared
 * with SettingsStore so it survives process death.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class AppListRepositoryRailTest {

    private lateinit var ctx: Context
    private lateinit var settings: SettingsStore
    private lateinit var repo: AppListRepository

    @Before
    fun setUp() {
        ctx = RuntimeEnvironment.getApplication()
        val prefs = ctx.getSharedPreferences("rail_test_${System.nanoTime()}", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        settings = SettingsStore(prefs)
        repo = AppListRepository(ctx, ctx.packageManager, settings)
    }

    private fun installLaunchable(pkg: String, activity: String, label: String) {
        val pm = shadowOf(ctx.packageManager)
        val pkgInfo = PackageInfo().apply {
            packageName = pkg
            applicationInfo = ApplicationInfo().apply {
                packageName = pkg
                name = label
                nonLocalizedLabel = label
            }
        }
        pm.installPackage(pkgInfo)
        val resolve = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = pkg
                name = activity
                applicationInfo = pkgInfo.applicationInfo
                nonLocalizedLabel = label
            }
            nonLocalizedLabel = label
        }
        val filter = IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        pm.addActivityIfNotPresent(ComponentName(pkg, activity))
        pm.addIntentFilterForActivity(ComponentName(pkg, activity), filter)
        pm.addOrUpdateActivity(resolve.activityInfo)
    }

    @Test
    fun `MAX_RAIL is 8`() {
        assertEquals(8, AppListRepository.MAX_RAIL)
    }

    @Test
    fun `pin appends a package and isPinned reports true`() {
        repo.pin("com.example.alpha")
        assertTrue(repo.isPinned("com.example.alpha"))
        assertEquals(listOf("com.example.alpha"), repo.pinnedPackages.value)
    }

    @Test
    fun `pinning the same package twice is a no-op`() {
        repo.pin("com.a")
        repo.pin("com.a")
        assertEquals(listOf("com.a"), repo.pinnedPackages.value)
    }

    @Test
    fun `unpin removes a package`() {
        repo.pin("com.a")
        repo.pin("com.b")
        repo.unpin("com.a")
        assertEquals(listOf("com.b"), repo.pinnedPackages.value)
        assertFalse(repo.isPinned("com.a"))
    }

    @Test
    fun `pinning a 9th app shifts out the oldest (FIFO)`() {
        listOf("a", "b", "c", "d", "e", "f", "g", "h").forEach { repo.pin("com.$it") }
        assertEquals(8, repo.pinnedPackages.value.size)
        repo.pin("com.i") // 9th
        val rail = repo.pinnedPackages.value
        assertEquals(8, rail.size)
        assertFalse("oldest (com.a) should have been evicted: $rail", rail.contains("com.a"))
        assertEquals("com.i", rail.last())
        assertEquals("com.b", rail.first())
    }

    @Test
    fun `pinning persists via SettingsStore (survives a fresh repository instance)`() {
        repo.pin("com.persisted")
        val repo2 = AppListRepository(ctx, ctx.packageManager, settings)
        assertTrue(repo2.isPinned("com.persisted"))
        assertEquals(listOf("com.persisted"), repo2.pinnedPackages.value)
    }

    @Test
    fun `rail flow emits AppEntries in pinned order, ignoring uninstalled packages`() {
        installLaunchable("com.example.alpha", "AlphaMain", "Alpha")
        installLaunchable("com.example.bravo", "BravoMain", "Bravo")
        repo.pin("com.example.bravo")
        repo.pin("com.example.gone") // not installed
        repo.pin("com.example.alpha")
        repo.refresh()
        val labels = repo.rail.value.map { it.label }
        assertEquals(listOf("Bravo", "Alpha"), labels)
    }
}
