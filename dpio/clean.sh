#!/usr/bin/env bash


if [ -f go-plex-import ]; then
    rm go-plex-import
fi

if [ -f gpi ]; then
    rm gpi
fi

#test_root_dir
#├── My.Adventures.with.Superman.S03E04.1080p.WEB.h264-EDITH
#│   └── file1.mkv
#├── 12.Monkeys.S04E01.The.End.1080p.BluRay.Dts-HDMa5.1.AVC-PiR8
#│   └── file2.txt
#├── World.War.II.With.Tom.Hanks.S01E12.Battle.For.The.Skies.1080p.NOW.WEB-DL.AAC2.0.H.264-RAWR
#│   └── file3.mp4
#├── Obsession.2025.NORDiC.1080p.WEB-DL.H.264-NORViNE
#│   └── file4.mkv
#└── Undertone.2025.2160p.iT.WEB-DL.DV.HDR10-BenTheMen-AsRequested
#    └── file5.mp4

if [ -d test_root_dir ]; then
    rm -r test_root_dir
fi

mkdir -p test_root_dir/My.Adventures.with.Superman.S03E04.1080p.WEB.h264-EDITH
mkfile -n 1g test_root_dir/My.Adventures.with.Superman.S03E04.1080p.WEB.h264-EDITH/file1.mkv
mkdir -p test_root_dir/12.Monkeys.S04E01.The.End.1080p.BluRay.Dts-HDMa5.1.AVC-PiR8
mkfile -n 1g test_root_dir/12.Monkeys.S04E01.The.End.1080p.BluRay.Dts-HDMa5.1.AVC-PiR8/file2.txt
mkdir -p test_root_dir/World.War.II.With.Tom.Hanks.S01E12.Battle.For.The.Skies.1080p.NOW.WEB-DL.AAC2.0.H.264-RAWR
mkfile -n 1g test_root_dir/World.War.II.With.Tom.Hanks.S01E12.Battle.For.The.Skies.1080p.NOW.WEB-DL.AAC2.0.H.264-RAWR/file3.mp4
mkdir -p test_root_dir/Obsession.2025.NORDiC.1080p.WEB-DL.H.264-NORViNE
mkfile -n 1g test_root_dir/Obsession.2025.NORDiC.1080p.WEB-DL.H.264-NORViNE/file4.mkv
mkdir -p test_root_dir/Undertone.2025.2160p.iT.WEB-DL.DV.HDR10-BenTheMen-AsRequested
mkfile -n 1g test_root_dir/Undertone.2025.2160p.iT.WEB-DL.DV.HDR10-BenTheMen-AsRequested/file5.mp4


#test_dest_dir
#├── Movies
#│   └── placeholder
#└── TV Shows
#    ├── Eureka
#    │   └── placeholder
#    ├── Rick and Morty
#    │   └── placeholder
#    ├── UFO
#    │   └── placeholder
#    └── Zoids
#        └── placeholder

if [ -d test_dest_dir ]; then
    rm -r test_dest_dir
fi

mkdir -p test_dest_dir/Movies
touch test_dest_dir/Movies/placeholder
mkdir -p test_dest_dir/TV\ Shows/Eureka
touch test_dest_dir/TV\ Shows/Eureka/placeholder
mkdir -p test_dest_dir/TV\ Shows/Rick\ and\ Morty
touch test_dest_dir/TV\ Shows/Rick\ and\ Morty/placeholder
mkdir -p test_dest_dir/TV\ Shows/UFO
touch test_dest_dir/TV\ Shows/UFO/placeholder
mkdir -p test_dest_dir/TV\ Shows/Zoids
touch test_dest_dir/TV\ Shows/Zoids/placeholder


if [ -f go.mod ]; then
    go mod tidy
fi


