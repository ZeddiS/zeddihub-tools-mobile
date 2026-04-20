#!/usr/bin/env bash
# Samostatne nahraje posledni debug APK na Google Drive.
set -euo pipefail
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PY="${PYTHON:-python}"
if ! command -v "$PY" >/dev/null 2>&1; then PY=python3; fi
"$PY" "$PROJECT_DIR/scripts/upload_apk.py" "$@"
