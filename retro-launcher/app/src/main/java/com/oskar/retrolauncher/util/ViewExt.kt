package com.oskar.retrolauncher.util

import android.content.res.Resources
import android.util.TypedValue
import android.view.View

fun Float.dp(res: Resources): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, res.displayMetrics)

fun Int.dp(res: Resources): Int = this.toFloat().dp(res).toInt()

fun Float.sp(res: Resources): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this, res.displayMetrics)

/** Hide the view; idempotent. */
fun View.gone() {
    visibility = View.GONE
}

/** Show the view; idempotent. */
fun View.visible() {
    visibility = View.VISIBLE
}

/**
 * Smoothly fade the view in or out. Fade-in pre-sets `alpha = 0` if the view
 * was hidden so the animation is visible. Fade-out flips visibility to GONE
 * in `withEndAction` so the view continues to occupy layout space until the
 * animation completes.
 */
fun View.fade(toVisible: Boolean, durationMs: Long = 200L) {
    if (toVisible) {
        if (visibility != View.VISIBLE) {
            alpha = 0f
            visibility = View.VISIBLE
        }
        animate().alpha(1f).setDuration(durationMs).start()
    } else {
        animate()
            .alpha(0f)
            .setDuration(durationMs)
            .withEndAction { visibility = View.GONE }
            .start()
    }
}
