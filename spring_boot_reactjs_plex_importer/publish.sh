#!/usr/bin/env bash
set -euo pipefail

./build.sh

GRADLE_CMD="${GRADLE_CMD:-./gradlew}"
if [ ! -x "$GRADLE_CMD" ]; then
  GRADLE_CMD="gradle"
fi

"$GRADLE_CMD" bootWar
