package com.oskar.retrolauncher.ui.player.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.music.Track
import com.oskar.retrolauncher.ui.player.formatTime
import com.oskar.retrolauncher.ui.player.loadAlbumArt

/**
 * Shared track-list row (Songs / Liked / album & playlist detail). The like
 * heart only appears when [onToggleLike] is supplied (M4) — otherwise it's gone.
 */
class SongRowAdapter(
    private val onClick: (Int) -> Unit,
    private val onToggleLike: ((Track) -> Unit)? = null,
    private val onLongClick: ((Track) -> Unit)? = null,
) : ListAdapter<Track, SongRowAdapter.VH>(DIFF) {

    private var likedIds: Set<Long> = emptySet()

    fun setLiked(ids: Set<Long>) {
        likedIds = ids
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_song_row, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val track = getItem(position)
        holder.title.text = track.title
        holder.subtitle.text = track.artist
        holder.duration.text = formatTime(track.durationMs)
        holder.art.loadAlbumArt(track.albumArtUri)
        holder.itemView.setOnClickListener { onClick(holder.bindingAdapterPosition) }
        holder.itemView.setOnLongClickListener {
            onLongClick?.invoke(track)
            onLongClick != null
        }
        if (onToggleLike == null) {
            holder.like.visibility = View.GONE
        } else {
            holder.like.visibility = View.VISIBLE
            holder.like.setImageResource(
                if (track.id in likedIds) R.drawable.ic_heart_filled else R.drawable.ic_heart,
            )
            holder.like.setOnClickListener { onToggleLike.invoke(track) }
        }
    }

    override fun onViewRecycled(holder: VH) {
        com.bumptech.glide.Glide.with(holder.art).clear(holder.art)
        super.onViewRecycled(holder)
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val art: ImageView = v.findViewById(R.id.song_art)
        val title: TextView = v.findViewById(R.id.song_title)
        val subtitle: TextView = v.findViewById(R.id.song_artist)
        val duration: TextView = v.findViewById(R.id.song_duration)
        val like: ImageButton = v.findViewById(R.id.song_like)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Track>() {
            override fun areItemsTheSame(a: Track, b: Track) = a.id == b.id
            override fun areContentsTheSame(a: Track, b: Track) = a == b
        }
    }
}
