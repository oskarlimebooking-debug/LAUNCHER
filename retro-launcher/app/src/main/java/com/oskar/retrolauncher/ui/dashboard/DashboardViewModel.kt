package com.oskar.retrolauncher.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oskar.retrolauncher.App
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

/**
 * Composes the dashboard's glanceable data: today's driving (from trips),
 * favourites (pinned rail) and now-playing (media). Speed reuses the existing
 * SpeedViewModel; the clock is driven by ClockTicker in the fragment.
 */
class DashboardViewModel : ViewModel() {

    private val now: () -> Long = System::currentTimeMillis

    val today = App.trips.recentTrips
        .map { trips ->
            val (start, end) = dayWindow()
            TodayStats.forDay(trips, start, end)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, TodayDriveStats())

    val favorites = App.appList.rail
    val nowPlaying = App.media.state

    private fun dayWindow(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        return start to (start + DAY_MS)
    }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
