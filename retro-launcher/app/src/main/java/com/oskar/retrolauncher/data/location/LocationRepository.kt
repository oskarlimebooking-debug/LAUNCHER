package com.oskar.retrolauncher.data.location

import android.location.Location
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class LocationRepository {

    private val _samples = MutableSharedFlow<LocationSample>(
        replay = 1,
        extraBufferCapacity = 64,
    )
    val samples: SharedFlow<LocationSample> = _samples

    private val _last = MutableStateFlow<LocationSample?>(null)
    val last: StateFlow<LocationSample?> = _last

    fun push(s: LocationSample) {
        _last.value = s
        _samples.tryEmit(s)
    }

    fun lastKnown(): Location? = _last.value?.let {
        Location("repo").apply {
            latitude = it.lat
            longitude = it.lon
            time = it.tsMs
            speed = it.speedMs
            accuracy = it.accuracy
            bearing = it.bearing
        }
    }
}
