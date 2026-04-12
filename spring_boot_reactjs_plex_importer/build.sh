#!/usr/bin/env bash
set -euo pipefail

GRADLE_CMD="${GRADLE_CMD:-./gradlew}"
if [ ! -x "$GRADLE_CMD" ]; then
  GRADLE_CMD="gradle"
fi

"$GRADLE_CMD" clean build
./reset.sh
(
  cd frontend
  npx playwright test
)
