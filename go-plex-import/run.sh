#!/usr/bin/env bash

# go run main.go -dryrun -source ./test_root_dir -dest ./test_dest_dir


if [ -f go.mod ]; then
    go mod tidy
fi


go run main.go -source ./test_root_dir -dest ./test_dest_dir

echo
echo

tree test_dest_dir

echo
echo

