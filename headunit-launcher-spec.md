# Retro Launcher — Implementation Plan

A modern Android Auto–style launcher targeting **Android 6.0+ (API 23)** head units, optimized for a **7-inch / 1024×600 landscape** display. Personal-use build, rooted device, Mini AA feature parity.

> Working name: **`retro-launcher`** (because we're going back to API 23). Rename freely.

---

## 1. Executive summary

Mini AA is gorgeous, but it requires API 29+ for several reasons that are mostly cosmetic, not fundamental:

| Mini AA dependency | API gate | Replaceable on Android 6? |
|---|---|---|
| Picture-in-Picture for app embedding | 26 | Yes — via `VirtualDisplay` + reflection (rooted) |
| Notification channels | 26 | Yes — wrap with `NotificationCompat`, no-op on 6 |
| Adaptive icons | 26 | Yes — fall back to legacy bitmap icons |
| `getSystemService(Class<T>)` overloads | 23 | Already on 6 |
| Vector drawables / ConstraintLayout | – | AndroidX backports cover everything |
| `WindowInsetsCompat` edge-to-edge | – | AndroidX |
| Coroutines / Flow | – | Pure Kotlin, fine |

The **only feature that fundamentally cannot be replicated as-is on Android 6 is true PiP**. We replace it with a `VirtualDisplay`-based embedding that reads off a `SurfaceTexture` and forwards input through root — which actually works *better* on a head unit because we control the entire window stack.

Everything else (media, weather, speed, trips, app grid, configurable layout) is a straight port — most of these APIs have been around since API 21 or earlier.

**Critical constraint that shapes the whole design:** Chinese head units with Android 6 almost never have Google Play Services. So **no Firebase, no Maps SDK, no FusedLocationProvider, no Play services Auth, no GMS Tasks**. All replacements use AOSP-only APIs.

---

## 2. Targets

### Hardware
- **CPU:** Quad-core ARM Cortex-A7/A53, ~1.2–1.6 GHz (typical Allwinner T3 / MTK MT8167)
- **RAM:** 1–2 GB
- **Storage:** 16 GB eMMC
- **Display:** 1024×600 IPS, capacitive touch, **landscape only**
- **GPS:** Discrete u-blox or via 4G modem; raw `LocationManager.GPS_PROVIDER` available
- **Network:** Wi-Fi (often via phone tether) and/or 4G dongle
- **Audio:** Pre-amp output to car stereo, mic via Bluetooth or aux

### Software
- **OS:** Android 6.0.1 (API 23), AOSP-based with manufacturer skin
- **Root:** Yes (assumed). System partition writable via `mount -o rw,remount /system` after `su`.
- **Play Services:** Assume **absent**.

### Build target
- `compileSdk = 34`, `minSdk = 23`, `targetSdk = 28` (avoid scoped storage / background location dialog regressions on a single-user head unit)
- Kotlin 1.9.x, AGP 8.2.x, JDK 17

---

## 3. Architecture overview

```
┌──────────────────────────────────────────────────────────────┐
│                       HomeActivity                           │
│  (single-Activity launcher, fragments inside ViewPager)      │
│                                                              │
│  ┌───────────┐  ┌─────────────────────────────────────────┐  │
│  │ LeftPanel │  │ RightPanel (swipeable: map/grid/trips)  │  │
│  │           │  │                                         │  │
│  │ Media     │  │  ┌─────────┐  ┌─────────┐  ┌─────────┐ │  │
│  │ Weather   │  │  │ Embed   │  │ AppGrid │  │ Trips   │ │  │
│  │           │  │  └─────────┘  └─────────┘  └─────────┘ │  │
│  └───────────┘  └─────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐    │
│  │ StatusBar  (clock · speed · trip stats · drawer)     │    │
│  └──────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────┘
                            ▲
                            │ LiveData / StateFlow
        ┌───────────────────┼───────────────────┐
        │                   │                   │
   ┌─────────┐         ┌─────────┐         ┌─────────┐
   │ Media   │         │ Weather │         │ Speed   │
   │Repository│         │Repo     │         │Repo     │
   └────┬────┘         └────┬────┘         └────┬────┘
        │                   │                   │
   ┌────▼────────┐    ┌─────▼──────┐    ┌──────▼──────┐
   │ Notification│    │ OpenWeather│    │ Location    │
   │ Listener    │    │   API      │    │ Service     │
   │ Service     │    │ (OkHttp)   │    │ (foreground)│
   └─────────────┘    └────────────┘    └──────┬──────┘
                                               │
                                          ┌────▼────┐
                                          │ Trip    │
                                          │ Recorder│
                                          │ (Room)  │
                                          └─────────┘
```

### Key design choices
- **Single Activity** — fewer transitions, faster on weak CPUs, simpler back stack.
- **Traditional Views (XML)** with View Binding — Compose works on API 21+ but rendering perf on a Cortex-A7 is mediocre and the head unit GPU is weak. Views are battle-tested.
- **MVVM** — `ViewModel` per fragment, `LiveData` for UI state, `Flow` inside repos.
- **Foreground service** for location/trip recording — survives Doze, survives launcher restart.
- **No DI framework** — manual wiring via an `App.kt` service locator. Hilt/Koin add overhead for a 4-screen app.
- **Room** for trip database. Simple, type-safe, debugged.
- **OkHttp + Moshi** for weather. No Retrofit needed at this scale.
- **Glide** for album art (reactivates `Bitmap` reuse — important on 1 GB RAM).
- **AndroidX Palette** for dominant-color extraction.

### Data flow example: media tile updates album art

1. User starts Spotify → Spotify posts a `MediaStyle` notification.
2. `MediaNotificationListener` (extends `NotificationListenerService`) receives `onNotificationPosted`.
3. Listener extracts `MediaSession.Token`, builds a `MediaController`, registers `Callback`.
4. On `onMetadataChanged` it pushes `MediaState(title, artist, art, duration, position)` into `MediaRepository.state` (a `MutableStateFlow`).
5. `MediaViewModel` collects the flow, transforms to `MediaUiState`, exposes as `LiveData`.
6. `MediaFragment` observes; Glide loads art into `ImageView`; Palette extracts dominant color; coordinator animates background gradient.

---

## 4. Tech stack & dependencies

### `gradle/libs.versions.toml`

```toml
[versions]
kotlin = "1.9.22"
agp = "8.2.2"
coroutines = "1.7.3"

androidx-core = "1.10.1"          # last version supporting API 19; we can go newer
androidx-appcompat = "1.6.1"
androidx-constraintlayout = "2.1.4"
androidx-recyclerview = "1.3.2"
androidx-viewpager2 = "1.0.0"
androidx-fragment = "1.6.2"
androidx-lifecycle = "2.6.2"
androidx-room = "2.6.1"
androidx-work = "2.9.0"
androidx-palette = "1.0.0"
androidx-preference = "1.2.1"
material = "1.11.0"

okhttp = "4.12.0"
moshi = "1.15.0"
glide = "4.16.0"
timber = "5.0.1"

[libraries]
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }

androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "androidx-core" }
androidx-appcompat = { module = "androidx.appcompat:appcompat", version.ref = "androidx-appcompat" }
androidx-constraintlayout = { module = "androidx.constraintlayout:constraintlayout", version.ref = "androidx-constraintlayout" }
androidx-recyclerview = { module = "androidx.recyclerview:recyclerview", version.ref = "androidx-recyclerview" }
androidx-viewpager2 = { module = "androidx.viewpager2:viewpager2", version.ref = "androidx-viewpager2" }
androidx-fragment-ktx = { module = "androidx.fragment:fragment-ktx", version.ref = "androidx-fragment" }
androidx-lifecycle-viewmodel = { module = "androidx.lifecycle:lifecycle-viewmodel-ktx", version.ref = "androidx-lifecycle" }
androidx-lifecycle-runtime = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "androidx-lifecycle" }
androidx-lifecycle-livedata = { module = "androidx.lifecycle:lifecycle-livedata-ktx", version.ref = "androidx-lifecycle" }
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "androidx-room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "androidx-room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "androidx-room" }
androidx-work = { module = "androidx.work:work-runtime-ktx", version.ref = "androidx-work" }
androidx-palette = { module = "androidx.palette:palette-ktx", version.ref = "androidx-palette" }
androidx-preference = { module = "androidx.preference:preference-ktx", version.ref = "androidx-preference" }
material = { module = "com.google.android.material:material", version.ref = "material" }

okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
moshi = { module = "com.squareup.moshi:moshi-kotlin", version.ref = "moshi" }
moshi-codegen = { module = "com.squareup.moshi:moshi-kotlin-codegen", version.ref = "moshi" }
glide = { module = "com.github.bumptech.glide:glide", version.ref = "glide" }
glide-compiler = { module = "com.github.bumptech.glide:compiler", version.ref = "glide" }
timber = { module = "com.jakewharton.timber:timber", version.ref = "timber" }
```

### Notably **not** included
- Hilt / Dagger / Koin — manual wiring instead.
- Retrofit — OkHttp + Moshi is enough for one endpoint.
- Compose — perf cost on weak GPU not worth it.
- Firebase / GMS — head unit likely doesn't have Play Services.
- Coil — Glide handles bitmap pooling better on low RAM.

---

## 5. Project structure

```
retro-launcher/
├── app/
│   ├── src/main/
│   │   ├── java/com/oskar/retrolauncher/
│   │   │   ├── App.kt                       # Application + service locator
│   │   │   ├── MainActivity.kt              # the launcher itself
│   │   │   ├── ui/
│   │   │   │   ├── home/                    # HomeFragment, layout coordinator
│   │   │   │   ├── media/                   # media tile + viewmodel
│   │   │   │   ├── weather/                 # weather tile + viewmodel
│   │   │   │   ├── speed/                   # speedometer view + viewmodel
│   │   │   │   ├── trips/                   # trip history fragment + adapter
│   │   │   │   ├── grid/                    # app drawer grid
│   │   │   │   ├── embed/                   # embedded-app surface (root only)
│   │   │   │   ├── status/                  # bottom status bar
│   │   │   │   └── settings/                # PreferenceFragment
│   │   │   ├── data/
│   │   │   │   ├── media/                   # MediaRepository, MediaState
│   │   │   │   ├── weather/                 # WeatherRepository, OpenWeather DTOs
│   │   │   │   ├── location/                # LocationRepository, SpeedFilter
│   │   │   │   ├── trip/                    # TripRepository, Room entities
│   │   │   │   ├── apps/                    # AppListRepository
│   │   │   │   └── prefs/                   # SettingsStore (SharedPreferences wrapper)
│   │   │   ├── service/
│   │   │   │   ├── LocationService.kt       # foreground service
│   │   │   │   ├── MediaNotificationListener.kt
│   │   │   │   └── WeatherWorker.kt         # WorkManager periodic task
│   │   │   ├── system/                      # root + reflection helpers
│   │   │   │   ├── Su.kt
│   │   │   │   ├── HiddenApi.kt
│   │   │   │   └── Embedding.kt
│   │   │   └── util/                        # extensions, formatters
│   │   ├── res/
│   │   │   ├── layout/                      # all XML layouts
│   │   │   ├── layout-land/                 # 7" landscape overrides
│   │   │   ├── values/                      # colors, dimens, themes
│   │   │   ├── values-w1024dp/              # 1024×600 specific dimens
│   │   │   ├── drawable/                    # gradients, glow, shapes
│   │   │   └── xml/                         # preferences.xml, prefs schema
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

Single module. The "modules" mentioned earlier are package-level — multi-module Gradle adds 30+ sec to incremental builds for no benefit at this scale.

---

## 6. Build configuration

### `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.oskar.retrolauncher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.oskar.retrolauncher"
        minSdk = 23
        targetSdk = 28
        versionCode = 1
        versionName = "0.1.0"

        // OpenWeather API key from local.properties — never commit
        buildConfigField(
            "String",
            "OWM_API_KEY",
            "\"${project.findProperty("OWM_API_KEY") ?: ""}\""
        )

        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug") // replace with platform key for embedding
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging {
        resources.excludes += listOf("META-INF/AL2.0", "META-INF/LGPL2.1")
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.android)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.work)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.preference)
    implementation(libs.material)

    implementation(libs.okhttp)
    implementation(libs.moshi)
    kapt(libs.moshi.codegen)
    implementation(libs.glide)
    kapt(libs.glide.compiler)

    implementation(libs.timber)
}
```

---

## 7. AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:sharedUserId="android.uid.system"  <!-- ONLY in system build, signed with platform key -->
    >

    <!-- Core -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- Location: speed, weather, trip -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <!-- Foreground services for trip recording (API 28+) -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

    <!-- Notification listener (for media controls) -->
    <uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
        tools:ignore="ProtectedPermissions" />

    <!-- Wake to keep screen on while driving -->
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <!-- Boot receiver to auto-launch -->
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <!-- App embedding (system build only) -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.INJECT_EVENTS"
        tools:ignore="ProtectedPermissions" />
    <uses-permission android:name="android.permission.INTERNAL_SYSTEM_WINDOW"
        tools:ignore="ProtectedPermissions" />

    <!-- Query installed apps for the drawer -->
    <queries>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent>
    </queries>

    <!-- Hardware features -->
    <uses-feature android:name="android.hardware.location.gps" android:required="false" />

    <!-- Flag the app as "lives on auxiliary display" — head unit -->
    <supports-screens
        android:smallScreens="true"
        android:normalScreens="true"
        android:largeScreens="true"
        android:resizeable="true"
        android:anyDensity="true" />

    <application
        android:name=".App"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.RetroLauncher"
        android:hardwareAccelerated="true"
        android:largeHeap="true">

        <!-- Launcher activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:screenOrientation="landscape"
            android:configChanges="orientation|screenSize|keyboardHidden|uiMode"
            android:resumeWhilePausing="true"
            android:theme="@style/Theme.RetroLauncher.Home">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Notification listener for media -->
        <service
            android:name=".service.MediaNotificationListener"
            android:exported="true"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService" />
            </intent-filter>
        </service>

        <!-- Foreground location/trip recorder -->
        <service
            android:name=".service.LocationService"
            android:exported="false"
            android:foregroundServiceType="location" />

        <!-- Boot receiver -->
        <receiver
            android:name=".service.BootReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.LOCKED_BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

    </application>
</manifest>
```

