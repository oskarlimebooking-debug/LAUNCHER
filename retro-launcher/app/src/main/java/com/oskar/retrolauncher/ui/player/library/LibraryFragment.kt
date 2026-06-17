package com.oskar.retrolauncher.ui.player.library

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.oskar.retrolauncher.R

/** Songs / Albums / Artists tabs over the on-device library. */
class LibraryFragment : Fragment(R.layout.fragment_library) {

    private val vm: LibraryViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm.ensureScanned()
        val pager = view.findViewById<ViewPager2>(R.id.library_pager)
        pager.adapter = SectionsAdapter(this)
        val tabs = view.findViewById<TabLayout>(R.id.library_tabs)
        TabLayoutMediator(tabs, pager) { tab, position ->
            tab.text = getString(TITLES[position])
        }.attach()
    }

    private class SectionsAdapter(host: Fragment) : FragmentStateAdapter(host) {
        override fun getItemCount() = TITLES.size
        override fun createFragment(position: Int): Fragment = when (position) {
            PLAYLISTS -> PlaylistsFragment()
            LIKED -> LikedFragment()
            else -> MusicBrowseFragment.create(SECTIONS[position])
        }
    }

    private companion object {
        const val PLAYLISTS = 3
        const val LIKED = 4
        val SECTIONS = listOf(
            MusicBrowseFragment.SECTION_SONGS,
            MusicBrowseFragment.SECTION_ALBUMS,
            MusicBrowseFragment.SECTION_ARTISTS,
        )
        val TITLES = listOf(
            R.string.music_tab_songs,
            R.string.music_tab_albums,
            R.string.music_tab_artists,
            R.string.music_tab_playlists,
            R.string.music_tab_liked,
        )
    }
}
