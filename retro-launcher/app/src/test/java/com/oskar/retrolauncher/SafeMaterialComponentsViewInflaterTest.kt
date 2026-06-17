package com.oskar.retrolauncher

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23])
class SafeMaterialComponentsViewInflaterTest {

    private val ctx: Context get() = RuntimeEnvironment.getApplication()
    private val fakeAttrs: AttributeSet get() = Robolectric.buildAttributeSet().build()

    @Test
    fun `createTextView returns AppCompatTextView fallback when Material super throws AIOOBE`() {
        val crashing = object : SafeMaterialComponentsViewInflater() {
            override fun materialCreateTextView(
                context: Context, attrs: AttributeSet,
            ): AppCompatTextView {
                throw ArrayIndexOutOfBoundsException(1264) // simulate API 23 StringBlock crash
            }
        }

        val view = crashing.createTextView(ctx, fakeAttrs)

        assertNotNull("fallback must return non-null instead of propagating AIOOBE", view)
        assertEquals(
            "fallback must be plain AppCompatTextView, not a MaterialTextView subclass",
            AppCompatTextView::class.java,
            view::class.java,
        )
    }

    @Test
    fun `createTextView propagates non-AIOOBE exceptions`() {
        val exploding = object : SafeMaterialComponentsViewInflater() {
            override fun materialCreateTextView(
                context: Context, attrs: AttributeSet,
            ): AppCompatTextView {
                throw IllegalStateException("must propagate")
            }
        }

        try {
            exploding.createTextView(ctx, fakeAttrs)
            fail("non-AIOOBE exception should propagate, not be swallowed")
        } catch (e: IllegalStateException) {
            assertEquals("must propagate", e.message)
        }
    }
}
