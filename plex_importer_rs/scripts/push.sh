#!/usr/bin/env bash



binary="./target/x86_64-unknown-linux-gnu/release/pi"

if [ ! -f "$binary" ]; then
    echo "didn't find the binary to push"
    exit 1
fi

scp -O $binary nas:~/bin

