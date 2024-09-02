#!/usr/bin/env bash


rm go-plex-import


#test_root_dir
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

rm -r test_root_dir

mkdir -p test_root_dir/dir1
touch test_root_dir/dir1/file1.mkv
mkdir -p test_root_dir/dir2
touch test_root_dir/dir2/file2.txt
mkdir -p test_root_dir/dir3
touch test_root_dir/dir3/file3.mp4
mkdir -p test_root_dir/dir4
touch test_root_dir/dir4/file4.mkv
mkdir -p test_root_dir/dir5
touch test_root_dir/dir5/file5.mp4


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

rm -r test_dest_dir

mkdir -p test_dest_dir/Movies
touch test_dest_dir/Movies/placeholder
mkdir -p test_dest_dir/TV\ Shows/Eureka
touch test_dest_dir/TV\ Shows/Eureka/placeholder
mkdir -p test_dest_dir/TV\ Shows/Rick\ and \Morty
touch test_dest_dir/TV\ Shows/Rick\ and \Morty/placeholder
mkdir -p test_dest_dir/TV\ Shows/UFO
touch test_dest_dir/TV\ Shows/UFO/placeholder
mkdir -p test_dest_dir/TV\ Shows/Zoids
touch test_dest_dir/TV\ Shows/Zoids/placeholder


