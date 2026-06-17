package com.oskar.retrolauncher.data.music

/**
 * Builds the MediaStore album-art content URI. Kept separate + pure so it can
 * be unit-tested and reused by Glide loaders without touching a ContentResolver.
 */
object AlbumArtUris {
    const val MEDIA_BASE = "content://media/external/audio/media"
    private const val ALBUM_ART_BASE = "content://media/external/audio/albumart"

    fun forAlbum(albumId: Long): String = "$ALBUM_ART_BASE/$albumId"

    fun forTrack(trackId: Long): String = "$MEDIA_BASE/$trackId"
}
