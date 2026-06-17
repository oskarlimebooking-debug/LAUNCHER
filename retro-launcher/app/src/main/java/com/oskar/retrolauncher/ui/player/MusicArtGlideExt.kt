package com.oskar.retrolauncher.ui.player

import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.oskar.retrolauncher.R

/**
 * Centralised album-art loading for the card + player. Falls back to a vector
 * placeholder when the URI is missing or the art can't be decoded (common for
 * tracks with no embedded/MediaStore art).
 */
fun ImageView.loadAlbumArt(uri: String?, fallbackRes: Int = R.drawable.ic_album_placeholder) {
    if (uri.isNullOrBlank()) {
        setImageResource(fallbackRes)
        return
    }
    Glide.with(this)
        .load(Uri.parse(uri))
        .placeholder(fallbackRes)
        .error(fallbackRes)
        .centerCrop()
        .into(this)
}
