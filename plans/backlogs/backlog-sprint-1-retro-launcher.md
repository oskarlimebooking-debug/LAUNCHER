# Sprint 1: Retro Launcher v0.1 → v0.3 (Android Auto–style HU launcher, API 23)

> Backlog for `bpsai-pair engage`. Implements the full feature set described in
> `headunit-launcher-spec.md` — v0.1 MVP, v0.2 polish, and v0.3 system-build
> embedding. Target: 1024×600 landscape head unit, Android 6.0 (API 23), rooted,
> no Google Play Services. Stack: Kotlin 1.9, AGP 8.2, AndroidX Views, OkHttp,
> Moshi, Glide, Room, WorkManager.
>
> Validate before running:
> ```bash
> bpsai-pair engage plans/backlogs/backlog-sprint-1-retro-launcher.md --dry-run
> ```

---

## Delivery Summary

| Metric | Value |
|---|---|
| Tasks | 49 |
| Phases | 11 |
| Complexity (sum) | 353 |
| Human-gated tasks | 2 |
| External tools | OpenWeatherMap key, GitHub CLI, rooted device for install |

| Phase | Tasks | Cx | Theme |
|---|---|---|---|
| 1. Scaffolding | 5 | 23 | Gradle, manifest, themes |
| 2. App shell | 5 | 26 | Application, MainActivity, HomeFragment, StatusBar |
| 3. Location & speed | 4 | 34 | Foreground service, Kalman filter, SpeedometerView |
| 4. Media player | 4 | 34 | NotificationListener, MediaController, Palette |
| 5. Weather card | 4 | 21 | OWM + Nominatim, WorkManager, icon map |
| 6. Trip recording | 5 | 42 | Room, state machine, calendar UI |
| 7. App grid | 3 | 21 | PackageManager scan, grid, pinned rail |
| 8. Settings & first-run | 4 | 21 | SharedPreferences, PreferenceFragment, wizard, BootReceiver |
| 9. Testing & build | 4 | 24 | Unit, instrumented, manual matrix, install script |
| 10. v0.2 polish | 5 | 36 | Trip detail, GPX, drag-reorder, themes, SWC keys |
| 11. v0.3 system build | 6 | 71 | Platform signing, hidden APIs, VirtualDisplay |

---

## Priority Order

1. **P0 (must ship)** — Phase 1 (scaffolding) and Phase 2 (app shell). Without these, nothing else builds or launches.
2. **P1 (MVP feature set)** — Phases 3–8. The launcher does its actual job once these are in.
3. **P2 (polish + system build)** — Phases 9–11. Tests, polish, and the v0.3 embedding work that requires a platform signature.

---

### Phase 1: Project scaffolding

### T1.1 -- Bootstrap Gradle project structure | Cx: 5 | P0

**Description:** Create the root Gradle project with `settings.gradle.kts`, root `build.gradle.kts`, and `gradle/libs.versions.toml`. Use Kotlin DSL throughout. Set Gradle wrapper to 8.5, AGP 8.2.2, Kotlin 1.9.22, JDK 17. The `app/` module is the only module for now.

**AC:**
- [ ] `./gradlew tasks` runs successfully and lists the `app:` task group
- [ ] `libs.versions.toml` declares all versions from spec section 4 (Kotlin, AGP, AndroidX Core/AppCompat/ConstraintLayout/RecyclerView/ViewPager2/Fragment/Lifecycle/Room/Work/Palette/Preference, Material, OkHttp, Moshi, Glide, Timber)
- [ ] Root `build.gradle.kts` registers the Android application and Kotlin plugins via `pluginManagement`
- [ ] `settings.gradle.kts` enables `dependencyResolutionManagement` with `repositoriesMode = FAIL_ON_PROJECT_REPOS`
- [ ] No Hilt, Retrofit, Compose, Coil, or Firebase dependencies — these are explicitly excluded per spec section 4

**Depends on:** None

### T1.2 -- Author app/build.gradle.kts with flavors | Cx: 5 | P0

**Description:** Create the `app/build.gradle.kts` per spec section 6. Two product flavors: `standard` (any user, no embedding) and `system` (platform-signed, embedding enabled). `compileSdk = 34`, `minSdk = 23`, `targetSdk = 28`. Enable `viewBinding`, `kapt` for Room and Moshi codegen and Glide annotation processor. Add a packaging exclude for `META-INF/{AL2.0,LGPL2.1}`.

**AC:**
- [ ] `./gradlew :app:assembleStandardDebug` produces an APK (even if empty)
- [ ] Two flavors `standard` and `system` are visible in `./gradlew :app:tasks`
- [ ] `system` flavor has a `signingConfig` placeholder pointing to a `platform.keystore` (file may not exist yet — placeholder is enough)
- [ ] ViewBinding is enabled and `kapt` is configured for Room, Moshi-codegen, and Glide-compiler
- [ ] Lint baseline file is created at `app/lint-baseline.xml` to suppress pre-existing AndroidX warnings

**Depends on:** T1.1

### T1.3 -- Author AndroidManifest.xml | Cx: 5 | P0

