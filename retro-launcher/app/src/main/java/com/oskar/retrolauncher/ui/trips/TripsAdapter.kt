package com.oskar.retrolauncher.ui.trips

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.prefs.Units
import com.oskar.retrolauncher.data.trip.TripEntity
import com.oskar.retrolauncher.util.formatHM
import com.oskar.retrolauncher.util.metersPerSecondToDisplay
import com.oskar.retrolauncher.util.metersToDisplay

class TripsAdapter : ListAdapter<TripEntity, TripsAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val units = App.settings.units
        val avgUnit = if (units == Units.METRIC) "kph" else "mph"
        val maxUnit = avgUnit
        val ep = listOfNotNull(item.startLabel, item.endLabel)
            .filter { it.isNotBlank() }
            .joinToString(" → ")
            .ifBlank { "Trip" }
        holder.endpoints.text = ep
        holder.time.text = "${item.startMs.formatHM()} – ${item.endMs.formatHM()}"
        holder.distance.text = item.distanceM.metersToDisplay(units)
        holder.avg.text = "%.0f $avgUnit avg".format(item.avgSpeedMs.metersPerSecondToDisplay(units))
        holder.max.text = "%.0f $maxUnit max".format(item.maxSpeedMs.metersPerSecondToDisplay(units))
    }

    fun getTrip(position: Int): TripEntity = getItem(position)

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val endpoints: TextView = v.findViewById(R.id.trip_endpoints)
        val time: TextView = v.findViewById(R.id.trip_time)
        val distance: TextView = v.findViewById(R.id.trip_distance)
        val avg: TextView = v.findViewById(R.id.trip_avg)
        val max: TextView = v.findViewById(R.id.trip_max)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TripEntity>() {
            override fun areItemsTheSame(a: TripEntity, b: TripEntity) = a.id == b.id
            override fun areContentsTheSame(a: TripEntity, b: TripEntity) = a == b
        }
    }
}
