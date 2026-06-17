package com.oskar.retrolauncher.ui.home

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.oskar.retrolauncher.R

/**
 * Tappable labelled page indicator for the home right-panel pager. One evenly
 * weighted text tab per page; the active tab uses the accent colour and bold.
 * Replaces the old dots indicator so pages are reachable by tap, not only swipe.
 *
 * Labels are supplied by the caller (they must match the pager's page order).
 */
class PanelTabStrip @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(ctx, attrs) {

    private var pager: ViewPager2? = null
    private val tabs = mutableListOf<TextView>()
    private val activeColor = ContextCompat.getColor(ctx, R.color.accent)
    private val inactiveColor = ContextCompat.getColor(ctx, R.color.text_tertiary)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
    }

    /** Bind to [pager] and render one tab per entry in [labels]. */
    fun attachTo(pager: ViewPager2, labels: List<String>) {
        this.pager = pager
        buildTabs(labels)
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = highlight(position)
        })
        highlight(pager.currentItem)
    }

    private fun buildTabs(labels: List<String>) {
        removeAllViews()
        tabs.clear()
        labels.forEachIndexed { index, label ->
            val tab = TextView(context).apply {
                text = label.uppercase()
                gravity = Gravity.CENTER
                textSize = 12f
                typeface = Typeface.MONOSPACE
                setTextColor(inactiveColor)
                setPadding(0, paddingForHeight(), 0, paddingForHeight())
                setOnClickListener { this@PanelTabStrip.pager?.setCurrentItem(index, true) }
            }
            addView(tab, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            tabs += tab
        }
    }

    private fun highlight(position: Int) {
        tabs.forEachIndexed { index, tab ->
            val active = index == position
            tab.setTextColor(if (active) activeColor else inactiveColor)
            tab.setTypeface(Typeface.MONOSPACE, if (active) Typeface.BOLD else Typeface.NORMAL)
            tab.alpha = if (active) 1f else 0.75f
        }
    }

    private fun paddingForHeight(): Int = (6f * resources.displayMetrics.density).toInt()
}
