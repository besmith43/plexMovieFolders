package com.example.jpi;

import java.nio.file.Path;

public final class PathBuilder {
    private final AppConfig config;

    public PathBuilder(AppConfig config) {
        this.config = config;
    }

    public ImportPlan buildMoviePlan(SourceVideo sourceVideo, MovieMetadata movie) {
        String title = sanitize(movie.title());
        String baseName = title + " (" + movie.year() + ")";
        String fileName = baseName;
        if (!movie.standardEdition()) {
            fileName = fileName + " {edition-" + sanitize(movie.edition()) + "}";
        }
        Path destination = config.destRoot()
                .resolve("Movies")
                .resolve(baseName)
                .resolve(fileName + "." + sourceVideo.extension());
        return new ImportPlan(sourceVideo, MediaType.MOVIE, movie, null, destination);
    }

    public ImportPlan buildTvPlan(SourceVideo sourceVideo, TvMetadata tv) {
        String series = sanitize(tv.seriesName());
        String season = "Season %02d".formatted(tv.seasonNumber());
        String fileName = "%s - s%02de%02d.%s"
                .formatted(series, tv.seasonNumber(), tv.episodeNumber(), sourceVideo.extension());
        Path destination = config.destRoot()
                .resolve("TV Shows")
                .resolve(series)
                .resolve(season)
                .resolve(fileName);
        return new ImportPlan(sourceVideo, MediaType.TV_SHOW, null, tv, destination);
    }

    static String sanitize(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        String cleaned = trimmed.replaceAll("[\\\\/:*?\"<>|]", "_");
        return cleaned.replaceAll("\\s+", " ").trim();
    }
}
