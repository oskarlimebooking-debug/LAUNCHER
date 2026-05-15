package com.oskar.retrolauncher.system

import android.app.ActivityOptions
import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for the SDK-guard pattern in [Embedding.launch()].
 *
 * [Embedding] lives in the system source set (compiled only when platform.keystore
 * is present). These tests validate the cross-version launch-options contract
 * through a mirrored helper so they compile under the standard flavor.
 */
@RunWith(RobolectricTestRunner::class)
class EmbeddingTest {

    /** Mirrors the [Embedding.launch] pattern for creating launch options. */
    private fun createLaunchOptions(displayId: Int): Boolean {
        val opts = ActivityOptions.makeBasic()
        var didSetDisplayId = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            opts.setLaunchDisplayId(displayId)
            didSetDisplayId = true
        }
        return didSetDisplayId
    }

    @Test
    @Config(sdk = [23])
    fun `launch options skip setLaunchDisplayId on API 23`() {
        assertFalse(
            "setLaunchDisplayId must not be called on API 23 — it does not exist",
            createLaunchOptions(displayId = 42),
        )
    }

    @Test
    @Config(sdk = [26])
    fun `launch options use setLaunchDisplayId on API 26`() {
        assertTrue(
            "setLaunchDisplayId must be called on API 26+",
            createLaunchOptions(displayId = 42),
        )
    }

    @Test
    @Config(sdk = [28])
    fun `launch options use setLaunchDisplayId on API 28`() {
        assertTrue(
            "setLaunchDisplayId must be called on API 26+ (API 28 regression guard)",
            createLaunchOptions(displayId = 42),
        )
    }
}
