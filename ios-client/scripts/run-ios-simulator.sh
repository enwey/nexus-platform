#!/bin/zsh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROJECT_PATH="$ROOT/NexusPlatformApp.xcodeproj"
SCHEME="NexusPlatformApp"
DERIVED_DATA_PATH="$ROOT/.build/ios-simulator"
SIMULATOR_NAME="${1:-iPhone 17}"
PLATFORM_API_BASE_URL="${PLATFORM_API_BASE_URL:-${PLATFORM_SIMULATOR_API_BASE_URL:-http://127.0.0.1:8080/api/v1}}"
BACKEND_CERT_SHA256="${BACKEND_CERT_SHA256:-}"

if [[ ! -d "$PROJECT_PATH" ]]; then
  echo "Missing $PROJECT_PATH"
  echo "Run: ruby $ROOT/scripts/generate_host_project.rb"
  exit 1
fi

if [[ -z "$PLATFORM_API_BASE_URL" ]]; then
  echo "Missing PLATFORM_API_BASE_URL."
  echo "Example: PLATFORM_API_BASE_URL=http://127.0.0.1:8080/api/v1 ./scripts/run-ios-simulator.sh"
  exit 1
fi

if ! xcrun simctl list devices available | grep -Fq "$SIMULATOR_NAME"; then
  echo "Simulator '$SIMULATOR_NAME' not found."
  echo
  echo "Available simulators:"
  xcrun simctl list devices available
  exit 1
fi

xcrun simctl boot "$SIMULATOR_NAME" >/dev/null 2>&1 || true
open -a Simulator >/dev/null 2>&1 || true
xcrun simctl bootstatus "$SIMULATOR_NAME" -b

xcodebuild \
  -project "$PROJECT_PATH" \
  -scheme "$SCHEME" \
  -configuration Debug \
  -destination "platform=iOS Simulator,name=$SIMULATOR_NAME" \
  -derivedDataPath "$DERIVED_DATA_PATH" \
  PLATFORM_API_BASE_URL="$PLATFORM_API_BASE_URL" \
  BACKEND_CERT_SHA256="$BACKEND_CERT_SHA256" \
  build

APP_PATH="$DERIVED_DATA_PATH/Build/Products/Debug-iphonesimulator/${SCHEME}.app"
if [[ ! -d "$APP_PATH" ]]; then
  echo "Built app not found at $APP_PATH"
  exit 1
fi

xcrun simctl install "$SIMULATOR_NAME" "$APP_PATH"
xcrun simctl launch --terminate-running-process "$SIMULATOR_NAME" "$(
  /usr/libexec/PlistBuddy -c "Print :CFBundleIdentifier" "$APP_PATH/Info.plist"
)" >/dev/null

echo
echo "Launched $SCHEME on simulator: $SIMULATOR_NAME"
echo "Backend URL: $PLATFORM_API_BASE_URL"
