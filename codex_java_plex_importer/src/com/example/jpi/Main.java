package com.example.jpi;

import com.example.jpi.ui.PlexImporterApp;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromEnvironment();
        LibraryScanner scanner = new LibraryScanner(config);
        PathBuilder pathBuilder = new PathBuilder(config);
        ImportExecutor executor = new ImportExecutor(scanner);
        new PlexImporterApp(config, scanner, pathBuilder, executor).run();
    }
}
