package com.oskar.retrolauncher.data.location

data class LocationSample(
    val lat: Double,
    val lon: Double,
    val speedMs: Float,
    val bearing: Float,
    val tsMs: Long,
    val accuracy: Float,
)
