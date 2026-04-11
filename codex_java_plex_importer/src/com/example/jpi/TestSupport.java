package com.example.jpi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TestSupport {
    private TestSupport() {
    }

    public static AppConfig createConfig(Path root) throws IOException {
        Path source = root.resolve("source");
        Path dest = root.resolve("dest");
        Files.createDirectories(source);
        Files.createDirectories(dest);
        return new AppConfig(source, dest, AppConfig.fromEnvironment().videoExtensions());
    }

    public static void touch(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, "x");
    }
}
