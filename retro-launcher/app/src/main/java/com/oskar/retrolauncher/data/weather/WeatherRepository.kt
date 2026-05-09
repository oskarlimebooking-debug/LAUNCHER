package com.oskar.retrolauncher.data.weather

import android.content.Context
import android.location.Geocoder
import com.oskar.retrolauncher.BuildConfig
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber

class WeatherRepository(
    private val ctx: Context,
    private val http: OkHttpClient,
    private val moshi: Moshi,
) {
    private val prefs = ctx.getSharedPreferences("weather", Context.MODE_PRIVATE)
    private val snapAdapter = moshi.adapter(WeatherSnapshot::class.java)
    private val dtoAdapter = moshi.adapter(WeatherDto::class.java)

    private val _state = MutableStateFlow(loadCached())
    val state: StateFlow<WeatherSnapshot?> = _state

    suspend fun refresh(lat: Double, lon: Double) {
        val key = BuildConfig.OWM_API_KEY
        if (key.isBlank()) {
            Timber.w("OWM_API_KEY missing — set it in local.properties")
            return
        }
        val url = "https://api.openweathermap.org/data/3.0/onecall?lat=$lat&lon=$lon" +
            "&exclude=minutely,hourly,alerts&units=metric&appid=$key"
        val req = Request.Builder().url(url).build()
        runCatching {
            withContext(Dispatchers.IO) {
                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("OWM ${resp.code}")
                    val src = resp.body?.source() ?: error("no body")
                    val dto = dtoAdapter.fromJson(src) ?: error("malformed json")
                    val city = reverseGeocode(lat, lon)
                    WeatherSnapshot.from(dto, city, System.currentTimeMillis())
                }
            }
        }.onSuccess { snap ->
            _state.value = snap
            prefs.edit().putString("snap", snapAdapter.toJson(snap)).apply()
        }.onFailure { Timber.w(it, "weather refresh failed") }
    }

    suspend fun reverseGeocodeBlocking(lat: Double, lon: Double): String =
        reverseGeocode(lat, lon)

    private suspend fun reverseGeocode(lat: Double, lon: Double): String =
        withContext(Dispatchers.IO) {
            // Geocoder lies on AOSP-only ROMs (returns null silently or throws), so we
            // wrap in runCatching and fall back to Nominatim.
            val viaGeocoder = runCatching {
                @Suppress("DEPRECATION")
                Geocoder(ctx).getFromLocation(lat, lon, 1)?.firstOrNull()?.locality
            }.getOrNull()

            viaGeocoder ?: runCatching {
                val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon"
                val req = Request.Builder().url(url)
                    .header("User-Agent", "retro-launcher/0.1 (${BuildConfig.APPLICATION_ID})")
                    .build()
                http.newCall(req).execute().use { resp ->
                    val body = resp.body?.string() ?: return@runCatching null
                    val addr = JSONObject(body).optJSONObject("address") ?: return@runCatching null
                    addr.optString("city").ifEmpty {
                        addr.optString("town").ifEmpty {
                            addr.optString("village").ifEmpty {
                                addr.optString("county").ifEmpty { null }
                            }
                        }
                    }
                }
            }.getOrNull() ?: "—"
        }

    private fun loadCached(): WeatherSnapshot? =
        prefs.getString("snap", null)?.let {
            runCatching { snapAdapter.fromJson(it) }.getOrNull()
        }
}
