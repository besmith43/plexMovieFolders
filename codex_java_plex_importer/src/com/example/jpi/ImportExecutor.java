package com.example.jpi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ImportExecutor {
    private final LibraryScanner scanner;

    public ImportExecutor(LibraryScanner scanner) {
        this.scanner = scanner;
    }

    public ImportResult execute(ImportPlan plan, boolean overwrite) {
        Path destination = plan.destinationFile();
        try {
            Files.createDirectories(destination.getParent());
            if (Files.exists(destination)) {
                if (!overwrite) {
                    return new ImportResult(
                            plan.sourceVideo(),
                            destination,
                            ImportResult.Status.SKIPPED,
                            false,
                            "Destination already exists");
                }
                Files.move(
                        plan.sourceVideo().file(),
                        destination,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
                boolean deleted = cleanupSourceDirectory(plan.sourceVideo().parentDirectory());
                return new ImportResult(
                        plan.sourceVideo(),
                        destination,
                        ImportResult.Status.OVERWRITTEN,
                        deleted,
                        "Overwrote existing destination");
            }

            Files.move(plan.sourceVideo().file(), destination, StandardCopyOption.ATOMIC_MOVE);
            boolean deleted = cleanupSourceDirectory(plan.sourceVideo().parentDirectory());
            return new ImportResult(
                    plan.sourceVideo(),
                    destination,
                    ImportResult.Status.MOVED,
                    deleted,
                    "Moved successfully");
        } catch (IOException e) {
            return new ImportResult(
                    plan.sourceVideo(),
                    destination,
                    ImportResult.Status.ERROR,
                    false,
                    e.getMessage());
        }
    }

    public boolean cleanupSourceDirectory(Path sourceDirectory) throws IOException {
        if (!Files.isDirectory(sourceDirectory)) {
            return false;
        }
        if (scanner.directoryContainsVideo(sourceDirectory)) {
            return false;
        }
        try (var entries = Files.list(sourceDirectory)) {
            if (entries.findAny().isPresent()) {
                return false;
            }
        }
        Files.delete(sourceDirectory);
        return true;
    }
}
