#!/usr/bin/env bash


cd PlexImport

dotnet publish -c release -r osx-arm64 --self-contained=true -p:PublishSingleFile=true

rm ./bin/Release/net10.0/osx-arm64/publish/appsettings.Development.json

rm ./bin/Release/net10.0/osx-arm64/publish/PlexImport.pdb

scp -r ./bin/Release/net10.0/osx-arm64/publish/* plexmini4:~/.local/PlexImport
