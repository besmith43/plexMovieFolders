I would like you to make a website to handle importing new content into an existing plex directory structure.

tech stack:

- java 25
- gradle (kts)
- spring boot
- reactjs


overview of workflow:

1. landing page needs to have a list of all directories with video files in them
2. upon selecting a directory, the user needs to be presented with a form asking if the new content is a "Movie" or "TV Show"
3. perform any path specific actions as defined later in paths 1 and 2
4. check with the user that the final path is correct
5. make any missing directories
6. check that there isn't anything already at that path and if so ask the user if they want to overwrite or skip with skip as the default
7. move the video file
8. delete the directory of the moved video file if its no longer contains any video files


path 1 (movie):
1. ask the user for the movie's title
2. ask the user for the movie's year (validate input as being an int with a value greater than 1900)
3. ask if this is a standard edition (default to yes).  if no, then ask the user for the edition of the movie
4. the final path needs to look like this `DEST/Movies/TITLE (YEAR)/TITLE (YEAR).ext` and for any non-standard edition movies, it'll look like this `DEST/Movies/TITLE (YEAR)/TITLE (YEAR) {edition-EDITION}.ext`


path 2 (tv show):
1. look at `DEST/TV Shows` for all directories (tv show names of series that already exist in the plex library)
2. give that list to the user with a new option "New Series" at the top for the user to select the tv series of the new video file
3. if the user selected "New Series", ask them what the name of the series is
4. ask the user for the tv show's season number
5. ask the user for the tv show's episode number
6. the final path needs to look like this `DEST/TV Shows/Series Name/Season XX/Series Name - sxxexx.ext`


project specific design considerations:

- I would like the SOURCE and DEST variables to have the following defaults, but can be overwritten via environment variables:
    - SOURCE=/Volumes/LightSpeed/containers/sabnzbd/config/Downloads/complete
    - DEST=/Volumes/Content_Vault/Plex
- the project's build system needs to be exclusively handled via the following bash scripts:
    - reset.sh
        - set and export SOURCE environment variable as `test_root_dir`
        - set and export DEST environment variable as `test_dest_dir`
        - delete SOURCE and DEST directories if they exist
        - create SOURCE and DEST directories
        - populate SOURCE directory by running the following commands:
            - mkdir $SOURCE/dir{1..5}
            - mkfile -n 1g $SOURCE/dir1/file1.mkv
            - mkfile -n 1g $SOURCE/dir2/file2.txt
            - mkfile -n 1g $SOURCE/dir3/file3.mp4
            - mkfile -n 1g $SOURCE/dir4/file4.mkv
            - mkfile -n 1g $SOURCE/dir5/file5.mp4
        - populate DEST directory by running the following commands:
            - mkdir $DEST/Movies
            - mkdir -p $DEST/TV\ Shows/Eureka
            - mkdir -p $DEST/TV\ Shows/Rick\ and\ Morty
            - mkdir -p $DEST/TV\ Shows/UFO
            - mkdir -p $DEST/TV\ Shows/Zoids
    - build.sh
        - use gradle to build the project
        - run any and all test suites
    - run.sh
        - execute `reset.sh`
        - execute `build.sh`
        - run project via gradle
    - publish.sh
        - execute `build.sh`
        - use gradle to produce a war file to be deployed into a tomcat 11 runtime



