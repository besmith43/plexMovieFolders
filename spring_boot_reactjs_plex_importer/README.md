# Plex Importer

Web application for moving downloaded video files from a source tree into an existing Plex directory structure.

## Stack
- Java 25
- Gradle Kotlin DSL
- Spring Boot
- React + Vite
- Playwright for browser e2e validation

## Default Paths
The application uses these defaults unless overridden by environment variables:

- `SOURCE=/Volumes/LightSpeed/containers/sabnzbd/config/Downloads/complete`
- `DEST=/Volumes/Content_Vault/Plex`

Override them before running the app if needed:

```bash
export SOURCE=/path/to/source
export DEST=/path/to/dest
```

## Scripts
### `./reset.sh`
Creates a local test fixture using:
- `SOURCE=test_root_dir`
- `DEST=test_dest_dir`

It deletes and recreates those directories, then populates them with sample files and Plex destination folders.

### `./build.sh`
Runs the full verification pipeline:
- backend compile and packaging
- frontend production build
- frontend unit tests
- backend tests
- WAR packaging
- fixture reset via `./reset.sh`
- Playwright e2e tests against the running application

### `./run.sh`
Runs the local workflow end to end:
1. `./reset.sh`
2. `./build.sh`
3. starts the app with Gradle

Use `-p` to bind the app to `0.0.0.0` so it is reachable from other devices on your local network:

```bash
./run.sh -p
```

### `./publish.sh`
Runs `./build.sh` and then produces the deployable WAR artifact.

### `./native-publish.sh`
Runs `./build.sh` and then produces a GraalVM native executable with Gradle's `nativeCompile` task.

## Local Development
### Start the app with the scripted test environment
```bash
./run.sh
```

This will use `test_root_dir` and `test_dest_dir` created by `./reset.sh`.

### Start the app with your real paths
```bash
export SOURCE=/your/downloads/path
export DEST=/your/plex/path
./gradlew bootRun
```

## Build and Test
Primary entrypoint:

```bash
./build.sh
```

Direct Gradle build:

```bash
./gradlew build
```

Run the Playwright suite directly:

```bash
cd frontend
npx playwright test
```

## Playwright Prerequisite
The e2e suite requires a local Playwright browser install. One-time setup:

```bash
cd frontend
npx playwright install chromium
```

## Publish Artifact
```bash
./publish.sh
```

WAR output:
- `build/libs/plex-importer.war`

## Native Publish Artifact
```bash
./native-publish.sh
```

Prerequisites:
- GraalVM with `native-image` installed and available on `PATH`

Native binary output:
- `build/native/nativeCompile/plex-importer`

## UI Workflow
1. Select a source directory that contains at least one supported video file.
2. Select exactly one video file from that directory.
3. Choose `Movie` or `TV Show`.
4. Fill in the required metadata.
5. Preview the final Plex destination path.
6. If the destination already exists, choose `Skip` or `Overwrite`.
7. Execute the import.

After a successful move, the app deletes the original source directory only if it no longer contains any video files.

## Supported Video Extensions
- `.mkv`
- `.mp4`
- `.avi`
- `.mov`
- `.m4v`
- `.wmv`

## API Endpoints
- `GET /api/directories`
- `GET /api/tv-series`
- `POST /api/preview`
- `POST /api/import`

## Tomcat Deployment
The project builds a WAR intended for Tomcat 11 deployment:

```bash
./publish.sh
```

Deploy:
- `build/libs/plex-importer.war`

## Docker Tomcat 11
Build and run the published WAR in Tomcat 11 with bind-mounted media paths:

```bash
./docker-run.sh
```

The script:
- runs `./publish.sh`
- builds the local Docker image
- starts Tomcat 11 with `build/libs/plex-importer.war` deployed as `ROOT.war`
- bind-mounts your host source and destination directories into the container

Default host paths:
- `HOST_SOURCE=/Volumes/LightSpeed/containers/sabnzbd/config/Downloads/complete`
- `HOST_DEST=/Volumes/Content_Vault/Plex`

Override them if needed:

```bash
HOST_SOURCE=/path/to/source HOST_DEST=/path/to/dest ./docker-run.sh
```

Useful overrides:
- `IMAGE_NAME`
- `CONTAINER_NAME`
- `PORT`
- `CONTAINER_SOURCE`
- `CONTAINER_DEST`

Tomcat container defaults:
- app URL: `http://localhost:8080`
- container `SOURCE=/data/source`
- container `DEST=/data/dest`

## Notes
- The repo includes a Gradle wrapper, so a global Gradle install is not required for normal use.
- Frontend assets are built and packaged into the Spring Boot application during the Gradle build.
- Playwright e2e tests live under `frontend/e2e` and use `frontend/playwright.config.js`.
