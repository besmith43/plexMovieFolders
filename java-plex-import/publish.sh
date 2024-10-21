#!/usr/bin/env bash


mvn package

# for anything else
# echo "#!/usr/bin/env java -jar" > app.jar

# for synology
echo "#!java -jar" > app.jar


echo "" >> app.jar
cat target/java-plex-import-1.0-SNAPSHOT.jar >> app.jar

chmod +x app.jar

mkdir source dest
touch source/test.txt

java -jar target/java-plex-import-1.0-SNAPSHOT.jar source/test.txt dest/test2.txt

ls -l source
ls -l dest

rm -r source dest
