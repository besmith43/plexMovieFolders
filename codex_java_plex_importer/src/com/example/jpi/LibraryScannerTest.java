package com.example.jpi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

public final class LibraryScannerTest {
    private LibraryScannerTest() {
    }

    public static void runAll() throws Exception {
        testCandidateDirectories();
        testExistingSeriesScan();
    }

    private static void testCandidateDirectories() throws Exception {
        AppConfig config = TestSupport.createConfig(Files.createTempDirectory("jpi-scan"));
        Path dir1 = config.sourceRoot().resolve("dir1");
        Path dir2 = config.sourceRoot().resolve("dir2");
        TestSupport.touch(dir1.resolve("video1.mkv"));
        TestSupport.touch(config.sourceRoot().resolve("dir1/notes.txt"));
        TestSupport.touch(dir2.resolve("video2.mp4"));
        TestSupport.touch(config.sourceRoot().resolve("dir3/other.txt"));
        Files.setLastModifiedTime(dir1, FileTime.fromMillis(1_000L));
        Files.setLastModifiedTime(dir2, FileTime.fromMillis(2_000L));
        LibraryScanner scanner = new LibraryScanner(config);
        List<CandidateDirectory> directories = scanner.scanCandidateDirectories();
        assert directories.size() == 2 : directories.size();
        assert directories.get(0).path().equals(dir2) : directories.get(0).path();
        assert directories.get(1).path().equals(dir1) : directories.get(1).path();
        assert directories.get(0).videos().size() == 1;
        assert directories.get(1).videos().size() == 1;
    }

    private static void testExistingSeriesScan() throws Exception {
        AppConfig config = TestSupport.createConfig(Files.createTempDirectory("jpi-series"));
        Files.createDirectories(config.destRoot().resolve("TV Shows").resolve("Eureka"));
        Files.createDirectories(config.destRoot().resolve("TV Shows").resolve("Zoids"));
        LibraryScanner scanner = new LibraryScanner(config);
        List<String> series = scanner.scanExistingSeries();
        assert series.equals(List.of("Eureka", "Zoids")) : series;
    }
}