**Description:** Author the manifest per spec section 7. Declare the launcher intent filter with `CATEGORY_HOME` and `CATEGORY_DEFAULT`. Declare permissions: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` (will be ignored on API 23, harmless), `BIND_NOTIFICATION_LISTENER_SERVICE`, `FOREGROUND_SERVICE`, `INTERNET`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `QUERY_ALL_PACKAGES` (for app grid). Declare `MainActivity`, `MediaNotificationListener`, `LocationService`, `BootReceiver`, and a `WeatherWorker` content provider stub.

**AC:**
- [ ] Manifest validates with `./gradlew :app:processStandardDebugMainManifest`
- [ ] `MainActivity` has `category.HOME` + `category.DEFAULT` intent filter so it can be set as default launcher
- [ ] `MediaNotificationListener` declares `BIND_NOTIFICATION_LISTENER_SERVICE` and the notification listener intent filter
- [ ] `LocationService` is declared with `foregroundServiceType` not set (API 23 doesn't require it; targetSdk 28 keeps us out of the API 29 type requirement)
- [ ] Theme is `@style/Theme.RetroLauncher` and orientation is locked to landscape (`screenOrientation="landscape"`)

**Depends on:** T1.2

### T1.4 -- Resource bundles: themes, colors, strings, dimens | Cx: 5 | P0

**Description:** Author the resource bundles per spec section 8. `values/themes.xml` with `Theme.RetroLauncher` (Material3 dark), `values/colors.xml` with neutral surfaces and accent, `values/strings.xml` for all user-facing copy, `values-w1024dp/dimens.xml` for the landscape grid (column gutter, panel radius, status bar height), plus `values-night/colors.xml` for night theme. Include shape and typography appendix per spec section 8.4.

**AC:**
- [ ] `Theme.RetroLauncher` extends `Theme.Material3.DayNight.NoActionBar` and sets `windowBackground`, `colorPrimary`, `colorSurface`
- [ ] `dimens.xml` defines `gutter`, `corner_lg`, `corner_md`, `panel_left_min_width`, `status_bar_height`, `tile_corner`
- [ ] All user-facing strings in spec section 8 are in `strings.xml` with English values
- [ ] Day and night theme variants build cleanly under `assembleStandardDebug`
- [ ] No hard-coded colors or strings in any layout file (verified by `lint --check HardcodedText,HardcodedColor`)

**Depends on:** T1.3

### T1.5 -- Application icon and adaptive icon fallback | Cx: 3 | P0

**Description:** Provide a launcher icon for `mipmap-*` densities. Since target HU is mdpi/hdpi (1024×600), focus on those. Provide a vector drawable `ic_launcher_vector.xml` and rasterized PNGs for mdpi/hdpi/xhdpi. Provide an adaptive-icon variant in `mipmap-anydpi-v26/` (no-op on API 23 but future-proof).

**AC:**
- [ ] Icon renders correctly in the standard launcher app drawer on a 1024×600 device
- [ ] Adaptive-icon XML at `mipmap-anydpi-v26/ic_launcher.xml` references a `foreground` and `background` drawable
- [ ] Round icon variant (`ic_launcher_round`) is also provided
- [ ] No reference to PNGs >256 KB total — keeps APK small for slow eMMC

**Depends on:** T1.4

---

### Phase 2: App shell

### T1.6 -- App.kt service locator and Application class | Cx: 5 | P0

**Description:** Implement `App.kt` per spec section 17.1. Application subclass with manual DI via a `service` object. Initialize Timber in debug, expose lazily-created repositories (`MediaRepository`, `WeatherRepository`, `LocationRepository`, `TripRepository`, `AppListRepository`, `SettingsStore`). Use lazy initialization so we don't pay startup cost for things we may not touch this session.

**AC:**
- [ ] `App` is registered in `AndroidManifest.xml` via `android:name=".App"`
- [ ] `App.service` is accessible from any Activity/Fragment via `(application as App).service`
- [ ] Timber is planted with `Timber.DebugTree()` only in `BuildConfig.DEBUG`
- [ ] Each repo is `by lazy {}` and constructed with the application context
- [ ] No service-locator dependency on Activity context (would leak)

**Depends on:** T1.5

### T1.7 -- MainActivity and activity_main.xml | Cx: 5 | P0

**Description:** Implement `MainActivity` per spec section 17.2. Single-activity launcher. Hosts `HomeFragment` in a top container and `StatusBarFragment` in a bottom strip. Lock to landscape, `WindowCompat.setDecorFitsSystemWindows(window, false)` for edge-to-edge. Handle `onBackPressed` to no-op (a launcher should not exit on back).

**AC:**
- [ ] `activity_main.xml` is a vertical `ConstraintLayout` with `home_container` (0dp,1.0 weight) above `status_bar_container` (height = `@dimen/status_bar_height`)
- [ ] Activity launches and shows the empty fragment containers without crashing
- [ ] Back press does not finish the activity (`onBackPressed` is a no-op)
- [ ] Edge-to-edge insets are consumed; no system bar overlap on the 1024×600 viewport
- [ ] Activity is set as default launcher via `intent-filter` (verified manually by long-pressing home icon and choosing it)

**Depends on:** T1.6

### T1.8 -- HomeFragment with ViewPager2 right panel | Cx: 8 | P0

**Description:** Implement `HomeFragment` per spec section 17.5. Two-column layout: left panel (Media + Weather stacked, 40% width) and right panel (`ViewPager2` swiping between Embed/AppGrid/Trips, 60% width). Panel split ratio is read from `SettingsStore`. Implement `RightPanelAdapter` returning the three fragments by position.

**AC:**
- [ ] `fragment_home.xml` uses `Guideline` for the panel split, position bound to `app:layout_constraintGuide_percent` from settings
- [ ] Swiping the right panel cycles between the three fragments smoothly (no jank on a Cortex-A7)
- [ ] `RightPanelAdapter` extends `FragmentStateAdapter` and returns `EmbedFragment`/`AppGridFragment`/`TripsFragment`
- [ ] Panel split ratio change triggers a layout pass without recreating the activity (use `ConstraintLayout.LayoutParams` updates)
- [ ] Default split is 0.4/0.6 as per spec section 8.1

**Depends on:** T1.7

### T1.9 -- StatusBarFragment (clock, speed, trip stats, drawer button) | Cx: 5 | P0

**Description:** Implement `StatusBarFragment` per spec section 16. Bottom strip showing: clock (left), current speed (center), trip distance/duration (right), drawer button (far right). Updates from `LocationRepository` (speed) and `TripRepository` (active-trip stats) via `LiveData`. Drawer button opens settings.

**AC:**
- [ ] Clock updates every second using a `Handler`-based ticker (no `BroadcastReceiver` overhead — saves CPU)
- [ ] Speed display switches between km/h and mph based on `SettingsStore.speedUnit`
- [ ] Trip stats appear only when a trip is active (visibility GONE otherwise)
- [ ] Drawer button starts `SettingsActivity` via an intent
- [ ] Layout fits within `@dimen/status_bar_height` (48dp at 1024dp width)

**Depends on:** T1.8

### T1.10 -- Util extensions: ColorExt, FormatExt, ViewExt | Cx: 3 | P1

**Description:** Per spec section 17 — create three small extension files. `ColorExt.kt` with palette-derived dominant-color helpers. `FormatExt.kt` with `formatDistance`, `formatDuration`, `formatSpeed` accepting unit prefs. `ViewExt.kt` with `View.fade()`, `View.gone()`, `View.visible()`. Each helper has a unit test.

**AC:**
- [ ] `formatDistance(123.456, Unit.METRIC)` returns `"123 m"` and `(1234.5, METRIC)` returns `"1.2 km"`
- [ ] `formatDuration(3725)` returns `"01:02:05"`
- [ ] `formatSpeed(20.5, IMPERIAL)` returns `"45.9 mph"`
- [ ] Each extension is in its own file and under 50 lines (architecture rule)
- [ ] Unit tests cover each public function with at least 3 cases including a zero/edge case

**Depends on:** T1.6

---

### Phase 3: Location service and speedometer

### T1.11 -- LocationService foreground service | Cx: 13 | P1

**Description:** Implement `LocationService` per spec section 11. Foreground service holding a partial `WakeLock`, requesting `LocationManager.GPS_PROVIDER` updates at 1 Hz with 0 m min displacement. Posts a low-priority notification (channel "location" — wrap with `NotificationCompat`, no-op channel on API 23). Survives Doze on rooted ROMs by holding the WakeLock. Pushes `LocationSample(lat, lon, speedMs, accuracy, time)` into a `MutableSharedFlow` exposed by the service.

**AC:**
- [ ] Service runs in the background and notification is visible while running
- [ ] `LocationManager` updates are received at 1 Hz when GPS fix is good (verified with `adb shell dumpsys location`)
- [ ] Service is restarted via `START_STICKY` if killed by the system
- [ ] WakeLock is released in `onDestroy` (no battery-leak unit test failure under StrictMode)
- [ ] No reliance on `FusedLocationProviderClient` — pure `LocationManager` (Play Services not available)

**Depends on:** T1.9

### T1.12 -- SpeedFilter with Kalman-style smoothing | Cx: 8 | P1

**Description:** Implement `SpeedFilter.kt` per spec section 11.2. Takes raw `LocationSample` and produces a smoothed speed in m/s. Uses a 1D Kalman filter with process noise = 0.5 m/s² and measurement noise scaled from `accuracy` field. Falls back to a 5-sample moving average when GPS accuracy >20 m. Exposes filtered speed via `Flow<Float>`.

**AC:**
- [ ] Filter converges to true speed within 5 samples when given clean GPS input (verified with synthetic 1 Hz test)
- [ ] When measurement noise is high, filter weights process model more — verified with a unit test injecting accuracy=100 m
- [ ] Outputs zero speed within 2 samples of `LocationSample.speedMs == 0`
- [ ] Cold-start sample produces a non-NaN output (no division-by-zero on the first reading)
- [ ] Unit test coverage ≥90% for the filter logic

**Depends on:** T1.11

### T1.13 -- SpeedometerView custom drawing | Cx: 8 | P1

**Description:** Implement `SpeedometerView` per spec section 11.3. Custom `View` drawing a circular gauge with current speed numeric, max-speed tick, and a colored arc (green→yellow→red transition above `speedThresholdKmh` from settings). All drawing uses `Paint` + `Canvas` directly (no Compose). Animates transitions over 250 ms using `ValueAnimator`. Handles `onSizeChanged` for dynamic sizing on the 1024×600 panel.

**AC:**
- [ ] View renders in the layout editor (Android Studio preview) without runtime errors
- [ ] Speed text is centered and uses `dimens.text_speed_xl` (≥48sp)
- [ ] Color arc transitions from green to red at the configured threshold
- [ ] Animation is GPU-accelerated (uses `setLayerType(LAYER_TYPE_HARDWARE)`)
- [ ] Frame rate stays ≥30 fps on a Cortex-A7 simulator (measured via `Choreographer`)

**Depends on:** T1.12

### T1.14 -- SpeedFragment + ViewModel | Cx: 5 | P1

**Description:** Implement `SpeedFragment.kt` and `SpeedViewModel.kt`. ViewModel exposes `LiveData<SpeedUiState>` collected from `LocationRepository`'s filtered-speed flow. Fragment binds the value to `SpeedometerView`. Settings reload (unit/threshold change) reaches the view without fragment recreation.

**AC:**
- [ ] Fragment displays the speedometer view in the right panel when the embed page is hidden (or as a tile in the left panel — final placement per HomeFragment)
- [ ] Speed updates at the same rate as `LocationRepository` (1 Hz)
- [ ] Switching `SettingsStore.speedUnit` triggers a `SpeedometerView` re-render with new units
- [ ] Speedometer reads zero speed when no GPS fix is acquired
- [ ] No memory leak when fragment is detached and reattached (verified with LeakCanary in debug builds)

**Depends on:** T1.13

---

### Phase 4: Media player

### T1.15 -- MediaNotificationListener service | Cx: 13 | P1

**Description:** Implement `MediaNotificationListener` per spec section 9.3. Extends `NotificationListenerService`. On `onNotificationPosted`, filter for notifications with a `MediaSession.Token`. Build a `MediaController` from the token, register a `Callback` for metadata + playback-state changes, and forward the changes into `MediaRepository`. Handle `onListenerConnected` / `onListenerDisconnected` to reset state.

**AC:**
- [ ] Listener is granted access via Settings → Apps → Notification Access (manual user step in first-run)
- [ ] Spotify, YouTube Music, and stock Music app are all detected (verified manually)
- [ ] `onMetadataChanged` extracts title, artist, album, art bitmap, duration
- [ ] `onPlaybackStateChanged` produces a state with isPlaying, position, speed
- [ ] Service does not crash when notification is removed mid-playback (null-safe controller cleanup)

**Depends on:** T1.10

### T1.16 -- MediaRepository state flow | Cx: 8 | P1

**Description:** Implement `MediaRepository.kt` per spec section 9.5. Holds a `MutableStateFlow<MediaState>` populated by the notification listener. Combines metadata + playback state into a single `MediaState(title, artist, art, duration, position, isPlaying, packageName)`. Exposes a position-tick coroutine that increments `position` every 250 ms while playing (so the UI seek bar is smooth without listener spam).

**AC:**
- [ ] `MediaState.empty` is emitted on cold start and when no controller is connected
- [ ] Position increments at 250 ms cadence while `isPlaying = true` and freezes when paused
- [ ] When a new track loads, position resets to 0 and duration updates atomically
- [ ] Album art bitmap is recycled when replaced (no Glide-pool corruption — verified with strict mode + LeakCanary)
- [ ] Thread-safe: tested with concurrent `emit` and collect from two coroutines

**Depends on:** T1.15

### T1.17 -- MediaFragment + ViewModel + Glide | Cx: 8 | P1

**Description:** Implement `MediaFragment.kt` + `MediaViewModel.kt` per spec section 9.6. Layout `fragment_media.xml` with album art (left), title/artist text, seek progress, and play/pause + skip buttons. Glide loads album art with `bitmapTransform(RoundedCorners)`. Buttons send transport actions to `MediaController` via the listener service.

**AC:**
- [ ] Album art renders correctly with rounded corners and no flicker on track change
- [ ] Title and artist text uses marquee animation when overflowing
- [ ] Seek bar updates smoothly (no ticker jitter)
- [ ] Play/pause/skip buttons control the active media session
- [ ] Empty state ("Nothing playing") renders when `MediaState.empty` is emitted

**Depends on:** T1.16

### T1.18 -- Album-art Palette dominant-color extraction | Cx: 5 | P1

**Description:** Use AndroidX Palette to extract the dominant color from album art and apply it as a tinted gradient behind the media tile. Cache extraction results keyed by track ID to avoid repeated work. Animate gradient transition over 400 ms.

**AC:**
- [ ] Dominant color is extracted asynchronously (off the main thread)
- [ ] Cache hit short-circuits extraction within the same session
- [ ] Gradient animates between old and new colors over 400 ms with `ArgbEvaluator`
- [ ] Falls back to `colorSurface` when extraction fails or art is null
- [ ] No noticeable frame drop on a Cortex-A7 during transition (measured with Systrace)

**Depends on:** T1.17

---

### Phase 5: Weather card

### T1.19 -- WeatherDto with Moshi adapters | Cx: 3 | P1

**Description:** Define `WeatherDto.kt` per spec section 10.3. Moshi data classes mapping the OpenWeatherMap "current weather" response. Use `@JsonClass(generateAdapter = true)` for codegen. Cover required fields: `main.temp`, `main.feels_like`, `weather[0].id`, `weather[0].icon`, `wind.speed`, `name`, `dt`. Tolerate missing optional fields.

**AC:**
- [ ] Moshi adapter is generated by `kapt` and registered with the global Moshi instance
- [ ] Sample OWM JSON fixtures parse without error in unit tests
- [ ] Missing optional fields (e.g., `wind.gust`) deserialize as null without throwing
- [ ] DTO is internal/data class (no leak to UI layer — UI consumes a `WeatherSnapshot` instead)
- [ ] File is under 100 lines

**Depends on:** T1.10

### T1.20 -- WeatherRepository OkHttp client | Cx: 8 | P1

**Description:** Implement `WeatherRepository.kt` per spec section 10.4. OkHttp client with a 10-second connect/read timeout, gzip enabled, and a small disk cache (5 MB). Method `fetch(lat, lon)` calls OWM `/data/2.5/weather` with the API key from `BuildConfig`. Reverse geocodes location name via Nominatim (`/reverse`) with proper `User-Agent` header (Nominatim TOS). Maps DTO → `WeatherSnapshot`. Returns `Result<WeatherSnapshot>` (no throwing).

**AC:**
- [ ] OWM API key is sourced from `BuildConfig.OWM_API_KEY` (not hardcoded; `local.properties` workflow)
- [ ] Cache is on-disk at `cacheDir/weather/`, evicts LRU at 5 MB
- [ ] Nominatim requests include `User-Agent: retro-launcher/0.1` per their TOS
- [ ] Network failure returns `Result.failure` without crashing
- [ ] Repository unit tests use OkHttp's `MockWebServer` with at least 4 scenarios (success, 401, timeout, malformed JSON)

**Depends on:** T1.19

### T1.21 -- WeatherWorker periodic refresh | Cx: 5 | P1

**Description:** Implement `WeatherWorker.kt` per spec section 10.5. WorkManager periodic worker scheduled every 15 minutes when network is available. Calls `WeatherRepository.fetch` and stores the latest snapshot in a `MutableStateFlow` exposed via `WeatherRepository.snapshot`. Respects OWM free-tier limits (96 calls/day at 15-min cadence — well under 1000/day).

**AC:**
- [ ] Worker runs every 15 min when on Wi-Fi or cellular (verified via `WorkManager.getWorkInfosByTag`)
- [ ] Worker is rescheduled on `BOOT_COMPLETED`
- [ ] Worker yields `Result.retry` on transient failures (network), `Result.success` otherwise
- [ ] Last successful snapshot is persisted to `SharedPreferences` so the tile shows stale data after reboot
- [ ] No crash when the OWM API key is empty (worker logs and returns `Result.failure`)

**Depends on:** T1.20

### T1.22 -- WeatherFragment + ViewModel + icon mapping | Cx: 5 | P1

**Description:** Implement `WeatherFragment.kt` + `WeatherViewModel.kt` + the icon-mapping utility per spec section 10.6. Layout shows current temp, feels-like, conditions text, wind, and a weather icon. Map OWM icon codes (`01d`, `01n`, …) to local vector drawables. Refreshes from `WeatherRepository.snapshot`.

**AC:**
- [ ] All 18 OWM icon codes are mapped to local SVG drawables (no remote icon fetches)
- [ ] Fragment displays placeholder ("—") when snapshot is null
- [ ] Temp displays in C° or F° based on `SettingsStore.tempUnit`
- [ ] Wind speed displays in m/s, km/h, or mph based on `SettingsStore.speedUnit`
- [ ] Tile is exactly half the height of the left panel (matches spec section 8.1)

**Depends on:** T1.21

---

### Phase 6: Trip recording

### T1.23 -- Room schema (TripEntity, TripPoint, TripDao, AppDb) | Cx: 8 | P1

**Description:** Implement the Room schema per spec section 12.3. `TripEntity(id, startTime, endTime, distanceM, durationS, maxSpeedMs, avgSpeedMs, gpxPath?)`. `TripPoint(tripId, lat, lon, speedMs, time)` linked by foreign key. `TripDao` with insertTrip, insertPoints (bulk), getRecentTrips(limit), getPointsForTrip(tripId), deleteTrip(tripId). `AppDb` extends `RoomDatabase` and exposes the DAO.

**AC:**
- [ ] Database migrates from version 0→1 cleanly (no migration needed yet, but `Room.fallbackToDestructiveMigration` is NOT used — explicit `Migration` objects only)
- [ ] Foreign-key cascade deletes points when a trip is deleted
- [ ] DAO methods are `suspend` or return `Flow` (no blocking on the main thread)
- [ ] Room compiles without warnings under `kapt`
- [ ] Unit tests using Room's in-memory builder cover insert/query/delete for ≥4 trips with 100 points each

**Depends on:** T1.10

### T1.24 -- TripRecorder state machine | Cx: 13 | P1

**Description:** Implement `TripRecorder.kt` per spec section 12.2. State machine: `Idle → Detecting → Recording → Stopping → Idle`. Transitions: `Idle→Detecting` when a `LocationSample` exceeds `tripStartSpeedKmh` (default 10), `Detecting→Recording` when sustained for 10 s, `Recording→Stopping` when speed <2 km/h sustained for 60 s, `Stopping→Idle` after a final `TripEntity` insert. Aggregates points into 5-second buckets to reduce DB writes.

**AC:**
- [ ] State machine never enters an invalid transition (verified with property-based tests)
- [ ] A 10-minute synthetic GPS trace produces exactly one `TripEntity` with the correct distance (within ±5%)
- [ ] Stopping at a traffic light for 30 s does NOT split the trip (well under the 60 s threshold)
- [ ] Bucket aggregation reduces DB inserts by ≥80% vs. raw 1 Hz writes
- [ ] Recovery: if the service is killed mid-trip, the partial trip is recovered on next start (state is persisted to `SharedPreferences`)

**Depends on:** T1.23

### T1.25 -- TripRepository | Cx: 5 | P1

**Description:** Implement `TripRepository.kt` exposing `Flow<List<TripEntity>>` of recent trips (last 30 days), `getPoints(tripId): Flow<List<TripPoint>>`, `delete(tripId): suspend`, and `activeTrip: StateFlow<TripEntity?>` published by the recorder.

**AC:**
- [ ] `recentTrips` flow updates within 100 ms of a new trip being inserted
- [ ] `activeTrip` reflects the recorder's state (null when idle, populated when recording)
- [ ] Delete operation also removes associated points (foreign key cascade)
- [ ] Repository is unit-tested against an in-memory Room DB
- [ ] No coroutine leaks (tests pass under `runTest` with no pending jobs)

**Depends on:** T1.24

### T1.26 -- TripCalendarView custom view | Cx: 8 | P2

**Description:** Implement `TripCalendarView.kt` per spec section 12.5. Custom view drawing a 7-column calendar with day cells colored by total distance driven that day (heatmap, blue→red). Tapping a cell selects that day. Renders the last 90 days. Uses `Canvas` drawing — no RecyclerView per spec to keep scrolling smooth on weak GPU.

**AC:**
- [ ] Calendar renders 13 weeks (~91 days) in a 7×13 grid that fits the right-panel width
- [ ] Day cells are color-mapped by distance (computed from `TripRepository.recentTrips`)
- [ ] Tap dispatches a `OnDaySelected(date)` listener
- [ ] Today's cell has a visible border
- [ ] No allocations in `onDraw` (pre-allocated `Paint` and `Rect`)

**Depends on:** T1.25

### T1.27 -- TripsFragment + adapter | Cx: 8 | P2

**Description:** Implement `TripsFragment.kt`. Top half: `TripCalendarView`. Bottom half: `RecyclerView` showing trips for the selected day with start time, distance, duration, max/avg speed. Uses `ListAdapter` + `DiffUtil`. Tapping a trip opens trip detail (placeholder for v0.2).

**AC:**
- [ ] Selecting a day in the calendar filters the list to that day's trips
- [ ] List shows date "Today/Yesterday/MMM dd" formatted appropriately
- [ ] DiffUtil prevents full re-binding on filter changes
- [ ] Empty state ("No trips") shows when the selected day has no trips
- [ ] Layout fits comfortably in the 60% right panel at 1024×600

**Depends on:** T1.26

---

### Phase 7: App grid

### T1.28 -- AppListRepository PackageManager scanning | Cx: 8 | P1

**Description:** Implement `AppListRepository.kt` per spec section 13.1. Queries `PackageManager.queryIntentActivities(MAIN, LAUNCHER)` to list installed launchable apps. Caches the list (rebuilds on `PACKAGE_ADDED`/`PACKAGE_REMOVED` broadcasts). Loads icons via Glide (using a custom `ApplicationInfo` model loader to keep cache hits). Sorts by user-defined order from `SettingsStore`.

**AC:**
- [ ] First scan completes in under 500 ms on a 1 GB device with 30 apps
- [ ] Adding/removing an app updates the list within 1 s via broadcast receiver
- [ ] Icons are loaded asynchronously and cached (Glide disk + memory)
- [ ] Sort order persists across launches via `SettingsStore`
- [ ] Excluded system apps (e.g., the launcher itself) are filtered out

**Depends on:** T1.10

### T1.29 -- AppGridFragment + adapter | Cx: 8 | P1

**Description:** Implement `AppGridFragment.kt` per spec section 13.2. `RecyclerView` with `GridLayoutManager` (4 columns landscape). Tapping an icon launches the app via `Context.startActivity(launchIntent)`. Long-press shows a popup with "Pin to rail" and "App info" options. Dimens: tile size 144dp, padding 12dp, label below icon.

**AC:**
- [ ] Grid renders all installed apps in 4 columns
- [ ] Tap launches the target app within 200 ms
- [ ] Long-press opens a `PopupMenu` with two actions
- [ ] Scroll is smooth (≥45 fps on Cortex-A7) — verified with Systrace
- [ ] Adapter uses `ListAdapter` + `DiffUtil` for icon swaps without flicker

**Depends on:** T1.28

### T1.30 -- Pinned app rail integration | Cx: 5 | P2

**Description:** Add a horizontal "rail" of pinned apps along the bottom of the right panel when the AppGrid page is active. Pinned apps are persisted in `SettingsStore.pinnedApps: List<String>` (package names). Rail tile is half the size of grid tile.

**AC:**
- [ ] Rail shows up to 8 pinned apps
- [ ] Tapping a rail tile launches the app
- [ ] "Pin to rail" long-press action adds an app to the rail (max 8; older entries shift out)
- [ ] Rail persists across launches
- [ ] Removing a pinned app via long-press menu is supported

**Depends on:** T1.29

---

### Phase 8: Settings and first-run

### T1.31 -- SettingsStore SharedPreferences wrapper | Cx: 5 | P1

**Description:** Implement `SettingsStore.kt` per spec section 14.2. Wraps `SharedPreferences` with typed property delegates for: `speedUnit` (METRIC/IMPERIAL), `tempUnit`, `panelRatio` (Float 0.3–0.5), `gridColumns` (Int 3–5), `tripStartSpeedKmh` (Int), `pinnedApps` (List<String> via JSON), `mapApp`, `voiceApp`, `weatherLat/lon` (Float?), `firstRunDone` (Bool). Each property exposes a `Flow` for reactive UI.

**AC:**
- [ ] All properties round-trip correctly (write → restart app → read same value)
- [ ] `pinnedApps` JSON-encodes/decodes safely with Moshi
- [ ] Each property exposes a `Flow<T>` that emits on change
- [ ] No `apply()` on the main thread (uses `commit` in coroutines or apply in non-blocking flow)
- [ ] Unit tests cover defaults, overrides, and serialization for each property

**Depends on:** T1.10

### T1.32 -- SettingsFragment PreferenceFragment | Cx: 5 | P1

**Description:** Implement `SettingsFragment.kt` per spec section 14.3 using `PreferenceFragmentCompat`. Categories: General (units, panel ratio, theme), Trip (start threshold, GPX export toggle), Apps (default map, default voice), Weather (location override, units, refresh interval), About (version, OSS licenses).

**AC:**
- [ ] All preferences are wired to `SettingsStore` (changes persist immediately)
- [ ] Panel ratio uses a `SeekBarPreference` (30–50)
- [ ] Default map/voice picker shows a list of installed apps that handle the relevant intents
- [ ] OSS licenses screen displays bundled LICENSE files
- [ ] Settings is launched from `StatusBarFragment` drawer button

**Depends on:** T1.31

### T1.33 -- First-run permission wizard | Cx: 8 | P1

**Description:** Implement a multi-step first-run wizard per spec section 15. Steps: (1) welcome, (2) request location permission (`ACCESS_FINE_LOCATION`), (3) prompt user to grant Notification Listener access via `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`, (4) prompt to set this app as default launcher via `MANAGE_DEFAULT_APPS`, (5) prompt to enter OWM API key (or skip), (6) prompt to set home location for weather. Sets `SettingsStore.firstRunDone = true` only after all steps complete.

**AC:**
- [ ] Wizard shows on first launch and never again unless user clears app data
- [ ] Skipping a step does not block the wizard but features are gated until granted
- [ ] Notification listener step actually opens the system settings screen
- [ ] OWM key step validates the key with a test fetch
- [ ] Wizard fits the 1024×600 viewport without scrolling

**Depends on:** T1.32

### T1.34 -- BootReceiver for auto-launch | Cx: 3 | P2

**Description:** Implement `BootReceiver.kt` to start `LocationService` and reschedule `WeatherWorker` on `BOOT_COMPLETED`. No UI activity start (the launcher is already the home app — system starts it).

**AC:**
- [ ] Receiver triggers on `BOOT_COMPLETED` (verified with `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED`)
- [ ] `LocationService` starts within 5 s of boot
- [ ] `WeatherWorker` is enqueued
- [ ] Receiver is `enabled` only when `firstRunDone = true` (avoid running before user setup)
- [ ] No reflection / hidden APIs used

**Depends on:** T1.33

---

### Phase 9: Testing and build

### T1.35 -- Unit tests for repos and recorders | Cx: 8 | P1

**Description:** Write unit-test coverage to ≥80% (project coverage_target) for `MediaRepository`, `WeatherRepository`, `LocationRepository`, `TripRepository`, `TripRecorder`, `SpeedFilter`, and util extensions. Use JUnit 4, MockK, Turbine for `Flow` testing, OkHttp `MockWebServer` for HTTP, and Room's in-memory DB.

**AC:**
- [ ] `./gradlew :app:testStandardDebugUnitTest jacocoTestReport` produces a coverage report ≥80%
- [ ] Each repo has at least 5 unit tests covering happy path, failure path, edge cases
- [ ] `TripRecorder` state-machine tests use property-based testing (kotest-property or similar)
- [ ] Tests run in under 60 s total
- [ ] No flaky tests across 5 consecutive runs

**Depends on:** T1.30, T1.34

### T1.36 -- Instrumented tests for UI fragments | Cx: 8 | P2

**Description:** Write Espresso instrumented tests for `HomeFragment`, `MediaFragment`, `WeatherFragment`, `SpeedFragment`, `TripsFragment`, `AppGridFragment`. Cover: rendering with empty state, rendering with populated state, user actions (tap play/pause, swipe right panel, tap calendar day, tap app icon).

**AC:**
- [ ] `./gradlew :app:connectedStandardDebugAndroidTest` passes on an API 23 emulator
- [ ] Each fragment has ≥3 instrumented tests
- [ ] Tests run in under 5 minutes
- [ ] Test data is injected via a test-only `App.service` override (no real network or sensors)
- [ ] CI artifact saves screenshot diffs on failure

**Depends on:** T1.35

### T1.37 -- Manual test matrix doc | Cx: 3 | P2

**Description:** Author `docs/manual-test-matrix.md` per spec section 19.3. Lists every feature and the manual verification steps on a real head unit (since instrumented tests can't simulate GPS, real notifications, or default-launcher behavior). Captures expected behavior for the 1024×600 panel size.

**AC:**
- [ ] Matrix covers all 6 v0.1 features and the v0.2 polish items
- [ ] Each row has steps + expected result
- [ ] Pre-flight checklist (root status, OWM key, notification listener access)
- [ ] Markdown table is readable in GitHub
- [ ] Tracked in `docs/` with the spec

**Depends on:** T1.36

### T1.38 -- Build and install scripts for rooted HU | Cx: 5 | P2

**Description:** Author `scripts/install-hu.sh` per spec section 20. ADB-based install: `adb install -r app-standard-debug.apk`. Sets the app as default launcher via `cmd package set-home-activity` (requires root over ADB). Documents the system-build flow (`cmd package install -t -g system-debug.apk` after platform-signing).

**External tools:** ADB, rooted head unit on Wi-Fi or USB

**Requires:** human

**AC:**
- [ ] Script runs end-to-end on a rooted device and ends with the launcher visible as home
- [ ] Failure mode "device not authorized" is reported clearly
- [ ] Script supports `--system` flag to install the system-flavor APK
- [ ] README explains how to enable USB debugging on the head unit
- [ ] Script is bash + portable (no zsh-specific syntax)

**Depends on:** T1.37

---

### Phase 10: v0.2 polish

### T1.39 -- Trip detail screen with offline mini-map | Cx: 13 | P2

**Description:** Implement `TripDetailFragment`. Shows a trip's points on a tiled map using a local `osmdroid` instance pointed at MBTiles offline tiles bundled with the app (or downloaded once on first run). Plots the polyline with `Polyline` overlay. Stats panel at the bottom: distance, duration, max/avg speed, start/end addresses (Nominatim cache).

**AC:**
- [ ] Map loads offline from bundled MBTiles or cached tiles (no live network required)
- [ ] Polyline renders all points smoothly (≥30 fps)
- [ ] Pinch-to-zoom and pan work
- [ ] Stats are computed correctly from the points (cross-checked against TripEntity aggregates)
- [ ] Addresses fall back to `lat, lon` if Nominatim cache is cold

**Depends on:** T1.38

### T1.40 -- GPX export per trip | Cx: 5 | P2

**Description:** Implement `GpxExporter.kt`. Converts `TripEntity` + `List<TripPoint>` into a GPX 1.1 XML file written to `Downloads/RetroLauncher/trip-{id}.gpx`. Uses `Xml.newSerializer()` (no third-party XML lib). Trip detail screen has an "Export GPX" button.

**AC:**
- [ ] Exported GPX validates against the GPX 1.1 schema (verified with `xmllint`)
- [ ] Track points include `<time>` ISO-8601 timestamps and `<speed>` in m/s
- [ ] File is written to a `MediaStore`-visible location
- [ ] `GpxExporter` has unit tests that snapshot the output XML for a 10-point trip
- [ ] Failure to write (no storage permission) shows a snackbar, not a crash

**Depends on:** T1.39

### T1.41 -- App rail drag-to-reorder | Cx: 8 | P2

**Description:** Add `ItemTouchHelper`-based drag-to-reorder to the pinned app rail and to the main grid. Persist the new order to `SettingsStore.pinnedApps` and `SettingsStore.gridOrder`. Long-press initiates drag; release commits.

**AC:**
- [ ] Long-press initiates drag with a haptic + visual lift
- [ ] Drop at a new index reorders the list and persists
- [ ] Drag is disabled in normal scroll mode
- [ ] Order survives app restart
- [ ] No crashes when dragging outside the RecyclerView bounds

**Depends on:** T1.40

### T1.42 -- Day/night theme variants | Cx: 5 | P2

**Description:** Add 3 additional themes (Mocha, Ocean, Forest) selectable from Settings. Each provides `values/colors-{theme}.xml` and `values-night/colors-{theme}.xml`. Theme is applied via `Activity.setTheme` before `super.onCreate`.

**AC:**
- [ ] User can select a theme in Settings
- [ ] All fragments re-render in the chosen theme without app restart (recreate activity)
- [ ] Day/night auto-switch uses `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_AUTO_TIME)`
- [ ] Selected theme persists across launches
- [ ] Speedometer color thresholds remain readable in all themes

**Depends on:** T1.41

### T1.43 -- Steering-wheel KEYCODE_MEDIA_* support | Cx: 5 | P2

**Description:** Override `MainActivity.dispatchKeyEvent` to forward `KEYCODE_MEDIA_PLAY_PAUSE`, `KEYCODE_MEDIA_NEXT`, `KEYCODE_MEDIA_PREVIOUS`, `KEYCODE_VOLUME_UP/DOWN` to the active `MediaController`. Falls through to system if no media is playing.

**AC:**
- [ ] Pressing the steering-wheel "next" button advances the track in Spotify (verified manually)
- [ ] Volume keys still control system volume when no media is active
- [ ] No interception of `KEYCODE_HOME` (would break launcher behavior)
- [ ] Key events are logged in debug builds for troubleshooting
- [ ] Works with stock Music, Spotify, YouTube Music

**Depends on:** T1.42

---

### Phase 11: v0.3 system-build features

### T1.44 -- System flavor scaffolding and platform signing | Cx: 8 | P2

**Description:** Wire the `system` product flavor end-to-end. Add the `system` source set under `app/src/system/` for platform-signed-only code (HiddenApi, Embedding). Document the platform-signing flow: extract `platform.x509.pem` and `platform.pk8` from the head unit ROM, convert to a JKS keystore, configure `signingConfigs.system`. Without a real signature, the build skips system-only features.

**External tools:** apksigner, openssl, head unit ROM extract

**Requires:** human

**AC:**
- [ ] `./gradlew :app:assembleSystemDebug` builds successfully when `platform.keystore` exists
- [ ] When the keystore is missing, build prints a clear warning and skips the system-only source set
- [ ] System flavor adds `<uses-permission android:name="android.permission.INJECT_EVENTS" />` and other platform-signature permissions
- [ ] Documentation `docs/platform-signing.md` walks through ROM extraction
- [ ] Standard flavor remains unaffected — `assembleStandardDebug` still works without the keystore

**Depends on:** T1.43

### T1.45 -- HiddenApi reflection helpers | Cx: 13 | P2

**Description:** Implement `HiddenApi.kt` (system flavor only) wrapping reflection access to `WindowManagerGlobal`, `ActivityManagerNative`, `IInputManager`, and similar hidden classes. Wraps each in a try/catch with a `Log.w` fallback. Provides `getDisplayManager()`, `injectInputEvent(MotionEvent)`, `getRunningTasks()`.

**AC:**
- [ ] All reflection calls are wrapped in `Class.forName` + `Method.invoke` with explicit exception handling
- [ ] Each helper logs its hidden-API dependency at registration time
- [ ] Unit tests use a fake `Class.forName` provider to verify behavior on missing classes
- [ ] No use of `MetaReflection` or other anti-detection schemes (we accept that this is rooted-only)
- [ ] Code compiles only under the `system` source set

**Depends on:** T1.44

### T1.46 -- VirtualDisplay embedding pipeline | Cx: 21 | P2

**Description:** Implement `Embedding.kt` (system flavor only) per spec section 13.3. Creates a `VirtualDisplay` of size matching the right panel (~614×600), captures its frames via `SurfaceTexture` to a `TextureView` in `EmbedFragment`. Routes app starts to the virtual display via `ActivityOptions.makeBasic().setLaunchDisplayId(virtualDisplay.display.displayId)`.

**AC:**
- [ ] A `VirtualDisplay` is created and visible in `dumpsys display`
- [ ] Maps app launches into the virtual display, not the main display
- [ ] Frame rate from the virtual display is ≥30 fps
- [ ] Virtual display is destroyed cleanly on `onPause` (no zombie displays)
- [ ] EmbedFragment falls back to a "Embedding unavailable" message on standard flavor

**Depends on:** T1.45

### T1.47 -- Touch-event forwarding via root | Cx: 13 | P2

**Description:** Forward touch events from the `TextureView` in `EmbedFragment` to the embedded virtual display. Uses `IInputManager.injectInputEvent` (hidden API). Translates `MotionEvent` coordinates from the texture's local space to virtual-display space.

**AC:**
- [ ] Tapping a button in the embedded Maps app activates it correctly
- [ ] Multi-touch (pinch zoom) forwards both pointers
- [ ] Coordinate translation is exact within ±1 px
- [ ] Long-press is honored (not synthesized as multiple taps)
- [ ] Event injection uses `INJECT_INPUT_EVENT_MODE_ASYNC` to avoid blocking the UI thread

**Depends on:** T1.46

### T1.48 -- Audio focus passthrough | Cx: 8 | P2

**Description:** When an embedded app requests audio focus, route the request to the system `AudioManager` so the launcher's media tile reflects it. Use `AudioFocusRequest` (or pre-O equivalent) with `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`.

**AC:**
- [ ] Embedded Maps app's voice prompts ducks Spotify (verified manually)
- [ ] Launcher's media tile updates `isPlaying = false` during the duck
- [ ] Focus is released cleanly when the embedded app finishes (Spotify resumes)
- [ ] No audio leaks (verified with `adb shell dumpsys audio`)
- [ ] Works with Spotify, YouTube Music, and stock Music app

**Depends on:** T1.47

### T1.49 -- EmbedFragment integration with HomeFragment | Cx: 8 | P2

**Description:** Wire `EmbedFragment` into the right-panel ViewPager from T1.8. Settings exposes a list of "embeddable" apps (the user picks which app to embed). Default is the system Maps app. Page indicator shows the selected app's icon.

**AC:**
- [ ] User can pick an app to embed from a list of installed apps
- [ ] Selected app launches into the virtual display when the embed page is visible
- [ ] App is paused (no CPU work) when the embed page is hidden
- [ ] Switching the embedded app does not crash (clean teardown)
- [ ] Standard-flavor build shows an "Upgrade to system flavor" placeholder

**Depends on:** T1.48

---

*End of backlog.*
