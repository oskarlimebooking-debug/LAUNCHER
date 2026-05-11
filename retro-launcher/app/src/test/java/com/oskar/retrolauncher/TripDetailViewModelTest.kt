package com.oskar.retrolauncher

import com.oskar.retrolauncher.data.trip.NominatimAddressCache
import com.oskar.retrolauncher.data.trip.TripDao
import com.oskar.retrolauncher.data.trip.TripEntity
import com.oskar.retrolauncher.data.trip.TripPoint
import com.oskar.retrolauncher.data.trip.TripRepository
import com.oskar.retrolauncher.ui.trips.TripDetailUiState
import com.oskar.retrolauncher.ui.trips.TripDetailViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class TripDetailViewModelTest {

    private lateinit var cache: NominatimAddressCache
    private lateinit var dao: TripDao
    private lateinit var repo: TripRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val ctx = RuntimeEnvironment.getApplication()
        ctx.getSharedPreferences("nominatim_addr", android.content.Context.MODE_PRIVATE)
            .edit().clear().apply()
        cache = NominatimAddressCache(ctx)
        dao = mockk(relaxed = true)
        repo = TripRepository(dao = dao, activeTripFlow = MutableStateFlow(null))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun trip(id: Long = 1L) = TripEntity(
        id = id, startMs = 0L, endMs = 30_000L,
        distanceM = 333.96, avgSpeedMs = 11.13, maxSpeedMs = 12.0,
        startLabel = null, endLabel = null,
    )

    private fun pts(id: Long = 1L) = listOf(
        TripPoint(tripId = id, tsMs = 0L, lat = 60.1, lon = 24.9, speedMs = 0f),
        TripPoint(tripId = id, tsMs = 10_000L, lat = 60.1001, lon = 24.9001, speedMs = 11f),
        TripPoint(tripId = id, tsMs = 20_000L, lat = 60.1002, lon = 24.9002, speedMs = 12f),
        TripPoint(tripId = id, tsMs = 30_000L, lat = 60.1003, lon = 24.9003, speedMs = 10f),
    )

    @Test
    fun `loads trip with points and emits Loaded state`() = runTest {
        coEvery { dao.points(1L) } returns pts()
        every { dao.byRange(any(), any()) } returns kotlinx.coroutines.flow.flowOf(listOf(trip()))

        val vm = TripDetailViewModel(repo, cache, tripId = 1L, entity = trip())
        vm.load()

        val state = vm.state.value
        assertTrue("expected Loaded, got $state", state is TripDetailUiState.Loaded)
        val loaded = state as TripDetailUiState.Loaded
        assertEquals(1L, loaded.trip.id)
        assertEquals(4, loaded.points.size)
        assertTrue("distance > 0", loaded.stats.distanceM > 0.0)
    }

    @Test
    fun `cold cache yields lat lon fallback addresses`() = runTest {
        coEvery { dao.points(1L) } returns pts()
        val vm = TripDetailViewModel(repo, cache, tripId = 1L, entity = trip())
        vm.load()
        val loaded = vm.state.value as TripDetailUiState.Loaded
        // First point is 60.1, 24.9 → "60.1000, 24.9000"
        assertEquals("60.1000, 24.9000", loaded.startAddress)
        // Last point is 60.1003, 24.9003 → "60.1003, 24.9003"
        assertEquals("60.1003, 24.9003", loaded.endAddress)
    }

    @Test
    fun `warm cache yields city names`() = runTest {
        cache.put(60.1, 24.9, "Helsinki")
        cache.put(60.1003, 24.9003, "Espoo")
        coEvery { dao.points(1L) } returns pts()
        val vm = TripDetailViewModel(repo, cache, tripId = 1L, entity = trip())
        vm.load()
        val loaded = vm.state.value as TripDetailUiState.Loaded
        assertEquals("Helsinki", loaded.startAddress)
        assertEquals("Espoo", loaded.endAddress)
    }

    @Test
    fun `explicit entity labels override cache`() = runTest {
        cache.put(60.1, 24.9, "Helsinki")
        coEvery { dao.points(1L) } returns pts()
        val entityWithLabels = trip().copy(startLabel = "Home", endLabel = "Work")
        val vm = TripDetailViewModel(repo, cache, tripId = 1L, entity = entityWithLabels)
        vm.load()
        val loaded = vm.state.value as TripDetailUiState.Loaded
        assertEquals("Home", loaded.startAddress)
        assertEquals("Work", loaded.endAddress)
    }

    @Test
    fun `empty points yields Empty state`() = runTest {
        coEvery { dao.points(1L) } returns emptyList()
        val vm = TripDetailViewModel(repo, cache, tripId = 1L, entity = trip())
        vm.load()
        assertTrue(vm.state.value is TripDetailUiState.Empty)
    }

    @Test
    fun `bounding box covers all points`() = runTest {
        coEvery { dao.points(1L) } returns pts()
        val vm = TripDetailViewModel(repo, cache, tripId = 1L, entity = trip())
        vm.load()
        val loaded = vm.state.value as TripDetailUiState.Loaded
        val bbox = loaded.bbox
        assertNotNull(bbox)
        bbox!!
        assertTrue(bbox.minLat <= 60.1)
        assertTrue(bbox.maxLat >= 60.1003)
        assertTrue(bbox.minLon <= 24.9)
        assertTrue(bbox.maxLon >= 24.9003)
    }
}
