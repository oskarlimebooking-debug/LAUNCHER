package com.oskar.retrolauncher.diag

import android.content.Context
import java.io.File

/**
 * Resolves the on-device log directory.
 *
 * Prefers `getExternalFilesDir(null)/logs/` so logs can be pulled via
 *   `adb pull /sdcard/Android/data/<app-id>/files/logs/`
 * which on API 23 requires no runtime permission (app-scoped storage).
 *
 * Falls back to `filesDir/logs/` if external storage is unavailable
 * (emulators without SD card, ROMs with unusual mount layouts).
 */
object LogPaths {
    fun forContext(ctx: Context): File {
        val external = runCatching { ctx.getExternalFilesDir(null) }.getOrNull()
        val parent = external ?: ctx.filesDir
        return File(parent, "logs")
    }
}
