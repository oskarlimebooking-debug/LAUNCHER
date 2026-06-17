package com.oskar.retrolauncher.ui.player.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.music.Artist

/** Artist list: name + "N albums · M songs". */
class ArtistListAdapter(
    private val onClick: (Artist) -> Unit,
) : ListAdapter<Artist, ArtistListAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_artist, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val artist = getItem(position)
        holder.name.text = artist.name
        holder.meta.text = holder.itemView.resources.getString(
            R.string.music_artist_meta, artist.albumCount, artist.trackCount,
        )
        holder.itemView.setOnClickListener { onClick(artist) }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.artist_name)
        val meta: TextView = v.findViewById(R.id.artist_meta)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Artist>() {
            override fun areItemsTheSame(a: Artist, b: Artist) = a.name == b.name
            override fun areContentsTheSame(a: Artist, b: Artist) = a == b
        }
    }
}
