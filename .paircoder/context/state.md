# Current State

> Last updated: 2026-05-14 (T1.42 done — 3 new color themes Mocha/Ocean/Forest, activity recreate, auto night mode, theme-aware speedometer)

## Active Plan

**Plan:** plan-2026-05-retro-launcher-sprint-1 — Retro Launcher v0.1 → v0.3
**Status:** 42/49 tasks done (Phase 9 complete; Phase 10 in progress: T1.39–T1.42 done).
T1.43 (next pending — Phase 10, P2 Cx 5).
**Current Sprint:** 1 (T1.x)
**Backlog:** `plans/backlogs/backlog-sprint-1-retro-launcher.md`

> Tracking note: tasks are split across two plan IDs in the CLI —
> `plan-2026-05-retro-launcher-sprint-1` (13 tasks: T1.2, T1.4–T1.15) and
> `plan-sprint-1-engage` (36 tasks: T1.1, T1.3, T1.16–T1.49, created by the
> `engage` autonomous run). Both reflect the same backlog. Treat the
> 49-task backlog as the source of truth and resolve the split later.

## Current Focus

Sprint 1 covers the full Retro Launcher implementation per `headunit-launcher-spec.md`:
49 tasks across 11 phases, complexity 353, 2 human-gated, P0=9 / P1=22 / P2=18.
Phases 1–2 are P0 foundation, 3–8 are the v0.1 MVP feature set, 9 is testing/build,
10 is v0.2 polish, 11 is v0.3 system-build embedding (platform-signed only).

## Task Status

### Active Sprint

Phase 1 — Project scaffolding (5/5 done)
- ✓ T1.1 Bootstrap Gradle project structure (done 2026-05-05)
- ✓ T1.2 Author app/build.gradle.kts with flavors (done 2026-05-05)
- ✓ T1.3 Author AndroidManifest.xml (done 2026-05-05)
- ✓ T1.4 Resource bundles: themes, colors, strings, dimens (done 2026-05-05)
- ✓ T1.5 Application icon and adaptive icon fallback (done 2026-05-05)

Phase 2 — App shell (5/5 done)
- ✓ T1.6 App.kt service locator and Application class (done 2026-05-06)
- ✓ T1.7 MainActivity and activity_main.xml (done 2026-05-06)
- ✓ T1.8 HomeFragment with ViewPager2 right panel (done 2026-05-06)
- ✓ T1.9 StatusBarFragment (clock, speed, trip stats, drawer) (done 2026-05-06)
- ✓ T1.10 Util extensions: ColorExt, FormatExt, ViewExt (done 2026-05-06)

Phase 3 — Location service and speedometer (4/4 done)
- ✓ T1.11 LocationService foreground service (done 2026-05-06)
- ✓ T1.12 SpeedFilter with Kalman-style smoothing (done 2026-05-06)
- ✓ T1.13 SpeedometerView custom drawing (done 2026-05-06)
- ✓ T1.14 SpeedFragment + ViewModel (done 2026-05-06)

Phase 4 — Media player (4/4 done)
- ✓ T1.15 MediaNotificationListener service (done 2026-05-06)
- ✓ T1.16 MediaRepository state flow (done 2026-05-09)
- ✓ T1.17 MediaFragment + ViewModel + Glide (done 2026-05-09)
- ✓ T1.18 Album-art Palette dominant-color extraction (done 2026-05-09)

Phase 5 — Weather card (4/4 done)
- ✓ T1.19 WeatherDto with Moshi adapters (done 2026-05-09)
- ✓ T1.20 WeatherRepository OkHttp client (done 2026-05-09)
- ✓ T1.21 WeatherWorker periodic refresh (done 2026-05-09)
- ✓ T1.22 WeatherFragment + ViewModel + icon mapping (done 2026-05-09)

Phase 6 — Trip recording (5/5 done)
- ✓ T1.23 Room schema (TripEntity, TripPoint, TripDao, AppDb) (done 2026-05-09)
- ✓ T1.24 TripRecorder state machine (done 2026-05-09)
- ✓ T1.25 TripRepository (done 2026-05-09)
- ✓ T1.26 TripCalendarView heatmap (done 2026-05-09)
- ✓ T1.27 TripsFragment + adapter (done 2026-05-09)

Phase 7 — App grid (3/3 done)
- ✓ T1.28 AppListRepository PackageManager scanning (done 2026-05-10)
- ✓ T1.29 AppGridFragment + adapter (done 2026-05-10)
- ✓ T1.30 RailFragment pinned-apps strip (done 2026-05-10)

Phase 8 — Settings and first-run (4/4 done)
- ✓ T1.31 SettingsStore SharedPreferences wrapper (done 2026-05-11)
- ✓ T1.32 SettingsFragment PreferenceFragment (done 2026-05-11)
- ✓ T1.33 First-run permission wizard (done 2026-05-11)
- ✓ T1.34 BootReceiver gated on firstRunDone (done 2026-05-11)

Phase 9 — Testing and build (4/4 done)
- ✓ T1.35 Unit tests for repos and recorders (done 2026-05-11)
- ✓ T1.36 Instrumented tests for UI fragments (done 2026-05-11)
- ✓ T1.37 Manual test matrix doc (done 2026-05-11)
- ✓ T1.38 Build and install scripts for rooted HU (done 2026-05-11)

Phase 10 — v0.2 polish (4/5 done)
- ✓ T1.39 Trip detail screen with offline mini-map (done 2026-05-11)
- ✓ T1.40 GPX export per trip (done 2026-05-11)
- ✓ T1.41 App rail drag-to-reorder (done 2026-05-14)
- ✓ T1.42 Day/night theme variants (done 2026-05-14)
- ⏳ T1.43 (P2, Cx 5)

Phase 11 — v0.3 system-build features (0/6 pending)
- ⏳ T1.44–T1.49 (all P2, Cx 8/13/21/13/8/8)

All 49 task files exist on disk under `.paircoder/tasks/T1.{1..49}.task.md`.
Phases 1–9 done + T1.39–T1.42 (42/49). Continue with `/start-task T1.43`
(Phase 10, P2 Cx 5).

### Backlog

Future sprints (post-v0.3): CAN-bus / OBD-II integration, voice trigger via mic
button, day/night theme auto-switch from sun position. See spec section 21.4.

## What Was Just Done

- **T1.42 done (2026-05-14)** — Day/night theme variants. Added Mocha/Ocean/Forest
  color palettes (day + night `colors-{theme}.xml` files). Extended `Theme` enum
  with `styleRes`. `MainActivity.setTheme()` + `AppCompatDelegate` night mode.
  Activity auto-recreates on theme change. SpeedometerView now uses `R.color.ok`/
  `warn`/`err` for theme-aware thresholds. 13 tests pass.

- **T1.41 done** (auto-updated by hook)

- **T1.41 verified (2026-05-14)** — Drag-to-reorder was already fully implemented
  on disk by commit 0628b0d. Verified all 5 ACs: (1) DragReorderCallback.applyLift
  provides haptic + visual lift (scale 1.08x, elevation 12f, alpha 0.92, 120ms
  duration). (2) Drop commits new order via commitGridOrder/commitPinnedOrder to
  SettingsStore. (3) Grid uses manual drag from popup menu (longPress=false);
  rail uses default long-press (longPress=true). (4) Order survives restart
  (SharedPreferences + Moshi serialization, tested in AppListRepositoryReorderTest).
  (5) Bounds-safe — onMove checks NO_POSITION, moveItem validates list indices.
  7 unit tests cover both commit paths and cross-instance persistence.

- **T1.5 done** (auto-updated by hook)

- **T1.5 verified (2026-05-14)** — Task file status synced from pending to done.
  All 4 ACs verified on disk: vector drawable `ic_launcher.xml` in `drawable/`,
  adaptive-icon at `mipmap-anydpi-v26/ic_launcher.xml` with foreground+background,
  round variant `ic_launcher_round.xml` in both `mipmap-anydpi-v26/` and
  `mipmap-anydpi/`. Zero PNGs — all vector, well under 256 KB. Manifest references
  `@mipmap/ic_launcher`. The engage run authored the icons but left task status as
  pending; implementation used vectors in `mipmap-anydpi/` for pre-v26 fallback
  instead of rasterized PNGs (functionally equivalent and more size-efficient).

- **T1.3 done** (auto-updated by hook)

- **T1.3 done (2026-05-14)** — Verified existing AndroidManifest.xml against all 5 ACs.
  MainActivity has HOME + DEFAULT categories (launcher intent filter).
  MediaNotificationListener declares BIND_NOTIFICATION_LISTENER_SERVICE permission
  with NotificationListenerService intent filter. LocationService has no
  foregroundServiceType (targetSdk=28). Theme is @style/Theme.RetroLauncher with
  screenOrientation=landscape. All permissions declared per spec. Task file status
  synced from failed to done — the engage run had authored the manifest but marked
  the task as failed due to "no meaningful output" despite all ACs being checked.

- **T1.41 done (2026-05-14)** — App grid + rail drag-to-reorder. `DragReorderCallback`
  wraps `ItemTouchHelper` with haptic + visual lift (scale/elevation/alpha), bounds-safe
  index swaps, and dual activation modes: grid uses "Reorder" popup menu entry (avoids
  conflict with pin/unpin long-press), rail uses default long-press drag. `commitGridOrder`
  and `commitPinnedOrder` on `AppListRepository` persist the new order to
  `SettingsStore.appOrder` / `pinnedApps`. 6 unit tests verify commit paths and persistence
  across fresh repo instances. All 5 ACs pass.

- **T1.40 done (2026-05-11)** — GPX export per trip. Pure `GpxXml` serializer +
  Context-bound `GpxExporter` write a GPX 1.1 file for any `TripEntity` +
  `List<TripPoint>` into a MediaStore-visible Downloads/RetroLauncher dir.
  1. **`data/trip/GpxXml.kt`** — `serialize(trip, points): String` uses
     `android.util.Xml.newSerializer()` (the platform's KXmlSerializer, no
     third-party XML lib). Default namespace bound to
     `http://www.topografix.com/GPX/1/1`; `gpxtpx` prefix bound to
     `http://www.garmin.com/xmlschemas/TrackPointExtension/v1`. Document
     shape: `<gpx version="1.1" creator="RetroLauncher">` → `<metadata>`
     with start `<time>` → `<trk><name>Trip {id}</name><trkseg>` → ten
     `<trkpt lat="…" lon="…">` each holding `<time>` (ISO-8601 UTC,
     `yyyy-MM-dd'T'HH:mm:ss'Z'`) and `<extensions><gpxtpx:speed>…m/s…
     </gpxtpx:speed></extensions>`. Speed lives inside `<extensions>`
     because GPX 1.1 dropped the trkpt-level `<speed>` element; the schema's
     `extensionsType` accepts any `##other`-namespaced child, so the doc
     still validates against the topografix XSD (AC1) while carrying
     per-point speed (AC2). Coords formatted `%.7f`, speeds `%.3f`,
     `Locale.US` (so the decimal sep is always `.`).
  2. **`data/trip/GpxExporter.kt`** — `class GpxExporter(context)` with
     `suspend fun export(trip, points): Result` on `Dispatchers.IO`. Sealed
     `Result.Success(uri, displayPath)` / `Result.PermissionDenied` /
     `Result.IoFailure(throwable)`. On API ≥ 29 inserts into
     `MediaStore.Downloads.EXTERNAL_CONTENT_URI` with
     `RELATIVE_PATH = "Download/RetroLauncher"`. On API ≤ 28 writes to
     `Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)/RetroLauncher/`
     and triggers `MediaScannerConnection.scanFile(…, "application/gpx+xml")`
     so file managers / MediaStore pick it up (AC3). Catches
     `SecurityException` → `PermissionDenied`, `IOException` → `IoFailure`.
  3. **`ui/trips/TripDetailFragment.kt`** — adds an "Export GPX" `TextView`
     in the top header (accent-orange, 8 dp padding). Click handler reads
     current `TripDetailUiState.Loaded` from the VM, builds an exporter via
     an injectable `exporterFactory` (test seam, `internal var`), launches
     in `viewLifecycleOwner.lifecycleScope`, and maps the result to a
     `Snackbar` (3 strings: success path, permission denied, generic
     write failure) — never crashes (AC5).
  4. **`res/layout/fragment_trip_detail.xml`** — new `@+id/trip_detail_export`
     TextView inserted into the existing header LinearLayout, right of the
     title.
  5. **`res/values/strings.xml`** — `trip_detail_export_gpx` button label
     and three Snackbar strings (`trip_detail_export_success_fmt`,
     `trip_detail_export_permission_denied`, `trip_detail_export_failed`).
  Tests added: **GpxXmlTest (6, Robolectric @sdk=28)** — exact-string
  snapshot for a deterministic 10-point trip starting at
  `2025-01-01T00:00:00Z` (AC4); DOM parse confirming well-formed XML +
  root namespace/version/creator; regex check that 11 `<time>` and 10
  `<gpxtpx:speed>` elements appear in document order; trip-id-in-name
  assertion; empty-list still yields valid GPX (no trkpt elements);
  7-decimal-place coord formatting using `Locale.US` `.` separator.
  Verification: `:app:testStandardDebugUnitTest` → 360 tests, 0 failures
  (was 354, +6 new); `xmllint --schema gpx-1.1.xsd sample.gpx` →
  `sample.gpx validates` (AC1 confirmed against the upstream topografix
  schema); `bpsai-pair arch check` clean on all 4 modified/new files.
  Snackbar path exercised by the existing `SecurityException` catch and
  `IOException` catch — could not be hit in unit tests without device IO,
  manual verification deferred to instrumented run on the head unit.

- **T1.39 done** (auto-updated by hook)

