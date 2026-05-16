package com.oskar.retrolauncher

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23])
class SafeInflaterFactoryTest {

    /** A factory that throws ArrayIndexOutOfBoundsException to simulate M3 bug. */
    private class BuggyFactory : LayoutInflater.Factory2 {
        var onCreateViewCalled = false
        override fun onCreateView(
            parent: View?, name: String, context: Context, attrs: AttributeSet,
        ): View? {
            onCreateViewCalled = true
            throw ArrayIndexOutOfBoundsException(1264)
        }

        override fun onCreateView(
            name: String, context: Context, attrs: AttributeSet,
        ): View? {
            onCreateViewCalled = true
            throw ArrayIndexOutOfBoundsException(1264)
        }
    }

    private val fakeAttrs: AttributeSet
        get() = org.robolectric.Robolectric.buildAttributeSet().build()

    @Test
    fun `SafeInflaterFactory catches ArrayIndexOutOfBounds and returns null`() {
        val buggy = BuggyFactory()
        val safe = SafeInflaterFactory(buggy)
        val ctx = RuntimeEnvironment.getApplication()

        val result = safe.onCreateView("TextView", ctx, fakeAttrs)

        assertNull("should return null so default view creation is used", result)
        assertTrue("delegate should have been called", buggy.onCreateViewCalled)
    }

    @Test
    fun `SafeInflaterFactory passes through successful creates`() {
        val ctx = RuntimeEnvironment.getApplication()
        val successFactory = object : LayoutInflater.Factory2 {
            override fun onCreateView(
                parent: View?, name: String, context: Context, attrs: AttributeSet,
            ): View = TextView(context)
            override fun onCreateView(
                name: String, context: Context, attrs: AttributeSet,
            ): View = TextView(context)
        }
        val safe = SafeInflaterFactory(successFactory)

        val view = safe.onCreateView(null, "TextView", ctx, fakeAttrs)
        assertTrue("should pass through to real factory on success", view is TextView)
    }

    @Test
    fun `SafeInflaterFactory delegates other exceptions`() {
        val exploding = object : LayoutInflater.Factory2 {
            override fun onCreateView(
                parent: View?, name: String, context: Context, attrs: AttributeSet,
            ): View? = throw RuntimeException("expected")
            override fun onCreateView(
                name: String, context: Context, attrs: AttributeSet,
            ): View? = throw RuntimeException("expected")
        }
        val safe = SafeInflaterFactory(exploding)

        try {
            safe.onCreateView("TextView", RuntimeEnvironment.getApplication(), fakeAttrs)
        } catch (e: RuntimeException) {
            assertEquals("expected", e.message)
        }
    }
}
