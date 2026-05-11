package com.oskar.retrolauncher.test

import android.graphics.Bitmap
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.Screenshot
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File
import java.io.FileOutputStream

/**
 * T1.36 AC5 — when a fragment instrumented test fails, dump a PNG of the
 * device under-test to the app's external cache dir so CI can pick it up
 * as a failure artifact. Falls back to the internal cache dir if external
 * isn't mounted (rare on the API 23 emulator we target).
 *
 * Files land at `{cacheDir}/screenshots/{ClassName}-{methodName}.png`.
 *
 * The "diffs" wording in the AC describes the artifact CI publishes — the
 * test rule itself only takes a snapshot at failure time. Reference-image
 * comparison would require golden bitmaps in VCS, which is intentionally
 * out of scope for v0.1.
 */
class ScreenshotOnFailureRule : TestWatcher() {

    override fun failed(e: Throwable?, description: Description) {
        runCatching {
            val ctx = InstrumentationRegistry.getInstrumentation().targetContext
            val dir = (ctx.externalCacheDir ?: ctx.cacheDir).resolve("screenshots").apply {
                mkdirs()
            }
            val name = "${description.className}-${description.methodName}.png"
            val outFile = File(dir, name.replace('/', '_'))

            // androidx.test.runner.screenshot.Screenshot uses UiAutomation.takeScreenshot
            // on API 18+ which is exactly what the AC23 emulator needs.
            val bmp: Bitmap = Screenshot.capture().bitmap
            FileOutputStream(outFile).use { os ->
                bmp.compress(Bitmap.CompressFormat.PNG, /* quality= */ 100, os)
            }
            android.util.Log.w(
                "ScreenshotOnFailureRule",
                "Saved failure screenshot to ${outFile.absolutePath} (api ${Build.VERSION.SDK_INT})",
            )
        }
    }
}
