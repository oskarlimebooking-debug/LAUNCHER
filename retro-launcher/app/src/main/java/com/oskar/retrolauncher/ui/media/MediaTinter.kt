package com.oskar.retrolauncher.ui.media

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting

/**
 * Owns the media-tile background gradient and animates colour transitions
 * over [ANIMATION_DURATION_MS] using [ArgbEvaluator] (T1.18 AC3).
 *
 * The drawable is allocated once and mutated in place so the per-frame
 * update path doesn't allocate (Cortex-A7 budget — AC5).
 */
class MediaTinter(
    @ColorInt private val fallback: Int,
    cornerRadiusPx: Float,
    @ColorInt private val gradientEnd: Int = GRADIENT_END,
    private val animationDurationMs: Long = ANIMATION_DURATION_MS,
) {

    val drawable: GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(fallback, gradientEnd),
    ).apply { cornerRadius = cornerRadiusPx }

    private var currentColor: Int = fallback
    private var targetColor: Int = fallback
    private var animator: ValueAnimator? = null

    val color: Int get() = currentColor

    @VisibleForTesting
    val animatorForTest: ValueAnimator? get() = animator

    fun animateTo(@ColorInt target: Int) {
        if (target == targetColor) return
        targetColor = target
        animator?.cancel()
        animator = ValueAnimator.ofObject(ArgbEvaluator(), currentColor, target).apply {
            duration = animationDurationMs
            addUpdateListener { va ->
                val c = va.animatedValue as Int
                currentColor = c
                drawable.colors = intArrayOf(c, gradientEnd)
            }
            start()
        }
    }

    fun cancel() {
        animator?.cancel()
        animator = null
    }

    companion object {
        const val ANIMATION_DURATION_MS = 400L
        private const val GRADIENT_END = 0xFF111111.toInt()
    }
}
