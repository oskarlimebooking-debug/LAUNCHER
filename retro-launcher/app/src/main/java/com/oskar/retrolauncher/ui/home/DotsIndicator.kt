package com.oskar.retrolauncher.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.viewpager2.widget.ViewPager2

/**
 * Tiny page indicator. Three filled dots; the active one uses the accent.
 * No external lib needed.
 */
class DotsIndicator @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
) : View(ctx, attrs) {

    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF8500")
    }
    private val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#444444")
    }
    private val density = resources.displayMetrics.density
    private val activeRadius = 4f * density
    private val inactiveRadius = 3f * density
    private val gap = 8f * density

    private var pageCount = 0
    private var positionOffset = 0f
    private var position = 0

    fun attachTo(pager: ViewPager2) {
        pageCount = pager.adapter?.itemCount ?: 0
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                this@DotsIndicator.position = position
                this@DotsIndicator.positionOffset = positionOffset
                invalidate()
            }
            override fun onPageSelected(position: Int) {
                this@DotsIndicator.position = position
                invalidate()
            }
        })
        pager.adapter?.registerAdapterDataObserver(object : androidx.recyclerview.widget.RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                pageCount = pager.adapter?.itemCount ?: 0
                requestLayout()
            }
        })
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (pageCount <= 0) return
        val totalWidth = pageCount * 2 * activeRadius + (pageCount - 1) * gap
        val cy = height / 2f
        var cx = (width - totalWidth) / 2f + activeRadius
        for (i in 0 until pageCount) {
            val isActive = i == position
            val r = if (isActive) activeRadius else inactiveRadius
            canvas.drawCircle(cx, cy, r, if (isActive) activePaint else inactivePaint)
            cx += 2 * activeRadius + gap
        }
    }
}
