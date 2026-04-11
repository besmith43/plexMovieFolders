#!/usr/bin/env bash
set -euo pipefail

./build.sh

native-image \
  -cp "dist:lib/*" \
  -H:Name=jpi \
  -H:IncludeResources='dev/tamboui/tui/bindings/.*\.properties' \
  com.example.jpi.Main
