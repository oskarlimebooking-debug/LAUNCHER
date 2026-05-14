package com.oskar.retrolauncher.ui.embed

import android.content.Context
import android.content.Intent
import android.view.MotionEvent
import android.view.TextureView

/**
 * Cross-flavor contract for the VirtualDisplay embedding pipeline.
 *
 * The real implementation ([com.oskar.retrolauncher.system.Embedding]) lives in
 * the `system` source set and is only compiled when `platform.keystore` exists.
 * [EmbedFragment] resolves it via [Class.forName] when [BuildConfig.ENABLE_EMBEDDING]
 * is true; on the `standard` flavor the fragment shows a placeholder instead.
 */
interface EmbeddingContract {
    /** Attach a [TextureView] and start rendering frames from the virtual display. */
    fun bind(textureView: TextureView, w: Int, h: Int, dpi: Int)

    /** Launch an [Intent] into the virtual display. */
    fun launch(intent: Intent)

    /** Destroy the virtual display and release the surface. */
    fun release()

    /** Forward a touch event from the TextureView to the embedded display. */
    fun forwardTouch(ev: MotionEvent, viewW: Int, viewH: Int, dispW: Int, dispH: Int)
}
