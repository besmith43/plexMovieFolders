#!/usr/bin/env bash
set -euo pipefail

REMOTE_HOST="${REMOTE_HOST:-plexmini4}"
REMOTE_DIR="/Users/besmith/.local/plex-importer"
LOCAL_BINARY="build/native/nativeCompile/plex-importer"
REMOTE_PLIST_NAME="com.besmith.plex-importer.plist"
REMOTE_PLIST_PATH="$REMOTE_DIR/$REMOTE_PLIST_NAME"
LAUNCH_AGENTS_DIR="\$HOME/Library/LaunchAgents"
LAUNCHD_LABEL="com.besmith.plex-importer"

./native-publish.sh

ssh "$REMOTE_HOST" << EOF
	if [ ! -d "$REMOTE_DIR" ]; then
		mkdir -p "$REMOTE_DIR"
	else
		launchctl bootout gui/\$(id -u) "$LAUNCH_AGENTS_DIR/$REMOTE_PLIST_NAME" 2>/dev/null || true
		pkill -x plex-importer || true
		rm -f "$REMOTE_DIR"/*
	fi
EOF

sleep 1

scp "$LOCAL_BINARY" "$REMOTE_HOST:$REMOTE_DIR/plex-importer"
scp server_start.sh "$REMOTE_HOST:$REMOTE_DIR/server_start.sh"
scp com.besmith.plex-importer.plist "$REMOTE_HOST:$REMOTE_PLIST_PATH"

ssh "$REMOTE_HOST" << EOF
	mkdir -p "$LAUNCH_AGENTS_DIR"
	chmod +x "$REMOTE_DIR/plex-importer" "$REMOTE_DIR/server_start.sh"
	cp "$REMOTE_PLIST_PATH" "$LAUNCH_AGENTS_DIR/$REMOTE_PLIST_NAME"
	launchctl bootstrap gui/\$(id -u) "$LAUNCH_AGENTS_DIR/$REMOTE_PLIST_NAME"
	launchctl kickstart -k gui/\$(id -u)/$LAUNCHD_LABEL
EOF
