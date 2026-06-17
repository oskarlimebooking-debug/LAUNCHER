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
import com.oskar.retrolauncher.data.trip.TripEntity
import com.oskar.retrolauncher.util.formatDurationShort
import com.oskar.retrolauncher.util.formatHMHM
import com.oskar.retrolauncher.util.formatRelativeDay
import com.oskar.retrolauncher.util.metersToDisplay

/**
 * Horizontal-strip adapter showing one mini card per trip:
 *   - day label (TODAY / YESTERDAY / MAR 13)
 *   - HH:MM-HH:MM time window
 *   - distance + duration
 *   - sparkline of the trip's speed profile, supplied lazily through
 *     [sparkProvider] which returns a cached FloatArray (or empty when not yet
 *     loaded — caller schedules a fetch and rebinds when ready).
 */
class TripCardAdapter(
    private val sparkProvider: (TripEntity) -> FloatArray,
    private val onClick: (TripEntity) -> Unit,
) : ListAdapter<TripEntity, TripCardAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val trip = getItem(position)
        val units = App.settings.units
        holder.day.text = trip.startMs.formatRelativeDay()
        holder.time.text = trip.startMs.formatHMHM(trip.endMs)
        holder.distance.text = trip.distanceM.metersToDisplay(units)
        holder.duration.text = (trip.endMs - trip.startMs).formatDurationShort()
        holder.spark.setValues(sparkProvider(trip))
        holder.itemView.setOnClickListener { onClick(trip) }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val day: TextView = v.findViewById(R.id.card_day)
        val time: TextView = v.findViewById(R.id.card_time)
        val distance: TextView = v.findViewById(R.id.card_distance)
        val duration: TextView = v.findViewById(R.id.card_duration)
        val spark: SparklineView = v.findViewById(R.id.card_spark)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TripEntity>() {
            override fun areItemsTheSame(a: TripEntity, b: TripEntity) = a.id == b.id
            override fun areContentsTheSame(a: TripEntity, b: TripEntity) = a == b
        }
    }
}
