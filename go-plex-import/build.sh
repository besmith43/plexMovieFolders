#!/bin/bash

# go tool dist list

# env GOOS=linux GOARCH=arm64 go build -o go-plex-import main.go
env GOOS=linux GOARCH=amd64 go build -o go-plex-import main.go



