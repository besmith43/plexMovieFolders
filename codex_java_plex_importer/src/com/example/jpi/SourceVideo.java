package com.example.jpi;

import java.nio.file.Path;

public record SourceVideo(Path file, Path parentDirectory, String extension) {
    public String fileName() {
        return file.getFileName().toString();
    }
}