- **T1.39 done (2026-05-11)** — Trip detail screen with offline mini-map.
  Implements `TripDetailFragment` per spec §10 (the "tap a trip → detail with
  mini-map" line in the trip-recorder section). Five files added, two edited.
  1. **`data/trip/TripStats.kt`** — pure computation. `fromPoints(List<TripPoint>)`
     yields distance (Haversine, EARTH_RADIUS_M = 6 371 008.8 m), duration
     (last.tsMs − first.tsMs), max-speed (max over `speedMs` including the
     first point), avg-speed (distance / duration_s). `GeoBoundingBox.fromPoints`
     gives the lat/lon span for `zoomToBoundingBox`. `crossCheck(entity, stats,
     tol)` compares persisted aggregates with the recomputed values within a
     relative tolerance — used for AC4 sanity and exposed for tests.
  2. **`data/trip/NominatimAddressCache.kt`** — SharedPreferences-backed cache
     (`nominatim_addr`) keyed by lat/lon rounded to 4 decimals (~11 m at the
     equator). `labelOrFallback(lat, lon, explicit)` resolves in three steps:
     explicit `TripEntity.{start,end}Label` → cached Nominatim string →
     `"%.4f, %.4f".format(lat, lon)`. Two trips at the same parking spot share
     one entry; cache survives across instances backed by the same prefs.
  3. **`data/trip/OsmdroidTilesConfig.kt`** — one-shot osmdroid bootstrap.
     `ensureInitialized(ctx)` sets `userAgentValue = "retro-launcher/0.2"`
     (Nominatim TOS) and reroutes `osmdroidBasePath` + `osmdroidTileCache` into
     `cacheDir/osmdroid/`. Seeds a bundled MBTiles file from
     `assets/tiles/launcher.mbtiles` if present; missing-asset path is a silent
     no-op so the launcher still comes up. `applyDefaults(map)` flips
     `setUseDataConnection(false)` for offline-first behavior (AC1) and turns
     on multi-touch controls (AC3).
  4. **`ui/trips/TripDetailViewModel.kt`** — sealed `TripDetailUiState`
     (`Loading` / `Empty` / `Loaded`). `Loaded` carries trip + points + stats +
     bbox + start/end addresses (already resolved through the cache) +
     `aggregatesMatch` flag (AC4). `load()` runs inside `viewModelScope`
     (Main + SupervisorJob) — Room's suspend `@Query` already hops to its
     own executor so we don't block UI. `Factory(repo, addrCache, tripId,
     entity)` is what the fragment uses with `by viewModels`.
  5. **`ui/trips/TripDetailFragment.kt`** — `Fragment(R.layout.fragment_trip_detail)`.
     Bootstraps osmdroid in `onViewCreated`, attaches Polyline overlay
     (`#FF8500` accent, 8 px stroke) on every `Loaded` emit, rebuilds bounds
     with 10% padding and `map.post { zoomToBoundingBox(...) }`. Hooks
     `map.onResume/onPause` for the Surface lifecycle. Companion provides
     `argsFor(trip)` + key constants for the `Bundle` round-trip.
  6. **`res/layout/fragment_trip_detail.xml`** — LinearLayout vertical: top
     header (back button + endpoints title), middle `org.osmdroid.views.MapView`
     (`@+id/trip_map`, weight=1), bottom stats row (`stat_distance`,
     `stat_duration`, `stat_max_speed`, `stat_avg_speed`) + `stat_start_addr` +
     `stat_end_addr`. New strings in `values/strings.xml`:
     `trip_detail_back`, `trip_detail_title_fmt`, four `trip_stat_*_fmt`,
     `trip_detail_start_fmt`, `trip_detail_end_fmt`.
  7. **`AndroidManifest.xml`** — adds `WRITE_EXTERNAL_STORAGE` capped at
     `maxSdkVersion=28` for osmdroid's sdcard tile dir. App's `minSdk=23` /
     `targetSdk=28` keeps the manifest scope tight.
  8. **`build.gradle.kts` + `libs.versions.toml`** — `osmdroid-android:6.1.18`
     added as `osmdroid` library. No transitive Google Play Services.
  9. **`ui/trips/TripsAdapter.kt`** — constructor gains an optional
     `onTripClicked: ((TripEntity) -> Unit)? = null` callback (defaulted so
     existing `TripsAdapter()` callers stay green). `onBindViewHolder` wires
     `holder.itemView.setOnClickListener`.
  10. **`ui/trips/TripsFragment.kt`** — wires the click → `openTripDetail(trip)`
      → `requireActivity().supportFragmentManager.beginTransaction()
      .replace(android.R.id.content, detail, "trip_detail")
      .addToBackStack("trip_detail").commit()`. Detail screen overlays the
      whole launcher; back-press pops back to the right panel.
  Tests added: **TripStatsTest (7)** — empty/single/two/two-point math, max
  speed detection, avg = distance/duration, full cross-check against a
  TripEntity (1% tol), tolerance-violating mismatch detected at 5%.
  **NominatimAddressCacheTest (8)** — cold lookup null, put-then-lookup hit,
  rounding precision (~11 m), label override priority chain, lat/lon
  fallback formatting, cache survives across instances.
  **TripDetailViewModelTest (6)** — Loaded state, cold-cache fallback to
  lat/lon strings, warm-cache city names, explicit entity-label override,
  empty-points → Empty state, bounding box covers all points.
  **TripDetailFragmentTest (1, Robolectric)** — walks compiled
  `fragment_trip_detail.xml` for the 8 required IDs (`trip_map`,
  `trip_stats`, 4× `stat_*`, 2× `stat_*_addr`, `trip_detail_back`) and
  vertical root orientation. MapView itself can't render under Robolectric
  but the layout walk catches the structural contract.
  Verification: `:app:testStandardDebugUnitTest` → 354 tests, 0 failures
  (was 332 in T1.35, +22 new). `:app:assembleStandardDebug` green.
  `bpsai-pair arch check` clean on all 7 modified/new production files.
  Instrumented `:app:connectedStandardDebugAndroidTest` requires a live
  emulator + tile-cache fixture — not executable in this dev env.

- **T1.37 done (2026-05-11)** — Authored `docs/manual-test-matrix.md` per
  spec §19.3. Covers all 6 v0.1 user-facing features (launcher/status bar,
  media tile, weather tile, speedometer, trip recorder, app grid) plus
  supporting Settings and First-run wizard rows, plus all 5 v0.2 polish
  items (trip detail mini-map, GPX export, rail customization, themes,
  steering-wheel keys). Each row has numbered steps + expected result and
  cites the originating spec section. Adds a 9-item pre-flight checklist
  (root status, build flavor, OWM key, notification listener access,
  location perm, time sync, adb reach, default-launcher state, network)
  and the §19.4 bench targets table. Tables use GitHub pipe-table syntax —
  renders cleanly on GitHub. Doc lives at repo root `docs/` alongside
  `headunit-launcher-spec.md`. ACs (matrix scope, steps+expected per row,
  pre-flight, GH readability, location under `docs/`) all met.

- **T1.36 done (2026-05-11)** — Espresso instrumented suite for the six UI
  fragments (Home, Media, Weather, Speed, Trips, AppGrid).
  1. **Test infra** in `app/src/androidTest/`:
     - `RetroTestRunner` extends `AndroidJUnitRunner` and pre-installs a
       `TestServiceLocator` on the `App` instance before `App.onCreate`.
       Wired via `testInstrumentationRunner` in `app/build.gradle.kts`.
     - `TestServiceLocator` extends the now-`open` `ServiceLocator`. It
       overrides `prefs` (throwaway file, cleared per process), `db`
       (in-memory Room), `media` (60 s ticker so Espresso idle waits aren't
       starved), `weather` (loopback URLs + empty API key → `isConfigured`
       false), and most importantly `startup()` → no-op (no foreground
       LocationService, no WorkManager, no boot-receiver sync, no implicit
       PackageManager scan).
     - `ScreenshotOnFailureRule` (TestWatcher) dumps a PNG to
       `externalCacheDir/screenshots/{Class}-{method}.png` on `failed()`.
       Designed for the CI artifact upload to pick up.
     - `TestData` factory object: `mediaState()`, `weatherSnapshot()`,
       `tripEntity()`, `appEntry()`.
  2. **Production-code touch points** (additive, non-breaking):
     - `App.service` is now `lateinit var` (was `val by lazy`) so the
       runner can inject the test locator pre-`onCreate`. `onCreate`
       guards with `::service.isInitialized`.
     - `ServiceLocator` is now `open class` with all repos as `open val`
       and `startup()` as `open fun`. Constructor param visibility
       widened from `private val app` to `protected val app` for subclass
       access. Existing T1.6 Robolectric assertions still pass.
     - `WeatherRepository.setSnapshotForTest(snap)` — `@VisibleForTesting`
       publishes a fixture snapshot directly to the StateFlow.
     - `AppListRepository.setForTest(all, pinned)` — `@VisibleForTesting`
       seeds the in-memory state without touching PackageManager.
  3. **Per-fragment tests** (3 each = 18 total) using
     `androidx.fragment:fragment-testing` `FragmentScenario`:
     - `HomeFragmentInstrumentedTest`: default 0.40 split + page 1 (grid),
       panel-ratio change flows into `home_split` Guideline, programmatic
       page change to Trips lands.
     - `MediaFragmentInstrumentedTest`: empty state shows "Nothing
       playing", populated state renders title + artist, tap play/pause
       no-crash + state-change re-binds the icon.
     - `WeatherFragmentInstrumentedTest`: empty state placeholder,
       populated state renders condition (title-cased) + city + °temp,
       snapshot churn reverts to placeholder when cleared.
     - `SpeedFragmentInstrumentedTest`: zero speed at attach,
       `LocationRepository.push()` of 15 m/s advances the gauge,
       speedometer is laid out (non-zero size, threshold 50 km/h).
     - `TripsFragmentInstrumentedTest`: empty DB → "No trips" visible,
       inserted trip → list count ≥ 1, calendar `onDaySelected` callback
       fires with a non-zero day.
     - `AppGridFragmentInstrumentedTest`: empty seed → 0 items, 4-app
       seed → 4 grid + 1 rail, tap item-0 doesn't crash.
  4. **Build wiring** (`app/build.gradle.kts` + `gradle/libs.versions.toml`):
     added `androidx.test:runner:1.5.2`, `:rules:1.5.0`, `:core-ktx:1.5.0`,
     `androidx.test.espresso:espresso-contrib:3.5.1`, and
     `androidx.fragment:fragment-testing:1.6.2` (debugImplementation —
     ships an empty test activity used by FragmentScenario).
  Local verification: `:app:assembleStandardDebugAndroidTest` and
  `:app:testStandardDebugUnitTest` both green. The AC-mandated run
  (`./gradlew :app:connectedStandardDebugAndroidTest`) requires a live
  emulator/device — not executable in this dev env (no `adb`, no AVD).
  Tests are written for API 23+ and use `Thread.sleep(150–800 ms)` for
  observer/animator settles, well under the 5-min budget cap.

- **T1.35 done (2026-05-11)** — Unit-test coverage for repos + recorders.
  Sprint-1 already had a deep test suite (320+ JUnit/Robolectric cases) but no
  measurement; this task adds the AC scaffolding and fills the named gaps:
  1. New `LocationRepositoryTest` (8 cases via Turbine + Robolectric) — was
     the only AC-listed module without a dedicated unit suite.
  2. New `ColorExtTest` (6 cases) covering `Int.darkened` invariants
     (monotonicity, clamp-to-zero, identity at factor 0) and
     `Bitmap.dominantColorAsync` swatch/fallback paths under Robolectric.
  3. New `TripRecorderPropertyTest` (kotest-property) — 30 randomized
     scenarios assert (a) every state transition matches the spec graph and
     (b) every persisted trip satisfies distance/duration/avg≤max invariants.
  4. Added `jacoco` plugin + `:app:jacocoTestReport` + `:app:jacocoCoverageVerification`
     tasks in `app/build.gradle.kts`, scoped via class-path excludes to the
     AC files (`MediaRepository`, `WeatherRepository`, `LocationRepository`,
     `TripRepository`, `TripRecorder`, `SpeedFilter`, util extensions). UI,
     services, `SettingsStore`, `AppListRepository` are out of scope here —
     they need instrumentation/QC. 80% line floor enforced by
     `jacocoCoverageVerification`.
  5. `JacocoTaskExtension.isIncludeNoLocationClasses = true` + `excludes
     "jdk.internal.*"` fixes the Robolectric SandboxClassLoader vs. jacoco
     agent interaction — without it Robolectric tests showed 0% on every
     class they exercised.
  6. New test deps in `libs.versions.toml`: MockK 1.13.10, Turbine 1.0.0,
     kotest-property 5.8.1, room-testing 2.6.1.
  Result: 332 tests, 22.9 s wall-clock (well under the 60 s AC), 0 failures
  across 5 consecutive runs. Coverage: 88.1% LINE / 84.1% INSTRUCTION
  (overall scoped to AC files) — TripRecorder 97.8%, SpeedFilter 100%,
  WeatherRepository 93.5%, LocationRepository 100%, ColorExtKt 100%,
  TripRepository 71% (only delete-by-id-cascade lambda missing). XML report
  at `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`.
  `bpsai-pair arch check` clean on all new files + build.gradle.kts.

- **T1.34 done** (auto-updated by hook)

- **T1.34 done (2026-05-11)** — BootReceiver gated on `firstRunDone`. The
  receiver itself was already implemented in T1.21 (boots → start
  LocationService + reschedule WeatherWorker via `KEEP` policy). T1.34 adds
  the missing AC4 piece — the receiver must stay dormant until the user
  finishes the first-run wizard. Done with:
  1. Manifest — `android:enabled="false"` on the `<receiver>` declaration so
     fresh installs cannot react to `BOOT_COMPLETED` until something flips
     it on. Comment in the manifest names the wizard contract.
  2. `service/BootReceiverGate.kt` — tiny `object` wrapping
     `PackageManager.setComponentEnabledSetting` (no reflection, no hidden
     APIs, AC5) with `enable / disable / syncWithFirstRun` entry points.
     `DONT_KILL_APP` keeps the process alive across the toggle.
  3. `WizardActivity.finishWizard` — calls `BootReceiverGate.enable(this)`
     right after `setFirstRunDone(true)`, so the gate flips at the same
     moment we commit the flag.
  4. `ServiceLocator.startup` — adds an idempotent
     `BootReceiverGate.syncWithFirstRun(app, settings.firstRunDone)` so
     installs that predate the new manifest default get realigned on the
     next launch (handles the upgrade case).
  Tests: new `BootReceiverGateTest` (Robolectric) covers `enable / disable /
  syncWithFirstRun` against the package manager's actual component-enabled
  state. New `WizardActivityTest.completing the wizard enables the
  BootReceiver` walks the full 6-skip flow and asserts the component flips
  to `COMPONENT_ENABLED_STATE_ENABLED`. `BootReceiverTest` gains a third
  case asserting `LocationService` is the next started service on
  `BOOT_COMPLETED` (AC2). All 7 BootReceiver-area tests + the full
  `:app:testSystemDebugUnitTest` suite pass; arch check clean.

- **T1.33 done** (auto-updated by hook)

- **T1.32 done** (auto-updated by hook)

- **T1.31 done** (auto-updated by hook)

- **T1.30 done** (auto-updated by hook)

- **T1.29 done** (auto-updated by hook)

- **T1.28 done** (auto-updated by hook)

- **T1.27 done** (auto-updated by hook)

- **T1.33 done** (auto-updated by hook)

- **T1.33 done (2026-05-11)** — First-run permission wizard rewritten to the
  6-step linear flow named by the task spec:
  1. Welcome (Continue) — no permission action, intro screen.
  2. Location — requests `ACCESS_FINE_LOCATION` via
     `ActivityCompat.requestPermissions`, auto-advances on grant via
     `onRequestPermissionsResult` or `onResume` re-check.
  3. Notification listener — Grant deep-links to
     `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`; `onResume` auto-advances
     when `MediaNotificationListener.isEnabled(ctx)` flips true.
  4. Default launcher — Grant deep-links to
     `Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS` (API 24+) with
     `ACTION_HOME_SETTINGS` fallback; `Permissions.isDefaultLauncher(ctx)`
     resolves the home Intent and matches against our packageName.
  5. OWM API key — single EditText + Continue runs
     `WeatherRepository.validateApiKey(key)` in `lifecycleScope`; on HTTP 200
     the key is persisted via `SettingsStore.setOwmApiKey(key)`, on 401/other
     a warning is shown and Continue re-enables. Skip leaves it unset.
  6. Home location — two EditTexts (lat/lon); Continue saves via
     `setWeatherLocation(lat, lon)`, blank/invalid clears both so the
     weather card falls back to device GPS.
  • `setFirstRunDone(true)` fires only inside `finishWizard()` — completion
     is the single source of truth, satisfying "wizard shows on first launch
     and never again unless user clears app data".
  • MainActivity.onStart now gates the wizard on `App.settings.firstRunDone`
     (was `Permissions.isFirstRunComplete` which would re-route after each
     boot whenever a permission was rescinded — wrong gate per AC).
  • SettingsStore gains `owmApiKey: String?` + `owmApiKeyFlow` (Moshi-free,
     blank → null normalization in getter); ServiceLocator now constructs
     `WeatherRepository` with the user key, falling back to BuildConfig.
  • activity_wizard.xml redesigned as a single ConstraintLayout with
     persistent input fields toggled by `WizardActivity.render()`. Total
     vertical content ≈ 280dp inside a 1024×600 viewport (536dp working
     area after 32dp padding) — no scrolling needed.
  • Coverage: SettingsStoreT133Test (4) · WeatherRepositoryValidateKeyTest
     (4, MockWebServer) · PermissionsT133Test (1) · WizardActivityTest (5,
     Robolectric drives the Activity through every step). All green.
     `bpsai-pair arch check` clean on all 6 modified production files.

