#!/usr/bin/env bash
set -euo pipefail

source ./reset.sh
./build.sh
java -cp "dist:lib/*" com.example.jpi.Main