---

## 8. UI/UX design

### Layout grid (1024×600 landscape)

```
┌─────┬──────────────────────────────────────────────────────┐
│ 60  │          (no top bar — full bleed)                   │
│ A   │                                                      │
│ p   │   ┌────────────────┐  ┌──────────────────────────┐   │
│ p   │   │                │  │                          │   │
│ s   │   │  MEDIA TILE    │  │                          │   │
│     │   │   240 × 240    │  │   RIGHT PANEL            │   │
│ r   │   │                │  │   (map / grid / trips)   │   │
│ a   │   └────────────────┘  │                          │   │
│ i   │   ┌────────────────┐  │   600 × 480              │   │
│ l   │   │  WEATHER TILE  │  │                          │   │
│     │   │   240 × 110    │  │                          │   │
│     │   └────────────────┘  └──────────────────────────┘   │
│     │                                                      │
├─────┴──────────────────────────────────────────────────────┤
│ STATUS BAR — clock · trip stats · speed · drawer            │
│                                  60 px                      │
└─────────────────────────────────────────────────────────────┘
```

Default split: **40 / 60** (left/right). User-configurable to 30/70, 40/60, 50/50, 60/40, 70/30.

### Themes
- **Pure dark** (default): `#000000` background, `#FF8500` orange accent (Mini AA's exact accent), `#1F1F1F` cards
- **Slate**: `#0E1320` bg, `#3FA9F5` accent
- **Forest**: `#0B1410` bg, `#3DDC84` accent

### Typography
- **Roboto** family (bundled in framework, no font file shipped):
  - Display: Roboto Light 36 sp (temperature, speed)
  - Title: Roboto Medium 18 sp (track name)
  - Body: Roboto Regular 14 sp
  - Mono: Roboto Mono 12 sp (debug)

### Density buckets
On 1024×600 head units the system reports `mdpi` or `tvdpi`. Dimens go in `values-w1024dp` so the layout doesn't break on a phone in dev.

### Key dimens (`values-w1024dp/dimens.xml`)
```xml
<resources>
    <dimen name="rail_width">60dp</dimen>
    <dimen name="status_bar_height">60dp</dimen>
    <dimen name="tile_corner">16dp</dimen>
    <dimen name="tile_padding">16dp</dimen>
    <dimen name="media_tile_size">240dp</dimen>
    <dimen name="weather_tile_height">110dp</dimen>
    <dimen name="speed_text">28sp</dimen>
    <dimen name="temp_text">36sp</dimen>
    <dimen name="track_title">18sp</dimen>
    <dimen name="grid_icon">56dp</dimen>
    <dimen name="grid_label">12sp</dimen>
</resources>
```

---

## 9. Feature 1 — Media player

### What we need
- Auto-detect any music app currently playing.
- Show title, artist, album art, position/duration, play/pause/skip.
- Extract dominant color from album art for tile background.

### Approach
1. **`NotificationListenerService`** is the magic — it gets a `MediaSession.Token` from any `MediaStyle` notification.
2. From the token, build a `MediaController` to read metadata and send transport commands.
3. Exists since API 21, no quirks on 6.

### `MediaNotificationListener.kt`
```kotlin
class MediaNotificationListener : NotificationListenerService() {

    private val sessionManager by lazy {
        getSystemService(MediaSessionManager::class.java)
    }
    private val componentName by lazy {
        ComponentName(this, MediaNotificationListener::class.java)
    }
    private var activeController: MediaController? = null
    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(md: MediaMetadata?) = pushState()
        override fun onPlaybackStateChanged(state: PlaybackState?) = pushState()
        override fun onSessionDestroyed() {
            activeController = null
            App.media.clear()
        }
    }
    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        rebind(list.orEmpty())
    }

    override fun onListenerConnected() {
        sessionManager.addOnActiveSessionsChangedListener(sessionsListener, componentName)
        rebind(sessionManager.getActiveSessions(componentName))
    }

    override fun onListenerDisconnected() {
        sessionManager.removeOnActiveSessionsChangedListener(sessionsListener)
        activeController?.unregisterCallback(callback)
        activeController = null
    }

    private fun rebind(sessions: List<MediaController>) {
        // pick the playing controller, fall back to most recent
        val newCtrl = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull()
        if (newCtrl?.sessionToken == activeController?.sessionToken) return
        activeController?.unregisterCallback(callback)
        activeController = newCtrl
        activeController?.registerCallback(callback, Handler(Looper.getMainLooper()))
        pushState()
    }

    private fun pushState() {
        val ctrl = activeController ?: run { App.media.clear(); return }
        val md = ctrl.metadata
        val ps = ctrl.playbackState
        App.media.update(
            MediaState(
                title = md?.getString(MediaMetadata.METADATA_KEY_TITLE),
                artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST),
                durationMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0,
                positionMs = ps?.position ?: 0,
                positionAtMs = ps?.lastPositionUpdateTime ?: 0,
                speed = ps?.playbackSpeed ?: 1f,
                playing = ps?.state == PlaybackState.STATE_PLAYING,
                art = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART),
                packageName = ctrl.packageName,
            )
        )
    }

    fun controller(): MediaController? = activeController
}
```

### `MediaState.kt`
```kotlin
data class MediaState(
    val title: String? = null,
    val artist: String? = null,
    val durationMs: Long = 0,
    val positionMs: Long = 0,
    val positionAtMs: Long = 0,
    val speed: Float = 1f,
    val playing: Boolean = false,
    val art: Bitmap? = null,
    val packageName: String? = null,
) {
    val isEmpty: Boolean get() = title == null
    /** Live position: the controller only updates on state change, so we extrapolate. */
    fun livePosition(now: Long = SystemClock.elapsedRealtime()): Long =
        if (playing) positionMs + ((now - positionAtMs) * speed).toLong()
        else positionMs
}
```

### `MediaRepository.kt`
```kotlin
class MediaRepository {
    private val _state = MutableStateFlow(MediaState())
    val state: StateFlow<MediaState> = _state

    fun update(s: MediaState) { _state.value = s }
    fun clear() { _state.value = MediaState() }

    fun play()  = controller()?.transportControls?.play()
    fun pause() = controller()?.transportControls?.pause()
    fun next()  = controller()?.transportControls?.skipToNext()
    fun prev()  = controller()?.transportControls?.skipToPrevious()

    private fun controller() = MediaNotificationListenerHolder.instance?.controller()
}
```

### Album-art color extraction
```kotlin
fun Bitmap.dominantColorAsync(onResult: (Int) -> Unit) {
    Palette.from(this).generate { p ->
        val c = p?.getDominantColor(Color.parseColor("#1F1F1F")) ?: 0xFF1F1F1F.toInt()
        onResult(c.darkened(0.4f))
    }
}
private fun Int.darkened(factor: Float): Int {
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(this, hsl)
    hsl[2] = (hsl[2] * (1 - factor)).coerceIn(0f, 1f)
    return androidx.core.graphics.ColorUtils.HSLToColor(hsl)
}
```

### Edge case: lockdown mode
Some Chinese ROMs strip notification access. Detect on first launch:
```kotlin
fun NotificationListenerService.isEnabled(ctx: Context): Boolean {
    val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
    return flat?.contains(ctx.packageName) == true
}
```
If not enabled, deep-link with `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`.

---

## 10. Feature 2 — Weather card

### What we need
- Current temperature, high/low, condition icon.
- Refresh every 15 min, on first location lock.
- Reverse-geocode current location to a city name.

### Approach
- Use **OpenWeatherMap "One Call 3.0" API** (free tier, 1000 calls/day). Endpoint: `https://api.openweathermap.org/data/3.0/onecall?lat=..&lon=..&units=metric&appid=KEY`.
- Reverse-geocode using `android.location.Geocoder` if available, fall back to **Nominatim** (OSM) — free, no key. Mini AA already does this.
- Cache last good response in SharedPreferences with timestamp; serve stale-while-revalidating.
- Refresh via **WorkManager** every 15 minutes when network is available; one-shot trigger on app open if cached response is older than 5 min.

### `WeatherDto.kt` (Moshi)
```kotlin
@JsonClass(generateAdapter = true)
data class WeatherDto(
    val current: Current,
    val daily: List<Daily>,
    val timezone: String,
) {
    @JsonClass(generateAdapter = true)
    data class Current(
        val temp: Double,
        @Json(name = "feels_like") val feelsLike: Double,
        val weather: List<Condition>,
    )
    @JsonClass(generateAdapter = true)
    data class Daily(val temp: Temp, val weather: List<Condition>) {
        @JsonClass(generateAdapter = true) data class Temp(val min: Double, val max: Double)
    }
    @JsonClass(generateAdapter = true)
    data class Condition(val id: Int, val main: String, val description: String, val icon: String)
}
```

### `WeatherRepository.kt`
```kotlin
class WeatherRepository(
    private val ctx: Context,
    private val http: OkHttpClient,
    private val moshi: Moshi,
) {
    private val prefs = ctx.getSharedPreferences("weather", Context.MODE_PRIVATE)
    private val adapter = moshi.adapter(WeatherSnapshot::class.java)

    private val _state = MutableStateFlow(loadCached())
    val state: StateFlow<WeatherSnapshot?> = _state

    suspend fun refresh(lat: Double, lon: Double) {
        val url = "https://api.openweathermap.org/data/3.0/onecall?lat=$lat&lon=$lon" +
                 "&exclude=minutely,hourly,alerts&units=metric&appid=${BuildConfig.OWM_API_KEY}"
        val req = Request.Builder().url(url).build()
        runCatching {
            withContext(Dispatchers.IO) {
                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("OWM ${resp.code}")
                    val dto = moshi.adapter(WeatherDto::class.java).fromJson(resp.body!!.source())!!
                    val city = reverseGeocode(lat, lon)
                    WeatherSnapshot.from(dto, city, System.currentTimeMillis())
                }
            }
        }.onSuccess { snap ->
            _state.value = snap
            prefs.edit().putString("snap", adapter.toJson(snap)).apply()
        }.onFailure { Timber.w(it, "weather refresh failed") }
    }

    private suspend fun reverseGeocode(lat: Double, lon: Double): String =
        withContext(Dispatchers.IO) {
            // Try Geocoder first (works on most ROMs that ship Google services or have a backend)
            runCatching {
                Geocoder(ctx).getFromLocation(lat, lon, 1)?.firstOrNull()?.locality
            }.getOrNull()
            // Fallback to Nominatim
            ?: runCatching {
                val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon"
                val req = Request.Builder().url(url)
                    .header("User-Agent", "retro-launcher/0.1 (${BuildConfig.APPLICATION_ID})")
                    .build()
                http.newCall(req).execute().use { resp ->
                    JSONObject(resp.body!!.string())
                        .getJSONObject("address")
                        .let { it.optString("city").ifEmpty { it.optString("town") } }
                }
            }.getOrNull() ?: "—"
        }

    private fun loadCached(): WeatherSnapshot? =
        prefs.getString("snap", null)?.let { adapter.fromJson(it) }
}

@JsonClass(generateAdapter = true)
data class WeatherSnapshot(
    val tempC: Double, val highC: Double, val lowC: Double,
    val iconId: Int, val condition: String,
    val city: String, val asOfMs: Long
) {
    companion object {
        fun from(dto: WeatherDto, city: String, asOf: Long) = WeatherSnapshot(
            tempC = dto.current.temp,
            highC = dto.daily.firstOrNull()?.temp?.max ?: dto.current.temp,
            lowC  = dto.daily.firstOrNull()?.temp?.min ?: dto.current.temp,
            iconId = dto.current.weather.firstOrNull()?.id ?: 800,
            condition = dto.current.weather.firstOrNull()?.description.orEmpty(),
            city = city, asOfMs = asOf,
        )
    }
}
```

### `WeatherWorker.kt`
```kotlin
class WeatherWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val loc = App.location.lastKnown() ?: return Result.retry()
        App.weather.refresh(loc.latitude, loc.longitude)
        return Result.success()
    }
}

fun WorkManager.scheduleWeather() {
    val req = PeriodicWorkRequestBuilder<WeatherWorker>(15, TimeUnit.MINUTES)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    enqueueUniquePeriodicWork("weather", ExistingPeriodicWorkPolicy.KEEP, req)
}
```

### Icon mapping
OWM `iconId`s (200–804) → local vector drawables. One of those tedious mappings:
```kotlin
fun Int.toWeatherIcon(): Int = when (this) {
    in 200..232 -> R.drawable.ic_thunder
    in 300..321 -> R.drawable.ic_drizzle
    in 500..531 -> R.drawable.ic_rain
    in 600..622 -> R.drawable.ic_snow
    in 700..781 -> R.drawable.ic_fog
    800 -> R.drawable.ic_sun
    in 801..804 -> R.drawable.ic_cloud
    else -> R.drawable.ic_unknown
}
```

---

## 11. Feature 3 — Speedometer

### What we need
- Real-time speed from GPS (km/h or mph).
- Smooth ring indicator with idle pulse animation.
- Accurate at low speed (false-positive filter for GPS jitter when stationary).

### Approach
- Pure `LocationManager.GPS_PROVIDER`, request 1s updates, 0m min distance.
- Use `Location.getSpeed()` (m/s) — Android 6 reports it from NMEA data when available.
- Apply **simple alpha-beta filter** to smooth jitter; clamp to 0 below user-configurable threshold (default 3 km/h).
- Custom `View` with `Canvas` drawing for the ring — perf-cheap, no inflation overhead.

### `LocationService.kt` (foreground)
```kotlin
class LocationService : Service(), LocationListener {
    private val lm by lazy { getSystemService(LocationManager::class.java) }
    private val filter = SpeedFilter(threshold = 3f / 3.6f)
    private val notifId = 1001

    override fun onCreate() {
        super.onCreate()
        startForeground(notifId, buildNotification())
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this)
        }
    }

    override fun onLocationChanged(loc: Location) {
        val speedMs = if (loc.hasSpeed()) loc.speed else 0f
        val filtered = filter.update(speedMs)
        App.location.push(LocationSample(
            lat = loc.latitude, lon = loc.longitude,
            speedMs = filtered, bearing = loc.bearing,
            tsMs = loc.time, accuracy = loc.accuracy,
        ))
    }

    override fun onProviderDisabled(provider: String) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onStatusChanged(p: String?, s: Int, b: Bundle?) {}

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification = NotificationCompat.Builder(this, "loc")
        .setContentTitle("Retro Launcher")
        .setContentText("GPS active")
        .setSmallIcon(R.drawable.ic_gps)
        .setOngoing(true).setShowWhen(false)
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .build()
}

class SpeedFilter(val threshold: Float, val alpha: Float = 0.3f) {
    private var smooth = 0f
    fun update(raw: Float): Float {
        smooth = alpha * raw + (1 - alpha) * smooth
        return if (smooth < threshold) 0f else smooth
    }
}

data class LocationSample(
    val lat: Double, val lon: Double,
    val speedMs: Float, val bearing: Float,
    val tsMs: Long, val accuracy: Float,
)
```

### `SpeedometerView.kt`
```kotlin
class SpeedometerView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    private var speedKmh: Float = 0f
    private var maxSpeed: Float = 200f

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(4f)
        color = Color.parseColor("#FF8500")
        strokeCap = Paint.Cap.ROUND
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(4f)
        color = Color.parseColor("#1F1F1F")
    }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = dp(28f)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
    private val unitPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888")
        textSize = dp(12f)
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()
    private val pulse = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2000; repeatMode = ValueAnimator.REVERSE; repeatCount = INFINITE
        addUpdateListener { invalidate() }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); pulse.start() }
    override fun onDetachedFromWindow() { pulse.cancel(); super.onDetachedFromWindow() }

    fun setSpeed(kmh: Float) {
        speedKmh = kmh.coerceIn(0f, maxSpeed); invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val pad = dp(6f)
        rect.set(pad, pad, width - pad, height - pad)
        canvas.drawArc(rect, 135f, 270f, false, trackPaint)
        val sweep = (speedKmh / maxSpeed) * 270f
        ringPaint.alpha = if (speedKmh < 1f) (128 + 127 * pulse.animatedFraction).toInt() else 255
        canvas.drawArc(rect, 135f, sweep, false, ringPaint)
        canvas.drawText("%.0f".format(speedKmh), width / 2f, height / 2f, textPaint)
        canvas.drawText("km/h", width / 2f, height / 2f + dp(20f), unitPaint)
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density
}
```

---

## 12. Feature 4 — Trip recording

### What we need
- Auto-detect trip start (sustained motion above 5 km/h for >10 s).
- Auto-end on sustained stop (<3 km/h for >2 min) or ignition off proxy (no GPS for >5 min).
- Persist start/end times, distance, max & avg speed, geocoded start/end labels, simplified polyline.
- Calendar UI showing trips per day, swipe-to-delete.

### State machine
```
        ┌──────┐  speed > 5 km/h
        │ IDLE │──────────────►┐
        └──────┘  for 10 s     │
            ▲                  ▼
            │           ┌──────────┐
            │           │ RECORDING │
            │           └──────────┘
            │              │   │
   stop > 2m│              │   │ no fix > 5 min
            │              │   │   ↓ flush
            │  speed < 3km/h   │
            │  for 2 min       │
            └──────────────────┘
```

### Room schema
```kotlin
@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMs: Long,
    val endMs: Long,
    val distanceM: Double,
    val avgSpeedMs: Double,
    val maxSpeedMs: Double,
    val startLabel: String?,
    val endLabel: String?,
)

@Entity(
    tableName = "trip_points",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class, parentColumns = ["id"], childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("tripId")]
)
data class TripPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long, val tsMs: Long,
    val lat: Double, val lon: Double, val speedMs: Float,
)

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startMs DESC") fun all(): Flow<List<TripEntity>>
    @Query("SELECT * FROM trips WHERE startMs >= :from AND startMs < :to ORDER BY startMs DESC")
    fun byRange(from: Long, to: Long): Flow<List<TripEntity>>
    @Insert suspend fun insert(t: TripEntity): Long
    @Update suspend fun update(t: TripEntity)
    @Delete suspend fun delete(t: TripEntity)
    @Insert suspend fun insertPoints(points: List<TripPoint>)
    @Query("SELECT * FROM trip_points WHERE tripId = :id ORDER BY tsMs ASC") suspend fun points(id: Long): List<TripPoint>
}

@Database(entities = [TripEntity::class, TripPoint::class], version = 1)
abstract class AppDb : RoomDatabase() { abstract fun trips(): TripDao }
```

### `TripRecorder.kt`
```kotlin
class TripRecorder(
    private val scope: CoroutineScope,
    private val db: AppDb,
    private val locationFlow: SharedFlow<LocationSample>,
    private val geocoder: (Double, Double) -> String? = { _, _ -> null },
) {
    private enum class State { IDLE, RECORDING }
    private var state = State.IDLE
    private var startMs = 0L
    private var lastMoveMs = 0L
    private val points = ArrayDeque<LocationSample>()
    private var maxSpeed = 0f
    private var distance = 0.0

    init { scope.launch { locationFlow.collect(::onSample) } }

    private suspend fun onSample(s: LocationSample) {
        val kmh = s.speedMs * 3.6f
        when (state) {
            State.IDLE -> {
                if (kmh > 5f) {
                    if (lastMoveMs == 0L) lastMoveMs = s.tsMs
                    if (s.tsMs - lastMoveMs > 10_000) start(s)
                } else lastMoveMs = 0L
            }
            State.RECORDING -> {
                accumulate(s)
                if (kmh < 3f) {
                    if (lastMoveMs == 0L) lastMoveMs = s.tsMs
                    if (s.tsMs - lastMoveMs > 120_000) end(s)
                } else lastMoveMs = 0L
            }
        }
    }

    private fun start(s: LocationSample) {
        state = State.RECORDING; startMs = s.tsMs; maxSpeed = 0f; distance = 0.0
        points.clear(); points += s
    }

    private fun accumulate(s: LocationSample) {
        val prev = points.lastOrNull() ?: return run { points += s }
        val dist = haversine(prev.lat, prev.lon, s.lat, s.lon)
        if (dist > 5.0) { distance += dist; points += s }
        if (s.speedMs > maxSpeed) maxSpeed = s.speedMs
    }

    private suspend fun end(s: LocationSample) {
        val avg = if (s.tsMs > startMs) distance / ((s.tsMs - startMs) / 1000.0) else 0.0
        val first = points.first(); val last = points.last()
        val tripId = db.trips().insert(
            TripEntity(
                startMs = startMs, endMs = s.tsMs,
                distanceM = distance, avgSpeedMs = avg, maxSpeedMs = maxSpeed.toDouble(),
                startLabel = geocoder(first.lat, first.lon),
                endLabel = geocoder(last.lat, last.lon),
            )
        )
        db.trips().insertPoints(points.map {
            TripPoint(tripId = tripId, tsMs = it.tsMs, lat = it.lat, lon = it.lon, speedMs = it.speedMs)
        })
        state = State.IDLE; lastMoveMs = 0L; points.clear()
    }

    private fun haversine(la1: Double, lo1: Double, la2: Double, lo2: Double): Double {
        val r = 6_371_000.0
        val dLa = Math.toRadians(la2 - la1); val dLo = Math.toRadians(lo2 - lo1)
        val a = sin(dLa / 2).pow(2) + cos(Math.toRadians(la1)) * cos(Math.toRadians(la2)) * sin(dLo / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
```

### Trip history UI
- Top: month strip with day chips, today highlighted in accent.
- Tap a day → list of trips that day with start time, distance, avg/max km/h.
- Long-press → context (delete, share GPX).
- Swipe-left to delete with `ItemTouchHelper`.
- Tap a trip → detail screen with mini-map (use a `Canvas` drawing of normalized polyline; no map SDK to keep GMS-free).

---

## 13. Feature 5 — App grid + embedded apps

### App grid
Configurable columns × rows (default 4 × 3). Pulls every launchable activity:
```kotlin
class AppListRepository(private val pm: PackageManager) {
    fun launchables(): List<AppEntry> {
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(main, 0).map {
            AppEntry(
                label = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName,
                componentName = ComponentName(it.activityInfo.packageName, it.activityInfo.name),
                icon = it.loadIcon(pm),
            )
        }.sortedBy { it.label.lowercase() }
    }
}

data class AppEntry(
    val label: String,
    val packageName: String,
    val componentName: ComponentName,
    val icon: Drawable,
)
```

`RecyclerView` with `GridLayoutManager`, custom `ItemDecoration` for spacing. Long-press an app to pin to the **app rail** (left column).

### Pinning to rail
- Stored in SharedPreferences as a JSON array of component names.
- Default rail: Maps, Music, Phone, Settings, plus 1 user-pinned slot.

### Embedded apps (system build, rooted)

This is the spicy bit. On Android 6 with system signature we can do:

#### Option chosen: VirtualDisplay + InputManager reflection

```
                ┌──────────────────────────────┐
                │     Launcher (us)            │
                │  ┌─────────────────────┐     │
                │  │   TextureView       │     │
                │  │  (renders Surface   │     │
                │  │   from VirtualDisp.)│     │
                │  └─────────────────────┘     │
                └──────────────────────────────┘
                            ▲
                            │ Surface
                ┌───────────┴──────────────────┐
                │   VirtualDisplay (1024×600)  │
                │   created via DisplayManager │
                └───────────┬──────────────────┘
                            │ startActivity(intent, displayId)
                            ▼
                ┌──────────────────────────────┐
                │      Embedded app            │
                │  (Maps / YouTube / etc.)     │
                └──────────────────────────────┘
```

#### `Embedding.kt`
```kotlin
class Embedding(private val ctx: Context) {

    private var virtualDisplay: VirtualDisplay? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null
    private val displayManager = ctx.getSystemService(DisplayManager::class.java)

    /** Attach a TextureView and start receiving frames. Call from MainThread. */
    fun bind(textureView: TextureView, w: Int, h: Int, dpi: Int = 213) {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, ww: Int, hh: Int) {
                surfaceTexture = st
                surface = Surface(st)
                virtualDisplay = displayManager.createVirtualDisplay(
                    /* name = */ "retro-embed",
                    /* width = */ w,
                    /* height = */ h,
                    /* dpi = */ dpi,
                    /* surface = */ surface,
                    /* flags = */ DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
                                  DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
                                  // VIRTUAL_DISPLAY_FLAG_PRESENTATION makes apps think it's a real display
                                  (1 shl 6)
                )
            }
            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, ww: Int, hh: Int) {
                virtualDisplay?.resize(ww, hh, dpi)
            }
            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                release(); return true
            }
            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
        }
    }

    fun launch(intent: Intent) {
        val displayId = virtualDisplay?.display?.displayId ?: return
        val opts = ActivityOptions.makeBasic()
            .setLaunchDisplayId(displayId)        // hidden API on 6, exposed on 8+
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), opts.toBundle())
    }

    fun release() {
        virtualDisplay?.release(); virtualDisplay = null
        surface?.release(); surface = null
        surfaceTexture?.release(); surfaceTexture = null
    }

    /** Forward a touch from the TextureView to the embedded display. */
    fun forwardTouch(ev: MotionEvent, viewW: Int, viewH: Int, dispW: Int, dispH: Int) {
        val x = ev.x * dispW / viewW
        val y = ev.y * dispH / viewH
        val mapped = MotionEvent.obtain(
            ev.downTime, ev.eventTime, ev.action, x, y, ev.metaState
        )
        // virtual touch source
        mapped.source = InputDevice.SOURCE_TOUCHSCREEN
        HiddenApi.injectInputEvent(mapped, virtualDisplay?.display?.displayId ?: 0)
        mapped.recycle()
    }
}
```

#### `HiddenApi.kt` (reflection wrappers)
```kotlin
object HiddenApi {

    private val inputManager: Any by lazy {
        Class.forName("android.hardware.input.InputManager")
            .getMethod("getInstance").invoke(null)!!
    }
    private val injectMethod by lazy {
        inputManager.javaClass.getMethod(
            "injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType
        ).apply { isAccessible = true }
    }
    /** 0 = wait for finish; 1 = wait for result; 2 = async. We use 2. */
    private const val INJECT_INPUT_EVENT_MODE_ASYNC = 2

    fun injectInputEvent(event: InputEvent, displayId: Int) {
        // displayId is best-effort: on API 23 InputManager doesn't accept a display arg,
        // so we set it on the MotionEvent before calling.
        if (event is MotionEvent) {
            try {
                MotionEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType)
                    .invoke(event, displayId)
            } catch (_: NoSuchMethodException) { /* API 28+ method, no-op on 6 */ }
        }
        injectMethod.invoke(inputManager, event, INJECT_INPUT_EVENT_MODE_ASYNC)
    }
}
```

> **Permission requirement:** `INJECT_EVENTS` is signature-protected. The APK must be signed with the platform key, hence `sharedUserId="android.uid.system"` in the system manifest variant. On a rooted device, extract `/system/etc/security/platform.x509.pem` + `platform.pk8` and sign the release APK with `apksigner` + those keys.

#### Build flavors
Two product flavors so the same source builds both:
```kotlin
android {
    flavorDimensions += "build"
    productFlavors {
        create("standard") {
            dimension = "build"
            // no system uid, no embedding
            manifestPlaceholders["sharedUserId"] = ""
            buildConfigField("boolean", "ENABLE_EMBEDDING", "false")
        }
        create("system") {
            dimension = "build"
            manifestPlaceholders["sharedUserId"] = "android.uid.system"
            buildConfigField("boolean", "ENABLE_EMBEDDING", "true")
        }
    }
}
```
Templated manifest tag:
```xml
<manifest ... android:sharedUserId="${sharedUserId}" >
```

#### Apps that won't embed
- Most apps with `singleTask`/`singleInstance` and no display affinity will fight back. Maps and Waze tend to behave; Spotify and YouTube can be temperamental.
- Workaround for stubborn apps: launch on the virtual display and immediately use `ActivityManager.moveTaskToFront(taskId)` after capture. Or fall back to launching on the real display when embedding fails.

#### Fallback: SYSTEM_ALERT_WINDOW
For non-system standard build, embedding is disabled. Tapping an embedded slot just `startActivity`s it on the main display. Same UX as Mini AA's standard build.

---

## 14. Feature 6 — Settings

`PreferenceFragmentCompat` with a custom theme, sliders for panel ratio, switches for trip recording / location-on-statusbar, units segmented control.

### `xml/preferences.xml`
```xml
<PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android">

    <PreferenceCategory android:title="@string/pref_units">
        <ListPreference
            android:key="units"
            android:title="@string/pref_units"
            android:entries="@array/units_entries"
            android:entryValues="@array/units_values"
            android:defaultValue="metric" />
    </PreferenceCategory>

    <PreferenceCategory android:title="@string/pref_layout">
        <SeekBarPreference
            android:key="panel_ratio"
            android:title="@string/pref_panel_ratio"
            android:max="70" android:min="30" android:defaultValue="40" />
        <ListPreference
            android:key="grid_cols"
            android:title="@string/pref_grid_cols"
            android:entries="@array/grid_cols_entries"
            android:entryValues="@array/grid_cols_values"
            android:defaultValue="4" />
    </PreferenceCategory>

    <PreferenceCategory android:title="@string/pref_trip">
        <SwitchPreferenceCompat
            android:key="record_trips"
            android:title="@string/pref_record_trips"
            android:defaultValue="true" />
        <SeekBarPreference
            android:key="speed_threshold"
            android:title="@string/pref_speed_threshold"
            android:max="10" android:min="0" android:defaultValue="3" />
    </PreferenceCategory>

    <PreferenceCategory android:title="@string/pref_advanced">
        <Preference
            android:key="notification_access"
            android:title="@string/pref_notification_access" />
        <Preference
            android:key="debug_logs"
            android:title="@string/pref_debug_logs" />
    </PreferenceCategory>
</PreferenceScreen>
```

### `SettingsStore.kt`
```kotlin
class SettingsStore(private val prefs: SharedPreferences) {
    val units: Units get() = if (prefs.getString("units", "metric") == "metric") Units.METRIC else Units.IMPERIAL
    val panelRatio: Int get() = prefs.getInt("panel_ratio", 40)
    val gridCols: Int get() = prefs.getString("grid_cols", "4")?.toIntOrNull() ?: 4
    val recordTrips: Boolean get() = prefs.getBoolean("record_trips", true)
    val speedThresholdKmh: Float get() = prefs.getInt("speed_threshold", 3).toFloat()
}
enum class Units { METRIC, IMPERIAL }
```

---

## 15. Permissions & first-run flow

```
Boot → MainActivity onCreate
            │
            ▼
   Check permissions
            │
   ┌────────┼─────────┐
   │        │         │
notif    fine_loc   battery_opt
ignored?  granted?  whitelisted?
   │        │         │
   └────────┴─────────┘
            │
        Setup wizard fragment
        (1 of 4) Welcome
        (2 of 4) Notification access
        (3 of 4) Location
        (4 of 4) Battery optimization
            │
            ▼
        HomeFragment
```

Each wizard step:
1. Short label (one line) + accent button.
2. Tapping deep-links to the right system screen using `Settings.ACTION_*`.
3. Auto-advance when the permission flips on (`onResume` re-check).
4. Skip button on each.

```kotlin
// Notification listener deep link
startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
// Battery whitelist (API 23+)
val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
    .setData(Uri.parse("package:$packageName"))
startActivity(intent)
```

---

## 16. Status bar (bottom strip)

Always-visible 60dp tall bottom strip:

```
[ AppDrawer ]   13:29  Fri, May 1   ↔   Trip 51m · 32km · 39 kph avg   →   [ 43 ]──────●
```

- Left: app drawer button (opens grid panel).
- Center: clock + date — uses `TextClock` for cheap auto-update.
- Center-right (when trip active): live trip stats.
- Right: **big speed number** + horizontal progress bar showing speed-as-fraction-of-max.
- Tap-and-hold the speed → quick-toggles units.

`StatusBarFragment.kt` is ~100 lines. Single `LinearLayout` with weighted children, `LiveData` observers driving each chunk.

---

## 17. Code skeletons — wiring everything together

### `App.kt` (service locator)
```kotlin
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        Timber.plant(Timber.DebugTree())

        http = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
            .build()
        moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        db = Room.databaseBuilder(this, AppDb::class.java, "retro").build()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        settings = SettingsStore(prefs)

        media = MediaRepository()
        weather = WeatherRepository(this, http, moshi)
        location = LocationRepository()
        appList = AppListRepository(packageManager)
        tripRecorder = TripRecorder(
            scope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
            db = db,
            locationFlow = location.samples,
            geocoder = { lat, lon -> runBlocking { weather.reverseGeocodeBlocking(lat, lon) } }
        )

        // start things
        startService(Intent(this, LocationService::class.java))
        WorkManager.getInstance(this).scheduleWeather()
    }

    companion object {
        lateinit var instance: App
        lateinit var http: OkHttpClient
        lateinit var moshi: Moshi
        lateinit var db: AppDb
        lateinit var prefs: SharedPreferences
        lateinit var settings: SettingsStore
        lateinit var media: MediaRepository
        lateinit var weather: WeatherRepository
        lateinit var location: LocationRepository
        lateinit var appList: AppListRepository
        lateinit var tripRecorder: TripRecorder
    }
}
```

### `MainActivity.kt`
```kotlin
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.host, HomeFragment())
            }
        }

        applyPanelRatio(App.settings.panelRatio)
    }

    private fun applyPanelRatio(leftPct: Int) {
        val cs = b.root as ConstraintLayout
        val set = ConstraintSet().apply { clone(cs) }
        set.setGuidelinePercent(R.id.panel_split, leftPct / 100f)
        set.applyTo(cs)
    }
}
```

### `activity_main.xml`
```xml
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000">

    <View android:id="@+id/app_rail"
        android:layout_width="@dimen/rail_width"
        android:layout_height="0dp"
        android:background="@color/black"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintBottom_toTopOf="@id/status_bar" />

    <androidx.constraintlayout.widget.Guideline
        android:id="@+id/panel_split"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        app:layout_constraintGuide_percent="0.40" />

    <FrameLayout
        android:id="@+id/host"
        android:layout_width="0dp" android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toEndOf="@id/app_rail"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toTopOf="@id/status_bar" />

    <fragment
        android:id="@+id/status_bar"
        android:name="com.oskar.retrolauncher.ui.status.StatusBarFragment"
        android:layout_width="0dp"
        android:layout_height="@dimen/status_bar_height"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toBottomOf="parent" />
</androidx.constraintlayout.widget.ConstraintLayout>
```

### `HomeFragment.kt`
```kotlin
class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val pager = view.findViewById<ViewPager2>(R.id.right_pager)
        pager.adapter = RightPanelAdapter(this)
        pager.offscreenPageLimit = 1

        // attach a dot indicator
        val dots = view.findViewById<DotsIndicator>(R.id.dots)
        dots.attachTo(pager)
    }

    private class RightPanelAdapter(f: Fragment) : FragmentStateAdapter(f) {
        override fun getItemCount() = 3
        override fun createFragment(pos: Int): Fragment = when (pos) {
            0 -> EmbedFragment()    // map / embedded app
            1 -> AppGridFragment()
            2 -> TripsFragment()
            else -> error("?")
        }
    }
}
```

### `MediaFragment.kt`
```kotlin
class MediaFragment : Fragment(R.layout.fragment_media) {

    private val vm: MediaViewModel by viewModels()
    private var positionJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val art = view.findViewById<ImageView>(R.id.art)
        val title = view.findViewById<TextView>(R.id.title)
        val artist = view.findViewById<TextView>(R.id.artist)
        val playPause = view.findViewById<ImageButton>(R.id.play_pause)
        val prev = view.findViewById<ImageButton>(R.id.prev)
        val next = view.findViewById<ImageButton>(R.id.next)
        val bar = view.findViewById<ProgressBar>(R.id.progress)

        vm.uiState.observe(viewLifecycleOwner) { s ->
            title.text = s.title ?: "—"
            artist.text = s.artist ?: ""
            playPause.setImageResource(if (s.playing) R.drawable.ic_pause else R.drawable.ic_play)
            if (s.art != null) {
                Glide.with(this).load(s.art).placeholder(R.drawable.bg_album).into(art)
                s.art.dominantColorAsync { c -> view.setBackgroundColor(c) }
            }
            bar.max = s.durationMs.toInt()
            bar.progress = s.positionMs.toInt()
        }

        playPause.setOnClickListener { vm.togglePlay() }
        prev.setOnClickListener { vm.prev() }
        next.setOnClickListener { vm.next() }

        // 100 ms tick to extrapolate position smoothly
        positionJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                vm.tick(); delay(100)
            }
        }
    }

    override fun onDestroyView() { positionJob?.cancel(); super.onDestroyView() }
}
```

```kotlin
class MediaViewModel : ViewModel() {
    private val _ui = MutableLiveData<MediaUiState>()
    val uiState: LiveData<MediaUiState> = _ui

    init {
        viewModelScope.launch {
            App.media.state.collect { _ui.value = MediaUiState.from(it) }
        }
    }
    fun tick() { _ui.value = _ui.value?.copy(positionMs = App.media.state.value.livePosition()) }
    fun togglePlay() { val s = App.media.state.value; if (s.playing) App.media.pause() else App.media.play() }
    fun prev() = App.media.prev()
    fun next() = App.media.next()
}

data class MediaUiState(
    val title: String?, val artist: String?, val art: Bitmap?,
    val playing: Boolean, val durationMs: Long, val positionMs: Long,
) {
    companion object {
        fun from(s: MediaState) = MediaUiState(s.title, s.artist, s.art, s.playing, s.durationMs, s.livePosition())
    }
}
```

### `WeatherFragment.kt`, `SpeedFragment.kt`, `TripsFragment.kt`, `AppGridFragment.kt`
Same shape. ViewModel collects from its repo's `StateFlow`, emits a UI state, fragment binds to views. Boilerplate that I'll skip here for length — once you've internalized the pattern you can write each in 50–80 lines.

---

## 18. Compatibility notes — Android 6 specifics

| Issue | Workaround |
|---|---|
| Runtime permissions newly required | Already handled with `ActivityCompat.requestPermissions`. |
| `getSystemService(Class)` overloaded form not on API 22 | We're API 23+, fine. |
| Vector drawables only render at runtime via AppCompat | `vectorDrawables.useSupportLibrary = true` already set. |
| `NotificationCompat.Builder(ctx, channelId)` API 26 channel | NotificationCompat ignores channel on < 26. Fine. |
| `WindowInsets` weirdness | Use `WindowInsetsCompat`. |
| `startForeground` requires foreground type API 29+ | `targetSdk = 28` so no. |
| `Geocoder.isPresent()` lies on AOSP-only ROMs | We catch and fall back to Nominatim. |
| `getRunningAppProcesses` returns only own pid | We don't rely on it. |
| `ActivityOptions.setLaunchDisplayId` is hidden until API 26 | Reflection: `setMethod("setLaunchDisplayId", int)`. |
| Doze mode kills GPS | Foreground service + battery whitelist. |
| `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` requires permission | Add `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` to manifest. |
| HTTP cleartext blocked from API 28 | `targetSdk = 28` doesn't enforce, but Nominatim is HTTPS anyway. |
| `Settings.Secure.LOCATION_MODE` deprecated | Still works on 6. |
| Some Chinese head units have a stuck system clock — RTC battery dead | Cache last NTP-synced time in prefs; use `SystemClock.elapsedRealtime` for relative deltas. |

### Hidden API blacklist
Android 9 introduced a hidden-API blacklist. We're on 6, so no blacklist — reflection on `InputManager.injectInputEvent` works without `--hidden-api-policy` shenanigans. Future-proof by wrapping calls in `try/catch(NoSuchMethodException)`.

---

## 19. Testing plan

### Unit tests (`src/test`)
- `SpeedFilter` — varied inputs, threshold behavior.
- `TripRecorder` — simulate sample stream, assert state transitions.
- `WeatherSnapshot.from` — DTO mapping.
- `MediaState.livePosition` — extrapolation accuracy.

### Instrumented tests (`src/androidTest`)
- `MediaNotificationListener` smoke test — post a `MediaStyle` notification, assert state propagates.
- `AppDb` migrations — even though we start at v1.

### Manual test matrix
| Scenario | Expected |
|---|---|
| First boot, no perms | Wizard auto-opens |
| GPS off, app open | Speedometer pulses idle, weather uses cached |
| Spotify playing | Tile updates, art color extracts within 200 ms |
| Drive 5 km | Trip recorded, visible in history |
| Pull power cable mid-trip | Trip flushed on next boot via WAL recovery |
| Embed Maps (system build) | Renders in right panel, touch works |
| Embed YouTube (system build) | Either renders or falls back gracefully |
| Switch units to imperial | All speed/temp display flips immediately |
| Reboot with launcher set as default | App auto-launches, GPS resumes |

### Bench targets
- Cold start to first frame: < 1500 ms on Cortex-A7 1.2 GHz
- Steady-state RAM: < 220 MB
- Steady-state CPU with GPS active: < 8% on one core
- Animation jank: 0 dropped frames during 60 s of media tile updates (Choreographer logging)

---

## 20. Build & install on rooted head unit

### One-time toolchain
```bash
# host machine
brew install --cask android-commandlinetools
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
# pull platform key off the head unit
adb shell su -c 'cat /system/etc/security/platform.pk8' > platform.pk8
adb shell su -c 'cat /system/etc/security/platform.x509.pem' > platform.x509.pem
```

### Standard build (no embedding, any user)
```bash
./gradlew :app:assembleStandardRelease
adb install app/build/outputs/apk/standard/release/app-standard-release.apk
```

### System build (embedding, requires platform signature)
```bash
./gradlew :app:assembleSystemRelease
# resign with platform key
java -jar signapk.jar platform.x509.pem platform.pk8 \
    app/build/outputs/apk/system/release/app-system-release-unsigned.apk \
    app-system-signed.apk
# install as system app
adb root
adb remount
adb push app-system-signed.apk /system/priv-app/RetroLauncher/RetroLauncher.apk
adb shell chmod 644 /system/priv-app/RetroLauncher/RetroLauncher.apk
adb reboot
```

### Set as default launcher
```bash
adb shell cmd package set-home-activity com.oskar.retrolauncher/.MainActivity
```
Or hold **HOME** in the head unit OS and pick Retro Launcher from the chooser.

### `INSTALL_FAILED_SHARED_USER_INCOMPATIBLE`
Same fix as Mini AA — install the **standard** flavor. Your ROM's platform key doesn't match AOSP, so the system flavor refuses.

### Boot persistence (rooted)
`/system/etc/permissions/privapp-permissions-retro.xml` to grant signature perms at boot:
```xml
<permissions>
  <privapp-permissions package="com.oskar.retrolauncher">
    <permission name="android.permission.INJECT_EVENTS"/>
    <permission name="android.permission.INTERNAL_SYSTEM_WINDOW"/>
  </privapp-permissions>
</permissions>
```

---

## 21. Roadmap & known limits

### v0.1 — MVP (what this doc covers)
- [x] Single-Activity scaffold + bottom status bar
- [x] Media tile (any music app, art, color)
- [x] Weather tile (OWM + Nominatim)
- [x] Speedometer view + foreground service
- [x] Trip recorder + Room DB
- [x] App grid
- [x] Settings (units, panel ratio, grid, threshold)
- [x] First-run wizard

### v0.2 — Polish
- [ ] Trip detail screen with mini-map
- [ ] GPX export per trip
- [ ] App rail customization (drag to reorder)
- [ ] More themes
- [ ] Steering-wheel key support (`KEYCODE_MEDIA_*`)

### v0.3 — System-build features
- [ ] Embedded apps via VirtualDisplay
- [ ] Touch forwarding with proper gesture handling
- [ ] Audio focus passthrough so embedded apps can take focus

### v0.4 — Nice-to-haves
- [ ] CAN-bus integration via OBD-II (Bluetooth ELM327) for RPM/coolant temp
- [ ] Mic-button voice trigger → launch Google/Bing voice
- [ ] Day/night theme auto-switch from sun position

### Known limits we accept
- **No PiP** on Android 6. Embedded apps via `VirtualDisplay` get us 80% of the way; the other 20% is apps that won't render to a virtual display.
- **No CarPlay/Android Auto bridging** — that's a mountain of HU2/HU3 protocol work.
- **No offline maps** — we rely on whatever map app the user already has.
- **OWM free tier limits** to 1000 calls/day. At 15 min refresh, 96 calls/day. Plenty.
- **Glide bitmap pool** caps at ~20 MB on 1 GB RAM devices. Big album art (>2K) gets downsampled.
- **Reflection-based hidden API calls** could break on a future ROM update if you flash one. We pin to the head unit's stock 6.0.

---

## 22. File checklist

Files you'll author from this spec, in suggested order:

```
☐ settings.gradle.kts, build.gradle.kts (root), libs.versions.toml
☐ app/build.gradle.kts
☐ app/src/main/AndroidManifest.xml
☐ res/values/themes.xml, colors.xml, strings.xml
☐ res/values-w1024dp/dimens.xml
☐ res/layout/activity_main.xml
☐ res/layout/fragment_home.xml
☐ res/layout/fragment_media.xml
☐ res/layout/fragment_weather.xml
☐ res/layout/view_speedometer.xml
☐ res/layout/fragment_trips.xml
☐ res/layout/fragment_grid.xml
☐ res/layout/fragment_status.xml
☐ App.kt
☐ MainActivity.kt
☐ ui/home/HomeFragment.kt + RightPanelAdapter
☐ ui/media/MediaFragment.kt + MediaViewModel.kt
☐ ui/weather/WeatherFragment.kt + WeatherViewModel.kt
☐ ui/speed/SpeedometerView.kt + SpeedFragment.kt
☐ ui/trips/TripsFragment.kt + TripCalendarView.kt + TripsAdapter.kt
☐ ui/grid/AppGridFragment.kt + AppGridAdapter.kt
☐ ui/embed/EmbedFragment.kt
☐ ui/status/StatusBarFragment.kt
☐ ui/settings/SettingsFragment.kt
☐ data/media/MediaRepository.kt + MediaState.kt
☐ data/weather/WeatherRepository.kt + WeatherSnapshot.kt + WeatherDto.kt
☐ data/location/LocationRepository.kt + LocationSample.kt + SpeedFilter.kt
☐ data/trip/TripRepository.kt + TripRecorder.kt + AppDb.kt + TripEntity.kt + TripPoint.kt + TripDao.kt
☐ data/apps/AppListRepository.kt + AppEntry.kt
☐ data/prefs/SettingsStore.kt
☐ service/MediaNotificationListener.kt
☐ service/LocationService.kt
☐ service/WeatherWorker.kt
☐ service/BootReceiver.kt
☐ system/HiddenApi.kt (system flavor only)
☐ system/Embedding.kt (system flavor only)
☐ util/ColorExt.kt, FormatExt.kt, ViewExt.kt
☐ xml/preferences.xml
```

That's 50-ish files for a full v0.1. Working full-time you can have a usable build in 4–6 days; on weekends, 3–4 weeks.

---

## 23. Quick-start checklist for day 1

1. `git init retro-launcher && cd retro-launcher`
2. Create the Gradle scaffolding (build files above).
3. Drop in the manifest, App.kt, MainActivity.kt, activity_main.xml.
4. Stub all 7 fragments with empty `onViewCreated`. Verify the launcher launches.
5. Implement `LocationService` + `SpeedometerView`. Drive once, confirm speed updates.
6. Implement `MediaNotificationListener` + media tile. Play Spotify, confirm art.
7. Implement `WeatherRepository` + tile. Confirm refresh.
8. Implement `TripRecorder` + `TripsFragment`. Drive twice, confirm history.
9. Implement `AppGridFragment`.
10. Polish: themes, panel ratio slider, settings.

The remaining v0.3+ features (embedding) only after v0.2 is rock-solid.

---

*Built for one head unit. By you, for you.*
