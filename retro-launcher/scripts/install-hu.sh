#!/usr/bin/env bash
# install-hu.sh — install retro-launcher onto a rooted head unit over ADB.
#
# Usage:
#   ./scripts/install-hu.sh                # standard flavor (any user, no embedding)
#   ./scripts/install-hu.sh --system       # system flavor (platform-signed, embedding on)
#
# Prereqs: adb in PATH; head unit reachable (USB or Wi-Fi); root over adb.
# See spec section 20 for the toolchain bootstrap.

set -euo pipefail

FLAVOR="standard"
if [[ "${1:-}" == "--system" ]]; then
  FLAVOR="system"
fi

PACKAGE="com.oskar.retrolauncher"
HOME_ACTIVITY="${PACKAGE}/.MainActivity"

# --- helpers ---------------------------------------------------------------

die() { printf 'install-hu: error: %s\n' "$*" >&2; exit 1; }
note() { printf 'install-hu: %s\n' "$*"; }

require_adb() {
  command -v adb >/dev/null 2>&1 || die "adb not found in PATH. Install Android platform-tools."
}

check_device() {
  local state
  state=$(adb get-state 2>&1 || true)
  case "$state" in
    device)
      return 0 ;;
    "unauthorized"*|*unauthorized*)
      die "device is unauthorized. On the head unit: enable USB debugging, then accept the RSA fingerprint prompt for this host." ;;
    "error: no devices"*|"unknown"|"")
      die "no device detected. Connect the head unit (USB or 'adb connect <ip>:5555') and confirm with 'adb devices'." ;;
    *)
      die "device in unexpected state: $state" ;;
  esac
}

# --- standard install ------------------------------------------------------

install_standard() {
  local apk="app/build/outputs/apk/standard/debug/app-standard-debug.apk"
  if [[ ! -f "$apk" ]]; then
    note "building $apk"
    ./gradlew :app:assembleStandardDebug
  fi
  note "installing standard flavor (any user)"
  adb install -r "$apk"
}

# --- system install --------------------------------------------------------

install_system() {
  local unsigned="app/build/outputs/apk/system/release/app-system-release-unsigned.apk"
  local signed="app-system-signed.apk"

  [[ -f "platform.pk8" && -f "platform.x509.pem" ]] || die \
    "platform.pk8 and platform.x509.pem must be present in repo root (pull them off the head unit; see spec section 20)."

  if [[ ! -f "$unsigned" ]]; then
    note "building $unsigned"
    ./gradlew :app:assembleSystemRelease
  fi

  command -v java >/dev/null 2>&1 || die "java not found; needed for signapk.jar"
  [[ -f "signapk.jar" ]] || die "signapk.jar missing in repo root; fetch from your AOSP build's out/host/linux-x86/framework/."

  note "platform-signing system APK → $signed"
  java -jar signapk.jar platform.x509.pem platform.pk8 "$unsigned" "$signed"

  note "remounting /system and pushing $signed"
  adb root
  adb remount
  adb shell mkdir -p /system/priv-app/RetroLauncher
  adb push "$signed" "/system/priv-app/RetroLauncher/RetroLauncher.apk"
  adb shell chmod 644 /system/priv-app/RetroLauncher/RetroLauncher.apk
  note "rebooting head unit to load /system/priv-app entry"
  adb reboot
  note "waiting for reboot to complete"
  adb wait-for-device
}

# --- set as home -----------------------------------------------------------

set_as_home() {
  note "marking $HOME_ACTIVITY as default home activity (requires root)"
  if ! adb shell su -c "cmd package set-home-activity $HOME_ACTIVITY" >/dev/null 2>&1; then
    note "root su failed; trying without su (may require manual selection in head unit OS)"
    adb shell "cmd package set-home-activity $HOME_ACTIVITY" || die \
      "could not set home activity. Hold HOME on the head unit and pick Retro Launcher from the chooser, or run this script after rooting."
  fi
}

# --- main ------------------------------------------------------------------

require_adb
check_device

case "$FLAVOR" in
  standard) install_standard ;;
  system)   install_system   ;;
  *)        die "unknown flavor: $FLAVOR" ;;
esac

set_as_home
note "installed retro-launcher ($FLAVOR). Press HOME on the head unit to verify."
