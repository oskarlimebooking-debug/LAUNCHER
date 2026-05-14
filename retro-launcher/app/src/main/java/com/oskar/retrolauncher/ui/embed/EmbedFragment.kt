package com.oskar.retrolauncher.ui.embed

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.oskar.retrolauncher.BuildConfig
import com.oskar.retrolauncher.R
import timber.log.Timber

/**
 * Hosts the VirtualDisplay embedding surface on system builds and falls back
 * to a placeholder message on standard builds.
 *
 * On the system flavor the real [EmbeddingContract] implementation is loaded
 * via [Class.forName] and bound to a [TextureView]. Lifecycle is tied to
 * [onResume] / [onPause] so the virtual display is never leaked.
 */
class EmbedFragment : Fragment(R.layout.fragment_embed) {

    private var embedding: EmbeddingContract? = null
    private var textureView: TextureView? = null
    private var placeholderView: TextView? = null

    private val rightPanelWidth: Int get() = 614
    private val rightPanelHeight: Int get() = 600
    private val virtualDpi: Int get() = 213

    // -- Lifecycle ---------------------------------------------------------

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        textureView = view.findViewById(R.id.embed_texture)
        placeholderView = view.findViewById(R.id.embed_placeholder)

        if (BuildConfig.ENABLE_EMBEDDING) {
            initEmbedding()
        } else {
            showPlaceholder()
        }
    }

    override fun onResume() {
        super.onResume()
        if (BuildConfig.ENABLE_EMBEDDING) {
            bindSurface()
        }
    }

    override fun onPause() {
        super.onPause()
        releaseEmbedding()
    }

    // -- Embedding setup ---------------------------------------------------

    private fun initEmbedding() {
        try {
            val clazz = Class.forName("com.oskar.retrolauncher.system.Embedding")
            val constructor = clazz.getConstructor(Context::class.java)
            embedding = constructor.newInstance(requireContext()) as EmbeddingContract
        } catch (e: Exception) {
            Timber.w(e, "Failed to load Embedding — falling back to placeholder")
            showPlaceholder()
        }
    }

    private fun bindSurface() {
        val emb = embedding ?: return
        val tv = textureView ?: return
        placeholderView?.visibility = View.GONE
        tv.visibility = View.VISIBLE

        tv.post {
            emb.bind(tv, tv.width.let { if (it > 0) it else rightPanelWidth },
                tv.height.let { if (it > 0) it else rightPanelHeight }, virtualDpi)
        }
    }

    private fun releaseEmbedding() {
        embedding?.release()
    }

    private fun showPlaceholder() {
        val placeholder = placeholderView ?: return
        placeholder.setText(R.string.embedding_unavailable)
        placeholder.visibility = View.VISIBLE
        textureView?.visibility = View.GONE
    }

    // -- Public API for external callers ------------------------------------

    /** Launch an app into the embedded display. */
    fun launchEmbedded(intent: Intent) {
        embedding?.launch(intent)
    }

    /** Forward a touch event to the embedded display. */
    fun forwardEmbeddedTouch(ev: MotionEvent) {
        val tv = textureView ?: return
        val emb = embedding ?: return
        emb.forwardTouch(ev, tv.width.let { if (it > 0) it else rightPanelWidth },
            tv.height.let { if (it > 0) it else rightPanelHeight },
            rightPanelWidth, rightPanelHeight)
    }
}
