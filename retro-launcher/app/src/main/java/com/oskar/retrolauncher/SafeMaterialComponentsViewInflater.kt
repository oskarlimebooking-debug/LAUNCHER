package com.oskar.retrolauncher

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.theme.MaterialComponentsViewInflater
import timber.log.Timber

/**
 * Reason: Material Components 1.11.0's [MaterialComponentsViewInflater] hits
 * an [ArrayIndexOutOfBoundsException] inside `StringBlock.get` on API 23-25
 * head units when reading styled-text attrs for the Material widget
 * variants. AppCompat instantiates this class from the theme's
 * `viewInflaterClass` attribute *before* `LayoutInflater.setFactory2()` is
 * locked, so substitution is guaranteed (an after-the-fact factory wrap
 * cannot work on API 23 — see commit history for the failed attempt).
 *
 * Each Material create*View override falls back to the plain AppCompat
 * widget on AIOOBE. Material-only theming is lost on the few crashing
 * views; the alternative is a launch-time crash.
 */
internal open class SafeMaterialComponentsViewInflater : MaterialComponentsViewInflater() {

    public override fun createTextView(context: Context, attrs: AttributeSet): AppCompatTextView = try {
        materialCreateTextView(context, attrs)
    } catch (e: ArrayIndexOutOfBoundsException) {
        Timber.w(e, "SafeMaterialComponentsViewInflater fallback for TextView")
        AppCompatTextView(context, attrs)
    }

    public override fun createButton(context: Context, attrs: AttributeSet): AppCompatButton = try {
        super.createButton(context, attrs)
    } catch (e: ArrayIndexOutOfBoundsException) {
        Timber.w(e, "SafeMaterialComponentsViewInflater fallback for Button")
        AppCompatButton(context, attrs)
    }

    public override fun createCheckBox(context: Context, attrs: AttributeSet): AppCompatCheckBox = try {
        super.createCheckBox(context, attrs)
    } catch (e: ArrayIndexOutOfBoundsException) {
        Timber.w(e, "SafeMaterialComponentsViewInflater fallback for CheckBox")
        AppCompatCheckBox(context, attrs)
    }

    public override fun createRadioButton(context: Context, attrs: AttributeSet): AppCompatRadioButton = try {
        super.createRadioButton(context, attrs)
    } catch (e: ArrayIndexOutOfBoundsException) {
        Timber.w(e, "SafeMaterialComponentsViewInflater fallback for RadioButton")
        AppCompatRadioButton(context, attrs)
    }

    public override fun createAutoCompleteTextView(
        context: Context, attrs: AttributeSet?,
    ): AppCompatAutoCompleteTextView = try {
        super.createAutoCompleteTextView(context, attrs)
    } catch (e: ArrayIndexOutOfBoundsException) {
        Timber.w(e, "SafeMaterialComponentsViewInflater fallback for AutoCompleteTextView")
        AppCompatAutoCompleteTextView(context, attrs)
    }

    /** Test seam: the Material super call for TextView. Overridden in tests to simulate the API 23 crash. */
    @VisibleForTesting
    protected open fun materialCreateTextView(
        context: Context, attrs: AttributeSet,
    ): AppCompatTextView = super.createTextView(context, attrs)
}
