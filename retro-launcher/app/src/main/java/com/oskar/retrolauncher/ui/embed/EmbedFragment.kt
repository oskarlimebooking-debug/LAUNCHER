package com.oskar.retrolauncher.ui.embed

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.BuildConfig
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.media.AudioFocusPassthrough
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Right-pager page 0. Default content is the [LocationHudView] — a native,
 * always-offline location HUD (range rings + trail + compass + readouts).
 * On system flavor + API 26+ [launchEmbedded] hides the HUD and shows a
 * [TextureView]-backed VirtualDisplay so a real activity can run inside the
 * right pane. On API 23-25 the HUD is the permanent content.
 */
class EmbedFragment : Fragment(R.layout.fragment_embed) {

    private var embedding: EmbeddingContract? = null
    private var textureView: TextureView? = null
    private var hudView: LocationHudView? = null
    private var audioFocusPassthrough: AudioFocusPassthrough? = null

    private val canRealEmbed: Boolean
        get() = BuildConfig.ENABLE_EMBEDDING && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    private val rightPanelWidth: Int get() = 614
    private val rightPanelHeight: Int get() = 600
    private val virtualDpi: Int get() = 213

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        textureView = view.findViewById(R.id.embed_texture)
        hudView = view.findViewById(R.id.embed_hud)

        observeLocation()
        if (canRealEmbed) initEmbedding()
    }

    override fun onResume() {
        super.onResume()
        if (canRealEmbed) bindSurface()
    }

    override fun onPause() {
        super.onPause()
        releaseEmbedding()
    }

    override fun onDestroyView() {
        textureView = null
        hudView = null
        super.onDestroyView()
    }

    // -- HUD ---------------------------------------------------------------

    private fun observeLocation() {
        // Seed with last-known immediately so the HUD doesn't sit on
        // "ACQUIRING GPS" if a fix already arrived before the fragment opened.
        App.location.last.value?.let { hudView?.update(it) }
        viewLifecycleOwner.lifecycleScope.launch {
            App.location.samples.collectLatest { sample ->
                hudView?.update(sample)
            }
        }
    }

    // -- VirtualDisplay embedding (API 26+ system flavor only) -------------

    private fun initEmbedding() {
        try {
            val clazz = Class.forName("com.oskar.retrolauncher.system.Embedding")
            val constructor = clazz.getConstructor(Context::class.java)
            embedding = constructor.newInstance(requireContext()) as EmbeddingContract
        } catch (e: Exception) {
            Timber.w(e, "Failed to load Embedding — keeping HUD view")
        }
    }

    private fun bindSurface() {
        val emb = embedding ?: return
        val tv = textureView ?: return
        startAudioFocusPassthrough()
        tv.setOnTouchListener { _, event ->
            forwardEmbeddedTouch(event)
            true
        }
        tv.post {
            emb.bind(tv, tv.width.let { if (it > 0) it else rightPanelWidth },
                tv.height.let { if (it > 0) it else rightPanelHeight }, virtualDpi)
        }
    }

    private fun releaseEmbedding() {
        audioFocusPassthrough?.stop()
        embedding?.release()
    }

    private fun startAudioFocusPassthrough() {
        if (audioFocusPassthrough != null) return
        val am = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioFocusPassthrough = AudioFocusPassthrough(am, App.media)
        audioFocusPassthrough!!.start()
    }

    // -- Public API (called from HomeFragment) -----------------------------

    /** Send the embedded app to background. No-op when real embedding is unavailable. */
    fun pauseEmbeddedApp() {
        if (!canRealEmbed) return
        launchEmbedded(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        })
    }

    /**
     * Launch an app into the embedded VirtualDisplay. Switches the right-pane
     * away from the HUD and toward the TextureView surface. No-op on API < 26
     * (multi-display launch unsupported) — the HUD stays visible.
     */
    fun launchEmbedded(intent: Intent) {
        if (!canRealEmbed) return
        embedding?.launch(intent)
        textureView?.visibility = View.VISIBLE
        hudView?.visibility = View.GONE
    }

    fun forwardEmbeddedTouch(ev: MotionEvent) {
        val tv = textureView ?: return
        val emb = embedding ?: return
        emb.forwardTouch(ev, tv.width.let { if (it > 0) it else rightPanelWidth },
            tv.height.let { if (it > 0) it else rightPanelHeight },
            rightPanelWidth, rightPanelHeight)
    }
}
