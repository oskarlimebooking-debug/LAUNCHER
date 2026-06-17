package com.oskar.retrolauncher.ui.grid

import android.app.PictureInPictureParams
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.apps.AppEntry
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Right-panel page that shows all installed launchable apps in a grid plus a
 * horizontal rail of pinned apps along the bottom (T1.29 + T1.30).
 *
 * Pin/unpin is exposed via a long-press context menu on grid tiles. Pinning
 * persists through `SettingsStore.pinnedApps`; the rail re-renders whenever
 * either the pinned list or the master app list changes.
 *
 * T1.41 — both lists are drag-to-reorder. The rail uses Android's default
 * long-press drag (no popup conflict). The grid keeps its long-press popup
 * for pin/unpin/app-info and exposes a "Reorder" entry that starts a manual
 * drag, so reorder is always one tap away from a single long-press. Drops
 * commit the new order to `SettingsStore.pinnedApps` / `appOrder` via
 * `AppListRepository.commitPinnedOrder` / `commitGridOrder`, so order
 * survives process death.
 */
class AppGridFragment : Fragment(R.layout.fragment_grid) {

    private companion object {
        const val GOOGLE_MAPS_PKG = "com.google.android.apps.maps"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val grid = view.findViewById<RecyclerView>(R.id.grid)
        val rail = view.findViewById<RecyclerView>(R.id.rail)

        grid.layoutManager = GridLayoutManager(requireContext(), App.settings.gridCols)
        val gridTouchHelper = attachDragReorder(
            recycler = grid,
            directions = DragReorderCallback.GRID_DIRS,
            longPress = false, // grid keeps its long-press popup; drag starts from the popup
            adapterMove = { from, to -> (grid.adapter as? AppGridAdapter)?.moveItem(from, to) },
            onDropped = {
                val current = (grid.adapter as? AppGridAdapter)?.currentEntries() ?: return@attachDragReorder
                App.appList.commitGridOrder(current)
            },
        )
        val gridAdapter = AppGridAdapter(
            onClick = ::launch,
            onLongClick = { entry, anchor ->
                showContextMenu(entry, anchor, gridTouchHelper, grid)
            },
        )
        grid.adapter = gridAdapter

        rail.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        attachDragReorder(
            recycler = rail,
            directions = DragReorderCallback.RAIL_DIRS,
            longPress = true,
            adapterMove = { from, to -> (rail.adapter as? RailAdapter)?.moveItem(from, to) },
            onDropped = {
                val current = (rail.adapter as? RailAdapter)?.currentEntries() ?: return@attachDragReorder
                App.appList.commitPinnedOrder(current.map { it.packageName })
            },
        )
        val railAdapter = RailAdapter(onClick = ::launch)
        rail.adapter = railAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            App.appList.refresh()
            App.appList.all.combine(App.settings.changes("grid_cols")) { list, _ -> list }
                .collect { list ->
                    (grid.layoutManager as? GridLayoutManager)?.spanCount = App.settings.gridCols
                    gridAdapter.submitList(list)
                }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            App.appList.rail.collect { railAdapter.submitList(it) }
        }

        // Rail icon size — when the pref changes, swap to a fresh adapter so the
        // new size is read in onCreateViewHolder. ListAdapter+DiffUtil would
        // otherwise reuse existing ViewHolders with stale layoutParams.
        viewLifecycleOwner.lifecycleScope.launch {
            App.settings.changes(com.oskar.retrolauncher.data.prefs.SettingsStore.KEY_RAIL_ICON_SIZE).collect {
                val fresh = RailAdapter(onClick = ::launch)
                rail.adapter = fresh
                fresh.submitList(App.appList.rail.value)
            }
        }
    }

    private fun attachDragReorder(
        recycler: RecyclerView,
        directions: Int,
        longPress: Boolean,
        adapterMove: (Int, Int) -> Unit,
        onDropped: () -> Unit,
    ): ItemTouchHelper {
        val callback = DragReorderCallback(
            directions = directions,
            longPressDragEnabled = longPress,
            onMoveStep = adapterMove,
            onDropped = onDropped,
            // While a drag is in progress, prevent the host ViewPager2 from
            // intercepting horizontal touches — otherwise the page swipes
            // away under the finger and the drop never registers.
            onDragStateChanged = { active ->
                findAncestorViewPager(recycler)?.isUserInputEnabled = !active
            },
        )
        return ItemTouchHelper(callback).also { it.attachToRecyclerView(recycler) }
    }

    private fun findAncestorViewPager(view: View): ViewPager2? {
        var p = view.parent
        while (p != null) {
            if (p is ViewPager2) return p
            p = p.parent
        }
        return null
    }

    private fun launch(entry: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(entry.componentName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        // PiP requires API 26. The SDK check MUST stay at the call site so the
        // PictureInPictureParams class is never resolved on Android 6 — ART
        // verifies lazily but it verifies the whole method on first call, and
        // pulling the symbol on API 23 would throw VerifyError.
        if (entry.packageName == GOOGLE_MAPS_PKG &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        ) {
            enterPipForMaps()
        }
        runCatching { startActivity(intent) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun enterPipForMaps() {
        runCatching {
            requireActivity().enterPictureInPictureMode(
                PictureInPictureParams.Builder().build()
            )
        }
    }

    private fun showContextMenu(
        entry: AppEntry,
        anchor: View,
        touchHelper: ItemTouchHelper,
        grid: RecyclerView,
    ): Boolean {
        val menu = PopupMenu(requireContext(), anchor)
        val pinned = App.appList.isPinned(entry.packageName)
        if (pinned) {
            menu.menu.add(0, 1, 0, R.string.grid_unpin)
        } else {
            menu.menu.add(0, 0, 0, R.string.grid_pin_to_rail)
        }
        menu.menu.add(0, 3, 0, R.string.grid_reorder)
        menu.menu.add(0, 2, 0, R.string.grid_app_info)
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> { App.appList.pin(entry.packageName); true }
                1 -> { App.appList.unpin(entry.packageName); true }
                2 -> {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(android.net.Uri.parse("package:${entry.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { startActivity(intent) }
                    true
                }
                3 -> {
                    grid.findContainingViewHolder(anchor)?.let { touchHelper.startDrag(it) }
                    true
                }
                else -> false
            }
        }
        menu.show()
        return true
    }
}
