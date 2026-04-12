#!/usr/bin/env bash
set -euo pipefail

PUBLISH_LOCAL_NETWORK=0

while getopts ":p" opt; do
  case "$opt" in
    p)
      PUBLISH_LOCAL_NETWORK=1
      ;;
    *)
      echo "Usage: ./run.sh [-p]" >&2
      exit 1
      ;;
  esac
done

./reset.sh
./build.sh

GRADLE_CMD="${GRADLE_CMD:-./gradlew}"
if [ ! -x "$GRADLE_CMD" ]; then
  GRADLE_CMD="gradle"
fi

export SOURCE=test_root_dir
export DEST=test_dest_dir

if [ "$PUBLISH_LOCAL_NETWORK" -eq 1 ]; then
  LAN_IP="$(ifconfig 2>/dev/null | awk '/inet / && $2 != "127.0.0.1" { print $2; exit }')"
  echo "Starting Plex Importer on all interfaces at http://0.0.0.0:8080"
  if [ -n "${LAN_IP:-}" ]; then
    echo "Local network URL: http://$LAN_IP:8080"
  fi
  "$GRADLE_CMD" bootRun --args='--server.address=0.0.0.0'
else
  "$GRADLE_CMD" bootRun
fi
