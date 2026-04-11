package com.example.jpi;

import java.nio.file.Path;
import java.util.List;

public record CandidateDirectory(Path path, List<SourceVideo> videos) {
    public String displayName(Path sourceRoot) {
        Path normalized = path.toAbsolutePath().normalize();
        Path root = sourceRoot.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString();
        }
        return normalized.toString();
    }
}
