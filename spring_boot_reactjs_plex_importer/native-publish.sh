#!/usr/bin/env bash
set -euo pipefail

./build.sh

GRADLE_CMD="${GRADLE_CMD:-./gradlew}"
if [ ! -x "$GRADLE_CMD" ]; then
  GRADLE_CMD="gradle"
fi

if ! command -v native-image >/dev/null 2>&1; then
  echo "native-image was not found on PATH. Install GraalVM and the native-image tool first." >&2
  exit 1
fi

NATIVE_IMAGE_BIN="$(command -v native-image)"
GRAALVM_HOME_DETECTED="$(cd "$(dirname "$NATIVE_IMAGE_BIN")/.." && pwd)"

export GRAALVM_HOME="${GRAALVM_HOME:-$GRAALVM_HOME_DETECTED}"
export JAVA_HOME="${JAVA_HOME:-$GRAALVM_HOME}"

echo "Using GRAALVM_HOME=$GRAALVM_HOME"

"$GRADLE_CMD" nativeCompile

NATIVE_OUTPUT="build/native/nativeCompile/plex-importer"
if [ -f "$NATIVE_OUTPUT" ]; then
  echo "Native binary created at $NATIVE_OUTPUT"
else
  echo "nativeCompile finished, but $NATIVE_OUTPUT was not found." >&2
  exit 1
fi
