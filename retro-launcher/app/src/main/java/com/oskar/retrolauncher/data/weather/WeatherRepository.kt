package com.oskar.retrolauncher.data.weather

import android.content.Context
import android.location.Geocoder
import androidx.annotation.VisibleForTesting
import com.oskar.retrolauncher.BuildConfig
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber

/**
 * Talks to OpenWeatherMap (`/data/2.5/weather`) for current conditions and to
 * Nominatim (`/reverse`) for the city name shown alongside the temperature.
 *
 * Base URLs and the OWM API key are constructor params (with production defaults)
 * so MockWebServer-driven tests can target loopback without monkey-patching.
 *
 * `fetch()` is the new spec'd surface (T1.20) — never throws, always returns
 * `Result`. `refresh()` is retained for callers that want the side-effecting
 * StateFlow update (the WeatherWorker and WeatherViewModel rely on it).
 */
class WeatherRepository(
    private val ctx: Context,
    private val http: OkHttpClient,
    private val moshi: Moshi,
    private val owmBaseUrl: String = "https://api.openweathermap.org",
    private val nominatimBaseUrl: String = "https://nominatim.openstreetmap.org",
    private val apiKey: String = BuildConfig.OWM_API_KEY,
) {
    private val prefs = ctx.getSharedPreferences("weather", Context.MODE_PRIVATE)
    private val snapAdapter = moshi.adapter(WeatherSnapshot::class.java)
    private val dtoAdapter = moshi.adapter(WeatherDto::class.java)

    private val _state = MutableStateFlow(loadCached())
    val state: StateFlow<WeatherSnapshot?> = _state

    /** True when an OWM API key was supplied — gates network work for callers like WeatherWorker. */
    val isConfigured: Boolean get() = apiKey.isNotBlank()

    /**
     * T1.33 — hits OWM with the supplied key to verify it. Returns true on
     * HTTP 200, false on 401/403/other failure or blank input. Never throws.
     * The wizard uses this on step 5 to gate "Continue" with real feedback.
     */
    suspend fun validateApiKey(key: String): Boolean = withContext(Dispatchers.IO) {
        if (key.isBlank()) return@withContext false
        runCatching {
            val url = "$owmBaseUrl/data/2.5/weather".toHttpUrl().newBuilder()
                .addQueryParameter("lat", "0")
                .addQueryParameter("lon", "0")
                .addQueryParameter("appid", key)
                .build()
            val req = Request.Builder().url(url).build()
            http.newCall(req).execute().use { resp -> resp.isSuccessful }
        }.getOrElse { false }
    }

    suspend fun fetch(lat: Double, lon: Double): Result<WeatherSnapshot> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (apiKey.isBlank()) error("OWM_API_KEY missing — set it in local.properties")
                val url = "$owmBaseUrl/data/2.5/weather".toHttpUrl().newBuilder()
                    .addQueryParameter("lat", lat.toString())
                    .addQueryParameter("lon", lon.toString())
                    .addQueryParameter("units", "metric")
                    .addQueryParameter("appid", apiKey)
                    .build()
                val req = Request.Builder().url(url).build()
                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("OWM ${resp.code}")
                    val src = resp.body?.source() ?: error("no body")
                    val dto = dtoAdapter.fromJson(src) ?: error("malformed json")
                    val city = reverseGeocode(lat, lon)
                    WeatherSnapshot.from(dto, city, System.currentTimeMillis())
                }
            }
        }

    suspend fun refresh(lat: Double, lon: Double): Result<WeatherSnapshot> =
        fetch(lat, lon)
            .onSuccess { snap ->
                _state.value = snap
                prefs.edit().putString("snap", snapAdapter.toJson(snap)).apply()
            }
            .onFailure { Timber.w(it, "weather refresh failed") }

    suspend fun reverseGeocodeBlocking(lat: Double, lon: Double): String =
        reverseGeocode(lat, lon)

    private suspend fun reverseGeocode(lat: Double, lon: Double): String =
        withContext(Dispatchers.IO) {
            // Geocoder lies on AOSP-only ROMs (returns null silently or throws),
            // so we wrap in runCatching and fall back to Nominatim.
            val viaGeocoder = runCatching {
                @Suppress("DEPRECATION")
                Geocoder(ctx).getFromLocation(lat, lon, 1)?.firstOrNull()?.locality
            }.getOrNull()

            viaGeocoder ?: runCatching {
                val url = "$nominatimBaseUrl/reverse".toHttpUrl().newBuilder()
                    .addQueryParameter("format", "json")
                    .addQueryParameter("lat", lat.toString())
                    .addQueryParameter("lon", lon.toString())
                    .build()
                // Nominatim TOS requires an identifying User-Agent — bare app name+version.
                val req = Request.Builder().url(url)
                    .header("User-Agent", "retro-launcher/0.1")
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

    /**
     * T1.36 — instrumented tests publish a fixture snapshot directly so the
     * fragment can render the populated state without hitting OpenWeatherMap.
     * Bypasses the on-disk cache (we never want a test fixture to leak across
     * runs as a "stale" cached snapshot).
     */
    @VisibleForTesting
    fun setSnapshotForTest(snap: WeatherSnapshot?) {
        _state.value = snap
    }

    private fun loadCached(): WeatherSnapshot? =
        prefs.getString("snap", null)?.let {
            runCatching { snapAdapter.fromJson(it) }.getOrNull()
        }
}
