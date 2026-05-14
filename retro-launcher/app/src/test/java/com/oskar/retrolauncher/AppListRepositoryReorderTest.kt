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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * T1.41 — drag-to-reorder commit paths on AppListRepository. The adapter
 * publishes a new ordered list when the user drops a drag; this is the
 * persistence + re-emission contract that the rest of the UI depends on.
 *
 * - `commitGridOrder` writes the full componentName order to
 *   `SettingsStore.appOrder`; the next `refresh()` (or new repo) replays it.
 * - `commitPinnedOrder` writes the package list to `SettingsStore.pinnedApps`,
 *   updates `pinnedPackages` and the resolved `rail` flow in place.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class AppListRepositoryReorderTest {

    private lateinit var ctx: Context
    private lateinit var settings: SettingsStore
    private lateinit var repo: AppListRepository

    @Before
    fun setUp() {
        ctx = RuntimeEnvironment.getApplication()
        val prefs = ctx.getSharedPreferences("reorder_${System.nanoTime()}", Context.MODE_PRIVATE)
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
    fun `commitGridOrder writes componentName keys to SettingsStore appOrder`() {
        installLaunchable("com.example.alpha", "AlphaMain", "Alpha")
        installLaunchable("com.example.bravo", "BravoMain", "Bravo")
        installLaunchable("com.example.charlie", "CharlieMain", "Charlie")
        repo.refresh()

        // Default order is alphabetical: Alpha, Bravo, Charlie.
        // User drag reorders to: Charlie, Alpha, Bravo.
        val current = repo.all.value
        val reordered = listOf(current[2], current[0], current[1])
        repo.commitGridOrder(reordered)

        assertEquals(
            listOf(
                "com.example.charlie/CharlieMain",
                "com.example.alpha/AlphaMain",
                "com.example.bravo/BravoMain",
            ),
            settings.appOrder,
        )
    }

    @Test
    fun `commitGridOrder order survives a fresh repo instance and refresh`() {
        installLaunchable("com.example.alpha", "AlphaMain", "Alpha")
        installLaunchable("com.example.bravo", "BravoMain", "Bravo")
        repo.refresh()
        val current = repo.all.value
        repo.commitGridOrder(listOf(current[1], current[0]))

        val repo2 = AppListRepository(ctx, ctx.packageManager, settings)
        repo2.refresh()
        assertEquals(listOf("Bravo", "Alpha"), repo2.all.value.map { it.label })
    }

    @Test
    fun `commitGridOrder updates the all StateFlow in place`() {
        installLaunchable("com.example.alpha", "AlphaMain", "Alpha")
        installLaunchable("com.example.bravo", "BravoMain", "Bravo")
        repo.refresh()
        val current = repo.all.value
        repo.commitGridOrder(listOf(current[1], current[0]))

        assertEquals(listOf("Bravo", "Alpha"), repo.all.value.map { it.label })
    }

    @Test
    fun `commitPinnedOrder writes the package list to SettingsStore pinnedApps`() {
        repo.pin("com.a")
        repo.pin("com.b")
        repo.pin("com.c")

        repo.commitPinnedOrder(listOf("com.c", "com.a", "com.b"))

        assertEquals(listOf("com.c", "com.a", "com.b"), settings.pinnedApps)
    }

    @Test
    fun `commitPinnedOrder updates pinnedPackages and rail flows in place`() {
        installLaunchable("com.example.alpha", "AlphaMain", "Alpha")
        installLaunchable("com.example.bravo", "BravoMain", "Bravo")
        repo.pin("com.example.alpha")
        repo.pin("com.example.bravo")
        repo.refresh()

        repo.commitPinnedOrder(listOf("com.example.bravo", "com.example.alpha"))

        assertEquals(listOf("com.example.bravo", "com.example.alpha"), repo.pinnedPackages.value)
        assertEquals(listOf("Bravo", "Alpha"), repo.rail.value.map { it.label })
    }

    @Test
    fun `commitPinnedOrder persistence survives a fresh repo`() {
        repo.pin("com.a")
        repo.pin("com.b")
        repo.commitPinnedOrder(listOf("com.b", "com.a"))

        val repo2 = AppListRepository(ctx, ctx.packageManager, settings)
        assertEquals(listOf("com.b", "com.a"), repo2.pinnedPackages.value)
    }
}
