package com.example.jpi;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ImportExecutorTest {
    private ImportExecutorTest() {
    }

    public static void runAll() throws Exception {
        testSkipExisting();
        testOverwriteExisting();
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
