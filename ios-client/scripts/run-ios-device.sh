#!/bin/zsh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROJECT_PATH="$ROOT/NexusPlatformApp.xcodeproj"
SCHEME="NexusPlatformApp"
DERIVED_DATA_PATH="$ROOT/.build/ios-device"
DEVICE_ID="${1:-}"
BACKEND_BASE_URL="${BACKEND_BASE_URL:-http://192.168.1.5:8080/api/v1}"
USER_SUFFIX="${USER//[^[:alnum:]]/}"
USER_SUFFIX="${USER_SUFFIX:l}"
PRODUCT_BUNDLE_IDENTIFIER="${PRODUCT_BUNDLE_IDENTIFIER:-com.nexusplatform.${USER_SUFFIX:-local}.host}"

if [[ ! -d "$PROJECT_PATH" ]]; then
  echo "Missing $PROJECT_PATH"
  echo "Run: ruby $ROOT/scripts/generate_host_project.rb"
  exit 1
fi

if [[ -z "$DEVICE_ID" ]]; then
  echo "Usage: $0 <device-udid>"
  echo
  echo "Known devices:"
  xcrun xcdevice list
  exit 1
fi

BUILD_ARGS=(
  xcodebuild
  -project "$PROJECT_PATH"
  -scheme "$SCHEME"
  -configuration Debug
  -destination "id=$DEVICE_ID"
  -derivedDataPath "$DERIVED_DATA_PATH"
  -allowProvisioningUpdates
  CODE_SIGN_STYLE=Automatic
  PRODUCT_BUNDLE_IDENTIFIER="$PRODUCT_BUNDLE_IDENTIFIER"
  BACKEND_BASE_URL="$BACKEND_BASE_URL"
  build
)

if [[ -n "${DEVELOPMENT_TEAM:-}" ]]; then
  BUILD_ARGS+=(DEVELOPMENT_TEAM="$DEVELOPMENT_TEAM")
fi

"${BUILD_ARGS[@]}"

APP_PATH="$DERIVED_DATA_PATH/Build/Products/Debug-iphoneos/${SCHEME}.app"
if [[ ! -d "$APP_PATH" ]]; then
  echo "Built app not found at $APP_PATH"
  exit 1
fi

xcrun devicectl device install app --device "$DEVICE_ID" "$APP_PATH"
DEVICECTL_CHILD_BACKEND_BASE_URL="$BACKEND_BASE_URL" \
  xcrun devicectl device process launch --device "$DEVICE_ID" --terminate-existing "$PRODUCT_BUNDLE_IDENTIFIER"
