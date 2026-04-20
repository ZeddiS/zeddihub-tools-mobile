#!/usr/bin/env bash
# Postavi debug APK pomoci vestaveneho toolchainu v ./tools a nahraje jej
# na Google Drive (/ZeddiHub App/<versionName>/app-debug.apk).
# Pro vypnuti uploadu: SKIP_UPLOAD=1 ./build.sh
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export JAVA_HOME="$PROJECT_DIR/tools/jdk17"
export ANDROID_HOME="$PROJECT_DIR/tools/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="$PROJECT_DIR/tools/gradle-cache"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

echo "=============================================="
echo "JAVA_HOME        = $JAVA_HOME"
echo "ANDROID_HOME     = $ANDROID_HOME"
echo "GRADLE_USER_HOME = $GRADLE_USER_HOME"
echo "=============================================="

cd "$PROJECT_DIR"
./gradlew :app:assembleDebug "$@"

APK_DIR="$PROJECT_DIR/app/build/outputs/apk/debug"
APK_PATH="$(ls -1 "$APK_DIR"/ZeddiHub-App-*.apk 2>/dev/null | head -1)"
if [ -z "$APK_PATH" ]; then
    APK_PATH="$APK_DIR/app-debug.apk"
fi
if [ ! -f "$APK_PATH" ]; then
    echo "[build] APK nenalezeno v $APK_DIR"
    exit 1
fi
echo "[build] APK: $APK_PATH"

if [ "${SKIP_UPLOAD:-0}" = "1" ]; then
    echo "[build] SKIP_UPLOAD=1, preskakuji upload."
    exit 0
fi

PY="${PYTHON:-python}"
if ! command -v "$PY" >/dev/null 2>&1; then
    PY=python3
fi
if ! command -v "$PY" >/dev/null 2>&1; then
    echo "[build] Python nenalezen - preskakuji upload. Nainstaluj Python 3 nebo spust: SKIP_UPLOAD=1 ./build.sh"
    exit 0
fi

echo "[build] Nahravam APK na Google Drive..."
"$PY" "$PROJECT_DIR/scripts/upload_apk.py" --apk "$APK_PATH"
