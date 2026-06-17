package com.oskar.retrolauncher.ui.player.library

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
import com.oskar.retrolauncher.data.music.Album
import com.oskar.retrolauncher.ui.player.loadAlbumArt

/** Album grid: cover art + name + artist. */
class AlbumGridAdapter(
    private val onClick: (Album) -> Unit,
) : ListAdapter<Album, AlbumGridAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_album_grid, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val album = getItem(position)
        holder.name.text = album.name
        holder.artist.text = album.artist
        holder.art.loadAlbumArt(album.artUri)
        holder.itemView.setOnClickListener { onClick(album) }
    }

    override fun onViewRecycled(holder: VH) {
        Glide.with(holder.art).clear(holder.art)
        super.onViewRecycled(holder)
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val art: ImageView = v.findViewById(R.id.album_art)
        val name: TextView = v.findViewById(R.id.album_name)
        val artist: TextView = v.findViewById(R.id.album_artist)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Album>() {
            override fun areItemsTheSame(a: Album, b: Album) = a.id == b.id
            override fun areContentsTheSame(a: Album, b: Album) = a == b
        }
    }
}
