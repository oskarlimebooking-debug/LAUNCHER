package com.oskar.retrolauncher.ui.trips

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.prefs.Units
import com.oskar.retrolauncher.data.trip.GpxExporter
import com.oskar.retrolauncher.data.trip.NominatimAddressCache
import com.oskar.retrolauncher.data.trip.OsmdroidTilesConfig
import com.oskar.retrolauncher.data.trip.TripEntity
import com.oskar.retrolauncher.data.trip.TripPoint
import com.oskar.retrolauncher.util.formatDurationShort
import com.oskar.retrolauncher.util.metersPerSecondToDisplay
import com.oskar.retrolauncher.util.metersToDisplay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

/**
 * T1.39 — trip detail screen.
 *
 * Renders the trip's recorded points on an `osmdroid` MapView pointed at offline
 * MBTiles tiles (or cached tiles if the user previously had a network). A
 * `Polyline` overlay draws the recorded path. The bottom panel shows distance,
 * duration, max + avg speed, and start/end addresses (cached Nominatim labels
 * with `lat, lon` fallback per AC5).
 *
 * Acceptance criteria:
 *  - AC1: offline-first map (configured in OsmdroidTilesConfig.applyDefaults).
 *  - AC2: polyline renders smoothly — osmdroid's native Polyline overlay uses
 *         hardware-accelerated Canvas drawing.
 *  - AC3: pinch-to-zoom and pan via setMultiTouchControls(true).
 *  - AC4: stats cross-checked against TripEntity aggregates via TripStats.crossCheck.
 *  - AC5: start/end addresses fall back to "lat, lon" when the cache is cold.
 */
class TripDetailFragment : Fragment(R.layout.fragment_trip_detail) {

    private lateinit var map: MapView

    /** Override seam for tests — production builds a real Context-bound exporter. */
    internal var exporterFactory: (android.content.Context) -> GpxExporter = { GpxExporter(it) }

