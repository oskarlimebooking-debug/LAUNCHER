# Retro Launcher — Manual Test Matrix

Companion to [`headunit-launcher-spec.md`](../headunit-launcher-spec.md) §19.3. Instrumented tests cannot simulate GPS fixes, real notification posters, default-launcher takeover, or `BOOT_COMPLETED` flows — those must be exercised on a real rooted head unit (or, where possible, a 1024×600 emulator with the same API level).

**Display target:** 1024×600 landscape, capacitive touch, **API 23 (Android 6.0.1)**.

---

## Pre-flight checklist

Run through these before exercising any feature row. A failed pre-flight item invalidates downstream results.

- [ ] **Device is rooted** — `adb shell su -c 'id'` returns `uid=0(root)`. (Boot-persistence and `/system` install paths need this.)
- [ ] **Build flavor matches intent** — `standard` flavor for normal install, `system` flavor (platform-signed + pushed to `/system/priv-app/`) for embedding tests.
- [ ] **OWM API key configured** — Settings → "Weather API key" shows a non-empty value, or `gradle.properties`/`local.properties` defines `OWM_API_KEY`. Validate via Settings "Test key" button (HTTP 200, not 401).
- [ ] **Notification listener access granted** — Settings → Sound & notification → Notification access → "Retro Launcher" toggle is ON. Without this the media tile stays empty.
- [ ] **Location permission granted** — `ACCESS_FINE_LOCATION` shown as allowed under App info; GPS toggle in quick settings is ON.
- [ ] **Time/date is correct** — affects weather cache TTL and trip timestamps. NTP sync (`adb shell settings get global auto_time` → `1`).
- [ ] **Device reachable via adb** — `adb devices` lists serial; USB-debug README path completed if not.
- [ ] **No competing launcher set as default** — `adb shell cmd package get-home-activity` reports Retro Launcher (or the chooser is cleared via `pm clear com.android.launcher3`).
- [ ] **Wi-Fi or 4G online** — weather + first map embed need network. Confirm with `ping 1.1.1.1`.

---

## v0.1 features — manual test matrix

The 6 user-facing v0.1 features, plus supporting Settings and First-run wizard. Each row references the spec section that drove the requirement.

### 1. Launcher takeover & status bar (§5, §8, §20)

| # | Scenario | Steps | Expected result |
|---|---|---|---|
| 1.1 | First install, no default launcher set | `adb install` standard flavor → press HOME on head unit | Launcher chooser appears; Retro Launcher listed; selecting it opens HomeActivity. |
| 1.2 | Set as default via adb | `adb shell cmd package set-home-activity com.oskar.retrolauncher/.MainActivity` → press HOME | HomeActivity opens directly, no chooser. |
| 1.3 | Status bar layout @ 1024×600 | Boot HomeActivity, observe bottom strip | Status bar pinned to bottom, full 1024 px wide, ~48 dp tall; clock left, speed center, trip stats right; no clipping. |
| 1.4 | Clock updates | Wait 60 s with launcher visible | Clock minute increments without restart; format honours device 12/24 h setting. |
| 1.5 | Reboot persistence | `adb reboot`; do not touch device until home shown | Launcher auto-launches as Home; GPS service resumes (status bar shows speed after fix); no permission re-prompt. |
| 1.6 | Status bar speed display | Drive (or feed mock locations) at >5 km/h | Status bar speed updates within 1 s of speedometer view; units honour Settings (km/h vs mph). |

### 2. Media tile (§6, §9)

| # | Scenario | Steps | Expected result |
|---|---|---|---|
| 2.1 | Spotify playing | Open Spotify, play any song with album art | Media tile shows track title, artist, art thumbnail within ~500 ms; play/pause/skip buttons respond. |
| 2.2 | Album art color extraction | Switch tracks with distinctly colored covers | Tile background gradient/accent shifts within 200 ms of art load (Palette extraction). |
| 2.3 | Generic music app | Open VLC or PowerAmp, start playback | Tile populates the same as Spotify (any `MediaStyle` notification poster). |
| 2.4 | No music playing | Stop all playback, force-stop music apps | Tile shows idle state ("No music" or empty stub); play buttons disabled or hidden. |
| 2.5 | Live position extrapolation | Start playback, observe progress bar | Bar advances smoothly between explicit notification updates (extrapolated via `MediaState.livePosition`). |
| 2.6 | Notification listener disabled mid-session | Revoke notification access in Settings, return to launcher | Tile clears within one refresh tick; banner or empty state shown; no crash. |
| 2.7 | Skip next/prev | Tap skip-next on tile while Spotify plays | Spotify advances track; tile updates within 500 ms. |

