package com.oskar.retrolauncher.data.trip

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * T1.40 — writes a [TripEntity] + its [TripPoint]s as a GPX 1.1 file into a
 * MediaStore-visible location (`Download/RetroLauncher/trip-{id}.gpx`).
 *
 * On API ≥ 29 we go through `MediaStore.Downloads` so the file is indexed by
 * the media provider without holding `WRITE_EXTERNAL_STORAGE`. On older
 * releases we write to the legacy Downloads dir and trigger a media-scan so it
 * shows up in file managers (AC3).
 *
 * The IO surface returns a sealed [Result] rather than throwing — callers map
 * each branch to a user-facing snackbar (AC5).
 */
class GpxExporter(private val context: Context) {

    sealed class Result {
        data class Success(val uri: Uri, val displayPath: String) : Result()
        object PermissionDenied : Result()
        data class IoFailure(val error: Throwable) : Result()
    }

    suspend fun export(trip: TripEntity, points: List<TripPoint>): Result =
        withContext(Dispatchers.IO) { runExport(trip, points) }

    private fun runExport(trip: TripEntity, points: List<TripPoint>): Result {
        val xml = GpxXml.serialize(trip, points)
        val name = "trip-${trip.id}.gpx"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeViaMediaStore(name, xml)
            } else {
                writeToDownloadsDir(name, xml)
            }
        } catch (e: SecurityException) {
            Result.PermissionDenied
        } catch (e: IOException) {
            Result.IoFailure(e)
        }
    }

    private fun writeViaMediaStore(name: String, xml: String): Result {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, MIME_GPX)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$SUBDIR")
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }
        val uri = context.contentResolver.insert(collection, values)
            ?: return Result.IoFailure(IOException("ContentResolver.insert returned null for $name"))
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(xml.toByteArray(Charsets.UTF_8))
        } ?: return Result.IoFailure(IOException("openOutputStream returned null for $uri"))
        return Result.Success(uri, "${Environment.DIRECTORY_DOWNLOADS}/$SUBDIR/$name")
    }

    private fun writeToDownloadsDir(name: String, xml: String): Result {
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = File(root, SUBDIR).apply { if (!exists() && !mkdirs()) throw IOException("mkdir failed: $absolutePath") }
        val file = File(dir, name)
        file.writeBytes(xml.toByteArray(Charsets.UTF_8))
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(MIME_GPX), null)
        return Result.Success(Uri.fromFile(file), file.absolutePath)
    }

    private companion object {
        const val SUBDIR = "RetroLauncher"
        const val MIME_GPX = "application/gpx+xml"
    }
}
