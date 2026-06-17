package com.oskar.retrolauncher.data.music

/** Status of the local-library scan, surfaced to the browse UI. */
sealed interface MusicScanState {
    data object Idle : MusicScanState
    data object Scanning : MusicScanState
    data class Done(val trackCount: Int) : MusicScanState
    data object PermissionDenied : MusicScanState
}