### 3. Weather tile (§7, §10)

| # | Scenario | Steps | Expected result |
|---|---|---|---|
| 3.1 | First fetch with valid key | Configure OWM key in Settings, ensure GPS fix, return to home | Temperature + condition icon appear within 5 s; refresh timestamp shown. |
| 3.2 | Invalid OWM key | Set obviously-wrong key (`xxx`), tap "Test key" | UI surfaces error ("Invalid key" / 401); tile shows stale cache or empty state, not silent failure. |
| 3.3 | No network | Disable Wi-Fi/4G, wait for next refresh tick | Tile keeps last cached snapshot; subtle stale indicator (icon/timestamp) shown; no crash. |
| 3.4 | GPS not yet fixed | Boot fresh, before first fix | Tile shows "Locating…" or cached last-known location; transitions to live data after fix. |
| 3.5 | Refresh cadence | Leave running 30 min, watch logs/timestamp | Refreshes every 15 min (±1 min); no extra calls (respects OWM free-tier 1000/day). |
| 3.6 | Unit flip | Switch Settings → Units to imperial | Temperature flips °C → °F immediately, no app restart. |

### 4. Speedometer + foreground service (§6, §11)

| # | Scenario | Steps | Expected result |
|---|---|---|---|
| 4.1 | GPS off, app open | Disable GPS in quick settings | Speedometer pulses idle ("no fix"); no crash; status bar speed reads "—". |
| 4.2 | GPS on, stationary | Enable GPS, wait for fix indoors with view of sky | Within ~30 s shows 0 km/h; needle stable; no spurious >1 km/h readings (SpeedFilter threshold honoured). |
| 4.3 | Driving | Drive at varying speeds 0–80 km/h | Needle/digits track speed within ±1 km/h; redraws ≥30 fps (no visible stutter). |
| 4.4 | Service lifecycle | Background launcher (open another app) | `LocationService` keeps running; foreground notification visible; speedometer resumes instantly on return. |
| 4.5 | Service survives doze | Leave parked >10 min then start moving | Speed resumes within 2 GPS samples (≤2 s); service was not killed. |
| 4.6 | Imperial units | Settings → mph | Needle scale + digits switch to mph immediately. |

### 5. Trip recorder + history (§12)

| # | Scenario | Steps | Expected result |
|---|---|---|---|
| 5.1 | Drive 5 km | Drive a 5 km route start-to-stop | After stop (>2 min idle), trip appears in Trips screen with distance ≈5 km, duration, avg speed. |
| 5.2 | Below-threshold movement | Walk around the car at <5 km/h for 1 min | No trip recorded (sub-threshold filtered out). |
| 5.3 | Power loss mid-trip | Drive, then pull head-unit power | After next boot, WAL recovery flushes the in-progress trip into history; no DB corruption (`PRAGMA integrity_check` clean). |
| 5.4 | Multiple trips per day | Record 3 distinct trips with stops between | All 3 listed separately on the calendar view for that date; not merged. |
| 5.5 | Trip detail readability @ 1024×600 | Open a recorded trip | List row fits 1024 px width; no horizontal scroll; date/time/distance legible. |
| 5.6 | Long history scroll | Seed or accumulate >50 trips | Scrolling RecyclerView is smooth (no jank); calendar marker dots render. |

### 6. App grid (§13)

| # | Scenario | Steps | Expected result |
|---|---|---|---|
| 6.1 | Grid populates | Swipe right-panel to App grid | Shows all installed launchable apps (`CATEGORY_LAUNCHER`); icons + labels visible. |
| 6.2 | Launch an app | Tap any tile | App launches; Retro Launcher backgrounded; pressing HOME returns to launcher. |
| 6.3 | Grid density setting | Settings → Grid columns 4 vs 6 | Layout reflows immediately; icon size scales; no clipping at 1024×600. |
| 6.4 | New app install | Sideload an APK while launcher is open | Within ~5 s the grid picks up the new app (PackageManager broadcast); no manual refresh needed. |
| 6.5 | Uninstall an app | `adb uninstall <pkg>` while grid is open | Tile disappears within ~5 s; no stale icon, no crash on subsequent grid open. |
| 6.6 | Long press / no-op | Long-press a tile | Either does nothing or shows context menu — must not crash or break grid state. |

