package com.oskar.retrolauncher

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View

/**
 * Wraps a [LayoutInflater.Factory2] and catches the [ArrayIndexOutOfBoundsException]
 * that Material Components' [MaterialComponentsViewInflater] throws on API 23-25
 * when inflating TextViews. Falls back to the standard Android [View] creation.
 */
internal class SafeInflaterFactory(
    private val delegate: LayoutInflater.Factory2,
) : LayoutInflater.Factory2 {

    override fun onCreateView(
        parent: View?, name: String, context: Context, attrs: AttributeSet,
    ): View? {
        return try {
            delegate.onCreateView(parent, name, context, attrs)
        } catch (_: ArrayIndexOutOfBoundsException) {
            null // fall through to default Android view creation
        }
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return try {
            delegate.onCreateView(name, context, attrs)
        } catch (_: ArrayIndexOutOfBoundsException) {
            null
        }
    }
}
