package com.oskar.retrolauncher.diag

import android.util.Log
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Timber tree that writes log lines to a rotating file on disk.
 *
 * Files live under [logDir] as `launcher.log` (active) and `launcher.log.1`
 * (previous, after rotation). When the active file grows past [rotateBytes]
 * the previous rotated file is dropped, the active file is renamed, and a
 * fresh active file is started.
 *
 * Used to diagnose head-unit crashes that cannot be reached over USB/adb.
 * Pull with `adb pull /sdcard/Android/data/<app-id>/files/logs/`.
 */
class FileLogger(
    private val logDir: File,
    private val rotateBytes: Long = DEFAULT_ROTATE_BYTES,
) : Timber.Tree(), Closeable {

    private val lock = Any()
    private val active = File(logDir, "launcher.log")
    private val rotated = File(logDir, "launcher.log.1")

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        synchronized(lock) {
            try {
                ensureDir()
                rotateIfNeeded()
                FileOutputStream(active, true).use { fos ->
                    OutputStreamWriter(fos, StandardCharsets.UTF_8).use { osw ->
                        osw.write(formatLine(priority, tag, message, t))
                        osw.flush()
                    }
                }
            } catch (_: Throwable) {
                // Disk failures must not bring down the app — the file logger
                // is a diagnostic best-effort, not a critical path.
            }
        }
    }

    override fun close() {
        // Nothing to do — FileOutputStream is opened per-line for crash safety.
    }

    private fun ensureDir() {
        if (!logDir.exists()) logDir.mkdirs()
    }

    private fun rotateIfNeeded() {
        if (active.length() < rotateBytes) return
        if (rotated.exists()) rotated.delete()
        active.renameTo(rotated)
    }

    private fun formatLine(priority: Int, tag: String?, message: String, t: Throwable?): String {
        val ts = synchronized(ISO_FORMAT) { ISO_FORMAT.format(Date()) }
        val sb = StringBuilder(message.length + 64)
        sb.append(ts).append(' ').append(levelChar(priority)).append(' ')
            .append(tag ?: "App").append(": ").append(message).append('\n')
        if (t != null) {
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            sb.append(sw)
            if (!sw.toString().endsWith("\n")) sb.append('\n')
        }
        return sb.toString()
    }

    private fun levelChar(priority: Int): Char = when (priority) {
        Log.VERBOSE -> 'V'
        Log.DEBUG -> 'D'
        Log.INFO -> 'I'
        Log.WARN -> 'W'
        Log.ERROR -> 'E'
        Log.ASSERT -> 'A'
        else -> '?'
    }

    companion object {
        const val DEFAULT_ROTATE_BYTES: Long = 256L * 1024
        private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
    }
}
