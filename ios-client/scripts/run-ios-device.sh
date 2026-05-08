#!/bin/zsh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROJECT_PATH="$ROOT/NexusPlatformApp.xcodeproj"
SCHEME="NexusPlatformApp"
DERIVED_DATA_PATH="$ROOT/.build/ios-device"
DEVICE_ID="${1:-}"
PLATFORM_API_BASE_URL="${PLATFORM_API_BASE_URL:-${BACKEND_BASE_URL:-}}"
BACKEND_CERT_SHA256="${BACKEND_CERT_SHA256:-}"
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

if [[ -z "$PLATFORM_API_BASE_URL" ]]; then
  echo "Missing PLATFORM_API_BASE_URL."
  echo "Inject your backend API base URL explicitly before running this script."
  echo 'Example: PLATFORM_API_BASE_URL=http://<your-mac-lan-ip>:8080/api/v1 ./scripts/run-ios-device.sh <device-udid>'
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
  PLATFORM_API_BASE_URL="$PLATFORM_API_BASE_URL"
  BACKEND_CERT_SHA256="$BACKEND_CERT_SHA256"
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
DEVICECTL_CHILD_PLATFORM_API_BASE_URL="$PLATFORM_API_BASE_URL" \
  xcrun devicectl device process launch --device "$DEVICE_ID" --terminate-existing "$PRODUCT_BUNDLE_IDENTIFIER"
