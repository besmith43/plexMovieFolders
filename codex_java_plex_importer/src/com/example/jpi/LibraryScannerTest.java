package com.example.jpi;

import java.nio.file.Files;
import java.nio.file.Path;
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
        TestSupport.touch(config.sourceRoot().resolve("dir1/video1.mkv"));
        TestSupport.touch(config.sourceRoot().resolve("dir1/notes.txt"));
        TestSupport.touch(config.sourceRoot().resolve("dir2/video2.mp4"));
        TestSupport.touch(config.sourceRoot().resolve("dir3/other.txt"));
        LibraryScanner scanner = new LibraryScanner(config);
        List<CandidateDirectory> directories = scanner.scanCandidateDirectories();
        assert directories.size() == 2 : directories.size();
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
