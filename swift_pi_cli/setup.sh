#!/usr/bin/env bash



if [ -d src ]; then
    rm -r src
fi

if [ -d dest ]; then
    rm -r dest
fi

mkdir src dest

#src
#├── dir1
#│   └── file1.mkv
#├── dir2
#│   └── file2.txt
#├── dir3
#│   └── file3.mp4
#├── dir4
#│   └── file4.mkv
#└── dir5
#    └── file5.mp4

mkdir -p src/dir1
touch src/dir1/file1.mkv
mkdir -p src/dir2
touch src/dir2/file2.txt
mkdir -p src/dir3
touch src/dir3/file3.mp4
mkdir -p src/dir4
touch src/dir4/file4.mkv
mkdir -p src/dir5
touch src/dir5/file5.mp4


#dest
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


mkdir -p dest/Movies
touch dest/Movies/placeholder
mkdir -p dest/TV\ Shows/Eureka
touch dest/TV\ Shows/Eureka/placeholder
mkdir -p dest/TV\ Shows/Rick\ and\ Morty
touch dest/TV\ Shows/Rick\ and\ Morty/placeholder
mkdir -p dest/TV\ Shows/UFO
touch dest/TV\ Shows/UFO/placeholder
mkdir -p dest/TV\ Shows/Zoids
touch dest/TV\ Shows/Zoids/placeholder


