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
import org.junit.After
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
 * T1.28 — AppListRepository scans launchable activities from PackageManager,
 * filters out the launcher's own package, applies a user-defined order from
 * SettingsStore, and refreshes via PACKAGE_ADDED/REMOVED broadcasts.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class AppListRepositoryTest {

    private lateinit var ctx: Context
    private lateinit var repo: AppListRepository
    private lateinit var settings: SettingsStore

    @Before
    fun setUp() {
        ctx = RuntimeEnvironment.getApplication()
        val prefs = ctx.getSharedPreferences("test_settings_${System.nanoTime()}", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        settings = SettingsStore(prefs)
        repo = AppListRepository(ctx, ctx.packageManager, settings)
    }

    @After
    fun tearDown() {
        repo.stop()
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
    fun `refresh excludes own launcher package`() {
        // Robolectric installs the launcher itself under BuildConfig.APPLICATION_ID.
        installLaunchable("com.example.maps", "MapsActivity", "Maps")
        installLaunchable(ctx.packageName, "com.oskar.retrolauncher.MainActivity", "RetroLauncher")
        repo.refresh()
        val packages = repo.all.value.map { it.packageName }
        assertFalse(
            "own package $packages must not appear in the app list",
            packages.contains(ctx.packageName),
        )
    }

    @Test
    fun `refresh returns scanned launchable apps sorted alphabetically by default`() {
        installLaunchable("com.example.zebra", "ZebraMain", "Zebra")
        installLaunchable("com.example.alpha", "AlphaMain", "Alpha")
        installLaunchable("com.example.middle", "MidMain", "Middle")
        repo.refresh()
        val labels = repo.all.value.map { it.label }
        // Default order is alphabetical (lowercase) when no custom order is saved.
        assertEquals(listOf("Alpha", "Middle", "Zebra"), labels)
    }

    @Test
    fun `refresh applies user-defined order from SettingsStore`() {
        installLaunchable("com.example.alpha", "AlphaMain", "Alpha")
        installLaunchable("com.example.bravo", "BravoMain", "Bravo")
        installLaunchable("com.example.charlie", "CharlieMain", "Charlie")
        // User pinned order: Charlie, Alpha, Bravo
        settings.setAppOrder(listOf(
            "com.example.charlie/CharlieMain",
            "com.example.alpha/AlphaMain",
            "com.example.bravo/BravoMain",
        ))
        repo.refresh()
        val labels = repo.all.value.map { it.label }
        assertEquals(listOf("Charlie", "Alpha", "Bravo"), labels)
    }

    @Test
    fun `unknown entries in saved order are dropped and new apps appear after ordered ones`() {
        installLaunchable("com.example.alpha", "AlphaMain", "Alpha")
        installLaunchable("com.example.bravo", "BravoMain", "Bravo")
        settings.setAppOrder(listOf(
            "com.example.bravo/BravoMain",
            "com.example.gone/GoneMain", // uninstalled — must be ignored
        ))
        repo.refresh()
        val labels = repo.all.value.map { it.label }
        // Bravo first (from saved order), then Alpha alphabetically (not in order).
        assertEquals(listOf("Bravo", "Alpha"), labels)
    }

    @Test
    fun `PACKAGE_ADDED broadcast triggers a refresh that picks up the new app`() {
        installLaunchable("com.example.alpha", "AlphaMain", "Alpha")
        repo.start()
        repo.refresh()
        assertEquals(listOf("Alpha"), repo.all.value.map { it.label })

        // New app appears.
        installLaunchable("com.example.bravo", "BravoMain", "Bravo")
        ctx.sendBroadcast(Intent(Intent.ACTION_PACKAGE_ADDED).apply {
            data = android.net.Uri.parse("package:com.example.bravo")
        })
        shadowOf(android.os.Looper.getMainLooper()).idle()

        val labels = repo.all.value.map { it.label }
        assertTrue("Bravo must appear after PACKAGE_ADDED broadcast: $labels", labels.contains("Bravo"))
    }

    @Test
    fun `PACKAGE_REMOVED broadcast triggers a refresh`() {
        installLaunchable("com.example.alpha", "AlphaMain", "Alpha")
        repo.start()
        repo.refresh()
        assertEquals(1, repo.all.value.size)

        // Install a second app between scans; if the broadcast triggers refresh,
        // the new app will appear even though no PACKAGE_ADDED was fired. Symmetric
        // proof that the receiver does refresh on PACKAGE_REMOVED actions too.
        installLaunchable("com.example.bravo", "BravoMain", "Bravo")
        ctx.sendBroadcast(Intent(Intent.ACTION_PACKAGE_REMOVED).apply {
            data = android.net.Uri.parse("package:com.example.removed")
        })
        shadowOf(android.os.Looper.getMainLooper()).idle()

        val packages = repo.all.value.map { it.packageName }
        assertTrue(
            "PACKAGE_REMOVED broadcast must trigger refresh (Bravo should now appear): $packages",
            packages.contains("com.example.bravo"),
        )
    }

    @Test
    fun `stop unregisters the receiver so further broadcasts are ignored`() {
        installLaunchable("com.example.alpha", "AlphaMain", "Alpha")
        repo.start()
        repo.refresh()
        repo.stop()

        installLaunchable("com.example.bravo", "BravoMain", "Bravo")
        ctx.sendBroadcast(Intent(Intent.ACTION_PACKAGE_ADDED).apply {
            data = android.net.Uri.parse("package:com.example.bravo")
        })
        shadowOf(android.os.Looper.getMainLooper()).idle()

        val labels = repo.all.value.map { it.label }
        assertFalse("after stop(), receiver must not refresh: $labels", labels.contains("Bravo"))
    }
}
