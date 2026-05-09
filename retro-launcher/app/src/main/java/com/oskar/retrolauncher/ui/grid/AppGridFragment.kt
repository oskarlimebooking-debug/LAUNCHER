package com.oskar.retrolauncher.ui.grid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.apps.AppEntry
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AppGridFragment : Fragment(R.layout.fragment_grid) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recycler = view.findViewById<RecyclerView>(R.id.grid)
        recycler.layoutManager = GridLayoutManager(requireContext(), App.settings.gridCols)
        val adapter = AppGridAdapter(
            onClick = ::launch,
            onLongClick = ::showContextMenu,
        )
        recycler.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            App.appList.refresh()
            App.appList.all.combine(App.settings.changes("grid_cols")) { list, _ -> list }
                .collect { list ->
                    (recycler.layoutManager as? GridLayoutManager)?.spanCount = App.settings.gridCols
                    adapter.submitList(list)
                }
        }
    }

    private fun launch(entry: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(entry.componentName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        runCatching { startActivity(intent) }
    }

    private fun showContextMenu(entry: AppEntry, anchor: View): Boolean {
        val menu = PopupMenu(requireContext(), anchor)
        val pinned = App.appList.isPinned(entry.componentName)
        if (pinned) {
            menu.menu.add(0, 1, 0, R.string.grid_unpin)
        } else {
            menu.menu.add(0, 0, 0, R.string.grid_pin_to_rail)
        }
        menu.menu.add(0, 2, 0, R.string.grid_app_info)
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> { App.appList.pin(entry.componentName); true }
                1 -> { App.appList.unpin(entry.componentName); true }
                2 -> {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(android.net.Uri.parse("package:${entry.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { startActivity(intent) }
                    true
                }
                else -> false
            }
        }
        menu.show()
        return true
    }
}
