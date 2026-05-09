#!/bin/zsh
# PairCoder plan task script for plan-2026-05-create-app
# Run from your LAUNCHER project root

PLAN="plan-2026-05-create-app"

# ── SCAFFOLD ────────────────────────────────────────────────────────
bpsai-pair plan add-task "$PLAN" --id TASK-001 --title "Create settings.gradle.kts, root build.gradle.kts, libs.versions.toml"
bpsai-pair plan add-task "$PLAN" --id TASK-002 --title "Create app/build.gradle.kts with minSdk=23, flavors standard/system"
bpsai-pair plan add-task "$PLAN" --id TASK-003 --title "Write AndroidManifest.xml with all permissions and service declarations"
bpsai-pair plan add-task "$PLAN" --id TASK-004 --title "Create App.kt service locator (OkHttp, Moshi, Room, all repos)"
bpsai-pair plan add-task "$PLAN" --id TASK-005 --title "Create MainActivity.kt + activity_main.xml (ConstraintLayout, panel guideline)"
bpsai-pair plan add-task "$PLAN" --id TASK-006 --title "Create HomeFragment + ViewPager2 with 3-panel RightPanelAdapter"
bpsai-pair plan add-task "$PLAN" --id TASK-007 --title "Create stub fragments for all panels, verify launcher boots"

# ── MEDIA PLAYER ────────────────────────────────────────────────────
bpsai-pair plan add-task "$PLAN" --id TASK-101 --title "Create MediaState data class with livePosition() extrapolation"
bpsai-pair plan add-task "$PLAN" --id TASK-102 --title "Create MediaRepository (MutableStateFlow, transport controls)"
bpsai-pair plan add-task "$PLAN" --id TASK-103 --title "Implement MediaNotificationListener (rebind logic, pushState)"
bpsai-pair plan add-task "$PLAN" --id TASK-104 --title "Create MediaViewModel + MediaUiState"
bpsai-pair plan add-task "$PLAN" --id TASK-105 --title "Build fragment_media.xml layout (art, title, artist, progress, controls)"
bpsai-pair plan add-task "$PLAN" --id TASK-106 --title "Implement MediaFragment with Glide art loading + Palette color extraction"
bpsai-pair plan add-task "$PLAN" --id TASK-107 --title "Handle notification access check + deep-link to settings if missing"

# ── WEATHER CARD ────────────────────────────────────────────────────
bpsai-pair plan add-task "$PLAN" --id TASK-201 --title "Create WeatherDto Moshi classes (Current, Daily, Condition)"
bpsai-pair plan add-task "$PLAN" --id TASK-202 --title "Implement WeatherRepository (OWM fetch, Nominatim fallback, SharedPrefs cache)"
bpsai-pair plan add-task "$PLAN" --id TASK-203 --title "Create WeatherSnapshot + from(dto) mapping + icon ID to drawable mapping"
bpsai-pair plan add-task "$PLAN" --id TASK-204 --title "Implement WeatherWorker (WorkManager 15-min periodic refresh)"
bpsai-pair plan add-task "$PLAN" --id TASK-205 --title "Build fragment_weather.xml (temp, high/low, city, condition icon)"
bpsai-pair plan add-task "$PLAN" --id TASK-206 --title "Implement WeatherFragment + WeatherViewModel"

# ── SPEEDOMETER ─────────────────────────────────────────────────────
bpsai-pair plan add-task "$PLAN" --id TASK-301 --title "Create LocationSample data class + SpeedFilter (alpha-beta)"
bpsai-pair plan add-task "$PLAN" --id TASK-302 --title "Implement LocationService foreground service (GPS 1s updates)"
bpsai-pair plan add-task "$PLAN" --id TASK-303 --title "Create LocationRepository (SharedFlow of LocationSample)"
bpsai-pair plan add-task "$PLAN" --id TASK-304 --title "Implement SpeedometerView (Canvas arc ring, idle pulse animation)"
bpsai-pair plan add-task "$PLAN" --id TASK-305 --title "Build SpeedFragment + SpeedViewModel observing LocationRepository"

# ── TRIP RECORDER ───────────────────────────────────────────────────
bpsai-pair plan add-task "$PLAN" --id TASK-401 --title "Define Room schema: TripEntity, TripPoint, TripDao, AppDb"
bpsai-pair plan add-task "$PLAN" --id TASK-402 --title "Implement TripRecorder state machine (IDLE -> RECORDING -> IDLE)"
bpsai-pair plan add-task "$PLAN" --id TASK-403 --title "Wire TripRecorder into App.kt with locationFlow + geocoder"
bpsai-pair plan add-task "$PLAN" --id TASK-404 --title "Build TripsFragment calendar strip + day-chip RecyclerView"
bpsai-pair plan add-task "$PLAN" --id TASK-405 --title "Implement TripsAdapter with swipe-to-delete (ItemTouchHelper)"
bpsai-pair plan add-task "$PLAN" --id TASK-406 --title "Trip detail screen: Canvas polyline mini-map + stats"

# ── APP GRID ────────────────────────────────────────────────────────
bpsai-pair plan add-task "$PLAN" --id TASK-501 --title "Implement AppListRepository (queryIntentActivities, sorted)"
bpsai-pair plan add-task "$PLAN" --id TASK-502 --title "Build AppGridFragment + AppGridAdapter (GridLayoutManager)"
bpsai-pair plan add-task "$PLAN" --id TASK-503 --title "App rail: long-press to pin, persist to SharedPrefs as JSON array"
bpsai-pair plan add-task "$PLAN" --id TASK-504 --title "Implement EmbedFragment (TextureView placeholder for v0.3)"

# ── SETTINGS ────────────────────────────────────────────────────────
bpsai-pair plan add-task "$PLAN" --id TASK-601 --title "Create SettingsStore.kt (SharedPreferences wrapper, Units enum)"
bpsai-pair plan add-task "$PLAN" --id TASK-602 --title "Write xml/preferences.xml (units, panel_ratio, grid_cols, record_trips)"
bpsai-pair plan add-task "$PLAN" --id TASK-603 --title "Implement SettingsFragment (PreferenceFragmentCompat, custom theme)"
bpsai-pair plan add-task "$PLAN" --id TASK-604 --title "Wire panel_ratio change to MainActivity.applyPanelRatio()"

# ── FIRST-RUN WIZARD ────────────────────────────────────────────────
bpsai-pair plan add-task "$PLAN" --id TASK-701 --title "Permission check on MainActivity.onCreate (notif, location, battery)"
bpsai-pair plan add-task "$PLAN" --id TASK-702 --title "Build 4-step wizard fragment (Welcome, NotifAccess, Location, BatteryOpt)"
bpsai-pair plan add-task "$PLAN" --id TASK-703 --title "Auto-advance on permission flip via onResume re-check"

# ── APP EMBEDDING (v0.3) ────────────────────────────────────────────
bpsai-pair plan add-task "$PLAN" --id TASK-801 --title "Implement HiddenApi.kt reflection wrappers (injectInputEvent)"
bpsai-pair plan add-task "$PLAN" --id TASK-802 --title "Implement Embedding.kt (VirtualDisplay + TextureView bind/launch/release)"
bpsai-pair plan add-task "$PLAN" --id TASK-803 --title "Implement touch forwarding in EmbedFragment (forwardTouch)"
bpsai-pair plan add-task "$PLAN" --id TASK-804 --title "Test Maps embed; implement graceful fallback for stubborn singleTask apps"

echo ""
echo "All tasks added! Run: bpsai-pair plan status"
