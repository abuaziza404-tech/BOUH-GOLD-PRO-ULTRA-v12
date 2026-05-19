#!/usr/bin/env bash
set -e
echo "Building APK with installed Android Gradle plugin environment..."
gradle :app:assembleDebug --no-daemon
echo "APK: app/build/outputs/apk/debug/app-debug.apk"
