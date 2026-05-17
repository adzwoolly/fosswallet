#!/usr/bin/env bash
set -e

echo "Install which build?"
echo "  1) Debug"
echo "  2) Release"
read -rp "Choice [1]: " choice
choice="${choice:-1}"

case "$choice" in
    1) APK="app/build/outputs/apk/debug/app-debug.apk" ;;
    2) APK="app/build/outputs/apk/release/app-release.apk" ;;
    *) echo "Invalid choice"; exit 1 ;;
esac

if [ ! -f "$APK" ]; then
    echo "APK not found: $APK"
    echo "Build it first with ./gradlew assembleDebug or ./build-release.sh"
    exit 1
fi

ADB="${ANDROID_HOME:-$HOME/Android/Sdk}/platform-tools/adb"
if ! command -v adb &>/dev/null && [ ! -x "$ADB" ]; then
    echo "adb not found — set ANDROID_HOME or add platform-tools to PATH"
    exit 1
fi
command -v adb &>/dev/null && ADB=adb

echo "Installing $APK..."
"$ADB" install -r "$APK"
