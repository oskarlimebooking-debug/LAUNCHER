package com.oskar.retrolauncher

import com.oskar.retrolauncher.util.Permissions
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * T1.33 — Wizard step 4 (Default launcher) needs a way to detect whether
 * this app is the system default launcher. The test asserts the helper
 * runs without throwing and returns a definite Boolean — Robolectric's
 * PackageManager resolves the launcher's own MainActivity (manifest declares
 * CATEGORY_HOME), so isDefaultLauncher returns true here, matching the
 * real-device path where being-the-default takes the same code path.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class PermissionsT133Test {

    @Test
    fun `isDefaultLauncher resolves home intent and returns true for our own package`() {
        val ctx = RuntimeEnvironment.getApplication()
        // Test app is the launcher under Robolectric — manifest CATEGORY_HOME
        // routes ACTION_MAIN+HOME back to our own MainActivity.
        assertTrue(Permissions.isDefaultLauncher(ctx))
    }
}
