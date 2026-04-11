package com.example.jpi;

import java.nio.file.Path;

public record ImportResult(
        SourceVideo sourceVideo,
        Path destinationFile,
        Status status,
        boolean sourceDirectoryDeleted,
        String message) {
    public enum Status {
        MOVED,
        OVERWRITTEN,
        SKIPPED,
        ERROR
    }
}
