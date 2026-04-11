#!/usr/bin/env bash
set -euo pipefail

LIB_DIR="lib"
DIST_DIR="dist"
SRC_DIR="src"

mkdir -p "$LIB_DIR" "$DIST_DIR"

download_if_missing() {
  local target="$1"
  local url="$2"
  if [[ ! -f "$target" ]]; then
    curl -fsSL -o "$target" "$url"
  fi
}

download_if_missing "$LIB_DIR/tamboui-toolkit.jar" "https://central.sonatype.com/repository/maven-snapshots/dev/tamboui/tamboui-toolkit/0.2.0-SNAPSHOT/tamboui-toolkit-0.2.0-20260321.225309-38.jar"
download_if_missing "$LIB_DIR/tamboui-tui.jar" "https://central.sonatype.com/repository/maven-snapshots/dev/tamboui/tamboui-tui/0.2.0-SNAPSHOT/tamboui-tui-0.2.0-20260321.225309-38.jar"
download_if_missing "$LIB_DIR/tamboui-jline3-backend.jar" "https://central.sonatype.com/repository/maven-snapshots/dev/tamboui/tamboui-jline3-backend/0.2.0-SNAPSHOT/tamboui-jline3-backend-0.2.0-20260321.225309-40.jar"
download_if_missing "$LIB_DIR/tamboui-core.jar" "https://central.sonatype.com/repository/maven-snapshots/dev/tamboui/tamboui-core/0.2.0-SNAPSHOT/tamboui-core-0.2.0-20260321.225309-40.jar"
download_if_missing "$LIB_DIR/tamboui-widgets.jar" "https://central.sonatype.com/repository/maven-snapshots/dev/tamboui/tamboui-widgets/0.2.0-SNAPSHOT/tamboui-widgets-0.2.0-20260321.225309-38.jar"
download_if_missing "$LIB_DIR/tamboui-css.jar" "https://central.sonatype.com/repository/maven-snapshots/dev/tamboui/tamboui-css/0.2.0-SNAPSHOT/tamboui-css-0.2.0-20260321.225309-40.jar"
download_if_missing "$LIB_DIR/tamboui-annotations.jar" "https://central.sonatype.com/repository/maven-snapshots/dev/tamboui/tamboui-annotations/0.2.0-SNAPSHOT/tamboui-annotations-0.2.0-20260321.225309-40.jar"
download_if_missing "$LIB_DIR/jline.jar" "https://repo.maven.apache.org/maven2/org/jline/jline/3.25.1/jline-3.25.1.jar"

rm -rf "$DIST_DIR"/*
javac -cp "$LIB_DIR/*" -d "$DIST_DIR" $(find "$SRC_DIR" -name '*.java' | sort)

java -ea -cp "$DIST_DIR:$LIB_DIR/*" com.example.jpi.TestMain
