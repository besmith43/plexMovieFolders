#!/usr/bin/env bash
set -euo pipefail

IMAGE_NAME="${IMAGE_NAME:-plex-importer-tomcat:local}"
CONTAINER_NAME="${CONTAINER_NAME:-plex-importer-tomcat}"
PORT="${PORT:-8080}"

export SOURCE=test_root_dir
export DEST=test_dest_dir

HOST_SOURCE="${HOST_SOURCE:-${SOURCE:-/Volumes/LightSpeed/containers/sabnzbd/config/Downloads/complete}}"
HOST_DEST="${HOST_DEST:-${DEST:-/Volumes/Content_Vault/Plex}}"

CONTAINER_SOURCE="${CONTAINER_SOURCE:-/data/source}"
CONTAINER_DEST="${CONTAINER_DEST:-/data/dest}"

if [ ! -d "$HOST_SOURCE" ]; then
  echo "Host source directory does not exist: $HOST_SOURCE" >&2
  exit 1
fi

if [ ! -d "$HOST_DEST" ]; then
  echo "Host destination directory does not exist: $HOST_DEST" >&2
  exit 1
fi

./publish.sh

docker build -t "$IMAGE_NAME" .
docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true

docker run \
  --name "$CONTAINER_NAME" \
  --publish "$PORT:8080" \
  --env SOURCE="$CONTAINER_SOURCE" \
  --env DEST="$CONTAINER_DEST" \
  --volume "$HOST_SOURCE:$CONTAINER_SOURCE" \
  --volume "$HOST_DEST:$CONTAINER_DEST" \
  "$IMAGE_NAME"