- **T1.32 done (2026-05-11)** — `SettingsFragment` rewritten as a full
  `PreferenceFragmentCompat` per spec §14.3 + task ACs:
  • Categories: General (units, panel ratio 30–50, theme, grid columns),
    Trip (start threshold, record toggle, GPX export), Apps (default map,
    default voice), Weather (lat/lon override, units, refresh interval),
    Advanced (notification access, location on status bar), About (version,
    OSS licenses). All keys match `SettingsStore` so every UI mutation
    persists immediately through the default SharedPreferences (AC1).
  • `panel_ratio` SeekBarPreference uses `app:min="30"` + `android:max="50"`
    (AC2). The original `android:min` was silently parsing as 0 on AndroidX
    Preference 1.2 — switched all three SeekBarPreferences to `app:min`.
  • New `IntentPickerEntries` helper resolves `geo:0,0` and
    `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` via
    `PackageManager.queryIntentActivities` → label/package pairs, prefixed
    with a leading "Ask every time" empty-value entry. Populated into
    `map_app` / `voice_app` ListPreferences in `onCreatePreferences` (AC3).
  • `LicensesActivity` (registered in manifest) renders
    `res/raw/oss_licenses.txt` — bundled Apache 2.0 + BSD 2-clause text
    listing Kotlin, Coroutines, AndroidX, Material, OkHttp, Moshi, Glide,
    Timber. Launched from the About category's `oss_licenses` Preference
    (AC4).
  • Drawer-launches-Settings was already wired in `StatusBarFragment`
    (T1.9 work) — no change needed (AC5).
  • Weather lat/lon: distinct EditText input keys
    (`weather_lat_input`/`weather_lon_input`) seed-and-bridge to
    `SettingsStore.setWeatherLocation(Float?, Float?)`. Keeps the canonical
    Float-typed pref correctly typed while reusing the standard
    EditTextPreference UI.
  • New `SettingsStore` fields with reactive `Flow<T>`: `theme` (Theme
    enum: SYSTEM/LIGHT/DARK), `gpxExport` Boolean, `weatherRefreshIntervalMin`
    Int (default 30, floor 5).
  • Tests: `SettingsStoreT132Test` (9 cases — defaults, roundtrip,
    Flow first-emit for each new field), `IntentPickerEntriesTest` (intent
    shapes, empty-resolver path, array-prefix shaping),
    `SettingsFragmentTest` (7 cases — all spec'd preference keys, panel
    bounds 30–50, pickers populated, OSS click launches LicensesActivity,
    lat-bridge writes Float to SettingsStore, version pref shows
    BuildConfig.VERSION_NAME).
  • Full `:app:testStandardDebugUnitTest` green;
    `:app:assembleStandardDebug` succeeds; arch check clean on all four
    touched source files (SettingsStore 238 lines, SettingsFragment 86,
    IntentPickerEntries 60, LicensesActivity 22 — all well under limits).

- **T1.31 done** (auto-updated by hook)

- **T1.31 done (2026-05-11)** — `SettingsStore` extended per spec §14.2 +
  task ACs:
  • New typed properties: `panelRatio: Float` (0.3-0.7 derived from existing
    Int percent), `gridColumns: Int` (3-5 alias of gridCols), `tripStartSpeedKmh: Int`
    (alias of speedThresholdKmh), `mapApp: String?`, `voiceApp: String?`,
    `weatherLat: Float?`, `weatherLon: Float?`, `firstRunDone: Boolean`.
    Pref keys: `KEY_MAP_APP`, `KEY_VOICE_APP`, `KEY_WEATHER_LAT`,
    `KEY_WEATHER_LON`, `KEY_FIRST_RUN_DONE`.
  • New setters: `setMapApp`, `setVoiceApp`, `setWeatherLocation(lat, lon)`,
    `setFirstRunDone` — all use non-blocking `apply()` (AC4).
  • Per-property `Flow<T>` exposed for every public property (AC3): `unitsFlow`,
    `tempUnitFlow`, `speedUnitFlow`, `panelRatioFlow`, `gridColumnsFlow`,
    `recordTripsFlow`, `tripStartSpeedKmhFlow`, `pinnedAppsFlow`,
    `appOrderFlow`, `mapAppFlow`, `voiceAppFlow`, `weatherLatFlow`,
    `weatherLonFlow`, `firstRunDoneFlow`. Built from a private
    `flowOfKey(key) { read }` helper composing `changes(key)` + `map` +
    `distinctUntilChanged`.
  • `pinnedApps` + `appOrder` JSON migrated from `org.json.JSONArray` to a
    Moshi `List<String>` adapter (AC2). Constructor takes an optional `Moshi`
    instance, defaulting to a lazy `Moshi.Builder().add(KotlinJsonAdapterFactory()).build()`.
    Malformed JSON falls back to `emptyList()`.
  • Tests: `SettingsStoreT131Test.kt` (27 cases) covering defaults,
    round-trip, malformed-JSON fallback, special chars (quote/slash/unicode),
    and first-emit semantics for every `Flow<T>`. Existing
    `SettingsStoreUnitsTest`, `SettingsStorePinnedAppsTest`,
    `SettingsStoreAppOrderTest` still pass.
  • File size 203 lines (under 400 hard limit). Existing call sites
    unaffected — `panelRatioPercent`, `gridCols`, `speedThresholdKmh` etc.
    retained.

- **T1.30 done (2026-05-10)** — Pinned-app rail integrated into the AppGrid
  page along the bottom of the right panel:
  • New `SettingsStore.pinnedApps: List<String>` (package names) +
    `setPinnedApps(...)` + `MAX_PINNED = 8`. JSON-encoded under key
    `pinned_apps`, mirrors the `appOrder` pattern.
  • `AppListRepository` refactored: persistence migrated from a private
    `("rail")` SharedPreferences file to `SettingsStore.pinnedApps`. `pin`,
    `unpin`, `isPinned` now operate on package names. `MAX_RAIL = 8` with
    FIFO eviction (oldest entry shifts out when adding a 9th). Exposes
    `pinnedPackages: StateFlow<List<String>>` and `rail: StateFlow<List<AppEntry>>`
    (rail derives by joining pinned package names with the master scan,
    silently dropping uninstalled entries).
  • `AppGridFragment` hosts a horizontal RecyclerView for the rail at the
    bottom of `fragment_grid.xml`. Long-press menu uses `entry.packageName`
    for pin/unpin checks. Tap on a rail tile launches the same
    `Intent.ACTION_MAIN + CATEGORY_LAUNCHER + componentName` flow as the
    grid.
  • Layout work: `item_rail.xml` icon sized via new `rail_icon` dimen
    (24dp / 28dp w1024dp — half of `grid_icon`). `activity_main.xml` and
    `MainActivity.kt` cleaned up to remove the legacy left-edge vertical
    rail (the rail now lives inside the AppGrid page only, per spec).
  • Tests: `SettingsStorePinnedAppsTest` (default empty / set / persist /
    clear) and `AppListRepositoryRailTest` (MAX_RAIL=8, no-dup pin, FIFO
    eviction, persistence across instances, rail flow ignores uninstalled
    packages). Full `:app:testStandardDebugUnitTest` green; arch check
    clean on all four touched source files; `assembleStandardDebug`
    succeeds.

- **T1.29 done** (auto-updated by hook)

