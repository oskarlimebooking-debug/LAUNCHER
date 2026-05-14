package com.oskar.retrolauncher.ui.grid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.apps.AppEntry

class AppGridAdapter(
    private val onClick: (AppEntry) -> Unit,
    private val onLongClick: (AppEntry, View) -> Boolean,
) : ListAdapter<AppEntry, AppGridAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        Glide.with(holder.icon).load(item.applicationInfo).into(holder.icon)
        holder.label.text = item.label
        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener { onLongClick(item, holder.itemView) }
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
        val icon: ImageView = v.findViewById(R.id.app_icon)
        val label: TextView = v.findViewById(R.id.app_label)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AppEntry>() {
            override fun areItemsTheSame(a: AppEntry, b: AppEntry) = a.componentName == b.componentName
            override fun areContentsTheSame(a: AppEntry, b: AppEntry) = a.label == b.label
        }
    }
}
