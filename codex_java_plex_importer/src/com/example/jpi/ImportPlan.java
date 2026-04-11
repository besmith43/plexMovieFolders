package com.example.jpi;

import java.nio.file.Path;

public record ImportPlan(
        SourceVideo sourceVideo,
        MediaType mediaType,
        MovieMetadata movieMetadata,
        TvMetadata tvMetadata,
        Path destinationFile) {
}
