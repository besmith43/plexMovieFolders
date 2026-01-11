#!/usr/bin/env bash


# scp go-plex-import nas:home/bin

if [ -f gpi ]; then
    echo "deleting old artifact" >&2
    rm gpi
fi

./build.sh

echo "pushing to plexmini4" >&2
scp gpi plexmini4:.local/bin