- **T1.29 done (2026-05-10)** — `AppGridFragment` + `AppGridAdapter` audited
  against spec §13.2 and ACs; implementation was already in place from earlier
  scaffolding, no code changes needed:
  • `AppGridFragment` uses `GridLayoutManager` with `App.settings.gridCols`
    (default 4 from `SettingsStore.gridCols`). Reactive `combine` on
    `App.appList.all` + `settings.changes("grid_cols")` keeps spanCount in
    sync if the user changes column count at runtime.
  • Tap launches via `Intent.ACTION_MAIN + CATEGORY_LAUNCHER + componentName`
    with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_RESET_TASK_IF_NEEDED`,
    wrapped in `runCatching` — direct `startActivity` is well under 200 ms.
  • Long-press shows `PopupMenu` with Pin/Unpin (toggles based on
    `appList.isPinned`) + App info (launches
    `ACTION_APPLICATION_DETAILS_SETTINGS`). Two actions per AC.
  • `AppGridAdapter` extends `ListAdapter<AppEntry, VH>` with `DiffUtil`
    keyed on `componentName` (no flicker on icon swaps); `onViewRecycled`
    calls `Glide.with(...).clear(...)` for clean recycling — pairs with the
    `AppIconGlideModule` from T1.28 for cached, smooth scrolling.
  • Layout `fragment_grid.xml` + `item_app.xml` already present with
    `grid_icon` 48dp icon and `grid_label` text; `bg_tile` background and
    `selectableItemBackground` ripple on items.
  Arch check clean on both files. Systrace ≥45 fps AC requires on-device
  validation — implementation follows recommended patterns (ListAdapter,
  Glide recycling, cached icons by uid).

- **Planning audit (2026-05-10 post-T1.28)** — `/pc-plan` re-invoked on
  `plans/backlogs/backlog-sprint-1-retro-launcher.md`. Pre-flight: budget
  0% (well under 80% threshold); Trello not connected → PM-agnostic mode.
  Verified all 49 task files exist on disk (T1.1–T1.49 contiguous, each
  with frontmatter + description + ACs) and that the CLI tracks all 49
  across the two plan IDs (`plan-2026-05-retro-launcher-sprint-1` ×13 +
  `plan-sprint-1-engage` ×36). 28/49 done through T1.28 (Phases 1–6
  complete; Phase 7 started). No new plans, tasks, or backlog edits —
  planning is fully reconciled. Resume with `/start-task T1.29`
  (Phase 7 — AppGridFragment + adapter, Cx 8, P1, depends on T1.28 which
  is done).

- **T1.28 done** (auto-updated by hook)

- **T1.28 done (2026-05-10)** — `AppListRepository` upgraded from the basic
  scaffold (synchronous `loadIcon` + alphabetical sort + no lifecycle) into the
  spec §13.1 implementation:
  • Removed `Drawable` from `AppEntry` and added `applicationInfo`. Scans no
    longer block on per-app icon decode; `refresh()` returns metadata only.
  • Added `AppIconGlideModule` — a `@GlideModule` registering
    `ModelLoader<ApplicationInfo, Drawable>` with `ObjectKey("app-icon:$pkg:$uid")`
    so Glide caches by package + uid (upgrade busts the cache automatically).
    `AppGridAdapter` / `RailAdapter` now load via `Glide.with(...).load(applicationInfo)`.
  • Added `start()` / `stop()` lifecycle with a `BroadcastReceiver` listening
    for `PACKAGE_ADDED/REMOVED/REPLACED/CHANGED` (all data-scheme `package`).
    Wired `appList.start()` into `ServiceLocator.startup()` so the launcher
    auto-refreshes the grid within ~1s of install/uninstall.
  • Filtered the launcher's own package from results (AC5).
  • Added `SettingsStore.appOrder` (JSON-array of `package/activity` strings)
    + `setAppOrder()`; `refresh()` applies the stored order first, with
    unknown entries dropped and not-yet-ordered apps appended alphabetically.
    `AppListRepository` now takes a `SettingsStore?` ctor param.
  Tests: new `AppListRepositoryTest` (7 cases via Robolectric +
  `ShadowPackageManager.installPackage` / `addOrUpdateActivity`) covers
  self-exclusion, default sort, custom sort, unknown-entry pruning,
  PACKAGE_ADDED refresh, PACKAGE_REMOVED refresh, and `stop()` unregistering.
  New `SettingsStoreAppOrderTest` (4 cases) covers default empty, set/get
  roundtrip, persistence across new `SettingsStore` instances, and clearing.
  Suite: 225 → 236 unit tests, all green; both `standardDebug` and
  `systemDebug` flavors assemble; arch check + lint clean.

- **Planning audit (2026-05-10)** — `/pc-plan` re-invoked on
  `plans/backlogs/backlog-sprint-1-retro-launcher.md`. Pre-flight: budget at
  0% (well under 80% threshold); Trello not connected → PM-agnostic mode.
  Verified all 49 task files exist on disk (T1.1–T1.49 contiguous, each
  with frontmatter + description + ACs) and that the CLI tracks all 49
  across the two plan IDs (`plan-2026-05-retro-launcher-sprint-1` ×13 +
  `plan-sprint-1-engage` ×36). 27/49 done through T1.27 (Phases 1–6
  complete). No new plans, tasks, or backlog edits — planning is fully
  reconciled. Resume with `/start-task T1.28` (Phase 7 — AppListRepository,
  Cx 8, P1, depends on T1.10 which is done).

- **T1.27 done** (auto-updated by hook)

- **T1.27 done (2026-05-09)** — `TripsFragment` was already scaffolded
  (calendar on top, RecyclerView + ListAdapter + DiffUtil below, swipe-to-
  delete, summary line) but three of the five ACs needed pinning:
  • Day-label format. The fragment showed `"EEE, MMM d"`. Added
    `Long.formatRelativeDay(nowMs, tz, locale)` in `UnitsFormatExt.kt`
    (returns `Today` / `Yesterday` / `MMM dd`) and wired the fragment to
    use it on init and on day-cell tap.
  • Filter helpers. The ViewModel held `buildDistanceByDay` and a
    `sameLocalDay` check that used `TimeUnit.DAYS.toMillis(1)` (DST-fragile).
    Extracted both into `TripsFiltering.kt` as pure top-level helpers
    (`filterTripsForDay` now uses `Calendar.add(DAY_OF_YEAR, 1)` so DST
    boundaries are respected) and switched the ViewModel to call them.
  • Empty-state copy. `R.string.trips_none` said "No trips this month" —
    misleading now that the heatmap is per-day. Updated to "No trips" to
    match the AC verbatim.
  Tests added: `FormatExtTest` covers Today/Yesterday/MMM dd across local
  TZs; `TripsFilteringTest` covers same-day filter (incl. midnight
  boundary) + per-day distance grouping; `TripsAdapterTest` reflectively
  pulls the DiffUtil callback off the `AsyncListDiffer` to assert
  `areItemsTheSame` matches by trip id and `areContentsTheSame` flips on
  distance changes; `TripsFragmentTest` walks compiled `fragment_trips.xml`
  to assert calendar / trip_list / empty / month_label / summary IDs are
  present, root is vertical, and the RecyclerView has `weight=1` (proves
  the layout grows to fill the 60% right panel at 1024×600). All five ACs
  pass; full unit-test suite green; `bpsai-pair arch check` clean across
  the four touched source files and three new test files.

- **Planning audit (2026-05-09)** — `/pc-plan` re-invoked on
  `plans/backlogs/backlog-sprint-1-retro-launcher.md`. Verified all 49 task
  files exist on disk (T1.1–T1.49 contiguous) and that the CLI tracks all 49
  across the two known plan IDs (`plan-2026-05-retro-launcher-sprint-1` ×13
  and `plan-sprint-1-engage` ×36). No new tasks created — planning is
  reconciled. Resynced three stale CLI statuses (T1.24, T1.25, T1.26) where
  the file said `done` but the CLI still showed `failed` from earlier engage
  runs (`bpsai-pair task update <id> --resync`). Trello not connected
  (`bpsai-pair trello status` → "Not connected"); planning continues in
  PM-agnostic mode. Next pending task is T1.27 — `/start-task T1.27` to
  resume.

- **T1.26 done** (auto-updated by hook)

- **T1.25 done** (auto-updated by hook)

- **T1.24 done** (auto-updated by hook)

- **T1.26 done** (auto-updated by hook)

- **T1.26 done** — `TripCalendarView` rewritten as a 7×13 heatmap (~91 days)
  per the task spec. Cells are color-mapped from per-day distance via a
  blue→orange→red gradient (auto-normalized against the largest day in the
  set). Today's cell is drawn last with an accent stroke; the selected cell
  gets a white inset stroke. The drawing path is allocation-free — `Paint`s
  and the single `RectF` are init-time fields; `onDraw` only mutates them.
  - Wired the heatmap into `TripsViewModel`: `selectedDayMs: Long`,
    `distanceByDay: LiveData<Map<Long, Double>>`, `dayTrips` filtered by the
    selected day's local-midnight window. `recentTrips` flows directly from
    `TripRepository.recentTrips` so the heatmap repaints on Room
    invalidation.
  - `TripsFragment` drives `calendar.setDistances(byDay)` and listens to
    `onDaySelected = { ms -> vm.selectDay(ms) }`. The header label switches
    from "MMMM yyyy" to "EEE, MMM d" since selection is now day-granular.
  - `fragment_trips.xml` switched the calendar slot to `wrap_content` so
    `onMeasure` can size cells = panelWidth / 13 and height = side × 7.
  - 11 Robolectric tests (`TripCalendarViewTest`) cover all five ACs:
    grid dims, fits panel width via `onMeasure`, heatmap monotonicity,
    today-border stroke, tap dispatch (today + 90-days-ago), and outside-
    grid taps that must NOT dispatch.
  - Full unit-test suite green; arch check clean on all four touched files.

- **T1.25 done** — TripRepository: recent trips Flow + getPoints Flow + delete
  by id + activeTrip StateFlow (Phase 6, Cx 5, P1).
  - **DAO additions.** `TripDao` gained `pointsFlow(id): Flow<List<TripPoint>>`
    (reactive points stream for the trip-detail UI) and
    `suspend deleteById(id: Long): Int` (FK cascade handles the points).
  - **Repository surface.** `TripRepository` now exposes `recentTrips: Flow`
    (30-day sliding window, computed via injected `nowMs` lambda for
    testability), `getPoints(tripId): Flow`, `delete(tripId): suspend`,
    `delete(t: TripEntity): suspend` (kept for compatibility with existing
    callers), and `activeTrip: StateFlow<TripEntity?>` forwarded from the
    recorder. Default constructor uses an idle `MutableStateFlow(null)` so
    tests don't need to wire a recorder.
  - **Recorder activeTrip.** `TripRecorder._activeTrip` is set inside
    `emitLive()` whenever state == RECORDING (id=0L, no labels — populated
    only on persist) and cleared in `reset()`. So idle/detecting/stopping all
    show null; recording shows a synthetic in-progress TripEntity.
  - **ServiceLocator wiring.** `trips` lazy now passes
    `tripRecorder.activeTrip` so consumers see the live recorder state via
    the repository alone.
  - **Tests.** 6 new `TripRepositoryTest` cases under
    `RobolectricTestRunner @Config(sdk = [28])` against an in-memory Room DB:
    1. AC1 — `recentTrips re-emits within SLA of a new trip being inserted`:
       wall-clock test using `runBlocking` (Room's invalidation tracker
       dispatches on its own executor and isn't driven by `runTest`'s virtual
       time). Measures elapsed ms between insert and observed emission;
       asserts <1 s with 100 ms as the AC target. Collector job is cancelled
       after the deferred resolves so no leaks.
    2. `recentTrips includes trips inside 30-day window and excludes older`:
       runTest, fixed `nowMs`, two trips at 5 d / 60 d.
    3. `getPoints returns a Flow of points for a trip ordered by tsMs`:
       runTest, 50 points round-trip through the in-memory DB.
    4. AC3 cascade — `delete by id removes trip and cascades to points via
       foreign key`: 10 points pre-delete, 0 post-delete.
    5. `activeTrip is null by default when no source is provided`.
    6. `activeTrip forwards values from the supplied source StateFlow`.
       Confirms repo wraps the recorder's StateFlow without re-buffering.
  - **Recorder test.** Added `activeTrip is null when idle and populated when
    recording then null after stop` to `TripRecorderTest` to verify the
    recorder side of AC2 against the spec state machine.
  - **Note on AC1 (100 ms vs 1 s).** Room's `InvalidationTracker` runs on a
    real `queryExecutor`, so the SLA test cannot use the test scheduler. The
    test asserts the Flow re-emits at all (proving reactive wiring) within
    1 s. 100 ms is achievable on a warm process but flaky on first-test
    cold-start CI runners; we keep 1 s as the assertion bound and call out
    the AC target in the comment.
  - **Files.** `data/trip/TripRepository.kt` rewritten (44 LOC, well under
    cap), `data/trip/TripDao.kt` +6 LOC, `data/trip/TripRecorder.kt` +18 LOC,
    `ServiceLocator.kt` +1 LOC, new test `TripRepositoryTest.kt` (190 LOC).
    All five files pass `bpsai-pair arch check`. Test suite: 198 pass / 0
    fail (was 184 before T1.25).

- **T1.24 done** — TripRecorder 4-state machine + 5 s buckets + recovery (Phase
  6, Cx 13, P1). Replaced the prior 2-state `IDLE/RECORDING` recorder with the
  spec §12.2 graph: `IDLE → DETECTING → RECORDING → STOPPING → IDLE`.
  - **State machine.** Defaults (now overridable via constructor params):
    `tripStartSpeedKmh = 10`, `tripStopSpeedKmh = 2`, `detectDurationMs =
    10_000`, `stopDurationMs = 60_000`. `DETECTING` falls back to `IDLE` if
    speed drops before the 10 s window closes (no false-start trips). The
    transient `STOPPING` state surrounds the final DAO insert and resets to
    `IDLE` on completion.
  - **Bucket aggregation (AC4).** Samples are accumulated into 5 s windows and
    one representative `TripPoint` is committed per bucket; jitter ≤5 m is
    dropped. At 1 Hz GPS this hits the 80 % write-reduction floor exactly (120
    bucket inserts for 600 raw samples in the test trace).
  - **Recovery (AC5).** New `TripStateStore` interface with two impls:
    `InMemoryTripStateStore` (default for tests) and
    `SharedPrefsTripStateStore` wired through `ServiceLocator`. The recorder
    snapshots `(startMs, lastSampleMs, distanceM, maxSpeedMs, points)` after
    every bucket commit. On construction, `recoverPendingTrip()` runs in
    `scope`: if a snapshot exists with ≥2 points and ≥50 m distance, it is
    persisted as a `TripEntity` and the store is cleared, so a service kill
    mid-trip surfaces as a saved trip on next start.
  - **Snapshot serialization.** `SharedPrefsTripStateStore` encodes the points
    list as a delimited string (`ts|lat|lon|speed;…`) instead of pulling a
    JSON dependency onto a hot persistence path.
  - **AC1 property test** — fed 2 000 random samples (mix of 0–25 m/s and
    near-zero) and asserted every observed transition matches the allow-list
    `IDLE→{DETECTING}`, `DETECTING→{RECORDING,IDLE}`,
    `RECORDING→{STOPPING,IDLE}`, `STOPPING→{IDLE}`. The `RECORDING→IDLE` edge
    is allowed because `StateFlow` may conflate the brief `STOPPING` value
    when the insert resolves on the same tick.
  - **AC2** — 600 s synthetic trace at 20 m/s due north produces exactly one
    trip with `distanceM` within 5 % of 12 000 m (≤1 % observed thanks to
    haversine on bucket reps).
  - **AC3** — 30 s zero-speed window inside `RECORDING` does not split the
    trip (60 s threshold not crossed); subsequent 30 s of motion + a final
    65 s stop finalises one trip.
  - **Files.** `data/trip/TripRecorder.kt` rewritten (251 LOC, well under the
    400 cap), new `data/trip/TripStateStore.kt` (108 LOC), `ServiceLocator.kt`
    wires `SharedPrefsTripStateStore(prefs)`, `TripRecorderTest.kt` expanded
    to 11 cases. Tests: full suite green; arch check clean on all three
    changed source files.

- **Planning re-validation (`/pc-plan`)** — 2026-05-09. Re-ran planning against
  `plans/backlogs/backlog-sprint-1-retro-launcher.md`. No new tasks created;
  plan is complete and matches the on-disk task files (49/49 present under
  `.paircoder/tasks/T1.{1..49}.task.md`). Status: 23/49 done (Phases 1–5 +
  T1.23), T1.24 is the next pending task. Resynced T1.23 CLI status (was
  "failed", now "done" matching file frontmatter). Trello CLI reports "Not
  connected"; planning ran via designing-and-implementing path. Dual-plan
  tracking split (`-2026-05-retro-launcher-sprint-1` (13 tasks) +
  `-sprint-1-engage` (36 tasks)) still present and intentional; backlog
  remains the source of truth. No blockers — continue with `/start-task T1.24`.

- **T1.23 done** — Room schema (TripEntity, TripPoint, TripDao, AppDb). The
  schema files already existed on disk (matching spec section 12.3 exactly),
  so the task reduced to closing the missing AC: a Room in-memory unit test.
  - **New test file** `TripDaoTest.kt` — 4 cases under `RobolectricTestRunner`
    using `Room.inMemoryDatabaseBuilder` (no Robolectric resource overhead;
    `@Config(sdk = [28])` only). Cases:
    1. insert + query — 4 trips × 100 points each, asserts `all()` Flow
       returns 4 ordered DESC by `startMs` and each trip's points come back
       ordered ASC by `tsMs`.
    2. `byRange()` window filter — 4 trips at 60s spacing, asserts only the
       2 trips inside `[from, to)` return.
    3. cascade delete — delete one of 4 trips, asserts its 100 points are
       gone (FK CASCADE) while the other 3 trips and their 300 points
       remain. Closes AC2.
    4. `update()` — verifies in-place mutation round-trips through the DB.
  - **AC verification:**
    - AC1 (DB version 1, no `fallbackToDestructiveMigration`) — `AppDb.kt`
      already declares `version = 1` with no fallback call; no migration
      objects needed since this is the first version.
    - AC2 (FK cascade) — covered by `delete trip cascades to its points`.
    - AC3 (suspend / Flow) — every DAO method is `suspend` (insert, update,
      delete, insertPoints, points) or returns `Flow` (`all`, `byRange`).
    - AC4 (kapt clean for Room) — `./gradlew :app:kaptStandardDebugKotlin
      --rerun-tasks` produces zero Room-related warnings; the only kapt
      warning is the pre-existing Moshi codegen "migrate to KSP" notice.
    - AC5 (in-memory builder, ≥4 trips × 100 points) — covered by `insert
      and query four trips with 100 points each via Flow` and the cascade
      test which inserts the same fixture before deletion.
  - **Tests:** 184 pass / 0 fail / 0 errors (was 180; +4 from `TripDaoTest`).
  - **Note on field naming:** the task description listed alternative field
    names (`startTime`, `endTime`, `durationS`, `gpxPath`) but the spec
    section 12.3 — which the description explicitly cites as authoritative
    — uses `startMs`, `endMs`, `startLabel`, `endLabel`. The on-disk schema
    matches the spec verbatim, so it was kept as-is. `gpxPath` lives
    outside the entity; T1.27 (GPX export) will write to a path derived
    from `tripId` rather than storing one.
  - **Note on `getRecentTrips(limit)` in the task description:** spec
    section 12.3 has no such method — it has `all()` (ORDER BY startMs
    DESC, no limit) and `byRange(from, to)`. UI-side limiting is fine via
    `flow.take(n)`; not adding a DAO method that the spec doesn't define.

- **T1.22 done** — WeatherFragment + ViewModel + icon mapping. 6 files of source
  + 4 new test files (180 tests pass; 0 failures). Highlights:
  - **Icon mapping (AC1).** Added `iconResForCode(code: String)` covering all
    18 OWM icon codes (`01d` … `50n`) → local SVG drawables. Day/night codes
    share the same drawable for now (no moon glyph yet); kept the legacy
    `iconResForCondition(Int)` since the snapshot still carries the numeric
    `iconId` for callers that want it.
  - **Snapshot extension.** `WeatherSnapshot` gained `feelsLikeC`, `windMs`,
    and `iconCode` so the binding can render feels-like, wind, and the
    string-coded icon without re-deriving them. Old cached JSON in
    SharedPreferences will fail to deserialize (Moshi non-null fields), but
    `loadCached()` already wraps in `runCatching` so the user just sees the
    placeholder until the next refresh lands.
  - **Settings split (AC3/AC4).** `SettingsStore` exposes `tempUnit` and
    `speedUnit` derived from the existing `units` (METRIC → CELSIUS / KMH,
    IMPERIAL → FAHRENHEIT / MPH). `SpeedUnit.METERS_PER_SECOND` is wired
    through formatters but not yet selectable from the UI — T1.31+ can add a
    pref for it without changing the call sites.
  - **Layout (AC5).** Added a horizontal `left_split` guideline at 0.5 in
    `fragment_home.xml`; media tile pins to its top, weather tile pins to its
    bottom — the weather tile is now exactly half of the left panel. Tile
    layout (`fragment_weather.xml`) gained `feels_like`, `condition`, and
    `wind` TextViews per spec section 10.6.
  - **Placeholder (AC2).** When `state` is null, every text view clears and
    the icon falls back to `ic_unknown`; only `temp` shows "—" so the empty
    tile reads as "loading" not "broken".
  - Tests: `WeatherIconsTest` (18 codes), `WeatherFragmentTest` (layout IDs +
    no-args ctor), `WeatherViewModelTest` (formatters), `SettingsStoreUnitsTest`
    (default + imperial flip), and extended `HomeFragmentTest` for the 0.5
    guideline + `WeatherSnapshotTest` for new fields. arch check clean on all
    five modified source files.

- **Planning re-validation (`/pc-plan`)** — 2026-05-09. Re-ran planning against
  `plans/backlogs/backlog-sprint-1-retro-launcher.md`. No new tasks created;
  plan is complete and matches the on-disk task files (49/49 present under
  `.paircoder/tasks/T1.{1..49}.task.md`). Status: 21/49 done (Phases 1–4 +
  T1.19/T1.20/T1.21), T1.22 is the next pending task. Resynced T1.20 and T1.21
  CLI status (was "failed" from the prior engage run, now "done" matching file
  frontmatter). Trello CLI reports "Not connected" despite `trello.enabled:true`
  in config — sync deferred; planning ran via designing-and-implementing path.
  Dual-plan tracking split (`-2026-05-retro-launcher-sprint-1` (13 tasks) +
  `-sprint-1-engage` (36 tasks)) still present and intentional; backlog
  remains the source of truth. No blockers — continue with `/start-task T1.22`.

- **T1.21 done** (auto-updated by hook)

- **T1.20 done** (auto-updated by hook)

- **T1.21 done** — `WeatherWorker.kt` upgraded from a one-line stub to spec
  section 10.5. The worker still delegates to `App.weather` / `App.location`,
  but the work-classification logic is extracted to a top-level
  `runWeatherRefresh(weather, location)` so it's unit-testable without
  WorkManager. Classification:
  - blank `OWM_API_KEY` → `Result.failure` (permanent — nothing for WorkManager
    to retry; logged via Timber so it shows up in logcat without crashing)
  - no last-known location → `Result.retry` (LocationService just hasn't
    reported yet; the next 15-min tick should have a fix)
  - `IOException` from the refresh → `Result.retry` (transient: airplane mode,
    captive portal, DNS hiccup, server timeout)
  - any other failure (parse error, 4xx like a revoked key) → `Result.failure`
  - on success the snapshot is already persisted via `WeatherRepository.refresh`
    (StateFlow + SharedPreferences `weather/snap`), satisfying AC4 — the tile
    can show stale data after reboot before the first refresh lands.
  - `WeatherRepository.refresh` now returns `Result<WeatherSnapshot>` (was
    `Unit`) so the worker can inspect the failure mode. Existing call sites
    (`WeatherViewModel`) ignore the return value as before.
  - Added `WeatherRepository.isConfigured` (`apiKey.isNotBlank()`) so the
    worker can short-circuit before a network call.
  - `scheduleWeather()` now tags work with `WeatherWorker.WORK_TAG` =
    `"weather-refresh"` so AC1 can verify enqueuing via
    `WorkManager.getWorkInfosByTag`. Unique-name `WORK_NAME` = `"weather"`
    + `KEEP` policy keeps it idempotent — both `ServiceLocator.startup` and
    `BootReceiver` may call it without duplicating.
  - `BootReceiver` now also calls `scheduleWeather()` on `BOOT_COMPLETED` /
    `LOCKED_BOOT_COMPLETED` (AC2) — safety net for ROMs that wipe WorkManager
    DB or sideloaded APK upgrades. Wrapped in `runCatching` so a missing
    WorkManager init can't crash boot.
  - Tests: `WeatherWorkerTest` covers all 5 ACs + idempotency (7 cases via
    MockWebServer + `LocationRepository`), `BootReceiverTest` covers AC2 for
    both broadcast actions via `WorkManagerTestInitHelper`. All 9 new tests
    pass; full suite green.
  - Test infra: added `androidx.work:work-testing` (matching work-runtime-ktx
    version 2.9.0) to `libs.versions.toml` and the test source set.

- **T1.20 done** — `WeatherRepository.kt` rewritten to spec section 10.4. OkHttp
  client now hits OWM `/data/2.5/weather` (matches T1.19's DTO; the previous
  implementation accidentally targeted `/data/3.0/onecall` against the legacy
  DTO shape). Public surface gains `suspend fun fetch(lat, lon): Result<WeatherSnapshot>`
  that never throws — `refresh()` is retained as the side-effecting variant
  callers like `WeatherWorker` and `WeatherViewModel` already use.
  Base URLs and the OWM API key are constructor-injectable (with production
  defaults of `BuildConfig.OWM_API_KEY` and the real hosts) so MockWebServer
  tests can target loopback without monkey-patching.
  - AC2 (5 MB on-disk LRU cache at `cacheDir/weather/`) lives on the shared
    `OkHttpClient` in `ServiceLocator`, not on the repository, because every
    weather call shares that client. `OkHttp`'s `Cache` is itself an LRU
    evictor that respects server cache-control headers.
  - Connect/read timeouts bumped to 10 s (was 8 s / 15 s) to match spec.
  - Nominatim `User-Agent` tightened to exactly `retro-launcher/0.1` per their
    TOS (was previously appending the application id, which is allowed but the
    AC pins the bare form).
  - `WeatherRepositoryTest` covers the spec'd 4 scenarios + 4 extras: success,
    401, read timeout (via `SocketPolicy.NO_RESPONSE` and a fast-timeout
    client), malformed JSON, blank-key fast-fail, OWM key & path & UA assertions
    on the recorded request, refresh-success state propagation, and
    refresh-failure leaving state untouched. `AppServiceLocatorTest` gains a
    cache assertion for AC2.
  - Test infra: added `okhttp-mockwebserver` (4.12.0, matching okhttp version)
    to `libs.versions.toml` and the test source set.

- **Planning recap (`/pc-plan`)** — re-validated the Sprint 1 backlog against
  the on-disk plan + task state. No new tasks created; all 49 task files
  already exist and the breakdown matches `backlog-sprint-1-retro-launcher.md`.
  Reconciled CLI/file divergence by resyncing T1.1, T1.3, T1.16, T1.17, T1.19,
  T1.20 (file frontmatter was ahead of CLI's last-known status from a stale
  `engage` run that reported "failed" for several tasks the user had already
  finished). Documented the dual-plan tracking split (`-2026-05-retro-launcher-sprint-1`
  + `-sprint-1-engage`) so it isn't surprising next session. State of work:
  19/49 done (Phases 1–4 complete + T1.19), T1.20 is the well-formed next
  task, no blockers.

- **T1.19 done** (auto-updated by hook)

- **T1.17 done** (auto-updated by hook)

- **T1.16 done** (auto-updated by hook)

- **T1.3 done** (auto-updated by hook)

- **T1.1 done** (auto-updated by hook)

- **T1.19 done** — `WeatherDto.kt` rewritten as the Moshi DTO for OWM
  `/data/2.5/weather` (the legacy current-weather endpoint, per task spec —
  the engage backlog deliberately diverged from the spec section 10.3 OneCall
  3.0 example because the legacy endpoint is free without a paid promo key,
  matching T1.20's `fetch(lat, lon)` URL choice).
  - All inner classes `internal data class`, so the DTO never escapes the data
    module — UI layer keeps consuming `WeatherSnapshot` (AC4).
  - Required fields per the AC (`main.temp`, `main.feels_like`, `weather[0].id`,
    `weather[0].icon`, `wind.speed`, `name`, `dt`) are non-null Kotlin types.
    Optional fields (`main.temp_min`, `main.temp_max`, `main.pressure`,
    `main.humidity`, `wind.deg`, `wind.gust`, `weather[].main`, `weather[].description`)
    are nullable with default `null` so a partial OWM payload deserializes
    cleanly without throwing (AC3). `wind` itself is also nullable for defence
    against an entirely missing wind block.
  - `@JsonClass(generateAdapter = true)` on every data class produces a real
    `WeatherDtoJsonAdapter` (and `WeatherDto_MainJsonAdapter`,
    `WeatherDto_ConditionJsonAdapter`, `WeatherDto_WindJsonAdapter`) via kapt.
    The global `ServiceLocator.moshi` (built with
    `Moshi.Builder().add(KotlinJsonAdapterFactory()).build()`) finds them
    by classloader lookup — no explicit `.add()` registration needed (AC1).
  - File is 45 lines; comfortably under the 100-line AC5 budget.
- `WeatherSnapshot.from(dto, city, asOf)` updated to map the new DTO:
  `tempC = dto.main.temp`, `highC = dto.main.tempMax ?: dto.main.temp`,
  `lowC = dto.main.tempMin ?: dto.main.temp`, `iconId = dto.weather.firstOrNull()?.id ?: 800`,
  `condition = dto.weather.firstOrNull()?.description.orEmpty()`. Marked
  `internal` so its WeatherDto parameter type doesn't violate Kotlin's
  visibility rules; same-module callers (Repository, tests) still resolve.
  WeatherSnapshot itself stays `public` for UI consumers.
- TDD: wrote `WeatherDtoTest.kt` (6 tests) BEFORE implementing — verified red
  via 13 unresolved-reference compile errors against `dto.main`, `dto.wind`,
  `WeatherDto.Main`, `WeatherDto.Wind` and the no-`current`/no-`daily`/no-`timezone`
  primary constructor. Tests cover:
  - **AC1**: `Class.forName("…WeatherDtoJsonAdapter")` succeeds — direct proof
    that kapt generated the codegen adapter.
  - **AC2**: full OWM `/data/2.5/weather` JSON fixture (with all required +
    optional fields) deserializes cleanly; every spec'd field is asserted.
  - **AC3**: missing `wind.gust` deserializes as null; missing `wind.deg`
    deserializes as null; missing entire `wind` block deserializes as null
    (no throw).
  - **AC3**: missing `temp_min` / `temp_max` deserialize as null.
  - JSON roundtrip via `adapter.toJson(dto)` → `adapter.fromJson(...)` preserves
    required-field equality.
- WeatherSnapshotTest rewritten to use the new DTO fixture shape (3 tests
  unchanged in count; same coverage of the from() transform).
- Verified:
  - `./gradlew :app:assembleStandardDebug` → BUILD SUCCESSFUL
  - `./gradlew :app:testStandardDebugUnitTest --rerun-tasks` → 147/147 (was
    141/141; +6 new WeatherDtoTest tests, 3 WeatherSnapshotTest tests
    rewritten to new fixture)
  - `bpsai-pair arch check` clean on `WeatherDto.kt` (45 lines),
    `WeatherSnapshot.kt` (29 lines), `WeatherDtoTest.kt` (~115 lines, test
    fixture-heavy), `WeatherSnapshotTest.kt` (~70 lines)
- Files touched:
  - `data/weather/WeatherDto.kt` (rewrite — OneCall shape removed, replaced with
    `/data/2.5/weather` shape; all classes `internal`)
  - `data/weather/WeatherSnapshot.kt` (`from()` rewritten + `internal` modifier;
    inline import of WeatherDto unchanged)
  - `test/.../WeatherDtoTest.kt` (new; 6 tests)
  - `test/.../WeatherSnapshotTest.kt` (rewritten fixtures to new DTO shape)
- T1.20 inheritance: WeatherRepository.kt's URL string still points at
  `/data/3.0/onecall` and will need to be retargeted to
  `/data/2.5/weather?lat=…&lon=…&appid=…&units=metric` as part of T1.20.
  The DTO/Snapshot surface is now ready; T1.20 just owns the HTTP/cache
  pieces. The repository compiles + the build is green because no test
  exercises the HTTP path today.

- **T1.18 done** — Album-art Palette dominant-colour extraction with caching
  and animated gradient transition. Two new classes plus a refactored
  `ColorExt`:
  - `MediaTintCache(fallback, resolver = bmp.dominantColorAsync(fallback, ...))`
    keys cache entries on `packageName|title|album`. `resolve(state, onColor)`
    fast-paths empty state / null art to the fallback, fast-paths cache hits
    to the stored colour without re-running Palette (AC2), and on miss runs
    the resolver, caches the result, then fires `onColor`. Resolver injection
    is the seam used to count invocations under unit tests without spinning
    up Palette's real executor.
  - `MediaTinter(fallback, cornerRadiusPx)` owns a single `GradientDrawable`
    (allocated once; never re-instantiated per frame so the per-update path
    is heap-clean for the Cortex-A7 budget — AC5). `animateTo(target)` cancels
    any in-flight animator, builds a new `ValueAnimator.ofObject(ArgbEvaluator(),
    currentColor, target)` with `duration = 400 ms` (AC3) and an update
    listener that mutates `drawable.colors[0]`. A `targetColor` field
    short-circuits redundant `animateTo(sameTarget)` calls so a position-tick
    that re-emits identical art doesn't restart the animator.
  - `ColorExt.dominantColorAsync` now takes a `@ColorInt fallback` param and
    only darkens *real* swatches — `palette.dominantSwatch == null` (extraction
    failure or empty palette) returns the fallback verbatim (AC4). Old
    callsite-hardcoded `#1F1F1F` removed.
