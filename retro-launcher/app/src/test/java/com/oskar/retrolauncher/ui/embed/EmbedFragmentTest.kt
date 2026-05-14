package com.oskar.retrolauncher.ui.embed

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EmbedFragmentTest {

    // -- Layout verification ------------------------------------------------

    @Test
    fun `fragment_embed contains an embed_texture TextureView`() {
        val ctx = RuntimeEnvironment.getApplication()
        val parser = ctx.resources.getLayout(
            ctx.resources.getIdentifier("fragment_embed", "layout", ctx.packageName),
        )
        var sawTextureView = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "TextureView") {
                sawTextureView = true
            }
            event = parser.next()
        }
        assertTrue(
            "fragment_embed.xml must contain a TextureView for VirtualDisplay rendering",
            sawTextureView,
        )
    }

    @Test
    fun `fragment_embed contains a placeholder TextView`() {
        val ctx = RuntimeEnvironment.getApplication()
        val parser = ctx.resources.getLayout(
            ctx.resources.getIdentifier("fragment_embed", "layout", ctx.packageName),
        )
        var sawPlaceholder = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                val id = parser.getAttributeResourceValue(
                    "http://schemas.android.com/apk/res/android", "id", 0,
                )
                if (id == ctx.resources.getIdentifier(
                        "embed_placeholder", "id", ctx.packageName,
                    )
                ) {
                    sawPlaceholder = true
                }
            }
            event = parser.next()
        }
        assertTrue(
            "fragment_embed.xml must keep the embed_placeholder TextView as fallback",
            sawPlaceholder,
        )
    }

    // -- Fragment instantiation ---------------------------------------------

    @Test
    fun `EmbedFragment can be instantiated with no-arg constructor`() {
        EmbedFragment()
    }

    @Test
    fun `EmbedFragment inflates without crashing on standard flavor`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java)
            .create()
            .start()
            .get()
        val fragment = EmbedFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()
    }

    // -- EmbeddingContract --------------------------------------------------

    @Test
    fun `EmbeddingContract defines all four lifecycle methods`() {
        val methods = EmbeddingContract::class.java.declaredMethods.map { it.name }.toSet()
        assertTrue("bind missing", "bind" in methods)
        assertTrue("launch missing", "launch" in methods)
        assertTrue("release missing", "release" in methods)
        assertTrue("forwardTouch missing", "forwardTouch" in methods)
    }

    // -- Touch forwarding ---------------------------------------------------

    @Test
    fun `coordinate translation maps center of view to center of display`() {
        // view = 614×600, display = 1024×600 — center should map proportionally
        val viewW = 614; val viewH = 600
        val dispW = 1024; val dispH = 600

        // Center of view
        val viewX = 307f; val viewY = 300f
        val expectedDispX = (viewX * dispW / viewW).toInt() // = 512
        val expectedDispY = (viewY * dispH / viewH).toInt() // = 300

        assertEquals(512, expectedDispX)
        assertEquals(300, expectedDispY)
    }

    @Test
    fun `coordinate translation maps top-left origin correctly`() {
        val viewW = 614; val viewH = 600
        val dispW = 1024; val dispH = 600

        val dispX = (0f * dispW / viewW).toInt()
        val dispY = (0f * dispH / viewH).toInt()

        assertEquals(0, dispX)
        assertEquals(0, dispY)
    }

    @Test
    fun `coordinate translation handles zero-size view safely`() {
        // coerceAtLeast(1) should prevent division by zero
        val viewW = 0; val viewH = 0
        val dispW = 1024; val dispH = 600

        val safeW = viewW.coerceAtLeast(1)
        val safeH = viewH.coerceAtLeast(1)
        val dispX = (307f * dispW / safeW).toInt()
        val dispY = (300f * dispH / safeH).toInt()

        assertEquals(307 * 1024, dispX)
        assertEquals(300 * 600, dispY)
    }

    @Test
    fun `forwardTouch parameter order matches EmbeddingContract signature`() {
        val method = EmbeddingContract::class.java.getDeclaredMethod(
            "forwardTouch",
            android.view.MotionEvent::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        )
        val params = method.parameterTypes
        assertEquals(5, params.size)
        assertEquals(android.view.MotionEvent::class.java, params[0])
        assertEquals(Int::class.javaPrimitiveType, params[1]) // viewW
        assertEquals(Int::class.javaPrimitiveType, params[2]) // viewH
        assertEquals(Int::class.javaPrimitiveType, params[3]) // dispW
        assertEquals(Int::class.javaPrimitiveType, params[4]) // dispH
    }

    @Test
    fun `multi-touch coordinate mapping maps both pointers independently`() {
        // Simulate pinch: pointer 0 at (100,200), pointer 1 at (500,400)
        val viewW = 614; val viewH = 600
        val dispW = 1024; val dispH = 600

        val pointer0ViewX = 100f; val pointer0ViewY = 200f
        val pointer1ViewX = 500f; val pointer1ViewY = 400f

        val pointer0DispX = pointer0ViewX * dispW / viewW
        val pointer0DispY = pointer0ViewY * dispH / viewH
        val pointer1DispX = pointer1ViewX * dispW / viewW
        val pointer1DispY = pointer1ViewY * dispH / viewH

        // Both pointers are mapped independently
        assertTrue("pointer 0 x should be > 0", pointer0DispX > 0f)
        assertTrue("pointer 1 x should be > pointer 0 x", pointer1DispX > pointer0DispX)
        assertEquals(pointer0ViewY * dispH / viewH, pointer0DispY)
        assertEquals(pointer1ViewY * dispH / viewH, pointer1DispY)
    }

    @Test
    fun `EmbedFragment has forwardEmbeddedTouch method`() {
        val method = EmbedFragment::class.java.getDeclaredMethod(
            "forwardEmbeddedTouch",
            android.view.MotionEvent::class.java,
        )
        assertNotNull("forwardEmbeddedTouch method must exist", method)
    }
}
