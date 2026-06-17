package com.oskar.retrolauncher.ui.player.queue

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.music.QueueItem
import com.oskar.retrolauncher.ui.player.loadAlbumArt

/** Playback queue; the current entry is accent-highlighted. Tap to jump. */
class QueueAdapter(
    private val onClick: (Int) -> Unit,
) : ListAdapter<QueueItem, QueueAdapter.VH>(DIFF) {

    private var current = 0

    fun setCurrent(index: Int) {
        val old = current
        current = index
        if (old != index) {
            notifyItemChanged(old)
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_queue, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.title.text = item.title
        holder.subtitle.text = item.artist
        holder.art.loadAlbumArt(item.artworkUri)
        val accent = ContextCompat.getColor(holder.itemView.context, R.color.accent)
        val normal = ContextCompat.getColor(holder.itemView.context, R.color.text_primary)
        holder.title.setTextColor(if (position == current) accent else normal)
        holder.itemView.setOnClickListener { onClick(holder.bindingAdapterPosition) }
    }

    override fun onViewRecycled(holder: VH) {
        Glide.with(holder.art).clear(holder.art)
        super.onViewRecycled(holder)
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val art: ImageView = v.findViewById(R.id.queue_art)
        val title: TextView = v.findViewById(R.id.queue_title)
        val subtitle: TextView = v.findViewById(R.id.queue_artist)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<QueueItem>() {
            override fun areItemsTheSame(a: QueueItem, b: QueueItem) = a.mediaId == b.mediaId
            override fun areContentsTheSame(a: QueueItem, b: QueueItem) = a == b
        }
    }
}
