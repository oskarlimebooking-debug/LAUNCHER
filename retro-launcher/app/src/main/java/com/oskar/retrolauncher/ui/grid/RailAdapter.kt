package com.oskar.retrolauncher.ui.grid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.apps.AppEntry

class RailAdapter(
    private val onClick: (AppEntry) -> Unit,
) : ListAdapter<AppEntry, RailAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rail, parent, false)
        // Apply the SettingsStore-driven rail icon size at inflate time so a
        // larger pref takes effect immediately (the adapter is rebuilt
        // whenever the pinned list changes, which happens on size change).
        val sizePx = (App.settings.railIconSizeDp * parent.resources.displayMetrics.density).toInt()
        val icon = view.findViewById<ImageView>(R.id.rail_icon)
        icon.layoutParams = icon.layoutParams.apply {
            width = sizePx; height = sizePx
        }
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        Glide.with(holder.icon).load(item.applicationInfo).into(holder.icon)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    /** T1.41 — apply a single drag-step swap; submitList lets DiffUtil drive notifyItemMoved. */
    fun moveItem(from: Int, to: Int) {
        val list = currentList.toMutableList()
        if (from !in list.indices || to !in list.indices || from == to) return
        list.add(to, list.removeAt(from))
        submitList(list)
    }

    fun currentEntries(): List<AppEntry> = currentList.toList()

    override fun onViewRecycled(holder: VH) {
        Glide.with(holder.icon).clear(holder.icon)
        super.onViewRecycled(holder)
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.rail_icon)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AppEntry>() {
            override fun areItemsTheSame(a: AppEntry, b: AppEntry) = a.componentName == b.componentName
            override fun areContentsTheSame(a: AppEntry, b: AppEntry) = a.label == b.label
        }
    }
}
