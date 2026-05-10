# Retro Launcher

An Android Auto–style launcher for **rooted Android 6.0+ (API 23) head units**, optimized for **1024×600 landscape**. v0.1 MVP per [`/Users/oskarsokolov/LAUNCHER/headunit-launcher-spec.md`](../headunit-launcher-spec.md).

## What it does

- **Single-Activity launcher** (`MainActivity`) that registers as `category.HOME`
- **Media tile** — auto-detects any music app via `NotificationListenerService` + `MediaSession.Token` → shows title, artist, album art, transport controls; tile background tinted with art's dominant color (Palette)
- **Weather tile** — OpenWeatherMap "One Call 3.0" + Nominatim reverse geocode (no GMS), refresh every 15 min via WorkManager
- **Speedometer** in the bottom status bar — foreground `LocationService` reading `LocationManager.GPS_PROVIDER`, alpha-smoothed and clamped under user-configurable threshold
- **Trip recording** — auto-detects start (>5 km/h sustained 10 s) and end (<3 km/h sustained 2 min); persists to Room
- **Trip history calendar** — month strip + day picker, swipe-to-delete
- **App grid** — `RecyclerView` of every launchable activity; long-press to pin to the left rail
- **First-run wizard** — guides through Notification access · Location · Battery whitelist
- **Settings** — units (metric / imperial), L/R panel ratio (30–70 %), grid columns, speed threshold, trip recording toggle

## Project layout

```
app/
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/oskar/retrolauncher/
│   │   ├── App.kt                       Application + service locator
│   │   ├── MainActivity.kt              the launcher
│   │   ├── ui/{home,media,weather,speed,trips,grid,embed,status,settings,wizard}/
│   │   ├── data/{media,weather,location,trip,apps,prefs}/
│   │   ├── service/{LocationService,MediaNotificationListener,WeatherWorker,BootReceiver}.kt
│   │   └── util/{ColorExt,FormatExt,ViewExt,Permissions}.kt
│   └── res/{layout,values,values-w1024dp,drawable,xml}/
├── src/test/                            JVM unit tests (SpeedFilter, TripRecorder, WeatherSnapshot, MediaState, FormatExt)
└── build.gradle.kts                     standard + system flavors (system reserved for v0.3)
```

48 Kotlin source files, ~50 layout/resource files.

## Prerequisites

- **JDK 17** (required by AGP 8.2)
  ```
  brew install --cask temurin@17
  ```
- **Android SDK** with `platforms;android-34` and `build-tools;34.0.0`
  ```
  brew install --cask android-commandlinetools
  sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
  ```
- An **OpenWeatherMap free API key** (1 000 calls/day; we only need 96)

## First-time setup

1. **API key** — create `local.properties` in this directory:
   ```
   OWM_API_KEY=your_key_here
   sdk.dir=/Users/<you>/Library/Android/sdk
   ```

2. **Bootstrap the Gradle wrapper** (only needed once — the wrapper jar isn't checked in):
   ```
   gradle wrapper --gradle-version 8.4
   ```
   …or just open the project in Android Studio, which sets up the wrapper automatically.

3. **Build**:
   ```
   ./gradlew :app:assembleStandardDebug
   ```

## Install on the head unit

### One-shot script (recommended)

`scripts/install-hu.sh` builds the APK if missing, installs over ADB, and sets retro-launcher as the default home activity (root over adb required for the last step).

```
./scripts/install-hu.sh             # standard flavor (any user, no embedding)
./scripts/install-hu.sh --system    # system flavor (platform-signed, embedding on)
```

The script reports a clear message for the two most common failures: `adb` missing from PATH, and `device unauthorized` (head unit needs USB debugging enabled and the RSA fingerprint accepted).

### Enabling USB debugging on the head unit

1. **Settings → About head unit** — tap the build number 7 times to unlock developer options.
2. **Settings → Developer options** — toggle **USB debugging** on.
3. Plug the USB cable from the host into the head unit's debug port (often labeled USB-A on the back; consult your HU manual).
4. On the host, run `adb devices`. The head unit prompts "Allow USB debugging?" — tick "Always allow from this computer" and accept.
5. For Wi-Fi: `adb tcpip 5555` (over USB once), then `adb connect <head-unit-ip>:5555`.

### Manual install (without the script)

```
# debug build, easy iteration
adb install app/build/outputs/apk/standard/debug/app-standard-debug.apk

# release build
./gradlew :app:assembleStandardRelease
adb install app/build/outputs/apk/standard/release/app-standard-release.apk

# set as default launcher (requires root over adb)
adb shell cmd package set-home-activity com.oskar.retrolauncher/.MainActivity
```

If the head unit refuses with `INSTALL_FAILED_SHARED_USER_INCOMPATIBLE`, you tried the system flavor on a non-rooted device. v0.1 ships only the `standard` flavor.

## Development tips

- **Emulator**: create an AVD with **1024×600** custom skin and **API 23**. Lower densities (`mdpi`/`tvdpi`) match real head units; the app's `values-w1024dp/dimens.xml` kicks in correctly.
- **Notification listener**: emulator → Settings → Apps → Special access → Notification access → enable Retro Launcher (or use the wizard's deep link).
- **GPS**: `adb emu geo fix <lon> <lat> <alt> <speed_ms>` to push a fake location.
- **Logs**: `adb logcat -s RetroLauncher:V Timber:V`.

## Tests

```
./gradlew :app:testStandardDebugUnitTest
```

Covers `SpeedFilter` (clamp + smoothing + reset), `TripRecorder` (state-machine + persistence on synthetic sample stream), `WeatherSnapshot.from` (DTO mapping), `MediaState.livePosition` (extrapolation), `FormatExt` (units conversions).

## What's intentionally not in v0.1

- App embedding via `VirtualDisplay` + `INJECT_EVENTS` reflection — needs platform-key resigning, lives in v0.3 (`system` flavor)
- Trip detail screen with mini-map polyline — v0.2
- GPX export, drag-reorder rail, more themes, `KEYCODE_MEDIA_*` — v0.2
- OBD-II / CAN-bus, voice trigger, day/night auto theme — v0.4

See spec §21 for full roadmap.

## Android 6 compatibility notes

| Issue | Mitigation |
|---|---|
| Vector drawables render as black squares without support library | `vectorDrawables.useSupportLibrary = true` (set in `app/build.gradle.kts`) |
| `Geocoder.isPresent()` lies on AOSP-only ROMs | Wrapped in `runCatching`; falls back to Nominatim with proper `User-Agent` |
| Doze kills GPS mid-trip | Foreground `LocationService` + `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (wizard step 4) |
| Notification access stripped on some Chinese ROMs | First-run check via `enabled_notification_listeners` setting + deep-link |
| RTC battery dead on some HUs (stuck system clock) | Trip durations use `loc.time` from GPS NMEA, not `System.currentTimeMillis` |

## License

Personal use, MIT-equivalent. Don't ship to Play Store — the bundled icon set is hand-rolled and uses the same accent (`#FF8500`) as Mini AA.
