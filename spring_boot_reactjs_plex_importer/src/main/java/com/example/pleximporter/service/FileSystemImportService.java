package com.example.pleximporter.service;

import com.example.pleximporter.config.AppProperties;
import com.example.pleximporter.dto.ImportNotification;
import com.example.pleximporter.dto.ImportRequest;
import com.example.pleximporter.dto.ImportResponse;
import com.example.pleximporter.dto.PreviewRequest;
import com.example.pleximporter.dto.PreviewResponse;
import com.example.pleximporter.dto.SourceDirectoryDto;
import com.example.pleximporter.dto.VideoFileDto;
import com.example.pleximporter.model.ConflictAction;
import com.example.pleximporter.model.ContentType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileSystemImportService {

    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mkv", "mp4", "avi", "mov", "m4v", "wmv");
    private static final String NEW_SERIES = "__NEW_SERIES__";

    private final Path sourceRoot;
    private final Path destRoot;
    private final ImportEventStreamService importEventStreamService;

    public FileSystemImportService(AppProperties appProperties, ImportEventStreamService importEventStreamService) {
        this.sourceRoot = Path.of(appProperties.source()).toAbsolutePath().normalize();
        this.destRoot = Path.of(appProperties.dest()).toAbsolutePath().normalize();
        this.importEventStreamService = importEventStreamService;
    }

    public List<SourceDirectoryDto> listSourceDirectories() {
        if (Files.notExists(sourceRoot)) {
            return List.of();
        }

        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            return paths
                    .filter(Files::isDirectory)
                    .map(this::toSourceDirectory)
                    .filter(dto -> !dto.videoFiles().isEmpty())
                    .sorted(Comparator.comparing(SourceDirectoryDto::absolutePath))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to scan source directories", e);
        }
    }

    public List<String> listExistingSeries() {
        Path tvShowsRoot = destRoot.resolve("TV Shows");
        if (Files.notExists(tvShowsRoot)) {
            return List.of(NEW_SERIES);
        }

        try (Stream<Path> paths = Files.list(tvShowsRoot)) {
            List<String> series = paths
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
            LinkedHashSet<String> result = new LinkedHashSet<>();
            result.add(NEW_SERIES);
            result.addAll(series);
            return List.copyOf(result);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list TV series", e);
        }
    }

    public PreviewResponse preview(PreviewRequest request) {
        ImportPreview preview = buildPreview(request);
        return new PreviewResponse(
                preview.sourceFile().getFileName().toString(),
                preview.destinationPath().toString(),
                preview.collision(),
                preview.message()
        );
    }

    public ImportResponse executeImport(ImportRequest request) {
        ConflictAction conflictAction = request.conflictAction() == null ? ConflictAction.SKIP : request.conflictAction();
        ImportPreview preview = buildPreview(request.preview());

        try {
            Files.createDirectories(preview.destinationPath().getParent());
            if (preview.collision()) {
                if (conflictAction == ConflictAction.SKIP) {
                    ImportResponse response = new ImportResponse("SKIPPED", preview.destinationPath().toString(), false,
                            "Destination already exists. File was skipped.");
                    importEventStreamService.publishImportComplete(ImportNotification.from(response));
                    return response;
                }
                Files.move(preview.sourceFile(), preview.destinationPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(preview.sourceFile(), preview.destinationPath());
            }

            boolean deletedSourceDir = cleanupSourceDirectory(preview.sourceDirectory());
            ImportResponse response = new ImportResponse("MOVED", preview.destinationPath().toString(), deletedSourceDir,
                    "Import completed successfully.");
            importEventStreamService.publishImportComplete(ImportNotification.from(response));
            return response;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to import file", e);
        }
    }

    ImportPreview buildPreview(PreviewRequest request) {
        Path sourceDirectory = validateSourceDirectory(request.sourceDirectory());
        Path sourceFile = validateSourceFile(sourceDirectory, request.sourceFile());
        Path destinationPath = switch (request.contentType()) {
            case MOVIE -> buildMoviePath(sourceFile, request);
            case TV_SHOW -> buildTvPath(sourceFile, request);
        };

        boolean collision = Files.exists(destinationPath);
        String message = collision
                ? "Destination already exists. Default action is skip."
                : "Destination path is available.";
        return new ImportPreview(sourceDirectory, sourceFile, destinationPath, collision, message);
    }

    private SourceDirectoryDto toSourceDirectory(Path directory) {
        try (Stream<Path> files = Files.list(directory)) {
            List<VideoFileDto> videoFiles = files
                    .filter(Files::isRegularFile)
                    .filter(this::isVideoFile)
                    .sorted()
                    .map(this::toVideoFile)
                    .toList();
            return new SourceDirectoryDto(directory.getFileName().toString(), directory.toString(), videoFiles);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to inspect directory: " + directory, e);
        }
    }

    private VideoFileDto toVideoFile(Path path) {
        return new VideoFileDto(path.getFileName().toString(), extensionOf(path), path.toString());
    }

    private Path validateSourceDirectory(String rawSourceDirectory) {
        if (!StringUtils.hasText(rawSourceDirectory)) {
            throw new IllegalArgumentException("Source directory is required.");
        }

        Path sourceDirectory = Path.of(rawSourceDirectory).toAbsolutePath().normalize();
        if (!sourceDirectory.startsWith(sourceRoot)) {
            throw new IllegalArgumentException("Source directory must be inside SOURCE.");
        }
        if (!Files.isDirectory(sourceDirectory)) {
            throw new IllegalArgumentException("Selected source directory does not exist.");
        }
        return sourceDirectory;
    }

    private Path validateSourceFile(Path sourceDirectory, String rawSourceFile) {
        if (!StringUtils.hasText(rawSourceFile)) {
            throw new IllegalArgumentException("Source file is required.");
        }

        Path sourceFile = sourceDirectory.resolve(rawSourceFile).normalize();
        if (!sourceFile.startsWith(sourceDirectory)) {
            throw new IllegalArgumentException("Source file must be inside the selected directory.");
        }
        if (!Files.isRegularFile(sourceFile) || !isVideoFile(sourceFile)) {
            throw new IllegalArgumentException("Selected source file does not exist or is not a supported video file.");
        }
        return sourceFile;
    }

    private Path buildMoviePath(Path sourceFile, PreviewRequest request) {
        String title = sanitizeRequired(request.title(), "Movie title is required.");
        Integer year = request.year();
        if (year == null || year <= 1900) {
            throw new IllegalArgumentException("Movie year must be greater than 1900.");
        }

        boolean standardEdition = request.standardEdition() == null || request.standardEdition();
        String directoryName = "%s (%d)".formatted(title, year);
        String baseFileName = "%s (%d)".formatted(title, year);
        String fileName = standardEdition
                ? baseFileName + "." + extensionOf(sourceFile)
                : baseFileName + " {edition-" + sanitizeRequired(request.edition(), "Edition is required for non-standard movies.") + "}." + extensionOf(sourceFile);

        return destRoot.resolve("Movies").resolve(directoryName).resolve(fileName);
    }

    private Path buildTvPath(Path sourceFile, PreviewRequest request) {
        String seriesName = resolveSeriesName(request.existingSeries(), request.newSeriesName());
        Integer seasonNumber = request.seasonNumber();
        Integer episodeNumber = request.episodeNumber();
        if (seasonNumber == null || seasonNumber < 1) {
            throw new IllegalArgumentException("Season number must be greater than 0.");
        }
        if (episodeNumber == null || episodeNumber < 1) {
            throw new IllegalArgumentException("Episode number must be greater than 0.");
        }

        String seasonFolder = "Season %02d".formatted(seasonNumber);
        String episodeFile = "%s - s%02de%02d.%s".formatted(seriesName, seasonNumber, episodeNumber, extensionOf(sourceFile));
        return destRoot.resolve("TV Shows").resolve(seriesName).resolve(seasonFolder).resolve(episodeFile);
    }

    private String resolveSeriesName(String existingSeries, String newSeriesName) {
        if (NEW_SERIES.equals(existingSeries) || !StringUtils.hasText(existingSeries)) {
            return sanitizeRequired(newSeriesName, "Series name is required for a new series.");
        }
        return existingSeries.trim();
    }

    private boolean cleanupSourceDirectory(Path sourceDirectory) throws IOException {
        if (containsVideoFiles(sourceDirectory)) {
            return false;
        }
        deleteRecursively(sourceDirectory);
        return true;
    }

    private boolean containsVideoFiles(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .anyMatch(this::isVideoFile);
        }
    }

    private void deleteRecursively(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean isVideoFile(Path path) {
        String extension = extensionOf(path);
        return VIDEO_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
    }

    private String extensionOf(Path path) {
        String fileName = path.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(extensionIndex + 1);
    }

    private String sanitizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim().replaceAll("[\\/]+", "-");
    }
}
