package com.example.jpi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

public final class ImportExecutor {
    private static final int BUFFER_SIZE = 1024 * 1024;
    private final LibraryScanner scanner;

    public ImportExecutor(LibraryScanner scanner) {
        this.scanner = scanner;
    }

    public ImportResult execute(ImportPlan plan, boolean overwrite) {
        return execute(plan, overwrite, progress -> {
        });
    }

    public ImportResult execute(ImportPlan plan, boolean overwrite, Consumer<ImportProgress> progressListener) {
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
                moveWithProgress(plan.sourceVideo().file(), destination, progressListener, true);
                boolean deleted = cleanupSourceDirectory(plan.sourceVideo().parentDirectory());
                return new ImportResult(
                        plan.sourceVideo(),
                        destination,
                        ImportResult.Status.OVERWRITTEN,
                        deleted,
                            "Overwrote existing destination");
            }

            moveWithProgress(plan.sourceVideo().file(), destination, progressListener, false);
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

    private void moveWithProgress(
            Path source,
            Path destination,
            Consumer<ImportProgress> progressListener,
            boolean overwrite) throws IOException {
        long totalBytes = Files.size(source);
        Path tempDestination = destination.resolveSibling(destination.getFileName() + ".jpi-part");
        Files.deleteIfExists(tempDestination);
        progressListener.accept(new ImportProgress(source, destination, 0L, totalBytes));
        try (InputStream in = Files.newInputStream(source);
             OutputStream out = Files.newOutputStream(
                     tempDestination,
                     StandardOpenOption.CREATE_NEW,
                     StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            long copied = 0L;
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
                copied += read;
                progressListener.accept(new ImportProgress(source, destination, copied, totalBytes));
            }
        } catch (IOException e) {
            Files.deleteIfExists(tempDestination);
            throw e;
        }

        try {
            if (overwrite) {
                Files.move(tempDestination, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(tempDestination, destination);
            }
            Files.delete(source);
        } catch (IOException e) {
            Files.deleteIfExists(tempDestination);
            throw e;
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
