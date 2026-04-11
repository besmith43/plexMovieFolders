package com.example.jpi;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public record AppConfig(Path sourceRoot, Path destRoot, Set<String> videoExtensions) {
    public static final String DEFAULT_SOURCE =
            "/Volumes/LightSpeed/containers/sabnzbd/config/Downloads/complete";
    public static final String DEFAULT_DEST = "/Volumes/Content_Vault/Plex";

    public static AppConfig fromEnvironment() {
        String source = System.getenv().getOrDefault("SOURCE", DEFAULT_SOURCE);
        String dest = System.getenv().getOrDefault("DEST", DEFAULT_DEST);
        return new AppConfig(
                Path.of(source).toAbsolutePath().normalize(),
                Path.of(dest).toAbsolutePath().normalize(),
                Set.of("mkv", "mp4", "avi", "mov", "m4v", "wmv"));
    }

    public boolean isVideoFile(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return false;
        }
        return videoExtensions.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }
}
