package com.oskar.retrolauncher.ui.trips

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.trip.TripEntity
import com.oskar.retrolauncher.util.formatDurationShort
import com.oskar.retrolauncher.util.metersPerSecondToDisplay
import com.oskar.retrolauncher.util.metersToDisplay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Right-panel page 2. Top card shows the live trip (or the most recent
 * finished trip when idle) with a speed-over-time chart; bottom strip is a
 * horizontal scroller of recent trips with sparklines. Swipe-left-to-delete
 * on the strip is preserved from the previous design.
 */
class TripsFragment : Fragment(R.layout.fragment_trips) {

    private val vm: TripsViewModel by viewModels()
    private lateinit var adapter: TripCardAdapter
    private lateinit var statusDot: TextView
    private lateinit var statusText: TextView
    private lateinit var statDist: StatCellView
    private lateinit var statDur: StatCellView
    private lateinit var statAvg: StatCellView
    private lateinit var statMax: StatCellView
    private lateinit var chart: LiveSpeedChartView
    private lateinit var weekSummary: TextView
    private lateinit var empty: TextView
    private lateinit var strip: RecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        statusDot = view.findViewById(R.id.trip_status_dot)
        statusText = view.findViewById(R.id.trip_status_text)
        statDist = view.findViewById(R.id.stat_distance)
        statDur = view.findViewById(R.id.stat_duration)
        statAvg = view.findViewById(R.id.stat_avg)
        statMax = view.findViewById(R.id.stat_max)
        chart = view.findViewById(R.id.speed_chart)
        weekSummary = view.findViewById(R.id.week_summary)
        empty = view.findViewById(R.id.empty)
        strip = view.findViewById(R.id.trip_strip)

        adapter = TripCardAdapter(
            sparkProvider = { trip ->
                val cached = vm.sparkValues(trip)
                if (cached.isEmpty()) vm.requestSpark(trip)
                cached
            },
            onClick = ::openTripDetail,
        )
        strip.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        strip.adapter = adapter
        attachSwipeToDelete(strip)

        // Initial labels for the stat cells — values fill in once data arrives.
        statDist.set(getString(R.string.trips_stat_distance), "—")
        statDur.set(getString(R.string.trips_stat_duration), "—")
        statAvg.set(getString(R.string.trips_stat_avg), "—")
        statMax.set(getString(R.string.trips_stat_max), "—")

        viewLifecycleOwner.lifecycleScope.launch {
            vm.displayed.collectLatest(::renderDisplay)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            // Rebind the strip when an async spark fetch finishes so the new line shows up.
            vm.sparkInvalidations.collectLatest { adapter.notifyDataSetChanged() }
        }

        vm.recentTrips.observe(viewLifecycleOwner) { trips ->
            adapter.submitList(trips)
            empty.visibility = if (trips.isEmpty()) View.VISIBLE else View.GONE
        }
        vm.weekStats.observe(viewLifecycleOwner) { stats ->
            weekSummary.text = getString(
                R.string.trips_week_summary_fmt,
                stats.totalM.metersToDisplay(App.settings.units),
                stats.count,
            )
        }
    }

    private fun renderDisplay(d: TripsViewModel.Display) {
        val units = App.settings.units
        val speedUnit = if (units == com.oskar.retrolauncher.data.prefs.Units.METRIC) "km/h" else "mph"
        when (d.mode) {
            TripsViewModel.Mode.RECORDING -> {
                statusDot.visibility = View.VISIBLE
                statusDot.setTextColor(Color.parseColor("#FF5252"))
                statusText.text = getString(R.string.trips_status_recording)
            }
            TripsViewModel.Mode.LAST -> {
                statusDot.visibility = View.VISIBLE
                statusDot.setTextColor(Color.parseColor("#4DD0E1"))
                statusText.text = getString(R.string.trips_status_last)
            }
            TripsViewModel.Mode.IDLE -> {
                statusDot.visibility = View.GONE
                statusText.text = getString(R.string.trips_status_idle)
            }
        }

        chart.setData(d.samples)

        val trip = d.trip
        if (trip == null) {
            statDist.set(getString(R.string.trips_stat_distance), "—")
            statDur.set(getString(R.string.trips_stat_duration), "—")
            statAvg.set(getString(R.string.trips_stat_avg), "—")
            statMax.set(getString(R.string.trips_stat_max), "—")
            return
        }
        statDist.set(getString(R.string.trips_stat_distance), trip.distanceM.metersToDisplay(units))
        statDur.set(getString(R.string.trips_stat_duration), (trip.endMs - trip.startMs).formatDurationShort())
        val avg = trip.avgSpeedMs.metersPerSecondToDisplay(units)
        val max = trip.maxSpeedMs.metersPerSecondToDisplay(units)
        statAvg.set(getString(R.string.trips_stat_avg), "%.0f %s".format(avg, speedUnit))
        statMax.set(getString(R.string.trips_stat_max), "%.0f %s".format(max, speedUnit))
    }

    private fun attachSwipeToDelete(list: RecyclerView) {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.UP) {
            private val redPaint = Paint().apply { color = Color.parseColor("#332222") }

            override fun onMove(
                rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder,
            ) = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val trip = adapter.currentList.getOrNull(vh.bindingAdapterPosition) ?: return
                vm.delete(trip)
            }

            override fun onChildDraw(
                c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean,
            ) {
                val v = vh.itemView
                if (dX < 0) {
                    c.drawRect(v.right + dX, v.top.toFloat(), v.right.toFloat(), v.bottom.toFloat(), redPaint)
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
            }
        }).attachToRecyclerView(list)
    }

    private fun openTripDetail(trip: TripEntity) {
        val detail = TripDetailFragment().apply {
            arguments = TripDetailFragment.argsFor(trip)
        }
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, detail, TAG_TRIP_DETAIL)
            .addToBackStack(TAG_TRIP_DETAIL)
            .commit()
    }

    private companion object {
        const val TAG_TRIP_DETAIL = "trip_detail"
    }
}
