package com.example.jpi;

import java.nio.file.Files;
import java.nio.file.Path;

public final class PathBuilderTest {
    private PathBuilderTest() {
    }

    public static void runAll() throws Exception {
        testMoviePathStandard();
        testMoviePathEdition();
        testTvPath();
    }

    private static void testMoviePathStandard() throws Exception {
        AppConfig config = TestSupport.createConfig(Files.createTempDirectory("jpi-path-standard"));
        PathBuilder builder = new PathBuilder(config);
        SourceVideo video = new SourceVideo(config.sourceRoot().resolve("file.mkv"), config.sourceRoot(), "mkv");
        ImportPlan plan = builder.buildMoviePlan(video, new MovieMetadata("Alien", 1979, true, ""));
        Path expected = config.destRoot().resolve("Movies").resolve("Alien (1979)").resolve("Alien (1979).mkv");
        assert expected.equals(plan.destinationFile()) : plan.destinationFile();
    }

    private static void testMoviePathEdition() throws Exception {
        AppConfig config = TestSupport.createConfig(Files.createTempDirectory("jpi-path-edition"));
        PathBuilder builder = new PathBuilder(config);
        SourceVideo video = new SourceVideo(config.sourceRoot().resolve("file.mp4"), config.sourceRoot(), "mp4");
        ImportPlan plan = builder.buildMoviePlan(video, new MovieMetadata("Blade Runner", 1982, false, "Final Cut"));
        Path expected = config.destRoot()
                .resolve("Movies")
                .resolve("Blade Runner (1982)")
                .resolve("Blade Runner (1982) {edition-Final Cut}.mp4");
        assert expected.equals(plan.destinationFile()) : plan.destinationFile();
    }

    private static void testTvPath() throws Exception {
        AppConfig config = TestSupport.createConfig(Files.createTempDirectory("jpi-path-tv"));
        PathBuilder builder = new PathBuilder(config);
        SourceVideo video = new SourceVideo(config.sourceRoot().resolve("file.mp4"), config.sourceRoot(), "mp4");
        ImportPlan plan = builder.buildTvPlan(video, new TvMetadata("Eureka", 2, 7));
        Path expected = config.destRoot()
                .resolve("TV Shows")
                .resolve("Eureka")
                .resolve("Season 02")
                .resolve("Eureka - s02e07.mp4");
        assert expected.equals(plan.destinationFile()) : plan.destinationFile();
    }
}