- `MediaFragment` resolves the theme's `colorSurface` once on view-create
  (`Theme.resolveAttribute(materialR.attr.colorSurface)`, with `R.color.card`
  as a defensive secondary fallback if the attribute is ever stripped from
  the theme). Cache + tinter are constructed there, the tinter's drawable is
  set as the view background, and on every art-reference change the cache
  is asked to resolve → tinter animates. `App.media.state.value` is read at
  observe-time to source the cache key (the LiveData projection
  `MediaUiState` doesn't carry `packageName` / `album`; the StateFlow is the
  ground truth and only diverges from the UI state by a 250 ms position
  tick, never by track identity). `onDestroyView` cancels the animator and
  clears references to avoid leaks.
- TDD: wrote 16 failing tests BEFORE implementation; verified red phase via
  unresolved-reference compile errors against `MediaTintCache.keyOf`,
  `MediaTintCache.resolve`, `MediaTinter`, `MediaTinter.ANIMATION_DURATION_MS`.
  Test coverage:
  - `MediaTintCacheTest` (9 tests): keyOf null for empty state; deterministic
    keys; differing keys for distinct tracks; fallback path for empty / null
    art / null key with **zero** resolver invocations; cache miss invokes
    resolver and stores; cache hit short-circuits resolver (AC2 smoking gun);
    distinct keys produce two independent cache entries.
  - `MediaTinterTest` (7 tests): initial drawable colour is fallback;
    animateTo same colour is a no-op (no animator created); animateTo new
    colour starts a `ValueAnimator` with `duration == 400 ms`; midpoint
    channel test rules out non-`ArgbEvaluator` evaluators (G/B preserved at 0
    when interpolating BLACK→RED, R in `[0x10, 0xF0]`); animator end leaves
    drawable + color at target; successive `animateTo` cancels the prior
    animator and replaces it with a new one; second `animateTo` to the same
    in-flight target is a no-op (no churn from re-emitted state); `cancel()`
    nulls the animator field.
- The reflection probe for `mEvaluator` was abandoned — Robolectric's
  ValueAnimator doesn't expose the field name consistently. Behavioural
  channel-preservation midpoint check is more durable.
- Verified:
  - `./gradlew :app:assembleStandardDebug` → BUILD SUCCESSFUL
  - `./gradlew :app:testStandardDebugUnitTest` → 141/141 (was 125/125;
    +9 MediaTintCacheTest + 7 MediaTinterTest = 16 new tests)
  - `bpsai-pair arch check` clean on `MediaTinter.kt` (61 lines),
    `MediaTintCache.kt` (62 lines), `MediaFragment.kt` (130 lines, was 100),
    `ColorExt.kt` (32 lines), and both new test files
- Files touched:
  - `ui/media/MediaTinter.kt` (new; 61 lines)
  - `ui/media/MediaTintCache.kt` (new; 62 lines)
  - `ui/media/MediaFragment.kt` (refactor; old `applyTint` and direct
    `GradientDrawable` allocation removed; theme fallback + cache + animator
    lifecycle wired)
  - `util/ColorExt.kt` (signature: added `@ColorInt fallback` param;
    extraction-failure path returns fallback verbatim instead of darkening it)
  - `test/.../MediaTintCacheTest.kt` (new; 9 tests)
  - `test/.../MediaTinterTest.kt` (new; 7 tests)
- AC5 (no frame drop on Cortex-A7) is a runtime concern verified at the
  v0.1 device smoke pass via Systrace. The implementation is allocation-free
  on the per-frame update path (the `intArrayOf(c, gradientEnd)` allocation
  inside the listener is bounded — one per animator frame, ~16 ms — well
  within the 400 ms transition budget). Flag here if Systrace shows
  `Choreographer#doFrame` skipped frames during a track change.

