package com.oskar.retrolauncher.ui.grid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.apps.AppEntry

class RailAdapter(
    private val onClick: (AppEntry) -> Unit,
) : ListAdapter<AppEntry, RailAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rail, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.icon.setImageDrawable(item.icon)
        holder.itemView.setOnClickListener { onClick(item) }
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
