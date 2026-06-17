package com.oskar.retrolauncher.diag

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Uncaught-exception handler that writes a self-contained crash report to disk
 * before delegating to the previous handler.
 *
 * Each crash produces a `crash-<timestamp>.txt` file under [logDir] containing
 * device/build context and the full stack trace. Older crash files are pruned
 * keeping the [MAX_CRASH_FILES] most recent.
 *
 * The handler is installed once from [com.oskar.retrolauncher.App.onCreate] and
 * runs for the lifetime of the process. Disk-write failures are swallowed so
 * the previous handler (and ultimately the OS) still receives the throwable.
 */
class CrashHandler private constructor(
    private val logDir: File,
    private val previous: Thread.UncaughtExceptionHandler?,
    private val info: DeviceInfo,
) : Thread.UncaughtExceptionHandler {

    data class DeviceInfo(
        val sdk: Int,
        val manufacturer: String,
        val model: String,
        val fingerprint: String,
        val versionName: String,
        val applicationId: String,
        val buildType: String,
    )

    override fun uncaughtException(t: Thread, e: Throwable) {
        runCatching { writeCrashFile(t, e) }
        if (previous != null) {
            previous.uncaughtException(t, e)
        } else {
            throw e
        }
    }

    private fun writeCrashFile(t: Thread, e: Throwable) {
        if (!logDir.exists() && !logDir.mkdirs()) {
            // logDir is a regular file or otherwise unusable — best-effort failure.
            return
        }
        val now = Date()
        val safeTs = FILE_TS_FORMAT.format(now)
        val displayTs = DISPLAY_TS_FORMAT.format(now)
        val crashFile = File(logDir, "crash-$safeTs.txt")

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        pw.println("=== Retro Launcher crash ===")
        pw.println("Time:         $displayTs")
        pw.println("App:          ${info.applicationId} ${info.versionName} (${info.buildType})")
        pw.println("Device:       ${info.manufacturer} ${info.model}")
        pw.println("Fingerprint:  ${info.fingerprint}")
        pw.println("SDK:          ${info.sdk}")
        pw.println("Thread:       ${t.name}")
        pw.println()
        pw.println("--- stack ---")
        e.printStackTrace(pw)
        pw.flush()

        crashFile.writeText(sw.toString())
        pruneOld()
    }

    private fun pruneOld() {
        val crashes = logDir.listFiles { f ->
            f.name.startsWith("crash-") && f.name.endsWith(".txt")
        } ?: return
        if (crashes.size <= MAX_CRASH_FILES) return
        crashes
            .sortedByDescending { it.lastModified() }
            .drop(MAX_CRASH_FILES)
            .forEach { runCatching { it.delete() } }
    }

    companion object {
        const val MAX_CRASH_FILES: Int = 10

        // Filename-safe (no colons): `crash-2026-05-18T13-42-07-321.txt`
        private val FILE_TS_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss-SSS", Locale.US)
        private val DISPLAY_TS_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)

        fun install(logDir: File, info: DeviceInfo): CrashHandler {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            val handler = CrashHandler(logDir, previous, info)
            Thread.setDefaultUncaughtExceptionHandler(handler)
            return handler
        }
    }
}
