package com.example.jpi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class LibraryScanner {
    private final AppConfig config;

    public LibraryScanner(AppConfig config) {
        this.config = config;
    }

    public List<CandidateDirectory> scanCandidateDirectories() throws IOException {
        if (!Files.isDirectory(config.sourceRoot())) {
            return List.of();
        }
        List<CandidateDirectory> results = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(config.sourceRoot())) {
            List<Path> directories = paths
                    .filter(Files::isDirectory)
                    .sorted()
                    .toList();
            for (Path directory : directories) {
                List<SourceVideo> videos = listVideos(directory);
                if (!videos.isEmpty()) {
                    results.add(new CandidateDirectory(
                            directory,
                            videos,
                            Files.getLastModifiedTime(directory).toMillis()));
                }
            }
        }
        results.sort(Comparator
                .comparingLong(CandidateDirectory::lastModifiedMillis)
                .reversed()
                .thenComparing(dir -> dir.path().toString()));
        return results;
    }

    public List<SourceVideo> listVideos(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.list(directory)) {
            return entries
                    .filter(Files::isRegularFile)
                    .filter(config::isVideoFile)
                    .sorted()
                    .map(path -> new SourceVideo(path, directory, extensionOf(path)))
                    .toList();
        }
    }

    public List<String> scanExistingSeries() throws IOException {
        Path tvRoot = config.destRoot().resolve("TV Shows");
        if (!Files.isDirectory(tvRoot)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.list(tvRoot)) {
            return entries
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
    }

    public boolean directoryContainsVideo(Path directory) throws IOException {
        return !listVideos(directory).isEmpty();
    }

    private static String extensionOf(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : "";
    }
}
