package com.oskar.retrolauncher.ui.trips

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.util.formatRelativeDay
import com.oskar.retrolauncher.util.metersToDisplay

class TripsFragment : Fragment(R.layout.fragment_trips) {

    private val vm: TripsViewModel by viewModels()
    private lateinit var adapter: TripsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val monthLabel = view.findViewById<TextView>(R.id.month_label)
        val calendar = view.findViewById<TripCalendarView>(R.id.calendar)
        val summary = view.findViewById<TextView>(R.id.summary)
        val list = view.findViewById<RecyclerView>(R.id.trip_list)
        val empty = view.findViewById<TextView>(R.id.empty)

        adapter = TripsAdapter()
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private val redPaint = Paint().apply { color = Color.parseColor("#332222") }

            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ) = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val trip = adapter.getTrip(vh.bindingAdapterPosition)
                vm.delete(trip)
            }

            override fun onChildDraw(
                c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean,
            ) {
                val v = vh.itemView
                if (dX < 0) {
                    c.drawRect(
                        v.right + dX, v.top.toFloat(),
                        v.right.toFloat(), v.bottom.toFloat(),
                        redPaint,
                    )
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
            }
        }).attachToRecyclerView(list)

        monthLabel.text = vm.selectedDayMs.value.formatRelativeDay()

        calendar.onDaySelected = { dayStartMs ->
            vm.selectDay(dayStartMs)
            monthLabel.text = dayStartMs.formatRelativeDay()
        }

        vm.distanceByDay.observe(viewLifecycleOwner) { byDay ->
            calendar.setDistances(byDay)
        }

        vm.recentTrips.observe(viewLifecycleOwner) { trips ->
            val units = App.settings.units
            val totalDist = trips.sumOf { it.distanceM }
            summary.text = if (trips.isEmpty()) "" else
                "${trips.size} trips · ${totalDist.metersToDisplay(units)} last 90 days"
        }

        vm.dayTrips.observe(viewLifecycleOwner) { dayTrips ->
            adapter.submitList(dayTrips)
            empty.visibility = if (dayTrips.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
