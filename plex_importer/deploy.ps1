#!/usr/bin/env pwsh

dotnet publish -c release -r linux-x64 --self-contained=true -p:PublishSingleFile=true

scp -O ./bin/Release/net8.0/linux-x64/publish/plex_importer nas:~/bin
