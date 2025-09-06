#!/usr/bin/env bash


# glibc_version="$(ssh nas /lib/libc.so.6 | head -n 1 | awk '{print $9 }')"

# glibc_version=${glibc_version::-1}

# echo building for linux x86_64 libc version $glibc_version

# cargo zigbuild --release --target x86_64-unknown-linux-gnu.$glibc_version

cargo build --release


