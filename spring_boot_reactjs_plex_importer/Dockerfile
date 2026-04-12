ARG TOMCAT_IMAGE=tomcat:11.0-jdk25-temurin
FROM ${TOMCAT_IMAGE}

ENV SOURCE=/data/source \
    DEST=/data/dest

WORKDIR /usr/local/tomcat

RUN rm -rf webapps/*
RUN mkdir -p /data/source /data/dest

COPY build/libs/plex-importer.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
