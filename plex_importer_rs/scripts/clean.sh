#!/usr/bin/env bash


cargo clean


if [ -d test_root_dir ]; then
    rm -r test_root_dir
fi


if [ -d test_dest_dir ]; then
    rm -r test_dest_dir
fi


