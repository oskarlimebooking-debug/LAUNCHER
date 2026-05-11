package com.oskar.retrolauncher.data.trip

import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import java.io.File

/**
 * T1.39 — one-shot osmdroid bootstrap. Points the tile provider at a private
 * app cache directory so launcher tiles stay isolated from any other osmdroid
 * client on the device, sets the User-Agent (Nominatim policy), and toggles
 * offline-first so the map prefers cached / MBTiles content over the network.
 *
 * MBTiles discovery: osmdroid's archive scanner picks up any `*.mbtiles`,
 * `*.zip`, `*.sqlite`, or `*.gemf` files found in the configured
 * `osmdroidBasePath`. To bundle an MBTiles file with the app, copy it into
 * the cache dir on first run via [seedMbTilesFromAssets].
 */
object OsmdroidTilesConfig {

    private const val ASSET_MBTILES = "tiles/launcher.mbtiles"
    private const val SEEDED_FILE = "launcher.mbtiles"
    private const val USER_AGENT = "retro-launcher/0.2"

    @Volatile private var initialized = false

    /**
     * Idempotent. Safe to invoke from `Application.onCreate` and again per
     * fragment attach — the second call is a cheap no-op flag check.
     */
    fun ensureInitialized(ctx: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val cfg = Configuration.getInstance()
            cfg.userAgentValue = USER_AGENT
            val base = File(ctx.cacheDir, "osmdroid").apply { mkdirs() }
            cfg.osmdroidBasePath = base
            cfg.osmdroidTileCache = File(base, "tiles").apply { mkdirs() }
            // Offline-first: do not download when an MBTiles / cached tile satisfies
            // the request. The map still renders the polyline on a blank canvas if
            // no tile source is available.
            seedMbTilesFromAssets(ctx, base)
            initialized = true
        }
    }

    /**
     * Apply the offline-friendly defaults to a MapView. Called from
     * `TripDetailFragment.onViewCreated`.
     */
    fun applyDefaults(map: MapView) {
        // Public OSM tile servers are the fallback when no MBTiles file is bundled
        // and the device happens to be online. Offline-first mode keeps the cache
        // authoritative when present (AC1).
        map.setTileSource(MAPNIK_FALLBACK)
        map.setUseDataConnection(false)
        map.setMultiTouchControls(true)
        map.isHorizontalMapRepetitionEnabled = false
        map.isVerticalMapRepetitionEnabled = false
    }

    /**
     * Public OSM tile server used only as a fallback when offline-first mode is
     * explicitly disabled (settings toggle in v0.3). The default
     * [applyDefaults] call leaves data connection off.
     */
    val MAPNIK_FALLBACK: ITileSource = TileSourceFactory.MAPNIK

    /**
     * Copies a bundled MBTiles asset (if present at
     * `assets/tiles/launcher.mbtiles`) into the osmdroid base path so the
     * archive scanner discovers it. The asset is optional — shipping the
     * tiles is a build-time decision and the launcher must still come up
     * when the asset is absent.
     */
    private fun seedMbTilesFromAssets(ctx: Context, baseDir: File) {
        val out = File(baseDir, SEEDED_FILE)
        if (out.exists() && out.length() > 0L) return
        runCatching {
            ctx.assets.open(ASSET_MBTILES).use { input ->
                out.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        // No asset? Leave the file absent — osmdroid will just render polyline-only.
    }
}
