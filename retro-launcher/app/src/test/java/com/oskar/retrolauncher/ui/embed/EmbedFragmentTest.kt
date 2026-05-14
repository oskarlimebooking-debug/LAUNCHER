package com.oskar.retrolauncher.ui.embed

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
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
}
