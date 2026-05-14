package com.oskar.retrolauncher.system

import android.content.Context
import android.content.Intent
import android.app.ActivityOptions
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.graphics.SurfaceTexture
import android.view.InputDevice
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import com.oskar.retrolauncher.ui.embed.EmbeddingContract

/**
 * Manages a [VirtualDisplay] that renders into a [TextureView] so embedded
 * apps appear inside the launcher's right panel.
 *
 * Compiled only under the `system` flavor. Instantiated via reflection by
 * [com.oskar.retrolauncher.ui.embed.EmbedFragment] when
 * [BuildConfig.ENABLE_EMBEDDING] is true.
 */
class Embedding(private val ctx: Context) : EmbeddingContract {

    private var virtualDisplay: VirtualDisplay? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null
    private val displayManager: DisplayManager =
        ctx.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    private val virtualDisplayFlags: Int
        get() = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
            (1 shl 6) // VIRTUAL_DISPLAY_FLAG_PRESENTATION

    // -- EmbeddingContract -------------------------------------------------

    override fun bind(textureView: TextureView, w: Int, h: Int, dpi: Int) {
        release()
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                st: SurfaceTexture, width: Int, height: Int,
            ) {
                surfaceTexture = st
                st.setDefaultBufferSize(w, h)
                surface = Surface(st)
                virtualDisplay = displayManager.createVirtualDisplay(
                    "retro-embed",
                    w, h, dpi, surface, virtualDisplayFlags,
                )
            }

            override fun onSurfaceTextureSizeChanged(
                st: SurfaceTexture, width: Int, height: Int,
            ) {
                virtualDisplay?.resize(width, height, dpi)
            }

            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                release()
                return true
            }

            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
        }
    }

    override fun launch(intent: Intent) {
        val displayId = virtualDisplay?.display?.displayId ?: return
        val opts = ActivityOptions.makeBasic()
            .setLaunchDisplayId(displayId)
        ctx.startActivity(
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            opts.toBundle(),
        )
    }

    override fun release() {
        virtualDisplay?.release()
        virtualDisplay = null
        surface?.release()
        surface = null
        surfaceTexture?.release()
        surfaceTexture = null
    }

    override fun forwardTouch(
        ev: MotionEvent, viewW: Int, viewH: Int, dispW: Int, dispH: Int,
    ) {
        val safeW = viewW.coerceAtLeast(1)
        val safeH = viewH.coerceAtLeast(1)
        val displayId = virtualDisplay?.display?.displayId ?: 0

        val mapped = if (ev.pointerCount <= 1) {
            val x = ev.x * dispW / safeW
            val y = ev.y * dispH / safeH
            MotionEvent.obtain(
                ev.downTime, ev.eventTime, ev.action, x, y, ev.metaState,
            )
        } else {
            val pointerCount = ev.pointerCount
            val props = Array(pointerCount) { i ->
                MotionEvent.PointerProperties().also {
                    it.id = ev.getPointerId(i)
                    it.toolType = ev.getToolType(i)
                }
            }
            val coords = Array(pointerCount) { i ->
                MotionEvent.PointerCoords().also {
                    it.x = ev.getX(i) * dispW / safeW
                    it.y = ev.getY(i) * dispH / safeH
                    it.pressure = ev.getPressure(i)
                    it.size = ev.getSize(i)
                }
            }
            MotionEvent.obtain(
                ev.downTime, ev.eventTime, ev.action,
                pointerCount, props, coords,
                ev.metaState, ev.buttonState,
                ev.xPrecision, ev.yPrecision,
                ev.deviceId, ev.edgeFlags,
                InputDevice.SOURCE_TOUCHSCREEN, ev.flags,
            )
        }
        mapped.source = InputDevice.SOURCE_TOUCHSCREEN
        HiddenApi.injectInputEvent(mapped, displayId)
        mapped.recycle()
    }
}
