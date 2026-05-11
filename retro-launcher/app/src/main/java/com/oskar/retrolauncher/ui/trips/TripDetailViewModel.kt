package com.oskar.retrolauncher.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oskar.retrolauncher.data.trip.GeoBoundingBox
import com.oskar.retrolauncher.data.trip.NominatimAddressCache
import com.oskar.retrolauncher.data.trip.TripEntity
import com.oskar.retrolauncher.data.trip.TripPoint
import com.oskar.retrolauncher.data.trip.TripRepository
import com.oskar.retrolauncher.data.trip.TripStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the trip-detail screen.
 *
 * `Loading` while the points list is being read from Room.
 * `Empty` if the trip has no points (rare — recorder filters those out).
 * `Loaded` carries everything the fragment renders: stats panel + map overlay
 *   inputs (points + bounding box) + start/end address strings (already with
 *   Nominatim cache fallback applied).
 */
sealed class TripDetailUiState {
    object Loading : TripDetailUiState()
    object Empty : TripDetailUiState()
    data class Loaded(
        val trip: TripEntity,
        val points: List<TripPoint>,
        val stats: TripStats,
        val bbox: GeoBoundingBox?,
        val startAddress: String,
        val endAddress: String,
        val aggregatesMatch: Boolean,
    ) : TripDetailUiState()
}

class TripDetailViewModel(
    private val repo: TripRepository,
    private val addrCache: NominatimAddressCache,
    private val tripId: Long,
    private val entity: TripEntity,
) : ViewModel() {

    private val _state = MutableStateFlow<TripDetailUiState>(TripDetailUiState.Loading)
    val state: StateFlow<TripDetailUiState> = _state

    /**
     * Loads the trip points off the main thread, computes stats, then publishes
     * a `Loaded` state. Idempotent — calling twice just re-publishes the same
     * snapshot.
     */
    fun load() {
        // viewModelScope is Main + SupervisorJob; Room's suspending @Query hops to its
        // own executor internally so we don't block the UI. Tests can swap the main
        // dispatcher via Dispatchers.setMain to observe the final state synchronously.
        viewModelScope.launch {
            val points = repo.pointsFor(tripId)
            if (points.isEmpty()) {
                _state.value = TripDetailUiState.Empty
                return@launch
            }
            val stats = TripStats.fromPoints(points)
            val bbox = GeoBoundingBox.fromPoints(points)
            val first = points.first()
            val last = points.last()
            val startAddress = addrCache.labelOrFallback(first.lat, first.lon, entity.startLabel)
            val endAddress = addrCache.labelOrFallback(last.lat, last.lon, entity.endLabel)
            _state.value = TripDetailUiState.Loaded(
                trip = entity,
                points = points,
                stats = stats,
                bbox = bbox,
                startAddress = startAddress,
                endAddress = endAddress,
                aggregatesMatch = TripStats.crossCheck(entity, stats, tolerancePct = 0.05),
            )
        }
    }

    /** Factory wires the per-trip `tripId` and entity through `ViewModelProvider`. */
    class Factory(
        private val repo: TripRepository,
        private val addrCache: NominatimAddressCache,
        private val tripId: Long,
        private val entity: TripEntity,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TripDetailViewModel(repo, addrCache, tripId, entity) as T
    }
}
