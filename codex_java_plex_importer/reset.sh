#!/usr/bin/env bash
set -euo pipefail

export SOURCE="test_root_dir"
export DEST="test_dest_dir"

rm -rf "$SOURCE" "$DEST"
mkdir -p "$SOURCE" "$DEST"

mkdir "$SOURCE"/dir{1..5}
mkfile -n 1g "$SOURCE/dir1/file1.mkv"
mkfile -n 1g "$SOURCE/dir2/file2.txt"
mkfile -n 1g "$SOURCE/dir3/file3.mp4"
mkfile -n 1g "$SOURCE/dir4/file4.mkv"
mkfile -n 1g "$SOURCE/dir5/file5.mp4"

mkdir "$DEST/Movies"
mkdir -p "$DEST/TV Shows/Eureka"
mkdir -p "$DEST/TV Shows/Rick and Morty"
mkdir -p "$DEST/TV Shows/UFO"
mkdir -p "$DEST/TV Shows/Zoids"
