package com.example.jpi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ImportExecutorTest {
    private ImportExecutorTest() {
    }

    public static void runAll() throws Exception {
        testSkipExisting();
        testOverwriteExisting();
        testMoveReportsProgress();
        testMoveFailureKeepsSource();
        testCleanupDeletesEmptyDirectory();
        testCleanupKeepsDirectoryWithNonVideoFile();
    }

    private static void testSkipExisting() throws Exception {
        AppConfig config = TestSupport.createConfig(Files.createTempDirectory("jpi-skip"));
        LibraryScanner scanner = new LibraryScanner(config);
        ImportExecutor executor = new ImportExecutor(scanner);
        PathBuilder builder = new PathBuilder(config);
        Path sourceFile = config.sourceRoot().resolve("dir1/source.mkv");
        TestSupport.touch(sourceFile);
        SourceVideo video = new SourceVideo(sourceFile, sourceFile.getParent(), "mkv");
        ImportPlan plan = builder.buildMoviePlan(video, new MovieMetadata("Alien", 1979, true, ""));
        TestSupport.touch(plan.destinationFile());
        ImportResult result = executor.execute(plan, false);
        assert result.status() == ImportResult.Status.SKIPPED;
        assert Files.exists(sourceFile);
    }

    private static void testOverwriteExisting() throws Exception {
        AppConfig config = TestSupport.createConfig(Files.createTempDirectory("jpi-overwrite"));
        LibraryScanner scanner = new LibraryScanner(config);
        ImportExecutor executor = new ImportExecutor(scanner);
        PathBuilder builder = new PathBuilder(config);
        Path sourceFile = config.sourceRoot().resolve("dir1/source.mkv");
        TestSupport.touch(sourceFile);
        SourceVideo video = new SourceVideo(sourceFile, sourceFile.getParent(), "mkv");
        ImportPlan plan = builder.buildMoviePlan(video, new MovieMetadata("Alien", 1979, true, ""));
        TestSupport.touch(plan.destinationFile());
        ImportResult result = executor.execute(plan, true);
        assert result.status() == ImportResult.Status.OVERWRITTEN;
        assert Files.exists(plan.destinationFile());
        assert !Files.exists(sourceFile);
    }

    private static void testMoveReportsProgress() throws Exception {
        AppConfig config = TestSupport.createConfig(Files.createTempDirectory("jpi-progress"));
        LibraryScanner scanner = new LibraryScanner(config);
        ImportExecutor executor = new ImportExecutor(scanner);
        PathBuilder builder = new PathBuilder(config);
        Path sourceFile = config.sourceRoot().resolve("dir1/source.mkv");
        TestSupport.write(sourceFile, "0123456789".repeat(512));
        SourceVideo video = new SourceVideo(sourceFile, sourceFile.getParent(), "mkv");
        ImportPlan plan = builder.buildMoviePlan(video, new MovieMetadata("Alien", 1979, true, ""));
        List<ImportProgress> updates = new ArrayList<>();
        ImportResult result = executor.execute(plan, false, updates::add);
        assert result.status() == ImportResult.Status.MOVED;
        assert !updates.isEmpty();
        assert updates.get(0).bytesCopied() == 0L;
        long previous = -1L;
        for (ImportProgress update : updates) {
            assert update.totalBytes() == 5120L;
            assert update.bytesCopied() >= previous;
            previous = update.bytesCopied();
        }
        assert updates.get(updates.size() - 1).bytesCopied() == 5120L;
    }

    private static void testMoveFailureKeepsSource() throws Exception {
        AppConfig config = TestSupport.createConfig(Files.createTempDirectory("jpi-failure"));
        LibraryScanner scanner = new LibraryScanner(config);
        ImportExecutor executor = new ImportExecutor(scanner);
        PathBuilder builder = new PathBuilder(config);
        Path sourceFile = config.sourceRoot().resolve("dir1/source.mkv");
        TestSupport.touch(sourceFile);
        SourceVideo video = new SourceVideo(sourceFile, sourceFile.getParent(), "mkv");
        ImportPlan plan = builder.buildMoviePlan(video, new MovieMetadata("Alien", 1979, true, ""));
        Files.createDirectories(plan.destinationFile());
        TestSupport.touch(plan.destinationFile().resolve("existing.txt"));
        ImportResult result = executor.execute(plan, true);
        assert result.status() == ImportResult.Status.ERROR;
        assert Files.exists(sourceFile);
    }

    private static void testCleanupDeletesEmptyDirectory() throws Exception {
        AppConfig config = TestSupport.createConfig(Files.createTempDirectory("jpi-clean-delete"));
        LibraryScanner scanner = new LibraryScanner(config);
        ImportExecutor executor = new ImportExecutor(scanner);
        PathBuilder builder = new PathBuilder(config);
        Path sourceFile = config.sourceRoot().resolve("dir1/source.mkv");
        TestSupport.touch(sourceFile);
        SourceVideo video = new SourceVideo(sourceFile, sourceFile.getParent(), "mkv");
        ImportPlan plan = builder.buildMoviePlan(video, new MovieMetadata("Alien", 1979, true, ""));
        ImportResult result = executor.execute(plan, false);
        assert result.status() == ImportResult.Status.MOVED;
        assert result.sourceDirectoryDeleted();
        assert !Files.exists(sourceFile.getParent());
    }

    private static void testCleanupKeepsDirectoryWithNonVideoFile() throws Exception {
        AppConfig config = TestSupport.createConfig(Files.createTempDirectory("jpi-clean-keep"));
        LibraryScanner scanner = new LibraryScanner(config);
        ImportExecutor executor = new ImportExecutor(scanner);
        PathBuilder builder = new PathBuilder(config);
        Path sourceFile = config.sourceRoot().resolve("dir1/source.mkv");
        TestSupport.touch(sourceFile);
        TestSupport.touch(config.sourceRoot().resolve("dir1/readme.txt"));
        SourceVideo video = new SourceVideo(sourceFile, sourceFile.getParent(), "mkv");
        ImportPlan plan = builder.buildMoviePlan(video, new MovieMetadata("Alien", 1979, true, ""));
        ImportResult result = executor.execute(plan, false);
        assert result.status() == ImportResult.Status.MOVED;
        assert !result.sourceDirectoryDeleted();
        assert Files.exists(sourceFile.getParent());
    }
}