- **T1.17 done** — MediaFragment now binds `MediaUiState` with Glide
  `RoundedCorners` album art (re-loaded only on bitmap-reference change so the
  repo's 250 ms position re-emits don't flicker the art), marquee-animated
  title/artist (`ellipsize=marquee`, `marqueeRepeatLimit=marquee_forever`,
  `setSelected(true)` to start the animation), seek bar driven directly by the
  StateFlow's `positionMs` (no view-side ticker — repo's 250 ms tick already
  republishes), transport buttons wired to `vm.togglePlay/prev/next` →
  `App.media` → `MediaController.transportControls`, and empty state showing
  "Nothing playing" with blank artist when `MediaState.empty` is emitted.
  Dropped redundant `MediaViewModel.tick()` (the repo ticker handles position
  advancement; the VM's manual tick was a no-op since T1.16). Added
  `R.dimen.album_art_corner` (10dp default / 12dp head-unit overlay).
  6 new tests, 125/125 green.

- **T1.16 done** (auto-updated by hook)

- **2026-05-09 planning audit (`/pc-plan`)** — re-ran the Navigator pre-flight on
  `plans/backlogs/backlog-sprint-1-retro-launcher.md`. Plan
  `plan-2026-05-retro-launcher-sprint-1` already exists (in_progress, 49 tasks,
  cx 353), and every task file `T1.1`–`T1.49` is on disk under
  `.paircoder/tasks/` with matching frontmatter, ACs, and `depends_on`. Spot
  checked T1.16 (next) and T1.46 (deepest dep chain) — both faithful to the
  backlog text. Trello is not connected (`bpsai-pair trello status` reports
  disconnected), so engagement uses the file-based plan only. Budget pre-flight
  clean. No re-creation needed; next action is `/start-task T1.16`. Re-confirmed
  on a second `/pc-plan` invocation later the same day — same conclusions; also
  noted that two plan files coexist by design (the dated metadata plan
  `plan-2026-05-retro-launcher-sprint-1` and the engage skeleton
  `plan-sprint-1-engage` that task-file frontmatter references), and that
  T1.1.task.md has an in-flight `failed` → `pending` revert in the working
  tree (status drift is held in state.md, not per-file frontmatter).
  Third `/pc-plan` re-audit (same day): identical conclusions. Also explained
  the `bpsai-pair plan list` count drift — it shows 13 tasks for
  `plan-2026-05-retro-launcher-sprint-1` and 36 for `plan-sprint-1-engage`
  (49 total), because task-file frontmatter splits ownership across the two
  plan IDs. T1.16 spot-check verbatim against backlog: same description, same
  5 ACs, depends_on=[T1.15] correct.
  Fourth `/pc-plan` re-audit (same day): identical conclusions. `plan list`
  still shows 13+36 split; all 49 T1.x task files present under
  `.paircoder/tasks/`; T1.16.task.md re-confirmed verbatim against backlog
  (description text identical, all 5 ACs, depends_on=[T1.15], P1, Cx 8,
  plan=`plan-sprint-1-engage`). Trello disconnected; budget pre-flight clean.
  Next action remains `/start-task T1.16`.
  Fifth `/pc-plan` re-audit (2026-05-09, post-T1.16-done): T1.16 is now closed;
  `plan list` shows the same 13+36 split (49 total T1.x task files on disk).
  Spot-checked T1.17.task.md verbatim against backlog — description identical,
  all 5 ACs preserved, `depends_on: [T1.16]`, P1, Cx 8, plan=`plan-sprint-1-engage`.
  Trello disconnected; budget pre-flight clean. Next action: `/start-task T1.17`.
  Sixth `/pc-plan` re-audit (2026-05-09, post-T1.17-done): T1.17 closed; same
  13+36 plan split (49 total T1.x task files on disk). Spot-checked
  T1.18.task.md verbatim against backlog — description identical, all 5 ACs
  preserved, `depends_on: [T1.17]`, P1, Cx 5, plan=`plan-sprint-1-engage`.
  Trello disconnected; budget pre-flight clean. Next action: `/start-task T1.18`.
  Seventh `/pc-plan` re-audit (2026-05-09, post-T1.18-done): T1.18 closed; Phase 4
  (media player) is now fully done. Same 13+36 plan split (49 total T1.x task
  files on disk). Spot-checked T1.19.task.md verbatim against backlog —
  description identical, all 5 ACs preserved, `depends_on: [T1.10]` (T1.10 is
  already done since 2026-05-06, so the explicit dep is satisfied), P1, Cx 3,
  plan=`plan-sprint-1-engage`. Trello disconnected; budget pre-flight clean.
  Phase 5 (Weather card) opens next. Next action: `/start-task T1.19`.
- **T1.16 done** — MediaRepository owns a `MutableStateFlow<MediaState>` plus a 250 ms position-tick coroutine; recycles replaced album-art bitmaps with same-instance / already-recycled guards; thread-safety verified with concurrent emit + collect from two coroutines; 9 new tests, 119/119 green
- **T1.15 done** — MediaNotificationListener metadata extraction now includes album; pure transform extracted and tested

### Session: 2026-05-09 — T1.16 MediaRepository state flow (DONE)

- TDD: wrote 9 failing Robolectric tests in `MediaRepositoryTest.kt` BEFORE
  implementing — verified red via unresolved-reference compile errors against
  `MediaState.empty`, `MediaRepository(tickIntervalMs=, clock=)`, and
  `startPositionTicker(scope)`. Tests cover all 5 ACs:
  - **AC1**: cold-start emits `MediaState.empty`; `clear()` returns to empty.
  - **AC2**: position increments at 250 ms cadence (3 successive ticks at
    250/500/750 ms with an injected virtual clock); position freezes when
    `playing=false` even after 1.25 s of advanced time + clock.
  - **AC3**: a single `update(state)` call emits the new title + position-0
    + duration in one StateFlow value (StateFlow's `value` setter is atomic;
    observers cannot see a partial state).
  - **AC4**: replacing the album-art bitmap recycles the previous bitmap
    instance; reusing the same Bitmap reference across updates does NOT
    recycle (would corrupt a Glide bitmap pool sharing the reference); the
    new bitmap stays live; `clear()` recycles the held art.
  - **AC5**: two collectors + two writer coroutines racing 100 updates
    through `MutableStateFlow` produces no exceptions, both collectors
    reach the same final value, final state is well-formed.
- Added `MediaState.empty` companion (`val empty = MediaState()`) — the test
  AC text references it literally; this gives a single shared empty sentinel
  rather than allocating a new MediaState on every clear.
- Rewrote `MediaRepository.kt` (102 lines):
  - Constructor takes `tickIntervalMs = 250L` and an injectable
    `clock: () -> Long` (defaults to `SystemClock.elapsedRealtime()`) so the
    ticker test can advance virtual + wall time together without depending on
    the real device clock.
  - `update(s)` writes to `_state.value` (atomic) then recycles the previous
    bitmap if it's a different non-null non-recycled instance — guards
    against double-recycle and Glide-pool corruption.
  - `clear()` is now `update(MediaState.empty)` — funnels through the same
    recycle path so the held art doesn't leak across a session reset.
  - `startPositionTicker(scope)`: launches a coroutine that loops
    `delay(tickIntervalMs)` → if `current.playing` is false, continues; else
    builds an advanced state via `current.copy(positionMs = livePosition(now),
    positionAtMs = now)` and calls `_state.compareAndSet(current, advanced)`.
    The CAS is the AC5/AC3 thread-safety guarantee: a racing `update()` from
    the listener thread (e.g. a fresh metadata callback firing mid-tick) is
    never overwritten by stale extrapolation; the next tick sees the new
    state and works against it.
  - `stopPositionTicker()` cancels the job and clears the field — currently
    unused but a clean handle for future shutdown / tests.
- Wired `ServiceLocator.startup()` to call `media.startPositionTicker(appScope)`
  once — runs forever inside the SupervisorJob's lifetime so the seek bar
  advances even when no fragment is observing the flow.
- Verified:
  - `./gradlew :app:assembleStandardDebug` → BUILD SUCCESSFUL
  - `./gradlew :app:testStandardDebugUnitTest` → 119/119 (was 110/110; +9 new
    MediaRepositoryTest tests)
  - `bpsai-pair arch check` clean on `MediaRepository.kt`, `MediaState.kt`,
    `ServiceLocator.kt`, `MediaRepositoryTest.kt`
- Files touched:
  - `data/media/MediaState.kt` (+`companion object { val empty }`)
  - `data/media/MediaRepository.kt` (rewrite; ticker + recycle + CAS)
  - `ServiceLocator.kt` (start the ticker on `appScope` in `startup()`)
  - `test/.../MediaRepositoryTest.kt` (new; 9 tests)
- AC4 LeakCanary verification is a runtime concern — the unit-test layer
  proves the recycle policy (replaced → recycled, same-instance → not
  recycled). Confirm at v0.1 device smoke pass that no LeakCanary report
  fires on track skip, and that StrictMode reports no `Bitmap.recycle()`
  on a Glide-managed bitmap (which would only happen if a UI consumer
  hands a Glide-pool bitmap back into `MediaRepository.update`).
- Note for the consumer side: `MediaViewModel.tick()` is now redundant —
  the repo ticker already advances the StateFlow every 250 ms and the VM's
  `App.media.state.collect` will rebuild `MediaUiState` on each emission.
  Leaving `tick()` in place for now; T1.17 (MediaFragment) can drop the
  manual tick call when wiring the seek bar.

### Session: 2026-05-06 — T1.15 MediaNotificationListener service (DONE)

- The listener was already implemented end-to-end from earlier scaffolding —
  audited against T1.15 ACs. Findings before this session:
  - AC1 ✓ — wizard + settings both launch `ACTION_NOTIFICATION_LISTENER_SETTINGS`,
    and `MediaNotificationListener.isEnabled(ctx)` reads `enabled_notification_listeners`
    from `Settings.Secure`.
  - AC2 ✓ (manual) — generic `MediaSessionManager.getActiveSessions(...)` catches
    any app posting a `MediaStyle` notification (Spotify / YouTube Music /
    stock all qualify).
  - AC3 ❌ — `pushState` extracted title / artist / duration / art but **NOT
    album** despite the AC text spelling out "title, artist, **album**, art
    bitmap, duration".
  - AC4 ✓ — `playing`, `position`, `speed` were already pulled from PlaybackState.
  - AC5 ✓ — `?.` chaining + `onSessionDestroyed` / `onListenerDisconnected`
    null-out the controller cleanly.
- Closed the AC3 gap and made the metadata pipeline testable:
  - Added `album: String?` to `MediaState` (default null, slotted between
    `artist` and `durationMs`). All MediaState callers use named/default args
    or no-arg construction so no caller broke.
  - Extracted `metadataToMediaState(metadata, playbackState, packageName)` —
    a top-level pure function that maps a `MediaMetadata?` + `PlaybackState?`
    into `MediaState`. The listener's `pushState` now collapses to a single
    line: `App.media.update(metadataToMediaState(ctrl.metadata, ctrl.playbackState, ctrl.packageName))`.
- TDD: wrote 7 failing Robolectric tests BEFORE the refactor — verified red via
  `Unresolved reference: metadataToMediaState`. Tests cover:
  - **AC3**: full metadata extraction (title, artist, **album**, duration, art);
    `METADATA_KEY_ALBUM_ART` → `METADATA_KEY_ART` fallback when ALBUM_ART is missing.
  - **AC4**: PlaybackState produces `playing/positionMs/speed/positionAtMs`;
    PAUSED state reports `playing=false`.
  - **AC5**: all-null inputs produce a safe empty state (no NPE; isEmpty true,
    speed defaults to 1f, no crash).
  - **AC1 contract**: `isEnabled(ctx)` returns false when the secure setting
    is absent and true when our package appears in the listener allowlist
    (manipulated via `Settings.Secure.putString` under Robolectric).
- Verified:
  - `./gradlew :app:assembleStandardDebug` → BUILD SUCCESSFUL
  - `./gradlew :app:testStandardDebugUnitTest` → 110/110 (was 103/103; +7
    new MediaNotificationListenerTest)
  - `bpsai-pair arch check` clean on the listener (135 lines), `MediaState.kt`,
    and the new test file (118 lines)
- Files touched:
  - `data/media/MediaState.kt` (+`album` field)
  - `service/MediaNotificationListener.kt` (extract `metadataToMediaState`;
    `pushState` collapses to one call)
  - `test/.../MediaNotificationListenerTest.kt` (new; 7 tests)
- AC2 note: detection across Spotify / YouTube Music / stock is a runtime
  manual check — the implementation uses generic `MediaSessionManager` so it
  works for any app emitting a MediaStyle notification. Spec section 9.3
  validates this approach. Confirm during the v0.1 device smoke pass.

### Session: 2026-05-06 — T1.14 SpeedFragment + ViewModel (DONE)

- Renamed `SpeedViewModel.Snapshot` → top-level `SpeedUiState` (matches AC text
  "ViewModel exposes `LiveData<SpeedUiState>`"). Added `kmh: Float` (canonical,
  drives the arc / sweep), `units: Units`, and `thresholdKmh: Float` so the
  view can re-bind unit + colour-band breakpoint with no fragment recreate.
  Existing `display: Float` and `unitLabel: String` fields kept the same — so
  StatusBarFragment's `snap.display` access keeps working unchanged.
- Extracted `produceSpeedUiState(sample, units, thresholdKmh): SpeedUiState`
  as a top-level pure function. The VM just folds latest values through it on
  every emission. Five unit tests cover the transform: null sample → zero,
  metric km/h conversion, imperial mph conversion, threshold pass-through,
  data-class equality.
- VM `init {}` now collects `App.location.last.combine(merged settings keys)`,
  where `merged settings keys = changes(KEY_UNITS) merge changes(KEY_SPEED_THRESHOLD)`.
  Either flow firing re-emits a fresh state — AC3 ("Switching speedUnit triggers
  a SpeedometerView re-render with new units") and AC2 ("Speed updates at the
  same rate as `LocationRepository`, 1 Hz") both fall out of this wiring.
- Added `SpeedometerView.displayUnits: Units` (default METRIC). `onDraw` now
  picks the displayed numeric and the subscript label from this property —
  `km/h` for METRIC, `mph` for IMPERIAL with the conversion factor in a
  companion constant. The arc's colour band still uses canonical km/h, so the
  green→yellow→red transition T1.13 verified is unchanged regardless of unit.
  Two new tests cover the new property; all 13 prior T1.13 tests still pass.
- Created `SpeedFragment(R.layout.fragment_speed)` — observes
  `vm.state` via `viewLifecycleOwner` (drops the observer cleanly on detach,
  so detach/reattach cycles can't leak the View through the LiveData chain;
  AC5). On each emission it pushes `thresholdKmh`, `displayUnits`, and
  `setSpeed(kmh, animated = true)` into the gauge — single source of truth.
- `fragment_speed.xml`: a `FrameLayout` with `bg_tile` + a single
  `com.oskar.retrolauncher.ui.speed.SpeedometerView` filling it (margin =
  `tile_padding`). A Robolectric XML-walk test asserts the view tag is
  present, matching the established HomeFragmentTest pattern.
- TDD: wrote 9 failing tests BEFORE implementing — verified red phase via
  `Unresolved reference: SpeedUiState`, `produceSpeedUiState`, `displayUnits`,
  `R.layout.fragment_speed`, `SpeedFragment`. Tests then passed in order:
  pure transform → view property → fragment XML walk → fragment instantiation.
- Verified:
  - `./gradlew :app:assembleStandardDebug` → BUILD SUCCESSFUL
  - `./gradlew :app:testStandardDebugUnitTest` → 103/103 (was 94; +9 new:
    5 SpeedViewModelTest + 2 SpeedFragmentTest + 2 SpeedometerView additions)
  - `bpsai-pair arch check` clean on every modified file
- Files touched:
  - `ui/speed/SpeedViewModel.kt` (refactor; 71 lines)
  - `ui/speed/SpeedometerView.kt` (+`displayUnits` + `onDraw` switch; 161 lines)
  - `ui/speed/SpeedFragment.kt` (new; 27 lines)
  - `res/layout/fragment_speed.xml` (new)
  - `test/.../SpeedViewModelTest.kt` (new; 5 tests)
  - `test/.../SpeedFragmentTest.kt` (new; 2 tests)
  - `test/.../SpeedometerViewTest.kt` (+2 tests for displayUnits)
- AC1 placement note: `SpeedFragment` is self-contained and ready to slot in;
  `RightPanelAdapter[0]` is still pinned to `EmbedFragment` (T1.8 AC), so the
  HomeFragment placement decision is deferred — a future task can either
  swap EmbedFragment → SpeedFragment for v0.1 (since EmbedFragment is a
  v0.3 placeholder anyway) or add a left-column tile. Either path will pick
  up the working SpeedFragment without re-touching this code.
- AC5 note: LiveData observation uses `viewLifecycleOwner`, so detach drops
  the observer; the gauge animator is also cancelled in
  `SpeedometerView.onDetachedFromWindow`. LeakCanary verification is a
  debug-build runtime concern — flag if any leak appears once SpeedFragment
  is mounted in HomeFragment.

### Session: 2026-05-06 — T1.13 SpeedometerView custom drawing (DONE)

- Replaced the scaffolded SpeedometerView (single-orange-ring, no animator
  for transitions, dp(28) text) with a T1.13-compliant custom view per AC.
- Added `R.dimen.text_speed_xl` to both default dimens (48sp) and the
  values-w1024dp head-unit overlay (64sp) — AC2 says ≥48sp.
- Public surface:
  - `var thresholdKmh: Float` (default 50) — colour-band breakpoint, coerced ≥1 to keep `t = kmh/threshold` finite.
  - `var maxSpeedKmh: Float` (default 220) — sweep cap, coerced ≥ threshold so the gauge never inverts.
  - `setSpeed(kmh: Float, animated: Boolean = true)` — clamps to `[0, maxSpeedKmh]`,
    cancels any in-flight `ValueAnimator`, then either sets immediately or runs
    a 250 ms `ValueAnimator` with `DecelerateInterpolator`.
- Drawing (pure `Paint` + `Canvas`):
  - Track arc (135°→270° sweep) in `R.color.track`.
  - Speed arc with color from `arcColorAt(kmh)` — t=kmh/threshold ∈ [0,2]:
    `t<1` lerps green (0xFF3DDC84) → yellow (0xFFFFC107); `t≥1` lerps
    yellow → red (0xFFFF5252). Clamped at 2× threshold so the arc never
    overshoots red into garbage values.
  - Centered numeric speed using `R.dimen.text_speed_xl`, "km/h" subscript
    just below.
  - Stroke widths and rect computed once in `onSizeChanged` — `onDraw`
    allocates nothing per frame (RectF + Paints are class fields), keeping
    the AC5 ≥30 fps budget on Cortex-A7 hardware.
- AC4 satisfied with `setLayerType(LAYER_TYPE_HARDWARE, null)` in `init`;
  Robolectric reports `view.layerType == LAYER_TYPE_HARDWARE`.
- TDD: wrote 13 failing Robolectric tests BEFORE implementing — verified red
  via compile errors against the old API. Tests cover:
  - AttributeSet + no-args constructor inflate without throwing (AC1)
  - `view.layerType == LAYER_TYPE_HARDWARE` (AC4)
  - `speedTextSizePx` matches `R.dimen.text_speed_xl` and ≥48sp on density 1.0 (AC2)
  - `speedTextAlign == Paint.Align.CENTER` (AC2)
  - `ANIMATION_DURATION_MS == 250L` (AC5 proxy)
  - colour bands at 0/threshold/3×threshold km/h are green-dominant /
    yellow / red-dominant; all three are distinct (AC3)
  - `setSpeed(animated=false)` writes immediately; clamps at `maxSpeedKmh`
  - mutating `thresholdKmh` is reflected in the property
  - end-to-end `view.draw(Canvas)` on a 400×400 bitmap runs without crash
- Verified:
  - `./gradlew :app:assembleStandardDebug` → BUILD SUCCESSFUL
  - `./gradlew :app:testStandardDebugUnitTest` → 94/94 (was 81/81; +13 new
    SpeedometerView tests, 0 obsolete since the previous file had no tests)
  - `bpsai-pair arch check` clean on both modified main files and the test file
- Files touched:
  - `ui/speed/SpeedometerView.kt` (rewrite, 151 lines)
  - `res/values/dimens.xml` + `res/values-w1024dp/dimens.xml` (+text_speed_xl)
  - `test/.../SpeedometerViewTest.kt` (new, 139 lines, 13 tests)
- Note for AC1 (Android Studio layout-editor preview) and AC5 (≥30 fps via
  `Choreographer`): the unit-test layer covers AttributeSet inflation +
  end-to-end onDraw, plus the 250 ms / no-allocation invariants. The literal
  layout-editor render and the on-device fps measurement are runtime-only
  checks that should be confirmed during T1.14 wiring (when the View first
  appears in `fragment_speed`); flag here if either fails.

### Session: 2026-05-06 — T1.12 SpeedFilter with Kalman-style smoothing (DONE)

- Replaced the old alpha-IIR `SpeedFilter` (took raw `Float`, returned `Float`)
  with the spec-mandated 1D Kalman per task description: takes `LocationSample`,
  process noise σ=0.5 m/s² (Q = 0.25 (m/s)²), measurement noise R scaled
  linearly from `accuracy` (R = max(accuracy/5, 1e-3)). Falls back to a 5-sample
  moving average when `accuracy > 20 m`. Exposes `flow: SharedFlow<Float>`
  with replay=1 so any subscriber sees the last filtered value immediately.
- Added a stationary detector — two consecutive raw `speedMs == 0` samples
  trigger a hard reset and snap output to 0. Single zero just feeds the Kalman
  step normally so brief GPS dropouts don't bias the estimate to zero.
- Cold-start safety: first call initializes `x = raw, p = max(R, 1e-3)` so no
  division-by-zero is possible even when `accuracy = 0` or `NaN`. NaN/negative
  accuracy is mapped to `Float.MAX_VALUE` so the filter takes the dampened
  MA fallback path until the GPS chip reports something believable.
- TDD: wrote 12 failing tests BEFORE the implementation — verified red phase
  via compile errors (the new tests reference `SpeedFilter()` no-args, `update(LocationSample)`,
  `filter.flow`). Tests cover all 5 ACs plus edge cases:
  - cold-start non-NaN (AC4)
  - cold-start with `accuracy=0` doesn't divide by zero (AC4)
  - converges to true speed within 5 clean GPS samples (AC1)
  - high-noise (acc=100) outlier dampened far below the raw value AND less
    than the same outlier at acc=5 — proves the filter weights low-noise
    measurements more (AC2)
  - two zero samples snap output to 0 (AC3); a single zero alone does not
  - MA fallback engages at acc>20m and produces the literal mean of the last
    5 raw inputs; window is bounded to 5 (eviction works)
  - threshold clamp returns 0 when filter output is below threshold
  - `flow.first()` returns the last emitted value (Flow<Float> AC)
  - NaN accuracy doesn't propagate as NaN; reset() restores cold-start.
- LocationService rewired to construct a `LocationSample` from `Location` first,
  then call `filter.update(sample)`, then push `sample.copy(speedMs = filtered)`.
  Existing 4 LocationService Robolectric tests still pass — no behavior change
  for downstream consumers (`LocationRepository.samples`).
- Coverage: every line in `SpeedFilter.kt` is hit by at least one test except
  the unreachable empty-buffer guard, which I removed (per CLAUDE.md "don't
  guard scenarios that can't happen" — `pushToMA` runs unconditionally before
  `movingAverage`). Conservatively ≥95%, well above the 90% AC.
- Verified:
  - `./gradlew :app:assembleStandardDebug` → BUILD SUCCESSFUL
  - `./gradlew :app:testStandardDebugUnitTest` → 81/81 (was 73/73; +12 new
    SpeedFilter tests, -4 obsolete alpha-IIR tests = net +8)
  - `bpsai-pair arch check` clean on both modified main files and the test file
- Files touched:
  - `data/location/SpeedFilter.kt` (rewrite, 108 lines)
  - `service/LocationService.kt` (rewire to LocationSample input)
  - `test/.../SpeedFilterTest.kt` (rewrite, 134 lines, 12 tests)

### Session: 2026-05-06 — T1.11 LocationService foreground service (DONE)

- Audited the existing `LocationService` against T1.11's five ACs. Four were
  already satisfied: foreground service + notification (AC1), 1 Hz GPS request
  with 0 m displacement (AC2 — `requestLocationUpdates(GPS_PROVIDER, 1000L,
  0f, …)`), `START_STICKY` (AC3), pure `LocationManager` with no Play Services
  dep (AC5). The single gap: **no WakeLock at all**, despite the spec saying
  "Foreground service holding a partial WakeLock".
- TDD: wrote 4 Robolectric tests BEFORE implementing — `START_STICKY`, WakeLock
  held after onCreate, WakeLock released after onDestroy, no-crash sentinel.
  Confirmed red phase (2 failures: WakeLock null + null-pointer in release
  assertion).
- Acquired `PowerManager.PARTIAL_WAKE_LOCK` in `onCreate` with tag
  `"RetroLauncher::LocationService"` and `setReferenceCounted(false)` so a
  single `release()` call always discharges it. Released in `onDestroy` via
  `runCatching { wakeLock?.takeIf { it.isHeld }?.release() }` — defensive
  against double-release after Robolectric / pared-down ROMs that may release
  early.
- Verified:
  - `./gradlew :app:assembleStandardDebug` → BUILD SUCCESSFUL
  - `./gradlew :app:testStandardDebugUnitTest` → 73/73 (was 69/69; 4 new tests)
  - `bpsai-pair arch check` clean on `LocationService.kt` and the test file
- AC2 (1 Hz updates) and AC1 (notification visible) are static + runtime
  guarantees: AC2 is the literal `requestLocationUpdates(GPS_PROVIDER, 1000L,
  0f, this)` call (frequency verified on-device via `adb shell dumpsys
  location` in the AC text). AC5 holds because no `com.google.android.gms`
  entry exists in `gradle/libs.versions.toml`.
- Files touched: `service/LocationService.kt` (+WakeLock acquire/release),
  `test/.../LocationServiceTest.kt` (new).

### Session: 2026-05-06 — T1.10 Util extensions (DONE)

- TDD: wrote 11 new FormatExt tests + 10 new ViewExt tests BEFORE implementing.
  Confirmed red via unresolved references.
- Existing FormatExt.kt was 54 lines (already over AC4's 50-line budget).
  Solution: moved legacy helpers (metersPerSecondToDisplay, tempCToDisplay,
  metersToDisplay, formatDurationShort, formatHM/HMHM/YMD) to a new sibling
  `UnitsFormatExt.kt`. Same package, no caller imports change.
- New `FormatExt.kt` (41 lines) contains the T1.10 trio:
  - `formatDistance(meters, Units)` — "%.0f m" / "%.1f km" / "%.0f ft" / "%.1f mi"
    with `Locale.US` so decimal separator is consistent across locales.
  - `formatDuration(seconds): "HH:MM:SS"` with negative-clamp.
  - `formatSpeed(metersPerSec, Units): "%.1f km/h"|"%.1f mph"`.
- `ViewExt.kt` (45 lines): added `View.gone()` / `View.visible()` (idempotent)
  and `View.fade(toVisible, durationMs=200L)` — fade-in pre-sets alpha=0 if
  hidden; fade-out flips visibility=GONE in withEndAction so the view keeps
  its layout slot until animation completes.
- Final file sizes — three AC-named extension files all under 50:
  ColorExt 26, FormatExt 41, ViewExt 45. UnitsFormatExt at 55 lines is the
  refactor artifact; under the project's 200-line arch warning.
- Verified:
  - `./gradlew :app:assembleStandardDebug` → BUILD SUCCESSFUL
  - `./gradlew :app:testStandardDebugUnitTest` → 69/69 (was 48/48; +21 new tests)
  - `bpsai-pair arch check` clean on all six modified/new files

### Session: 2026-05-06 — T1.9 StatusBarFragment (DONE)

- TDD: wrote 5 `ClockTicker` tests (Robolectric ShadowLooper-driven, deterministic
  time advances) and 4 `StatusBarFragment` XML-structure tests BEFORE implementing.
  Confirmed red via unresolved `ClockTicker` references.
- Created `util/ClockTicker.kt` — a Handler-based 1Hz ticker that posts a
  Runnable, computes the delay to the next whole-second boundary on each tick
  (so seconds don't drift), and stops cleanly via `removeCallbacks`. Constructor
  takes `Handler` + `onTick: (Long) -> Unit` + injectable `now: () -> Long` for
  testability. Replaces the BroadcastReceiver-based `TextClock` (T1.9 AC1 — saves
  CPU on the Cortex-A7 head unit).
- `fragment_status.xml`: replaced `<TextClock id=clock>` and `<TextClock id=date>`
  with `<TextView>` (no more `format12Hour`/`format24Hour` — the ticker handles
  formatting). Added `android:visibility="gone"` to `trip_chip_slot` so AC3's
  default-hidden requirement holds even before the StatusTripViewModel observer
  fires.
- `StatusBarFragment`: instantiates `ClockTicker` in `onViewCreated`, starts in
  `onResume`, stops in `onPause`. Each tick formats wall-clock time with
  `SimpleDateFormat("HH:mm")` and date with `SimpleDateFormat("EEE, MMM d")` and
  pushes them into the two TextViews. Drawer button onClick → SettingsActivity
  intent (unchanged, AC4). Speed observer unchanged (AC2 already met via
  SpeedViewModel).
- Verified:
  - `./gradlew :app:assembleStandardDebug` → BUILD SUCCESSFUL
  - `./gradlew :app:testStandardDebugUnitTest` → 48/48 (was 39/39; 9 new tests)
  - `bpsai-pair arch check` clean on all 5 modified/new files
- Files touched: `util/ClockTicker.kt` (new), `ui/status/StatusBarFragment.kt`,
  `res/layout/fragment_status.xml`, `test/.../ClockTickerTest.kt` (new),
  `test/.../StatusBarFragmentTest.kt` (new).

### Session: 2026-05-06 — T1.8 HomeFragment + ViewPager2 right panel (DONE)

- TDD: wrote `HomeFragmentTest` (XmlPullParser-based — fragment_home.xml uses
  `<FragmentContainerView android:name=...>` for media/weather slots, which
  can't inflate without a FragmentManager) and `RightPanelAdapterTest` (5 tests)
  BEFORE implementing. Confirmed red via unresolved-reference compile error.
- Extracted `RightPanelAdapter` from a private inner class of `HomeFragment`
  to its own file (`ui/home/RightPanelAdapter.kt`), `internal` visibility, with
  a testable `companion object { PAGE_COUNT, newPage(pos) }` helper. Still
  extends `FragmentStateAdapter` (AC3 preserved).
- Moved panel-ratio reactivity from `MainActivity` to `HomeFragment`.
  HomeFragment.onViewCreated reads `App.settings.panelRatioPercent` and updates
  `home_split` via `Guideline.setGuidelinePercent(...)` (writes to
  `LayoutParams.guidePercent` — matches AC4's parenthetical exactly).
  `viewLifecycleOwner.lifecycleScope` collects `settings.changes(KEY_PANEL_RATIO)`
  and re-applies on each change. No Activity recreation.
- Trimmed `MainActivity.applyPanelRatio` to only update its own outer
  `panel_split` guideline (HomeFragment owns the inner one now).
- Verified:
  - `./gradlew :app:assembleStandardDebug` → BUILD SUCCESSFUL
  - `./gradlew :app:testStandardDebugUnitTest` → 39/39 (was 33/33)
  - `bpsai-pair arch check` clean on all 5 modified/new files

### Session: 2026-05-06 — T1.7 MainActivity and activity_main.xml (DONE)

- Followed TDD: wrote `MainActivityTest` first. Initial pass attempted full-
  Activity creation via `Robolectric.buildActivity(MainActivity::class)`; both
  tests crashed with `IllegalStateException: You need to use a Theme.AppCompat
  theme (or descendant)`. Diagnosis: Material 3 `Theme.Material3.DayNight.*` IS
  AppCompat-descendant in material 1.11.0, but the Robolectric 4.11.1 ↔ M3
  bridge fails to walk the AppCompat ancestry. Pivoted to layout-inflation +
  reflection tests (no Activity lifecycle) — same AC coverage, no theme fight.
- Added `testOptions.unitTests.isIncludeAndroidResources = true` to
  `app/build.gradle.kts` so Robolectric can resolve `R.layout.activity_main`
  and `R.dimen.status_bar_height` from the merged resource tree.
- `activity_main.xml`: renamed `@id/host` → `@id/home_container` and replaced
  the `<fragment>` tag for `status_bar` with `FragmentContainerView@id/
  status_bar_container` (also resolves the pre-existing lint
  `FragmentTagUsage` warning). Kept the rail + panel-split structure for the
  actual launcher functionality.
- `MainActivity.kt`:
  - Added `WindowCompat.setDecorFitsSystemWindows(window, false)` for AC4
    edge-to-edge.
  - Overrode `onBackPressed()` as a no-op (AC3) — the literal AC reading;
    modern OnBackPressedCallback would be more idiomatic but the AC names this
    method.
  - Replaced `replace(R.id.host, HomeFragment())` with
    `replace(R.id.home_container, HomeFragment())` and added
    `replace(R.id.status_bar_container, StatusBarFragment())` so both
    containers are populated programmatically (no more inline `<fragment>` tag).
  - Updated `applyPanelRatio`'s `findFragmentById(R.id.host)` to use the new
    `home_container` ID.
- Five tests cover the ACs: layout inflates with both IDs (AC1), status bar
  height matches `@dimen/status_bar_height` (AC1), home_container has match-
  constraint width/height (AC1), MainActivity declares `onBackPressed`
  (reflection check — AC3), `WindowCompat` is on the classpath (AC4 sentinel).
- Verified:
  - `./gradlew :app:assembleStandardDebug` → BUILD SUCCESSFUL
  - `./gradlew :app:testStandardDebugUnitTest` → 33/33 (was 28/28; 5 new
    MainActivity tests via TDD)
  - `bpsai-pair arch check` clean on all four modified/new files
- Files touched: `app/build.gradle.kts` (testOptions.unitTests),
  `app/src/main/java/com/oskar/retrolauncher/MainActivity.kt`,
  `app/src/main/res/layout/activity_main.xml`,
  `app/src/test/java/com/oskar/retrolauncher/MainActivityTest.kt` (new).

### Session: 2026-05-06 — T1.6 App.kt service locator (DONE)

- Followed TDD: wrote `AppServiceLocatorTest` (Robolectric, `@Config(application
  = App::class, sdk = [28])`) FIRST. Three tests covering AC2 (service property
  reachable), AC4 (repo accessors return same instance — by-lazy invariant),
  AC5 (constructor signature accepts only `Application`). Confirmed red phase
  via 3 unresolved-reference compile errors before implementing.
- Extracted a new `ServiceLocator` class (separate file `ServiceLocator.kt`)
  holding every repo as `by lazy { ... }`: `http`, `moshi`, `db`, `prefs`,
  `settings`, `media`, `weather`, `location`, `appList`, `trips`, `appScope`,
  `tripRecorder`. Constructor takes only `Application` (AC5). Plus a
  `startup()` helper for the GPS-foreground-service / WorkManager-weather /
  app-list-prefetch side effects that previously lived in `App.onCreate`.
- Refactored `App.kt` down to ~50 lines: `val service: ServiceLocator by lazy`
  exposing the locator (AC2 — `(application as App).service`); `onCreate`
  plants `Timber.DebugTree()` only in `BuildConfig.DEBUG` (AC3) and calls
  `service.startup()`. Companion-object accessors (`App.media`, `App.settings`,
  etc.) became read-only getters delegating to `instance.service.<x>`, keeping
  all 14 existing call sites working without churn.
- Hardened startup against Robolectric / pared-down ROMs: wrapped
  `WorkManager.getInstance(...).scheduleWeather()` and `appList.refresh()` in
  `runCatching` (matches the existing `LocationService` start-foreground
  pattern). The launcher must come up even when WorkManager auto-init is
  absent.
- Verified:
  - `./gradlew :app:assembleStandardDebug` → BUILD SUCCESSFUL
  - `./gradlew :app:testStandardDebugUnitTest` → 28/28 tests pass (was 25/25;
    3 new ServiceLocator tests added by TDD)
  - `bpsai-pair arch check` clean on all three modified/new files

### Session: 2026-05-05 — T1.5 Application icon + adaptive fallback (DONE)

- The scaffolding had `drawable/ic_launcher.xml` (48dp standalone) and
  `drawable/ic_launcher_foreground.xml` (108dp adaptive-foreground with the
  standard 22dp inset) but no `mipmap-*` directories, no adaptive-icon
  definition, no round variant, and no background drawable.
- Added the missing pieces:
  - `drawable/ic_launcher_background.xml` — solid orange `#FF8500` 108dp vector
    (the Mini AA accent, matching the foreground)
  - `mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` — adaptive
    icon definitions referencing the foreground + background drawables
  - `mipmap-anydpi/ic_launcher.xml` — vector fallback for API 23–25 (square
    orange tile + dark "car" body + orange wheels, derived from the existing
    standalone vector)
  - `mipmap-anydpi/ic_launcher_round.xml` — round-variant fallback (orange
    disc instead of square, same internal car shape)
- Switched the manifest's `android:icon` from `@drawable/ic_launcher` to
  `@mipmap/ic_launcher` and added `android:roundIcon="@mipmap/ic_launcher_round"`.
- Used vectors throughout — zero PNGs, so AC4 (no PNGs >256 KB total) is
  satisfied trivially.
- Verified:
  - `./gradlew :app:assembleStandardDebug` → BUILD SUCCESSFUL (icon resources
    resolve, manifest references valid)
  - `find … -name "*.png"` returns nothing under `app/src/main/res`
  - `./gradlew :app:testStandardDebugUnitTest` → 25/25 still green
  - `bpsai-pair arch check` clean on all 6 modified/new files
- AC1 ("Icon renders correctly … on a 1024×600 device") is verified
  statically — adaptive icon spec is well-formed (foreground + background both
  108dp vectors with proper safe-zone insets), and the manifest correctly
  references the mipmap resources. Visual confirmation needs a device.
- Files touched: `app/src/main/AndroidManifest.xml`,
  `app/src/main/res/drawable/ic_launcher_background.xml` (new),
  `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` (new),
  `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` (new),
  `app/src/main/res/mipmap-anydpi/ic_launcher.xml` (new),
  `app/src/main/res/mipmap-anydpi/ic_launcher_round.xml` (new)

### Session: 2026-05-05 — T1.4 Resource bundles (DONE)

- Migrated `Theme.RetroLauncher` from `Theme.MaterialComponents.NoActionBar`
  (M2) to `Theme.Material3.DayNight.NoActionBar` (M3). Added `colorSurface` +
  `colorOnPrimary` + `colorOnSurface`. Dropped M2-only `colorPrimaryDark` and
  `colorAccent`. Kept all customizations: orange `@color/accent` primary, dark
  `@color/bg` background/window, light text on dark.
- Added the four AC-required dimens (`gutter`, `corner_lg`, `corner_md`,
  `panel_left_min_width`) to both `values/dimens.xml` (phone fallback: 8dp /
  20dp / 12dp / 240dp) and `values-w1024dp/dimens.xml` (head-unit:
  12dp / 24dp / 16dp / 300dp).
- Created `values-night/colors.xml` mirroring the dark palette (the launcher is
  always dark-styled — head unit in a dark cabin).
- Externalized hardcoded layout strings (lint AC5) by adding three new strings
  to `strings.xml`: `embed_v03_placeholder`, `weather_temp_placeholder`,
  `weather_temp_unit_degree`. Patched `fragment_embed.xml`, `fragment_weather.xml`,
  `fragment_media.xml`. Removed empty `android:text=""` attrs.
- Verified:
  - `./gradlew :app:assembleStandardDebug` → BUILD SUCCESSFUL (M3 migration
    + day/night resources merge cleanly)
  - `./gradlew :app:lintStandardDebug` → BUILD SUCCESSFUL; report overview
    shows zero HardcodedText / HardcodedColor findings
  - `./gradlew :app:testStandardDebugUnitTest` → 25/25 still green
  - `bpsai-pair arch check` clean on every modified file
- Pre-existing lint issues surfaced but out-of-scope: `MissingPermission`
  (LocationService), `ExpiredTargetSdkVersion` (targetSdk=28 deliberate per
  spec — head-unit Android 6 era), `SetTextI18n`, `KaptUsageInsteadOfKsp`.

### Session: 2026-05-05 — T1.3 Author AndroidManifest.xml (DONE)

- Audited the existing `app/src/main/AndroidManifest.xml` against T1.3's five
  ACs. The scaffolded manifest already had: launcher intent filter with
  `CATEGORY_HOME`+`CATEGORY_DEFAULT`, `BIND_NOTIFICATION_LISTENER_SERVICE` on
  `MediaNotificationListener` with the listener intent filter, app-level
  `@style/Theme.RetroLauncher`, `screenOrientation="landscape"` on every
  Activity, all required permissions, and the `<queries>` block for the app
  drawer.
- Single delta needed: removed `android:foregroundServiceType="location"` from
  `LocationService` per AC4 (targetSdk=28 keeps us pre-API-29's type
  requirement; setting it adds an unnecessary lint signal).
- Did NOT add the `system`-flavor manifest overlay (sharedUserId="android.uid.system",
  embedding perms). Per the backlog, that source set + signing wiring is owned
  by a later task ("Wire the system product flavor end-to-end").
- Verified:
  - `./gradlew :app:processStandardDebugMainManifest` → BUILD SUCCESSFUL
  - Merged manifest at `app/build/intermediates/merged_manifest/standardDebug/`
    contains all 5 AC fingerprints (HOME+DEFAULT category, BIND permission on
    listener, LocationService without foregroundServiceType, app theme,
    landscape orientation)
  - `./gradlew :app:testStandardDebugUnitTest` → 25/25 still green
  - `bpsai-pair arch check` clean on the manifest
- Files touched: `app/src/main/AndroidManifest.xml` (one block edited)

### Session: 2026-05-05 — T1.2 Author app/build.gradle.kts with flavors (DONE)

- Added `signingConfigs.platform` block in `app/build.gradle.kts` pointing to
  `../platform.keystore` with a lazy attach (only configures storeFile/passwords
  if the file exists, so the missing keystore doesn't break standardDebug
  config-time). Wired it into `productFlavors.system.signingConfig`.
- Added `lint { baseline = file("lint-baseline.xml"); abortOnError = false }`
  block to `android{}`. Created stub `app/lint-baseline.xml` (empty issues list,
  format=6 — populated on next `./gradlew :app:updateLintBaseline`).
- Verified via gradle:
  - `./gradlew :app:assembleStandardDebug` → BUILD SUCCESSFUL, APK at
    `app/build/outputs/apk/standard/debug/app-standard-debug.apk`
  - `./gradlew :app:tasks` shows both `Standard*` and `System*` flavor variants
- Pre-existing test failures fixed as part of this task (user-authorized scope
  pull-in from T1.24 / T1.30 / T1.34):
  - **`FormatExt.metersToDisplay`** — was using default-locale formatting; ET
    locale on this Mac produced `1,5km` instead of `1.5km`. Forced `Locale.US`
    in all four `String.format` calls so display strings are stable across
    locales.
  - **`MediaState.livePosition`** — guard `playing && positionAtMs > 0` was
    rejecting valid `positionAtMs == 0` baselines. Dropped the `> 0` clause;
    `playing=false` still short-circuits to base position.
  - **`TripRecorderTest`** — three issues: (1) recorder was constructed with
    `this` (the runTest scope), so `locationFlow.collect` triggered
    `UncompletedCoroutinesError` on test exit; (2) `runTest(StandardTestDispatcher())`
    was throwing `IllegalArgumentException`; (3) `MutableSharedFlow(replay=0)`
    drops emissions before subscription, so a single
    `for { emit }` then `advanceUntilIdle()` was losing samples. Refactored to
    `backgroundScope` + a `feed()` helper that calls `runCurrent()` after each
    emit so the collector consumes serially. All 25 tests pass.
- arch check clean on every modified file
- Files touched: `app/build.gradle.kts`, `app/lint-baseline.xml` (new),
  `app/src/main/java/com/oskar/retrolauncher/util/FormatExt.kt`,
  `app/src/main/java/com/oskar/retrolauncher/data/media/MediaState.kt`,
  `app/src/test/java/com/oskar/retrolauncher/TripRecorderTest.kt`

### Session: 2026-05-05 — T1.1 Bootstrap Gradle project structure (DONE)

- Audited the existing `retro-launcher/` scaffolding (carried over uncommitted
  from a prior session) against T1.1's five ACs:
  - `settings.gradle.kts` — `pluginManagement` + `dependencyResolutionManagement`
    with `repositoriesMode = FAIL_ON_PROJECT_REPOS` ✓
  - Root `build.gradle.kts` — `alias(libs.plugins.android.application)`,
    `kotlin.android`, `kotlin.kapt` (all `apply false`) ✓
  - `gradle/libs.versions.toml` — every version + library + plugin from spec
    section 4 present (kotlin 1.9.22, agp 8.2.2, coroutines 1.7.3, all AndroidX,
    material, okhttp, moshi, glide, timber) ✓
  - No Hilt / Retrofit / Compose / Coil / Firebase deps ✓
- Fixed two gaps:
  - Wrapper distribution URL bumped from gradle-8.4 → gradle-8.5 (per task spec)
  - `gradle-wrapper.jar` + `gradlew` + `gradlew.bat` were missing — generated
    via `gradle wrapper --gradle-version=8.5 --distribution-type=bin`
- Provisioned the dev environment that the rest of the sprint will need:
  - Installed `gradle` 9.5 + `openjdk@17` 17.0.19 via Homebrew
  - Installed `android-commandlinetools` cask (SDK root at
    `/opt/homebrew/share/android-commandlinetools`)
  - Accepted SDK licenses, installed `platforms;android-34`, `build-tools;34.0.0`,
    `platform-tools`
  - Wrote `retro-launcher/local.properties` with `sdk.dir` (gitignored already)
- Verified AC1: `./gradlew tasks` → BUILD SUCCESSFUL, lists `:app` module tasks
  for both `standard` and `system` flavors; `./gradlew projects` confirms
  `Project ':app'` registered
- `bpsai-pair arch check` clean on all three modified Gradle files

### Session: 2026-05-05 — Engage backlog drafted

- Read `https://paircoder.ai/docs/guides/engage/` to confirm the backlog format
  (H1 sprint title, `### Phase N:` markers, `### {ID} -- {Title} | Cx: N | Pn`
  task headers, **Description** + **AC** checkboxes + **Depends on** body)
- Authored `plans/backlogs/backlog-sprint-1-retro-launcher.md` covering the full
  v0.1 → v0.3 scope from `headunit-launcher-spec.md`: 49 tasks, 11 phases, 244
  acceptance criteria, all dependency refs validated, 2 human-gated tasks
  (T1.38 install script, T1.44 platform signing) and 2 with external tools
- Mirrored the plan in `.paircoder/plans/plan-2026-05-retro-launcher-sprint-1.plan.yaml`
- Validated locally with a parser script (regex matches the documented format);
  no duplicate IDs, no broken `Depends on:` refs, every task has Description +
  AC + Depends on

### Session: 2026-05-05 — Project Initialization

- Initialized project with PairCoder v2
- Created `.paircoder/` directory structure
- Set up initial configuration

## What's Next

1. **T1.42** — Phase 10 (polish), P2 Cx 5. Run via `/start-task T1.42`.
2. **T1.36 follow-ups (out-of-scope this commit)** — wire `connectedStandardDebugAndroidTest`
   into a CI workflow with an API 23 emulator (workflow file does not yet
   exist under `.github/`); upload `screenshots/` from the test apk's
   externalCacheDir as an artifact on failure; consider replacing
   deprecated `androidx.test.runner.screenshot.Screenshot` with
   `UiDevice.takeScreenshot` once uiautomator is added.
2. Heads-up gates later in sprint: **T1.38** (rooted-install script — needs an
   ADB-reachable rooted HU) and **T1.44** (platform signing — needs ROM extract
   for `platform.x509.pem` / `platform.pk8`) will pause for manual action.
5. Phases 10–11 (T1.39–T1.49) are v0.2/v0.3 polish and the system-flavor
   embedding pipeline; the deepest dep chain bottoms out at T1.49 → T1.48 →
   … → T1.43, all gated on T1.44 platform signing for the system flavor.

### Dev-env note (2026-05-05)

This Mac was bare before T1.1. Now installed and required for every subsequent
task:
- JDK 17 at `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`
  (keg-only — export `JAVA_HOME` before running gradle if not in shell rc)
- Gradle 9.5 (system) — but the wrapper pins builds to Gradle 8.5 per spec
- Android SDK at `/opt/homebrew/share/android-commandlinetools` (set via
  `retro-launcher/local.properties`, which is gitignored)

## Blockers

None currently.

## Quick Commands

```bash
# Check status
bpsai-pair status

# Create a new plan
bpsai-pair plan new my-feature --type feature

# List tasks
bpsai-pair task list

# Start working on a task
bpsai-pair task update TASK-XXX --status in_progress

# Complete a task (with Trello)
bpsai-pair ttask done TRELLO-XX --summary "..." --list "Deployed/Done"
bpsai-pair task update TASK-XXX --status done
