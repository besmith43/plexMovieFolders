#!/bin/bash

# go tool dist list

# env GOOS=linux GOARCH=arm64 go build -o go-plex-import main.go
# env GOOS=linux GOARCH=amd64 go build -o go-plex-import main.go

if [ -f go.mod ]; then
    go mod tidy
fi

echo "building project" >&2

go build -o gpi main.go