### Supporting — Settings (§14)

| # | Scenario | Steps | Expected result |
|---|---|---|---|
| S.1 | Unit toggle | Toggle metric ↔ imperial | All speed and temperature readings flip immediately across speedometer, weather, status bar, trips. |
| S.2 | Panel ratio slider | Move left/right panel split slider | Live preview; persists across app restart. |
| S.3 | Grid column count | Change 4 ↔ 6 columns | App grid re-lays out without restart. |
| S.4 | Speed threshold | Change "Trip start threshold" (e.g. 5 km/h → 10 km/h) | Subsequent `TripRecorder` runs use new threshold; existing trips unaffected. |
| S.5 | OWM key field | Paste key, tap "Test" | Inline result: ✓ for 200, ✗ for 401; saved on success. |
| S.6 | Settings reachable from launcher | Tap settings gear / drawer entry | Opens SettingsActivity; back returns to HomeActivity at same fragment. |

### Supporting — First-run wizard (§14, §15)

| # | Scenario | Steps | Expected result |
|---|---|---|---|
| W.1 | Fresh install, no perms | `pm clear com.oskar.retrolauncher` → open | Wizard auto-opens before HomeActivity; cannot skip granting required perms (location, notification listener). |
| W.2 | Perm grant flow | Tap each "Grant" button | Routes to correct system settings screen; returning to wizard advances step. |
| W.3 | Re-deny on the way back | Grant then revoke notification access via Settings | Next launcher launch reopens the wizard step for that perm. |
| W.4 | Completion | Grant all perms, finish wizard | HomeActivity opens; wizard does not reappear on subsequent boots unless perms revoked. |
| W.5 | Skippable optional steps | OWM key step (optional) | Skip permitted; wizard completes; weather tile shows "Configure key" prompt. |

---

## v0.2 — polish items (smoke tests; gate before bumping version)

Required only when v0.2 features land. Skip when running v0.1 sign-off.

| # | Feature | Steps | Expected result |
|---|---|---|---|
| P.1 | Trip detail screen with mini-map | Tap a trip from Trips list | Detail screen opens; mini-map shows recorded polyline; distance/time/avg/max stats render. |
| P.2 | GPX export per trip | Trip detail → Export GPX → choose destination | File saved (`<date>.gpx`); opens cleanly in a GPX viewer (e.g. GPX Studio); points match recorded path. |
| P.3 | App rail customization | Long-press app in rail → drag to new position | Order persists across app restart; remove option works; cannot create empty rail. |
| P.4 | Theme selection | Settings → Themes → pick alternate | Colors apply immediately to status bar, tiles, dividers; survives restart. |
| P.5 | Steering-wheel media keys | Press `KEYCODE_MEDIA_NEXT` via `adb shell input keyevent 87` while music playing | Track advances; same for `MEDIA_PREVIOUS` (88), `MEDIA_PLAY_PAUSE` (85). |

---

## Bench targets (spec §19.4)

Verify periodically; not strictly per-release.

| Target | Method | Pass criterion |
|---|---|---|
| Cold start → first frame | `adb shell am start -W -n com.oskar.retrolauncher/.MainActivity` | `TotalTime` < 1500 ms on Cortex-A7 1.2 GHz |
| Steady-state RAM | `adb shell dumpsys meminfo com.oskar.retrolauncher` after 10 min idle | PSS < 220 MB |
| Steady-state CPU (GPS active) | `adb shell top -m 5 -d 2` over 60 s | < 8% single-core for `retrolauncher` |
| Animation jank | Choreographer logs over 60 s of media tile updates | 0 dropped frames reported |

---

## Reporting

For each release tag, run the v0.1 matrix end-to-end on the actual head unit and record results in a dated test log (e.g. `docs/test-runs/2026-05-11-v0.1.md`). Mark blocking failures inline; defer non-blocking findings to issues.
