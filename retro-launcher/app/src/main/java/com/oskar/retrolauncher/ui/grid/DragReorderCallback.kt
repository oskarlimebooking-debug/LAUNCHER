package com.oskar.retrolauncher.ui.grid

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

/**
 * T1.41 — long-press drag-to-reorder for the grid and the rail.
 *
 * The callback is RecyclerView-agnostic: it forwards the index swap to
 * [onMoveStep] during drag and the final list snapshot to [onDropped] when
 * the gesture finishes. The fragment owns persistence so the callback stays
 * free of `App`/repository wiring.
 *
 * Bounds safety (AC5): `onMove` only fires while ItemTouchHelper holds a
 * valid target index, and the indices are re-checked inside the adapter's
 * `moveItem`. Dragging outside the RecyclerView is a no-op and cannot
 * crash.
 *
 * Visual lift + haptic (AC1): when ItemTouchHelper enters ACTION_STATE_DRAG
 * we perform [HapticFeedbackConstants.LONG_PRESS] and bump the row's
 * elevation/scale; both reset in `clearView`. `isLongPressDragEnabled` is
 * left as a constructor flag so the grid (which already long-presses for a
 * popup) can opt out and start drag manually via [ItemTouchHelper.startDrag].
 */
class DragReorderCallback(
    private val directions: Int,
    private val longPressDragEnabled: Boolean,
    private val onMoveStep: (from: Int, to: Int) -> Unit,
    private val onDropped: () -> Unit,
) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
    ): Int = makeMovementFlags(directions, 0)

    override fun isLongPressDragEnabled(): Boolean = longPressDragEnabled

    override fun isItemViewSwipeEnabled(): Boolean = false

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder,
    ): Boolean {
        val from = viewHolder.bindingAdapterPosition
        val to = target.bindingAdapterPosition
        if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
        onMoveStep(from, to)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
            applyLift(viewHolder.itemView)
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        clearLift(viewHolder.itemView)
        onDropped()
    }

    private fun applyLift(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        view.animate().cancel()
        view.animate()
            .scaleX(LIFT_SCALE)
            .scaleY(LIFT_SCALE)
            .setDuration(LIFT_DURATION_MS)
            .start()
        view.translationZ = LIFT_TRANSLATION_Z
        view.alpha = LIFT_ALPHA
    }

    private fun clearLift(view: View) {
        view.animate().cancel()
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(LIFT_DURATION_MS)
            .start()
        view.translationZ = 0f
        view.alpha = 1f
    }

    companion object {
        const val GRID_DIRS =
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        const val RAIL_DIRS = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT

        private const val LIFT_SCALE = 1.08f
        private const val LIFT_ALPHA = 0.92f
        private const val LIFT_TRANSLATION_Z = 12f
        private const val LIFT_DURATION_MS = 120L
    }
}