    private val vm: TripDetailViewModel by viewModels {
        val args = requireArguments()
        val entity = TripEntity(
            id = args.getLong(ARG_TRIP_ID),
            startMs = args.getLong(ARG_START_MS),
            endMs = args.getLong(ARG_END_MS),
            distanceM = args.getDouble(ARG_DISTANCE_M),
            avgSpeedMs = args.getDouble(ARG_AVG_MS),
            maxSpeedMs = args.getDouble(ARG_MAX_MS),
            startLabel = args.getString(ARG_START_LABEL),
            endLabel = args.getString(ARG_END_LABEL),
        )
        TripDetailViewModel.Factory(
            repo = App.trips,
            addrCache = NominatimAddressCache(requireContext()),
            tripId = entity.id,
            entity = entity,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        OsmdroidTilesConfig.ensureInitialized(requireContext().applicationContext)

        map = view.findViewById(R.id.trip_map)
        OsmdroidTilesConfig.applyDefaults(map)

        view.findViewById<TextView>(R.id.trip_detail_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val exportBtn = view.findViewById<TextView>(R.id.trip_detail_export)
        exportBtn.setOnClickListener { onExportClicked(view) }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collectLatest { state -> render(view, state) }
        }
        vm.load()
    }

    private fun onExportClicked(view: View) {
        val state = vm.state.value as? TripDetailUiState.Loaded ?: return
        val exporter = exporterFactory(requireContext().applicationContext)
        viewLifecycleOwner.lifecycleScope.launch {
            val result = exporter.export(state.trip, state.points)
            showExportResult(view, result)
        }
    }

    private fun showExportResult(view: View, result: GpxExporter.Result) {
        val msg = when (result) {
            is GpxExporter.Result.Success ->
                getString(R.string.trip_detail_export_success_fmt, result.displayPath)
            GpxExporter.Result.PermissionDenied ->
                getString(R.string.trip_detail_export_permission_denied)
            is GpxExporter.Result.IoFailure ->
                getString(R.string.trip_detail_export_failed)
        }
        Snackbar.make(view, msg, Snackbar.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        if (::map.isInitialized) map.onResume()
    }

    override fun onPause() {
        if (::map.isInitialized) map.onPause()
        super.onPause()
    }

    private fun render(view: View, state: TripDetailUiState) {
        val title = view.findViewById<TextView>(R.id.trip_detail_title)
        val distTv = view.findViewById<TextView>(R.id.stat_distance)
        val durTv = view.findViewById<TextView>(R.id.stat_duration)
        val maxTv = view.findViewById<TextView>(R.id.stat_max_speed)
        val avgTv = view.findViewById<TextView>(R.id.stat_avg_speed)
        val startTv = view.findViewById<TextView>(R.id.stat_start_addr)
        val endTv = view.findViewById<TextView>(R.id.stat_end_addr)

        if (state !is TripDetailUiState.Loaded) {
            title.setText(R.string.trips_title)
            return
        }

        val units = App.settings.units
        val speedUnit = if (units == Units.METRIC) "kph" else "mph"

        title.text = getString(
            R.string.trip_detail_title_fmt,
            state.startAddress,
            state.endAddress,
        )
        distTv.text = getString(R.string.trip_stat_distance_fmt, state.stats.distanceM.metersToDisplay(units))
        durTv.text = getString(R.string.trip_stat_duration_fmt, state.stats.durationMs.formatDurationShort())
        maxTv.text = getString(
            R.string.trip_stat_max_fmt,
            state.stats.maxSpeedMs.metersPerSecondToDisplay(units),
            speedUnit,
        )
        avgTv.text = getString(
            R.string.trip_stat_avg_fmt,
            state.stats.avgSpeedMs.metersPerSecondToDisplay(units),
            speedUnit,
        )
        startTv.text = getString(R.string.trip_detail_start_fmt, state.startAddress)
        endTv.text = getString(R.string.trip_detail_end_fmt, state.endAddress)

        drawPolyline(state.points)
        zoomToBounds(state.points, state.bbox)
    }

    private fun drawPolyline(points: List<TripPoint>) {
        if (!::map.isInitialized) return
        map.overlays.removeAll { it is Polyline }
        if (points.size < 2) return
        val polyline = Polyline().apply {
            outlinePaint.color = Color.parseColor("#FF8500") // accent orange
            outlinePaint.strokeWidth = 8f
            setPoints(points.map { GeoPoint(it.lat, it.lon) })
        }
        map.overlays.add(polyline)
        map.invalidate()
    }

    private fun zoomToBounds(
        points: List<TripPoint>,
        bbox: com.oskar.retrolauncher.data.trip.GeoBoundingBox?,
    ) {
        if (!::map.isInitialized) return
        if (bbox == null || points.isEmpty()) return
        // osmdroid wants north/east/south/west — but its BoundingBox constructor
        // takes (north, east, south, west). Padding the box ~5% so the polyline
        // doesn't kiss the edges.
        val latPad = (bbox.maxLat - bbox.minLat).coerceAtLeast(0.0005) * 0.1
        val lonPad = (bbox.maxLon - bbox.minLon).coerceAtLeast(0.0005) * 0.1
        val osmBox = BoundingBox(
            bbox.maxLat + latPad,
            bbox.maxLon + lonPad,
            bbox.minLat - latPad,
            bbox.minLon - lonPad,
        )
        map.post {
            map.zoomToBoundingBox(osmBox, false)
        }
    }

    companion object {
        const val ARG_TRIP_ID = "trip_id"
        const val ARG_START_MS = "start_ms"
        const val ARG_END_MS = "end_ms"
        const val ARG_DISTANCE_M = "distance_m"
        const val ARG_AVG_MS = "avg_ms"
        const val ARG_MAX_MS = "max_ms"
        const val ARG_START_LABEL = "start_label"
        const val ARG_END_LABEL = "end_label"

        fun argsFor(trip: TripEntity): Bundle = Bundle().apply {
            putLong(ARG_TRIP_ID, trip.id)
            putLong(ARG_START_MS, trip.startMs)
            putLong(ARG_END_MS, trip.endMs)
            putDouble(ARG_DISTANCE_M, trip.distanceM)
            putDouble(ARG_AVG_MS, trip.avgSpeedMs)
            putDouble(ARG_MAX_MS, trip.maxSpeedMs)
            putString(ARG_START_LABEL, trip.startLabel)
            putString(ARG_END_LABEL, trip.endLabel)
        }
    }
}
