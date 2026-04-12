package com.example.jpi.ui;

import com.example.jpi.AppConfig;
import com.example.jpi.CandidateDirectory;
import com.example.jpi.ImportExecutor;
import com.example.jpi.ImportPlan;
import com.example.jpi.ImportProgress;
import com.example.jpi.ImportResult;
import com.example.jpi.LibraryScanner;
import com.example.jpi.MediaType;
import com.example.jpi.MovieMetadata;
import com.example.jpi.PathBuilder;
import com.example.jpi.SourceVideo;
import com.example.jpi.TvMetadata;
import dev.tamboui.style.Color;
import dev.tamboui.style.Overflow;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.input.TextInputState;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.list;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.text;
import static dev.tamboui.toolkit.Toolkit.textInput;

public final class PlexImporterApp extends ToolkitApp {
    private static final String NEW_SERIES = "New Series";
    private static final List<String> SPINNER_FRAMES = List.of("|", "/", "-", "\\");
    private static final int MAX_VISIBLE_ROWS = 12;

    private final AppConfig config;
    private final LibraryScanner scanner;
    private final PathBuilder pathBuilder;
    private final ImportExecutor executor;

    private Screen screen = Screen.LOADING;
    private String errorMessage = "";
    private String infoMessage = "";

    private List<CandidateDirectory> candidateDirectories = List.of();
    private final Set<Integer> selectedDirectoryIndexes = new LinkedHashSet<>();
    private int directoryCursor = 0;

    private final List<SourceVideo> queuedVideos = new ArrayList<>();
    private final List<ImportResult> results = new ArrayList<>();
    private int currentVideoIndex = -1;
    private SourceVideo currentVideo;

    private MediaType selectedMediaType = MediaType.MOVIE;
    private int mediaTypeCursor = 0;

    private final TextInputState movieTitleState = new TextInputState();
    private final TextInputState movieYearState = new TextInputState();
    private int movieStandardCursor = 0;
    private final TextInputState movieEditionState = new TextInputState();
    private final TextInputState directorySearchState = new TextInputState();
    private boolean directorySearchActive = false;

    private List<String> existingSeries = List.of();
    private int seriesCursor = 0;
    private final TextInputState newSeriesState = new TextInputState();
    private final TextInputState seasonState = new TextInputState();
    private final TextInputState episodeState = new TextInputState();

    private ImportPlan currentPlan;
    private int confirmCursor = 0;
    private int conflictCursor = 0;
    private int resultsCursor = 0;
    private int finalActionCursor = 0;
    private ToolkitRunner.ScheduledAction cursorBlinkAction;
    private ToolkitRunner.ScheduledAction spinnerAction;
    private boolean cursorVisible = true;
    private String killBuffer = "";
    private int spinnerFrameIndex = 0;
    private String loadingMessage = "Loading importer state...";
    private String loadingDetail = "";
    private ImportProgress currentImportProgress;
    private int queuedImportCount = 0;
    private boolean pendingSecondG = false;

    public PlexImporterApp(
            AppConfig config,
            LibraryScanner scanner,
            PathBuilder pathBuilder,
            ImportExecutor executor) {
        this.config = config;
        this.scanner = scanner;
        this.pathBuilder = pathBuilder;
        this.executor = executor;
    }

    @Override
    protected void onStart() {
        infoMessage = "SOURCE=" + config.sourceRoot() + "  DEST=" + config.destRoot();
        startAnimations();
        beginLibraryScan();
    }

    private void startAnimations() {
        if (runner() == null) {
            return;
        }
        cursorBlinkAction = runner().scheduleRepeating(() ->
                        runner().runOnRenderThread(() -> {
                            if (isInputScreen()) {
                                cursorVisible = !cursorVisible;
                            } else {
                                cursorVisible = true;
                            }
                        }),
                Duration.ofMillis(500));
        spinnerAction = runner().scheduleRepeating(() ->
                        runner().runOnRenderThread(() -> spinnerFrameIndex = (spinnerFrameIndex + 1) % SPINNER_FRAMES.size()),
                Duration.ofMillis(120));
    }

    private void beginLibraryScan() {
        screen = Screen.LOADING;
        loadingMessage = "Scanning source and TV libraries...";
        loadingDetail = "Waiting for candidate directories and existing TV series.";
        clearError();
        Thread thread = new Thread(() -> {
            try {
                List<CandidateDirectory> scannedDirectories = scanner.scanCandidateDirectories();
                List<String> scannedSeries = scanner.scanExistingSeries();
                onUiThread(() -> {
                    candidateDirectories = scannedDirectories;
                    existingSeries = scannedSeries;
                    selectedDirectoryIndexes.clear();
                    queuedVideos.clear();
                    results.clear();
                    currentVideo = null;
                    currentVideoIndex = -1;
                    directoryCursor = 0;
                    seriesCursor = 0;
                    resultsCursor = 0;
                    finalActionCursor = 0;
                    queuedImportCount = 0;
                    currentImportProgress = null;
                    pendingSecondG = false;
                    resetDirectorySearch();
                    clearError();
                    screen = candidateDirectories.isEmpty() ? Screen.EMPTY : Screen.SELECT_DIRECTORIES;
                });
            } catch (IOException e) {
                onUiThread(() -> {
                    screen = Screen.ERROR;
                    errorMessage = e.getMessage();
                });
            }
        }, "jpi-library-scan");
        thread.setDaemon(true);
        thread.start();
    }

    private void onUiThread(Runnable action) {
        if (runner() != null) {
            runner().runOnRenderThread(action);
        } else {
            action.run();
        }
    }

    @Override
    protected void onStop() {
        if (cursorBlinkAction != null) {
            cursorBlinkAction.cancel();
            cursorBlinkAction = null;
        }
        if (spinnerAction != null) {
            spinnerAction.cancel();
            spinnerAction = null;
        }
    }

    @Override
    protected Element render() {
        return switch (screen) {
            case LOADING -> shell("Loading", column(
                    text(spinnerFrame() + " " + loadingMessage).cyan().bold(),
                    text(loadingDetail).dim()
            ).spacing(1));
            case EMPTY -> shell("No Videos Found", column(
                    text("No candidate directories with supported video files were found.").yellow(),
                    text("Press q to quit.").dim()
            ));
            case ERROR -> shell("Error", column(
                    text("The importer hit an unrecoverable error.").red().bold(),
                    text(errorMessage).red(),
                    text("Press q to quit.").dim()
            ));
            case SELECT_DIRECTORIES -> renderDirectorySelection();
            case CHOOSE_MEDIA_TYPE -> renderChoiceScreen(
                    "Classify Video",
                    "Choose whether the current file is a movie or a TV episode.",
                    List.of("Movie", "TV Show"),
                    mediaTypeCursor);
            case MOVIE_TITLE -> renderInputScreen(
                    "Movie Title",
                    "Enter the movie title for " + currentVideo.fileName(),
                    movieTitleState,
                    "Title");
            case MOVIE_YEAR -> renderInputScreen(
                    "Movie Year",
                    "Enter the release year for " + currentVideo.fileName(),
                    movieYearState,
                    "Year");
            case MOVIE_STANDARD -> renderChoiceScreen(
                    "Movie Edition",
                    "Is this the standard edition?",
                    List.of("Yes", "No"),
                    movieStandardCursor);
            case MOVIE_EDITION -> renderInputScreen(
                    "Movie Edition",
                    "Enter the edition label, for example Director's Cut.",
                    movieEditionState,
                    "Edition");
            case TV_SERIES -> renderChoiceScreen(
                    "TV Series",
                    "Select an existing series or choose New Series.",
                    seriesOptions(),
                    seriesCursor);
            case TV_NEW_SERIES -> renderInputScreen(
                    "New Series",
                    "Enter the series name for " + currentVideo.fileName(),
                    newSeriesState,
                    "Series Name");
            case TV_SEASON -> renderInputScreen(
                    "Season Number",
                    "Enter the season number.",
                    seasonState,
                    "Season");
            case TV_EPISODE -> renderInputScreen(
                    "Episode Number",
                    "Enter the episode number.",
                    episodeState,
                    "Episode");
            case CONFIRM_DESTINATION -> renderChoiceScreen(
                    "Confirm Destination",
                    "Review the final destination path before moving the file.",
                    List.of("Confirm", "Edit"),
                    confirmCursor,
                    text(currentPlan.destinationFile().toString()).cyan().overflow(Overflow.WRAP_WORD));
            case CONFLICT -> renderChoiceScreen(
                    "Destination Exists",
                    "A file already exists at the destination path.",
                    List.of("Skip", "Overwrite"),
                    conflictCursor,
                    text(currentPlan.destinationFile().toString()).yellow().overflow(Overflow.WRAP_WORD));
            case IMPORTING -> renderImporting();
            case RESULTS -> renderResults();
        };
    }

    private Element renderDirectorySelection() {
        List<Integer> visibleIndexes = filteredCandidateIndexes();
        List<String> rows = new ArrayList<>();
        for (int candidateIndex : visibleIndexes) {
            CandidateDirectory candidate = candidateDirectories.get(candidateIndex);
            String marker = selectedDirectoryIndexes.contains(candidateIndex) ? "[x] " : "[ ] ";
            String suffix = " (" + candidate.videos().size() + " video";
            if (candidate.videos().size() != 1) {
                suffix += "s";
            }
            suffix += ")";
            rows.add(marker + candidate.displayName(config.sourceRoot()) + suffix);
        }

        var directoryWindow = windowedRows(rows, directoryCursor);
        var directoryList = list(directoryWindow.rows())
                .selected(directoryWindow.selectedIndex())
                .highlightColor(Color.CYAN)
                .highlightSymbol("> ")
                .title("Candidate Directories")
                .rounded()
                .fill();

        return shell(
                "Select Imports",
                column(
                        text(directorySearchActive
                                ? "Search candidates. Type to filter. Enter closes search."
                                : "Select one or more directories. Space toggles. / searches. Enter begins the wizard. gg/G jump.").dim(),
                        textInput(directorySearchState)
                                .focusable(false)
                                .showCursor(directorySearchActive && cursorVisible)
                                .cursorRequiresFocus(false)
                                .title("Search")
                                .rounded()
                                .focusedBorderColor(Color.CYAN),
                        rows.isEmpty() ? text("No candidate directories match the current search.").yellow() : directoryList,
                        footerLine()
                ).spacing(1))
                .focusable()
                .onKeyEvent(this::handleDirectorySelectionKey);
    }

    private Element renderChoiceScreen(String title, String prompt, List<String> options, int selectedIndex) {
        return renderChoiceScreen(title, prompt, options, selectedIndex, null);
    }

    private Element renderChoiceScreen(
            String title,
            String prompt,
            List<String> options,
            int selectedIndex,
            Element extra) {
        var optionWindow = windowedRows(options, selectedIndex);
        var optionList = list(optionWindow.rows())
                .selected(optionWindow.selectedIndex())
                .highlightColor(Color.CYAN)
                .highlightSymbol("> ")
                .rounded()
                .title("Options");

        var content = column(
                text(prompt),
                text("Current file: " + currentVideo.file()).dim(),
                extra == null ? text("") : extra,
                optionList,
                footerLine()
        ).spacing(1);

        return shell(title, content)
                .focusable()
                .onKeyEvent(this::handleChoiceKey);
    }

    private Element renderInputScreen(String title, String prompt, TextInputState state, String fieldTitle) {
        return shell(
                title,
                column(
                        text(prompt),
                        text("Current file: " + currentVideo.file()).dim(),
                        textInput(state)
                                .focusable(false)
                                .showCursor(cursorVisible)
                                .cursorRequiresFocus(false)
                                .title(fieldTitle)
                                .rounded()
                                .focusedBorderColor(Color.CYAN)
                                .onSubmit(this::submitCurrentInput),
                        footerLine()
                ).spacing(1))
                .focusable()
                .onKeyEvent(this::handleInputScreenKey);
    }

    private Element renderImporting() {
        ImportProgress progress = currentImportProgress;
        long copied = progress == null ? 0L : progress.bytesCopied();
        long total = progress == null ? 0L : progress.totalBytes();
        int currentIndex = Math.max(0, currentVideoIndex + 1);
        String source = currentVideo == null ? "" : currentVideo.fileName();
        String destination = currentPlan == null ? "" : currentPlan.destinationFile().toString();

        return shell("Importing", column(
                text(spinnerFrame() + " Moving " + source).cyan().bold(),
                text("File " + currentIndex + " of " + Math.max(queuedImportCount, currentIndex)).dim(),
                text(progressBar(copied, total)).green(),
                text(formatBytes(copied) + " / " + formatBytes(total)).dim(),
                text(destination).overflow(Overflow.WRAP_WORD),
                text("Please wait while the move completes.").dim()
        ).spacing(1));
    }

    private Element renderResults() {
        List<String> rows = new ArrayList<>();
        if (results.isEmpty()) {
            rows.add("No files were processed.");
        } else {
            for (ImportResult result : results) {
                String cleanup = result.sourceDirectoryDeleted() ? " | cleaned source dir" : "";
                rows.add(result.status() + " | " + result.sourceVideo().fileName() + " -> " + result.destinationFile() + cleanup);
            }
        }

        var resultWindow = windowedRows(rows, resultsCursor);
        var resultList = list(resultWindow.rows())
                .selected(resultWindow.selectedIndex())
                .highlightColor(Color.GREEN)
                .highlightSymbol("> ")
                .title("Import Results")
                .rounded()
                .fill();

        var actionList = list(List.of("Quit", "Start Over"))
                .selected(finalActionCursor)
                .highlightColor(Color.CYAN)
                .highlightSymbol("> ")
                .title("Next Action")
                .rounded();

        return shell("Complete", column(
                text("Processed " + results.size() + " video file(s).").green().bold(),
                resultList,
                actionList,
                text("Press Enter to continue or q to quit.").dim()
        ).spacing(1))
                .focusable()
                .onKeyEvent(event -> {
                    if (handleGlobalKey(event).isHandled()) {
                        return EventResult.HANDLED;
                    }
                    if (isUpKey(event) && finalActionCursor > 0) {
                        finalActionCursor--;
                        return EventResult.HANDLED;
                    }
                    if (isDownKey(event) && finalActionCursor < 1) {
                        finalActionCursor++;
                        return EventResult.HANDLED;
                    }
                    if (event.isConfirm()) {
                        if (finalActionCursor == 0) {
                            quit();
                        } else {
                            restartWorkflow();
                        }
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });
    }

    private dev.tamboui.toolkit.elements.Panel shell(String title, Element content) {
        var root = panel(title,
                column(
                        text(infoMessage).gray(),
                        errorMessage.isBlank() ? text("") : text(errorMessage).red().bold(),
                        content
                ).spacing(1)
        ).rounded().borderColor(Color.CYAN);
        root.fill();
        return root;
    }

    private Element footerLine() {
        return text("q quit | / search | enter confirm | gg/G jump").dim();
    }

    private EventResult handleDirectorySelectionKey(KeyEvent event) {
        EventResult global = handleGlobalKey(event);
        if (global.isHandled()) {
            return global;
        }
        List<Integer> visibleIndexes = filteredCandidateIndexes();
        if (directorySearchActive) {
            if (event.isConfirm()) {
                directorySearchActive = false;
                pendingSecondG = false;
                return EventResult.HANDLED;
            }
            if (handleReadlineKey(directorySearchState, event)) {
                clampDirectoryCursor();
                cursorVisible = true;
                clearError();
                return EventResult.HANDLED;
            }
            return EventResult.UNHANDLED;
        }
        if (event.isChar('/')) {
            directorySearchActive = true;
            cursorVisible = true;
            pendingSecondG = false;
            return EventResult.HANDLED;
        }
        if (event.isChar(' ')) {
            if (visibleIndexes.isEmpty()) {
                return EventResult.HANDLED;
            }
            int candidateIndex = visibleIndexes.get(directoryCursor);
            if (selectedDirectoryIndexes.contains(candidateIndex)) {
                selectedDirectoryIndexes.remove(candidateIndex);
            } else {
                selectedDirectoryIndexes.add(candidateIndex);
            }
            clearError();
            return EventResult.HANDLED;
        }
        if (event.isConfirm()) {
            if (selectedDirectoryIndexes.isEmpty()) {
                errorMessage = "Select at least one directory before continuing.";
                return EventResult.HANDLED;
            }
            queueSelectedVideos();
            moveToNextVideo();
            return EventResult.HANDLED;
        }
        if (handleTopBottomMotion(event, visibleIndexes.size(), this::setDirectoryCursor)) {
            return EventResult.HANDLED;
        }
        if (isUpKey(event) && directoryCursor > 0) {
            directoryCursor--;
            return EventResult.HANDLED;
        } else if (isDownKey(event) && directoryCursor < visibleIndexes.size() - 1) {
            directoryCursor++;
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private EventResult handleChoiceKey(KeyEvent event) {
        EventResult global = handleGlobalKey(event);
        if (global.isHandled()) {
            return global;
        }

        int maxIndex = currentOptionsSize() - 1;
        int cursor = currentCursor();
        if (handleTopBottomMotion(event, maxIndex + 1, this::setCurrentCursor)) {
            return EventResult.HANDLED;
        }
        if (isUpKey(event) && cursor > 0) {
            setCurrentCursor(cursor - 1);
            return EventResult.HANDLED;
        }
        if (isDownKey(event) && cursor < maxIndex) {
            setCurrentCursor(cursor + 1);
            return EventResult.HANDLED;
        }
        if (event.isConfirm()) {
            submitCurrentChoice();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private EventResult handleGlobalKey(KeyEvent event) {
        if (screen == Screen.IMPORTING || screen == Screen.LOADING) {
            pendingSecondG = false;
        }
        if (event.isQuit() || event.isCharIgnoreCase('q')) {
            quit();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private EventResult handleInputScreenKey(KeyEvent event) {
        EventResult global = handleGlobalKey(event);
        if (global.isHandled()) {
            return global;
        }

        if (event.isConfirm()) {
            cursorVisible = true;
            submitCurrentInput();
            return EventResult.HANDLED;
        }

        TextInputState state = activeInputState();
        if (state == null) {
            return EventResult.UNHANDLED;
        }

        if (handleReadlineKey(state, event)) {
            cursorVisible = true;
            clearError();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private void submitCurrentInput() {
        clearError();
        switch (screen) {
            case MOVIE_TITLE -> {
                if (movieTitleState.text().trim().isBlank()) {
                    errorMessage = "Movie title is required.";
                    return;
                }
                screen = Screen.MOVIE_YEAR;
            }
            case MOVIE_YEAR -> {
                if (!isMovieYear(movieYearState.text())) {
                    errorMessage = "Movie year must be greater than 1900.";
                    return;
                }
                screen = Screen.MOVIE_STANDARD;
            }
            case MOVIE_EDITION -> {
                if (movieEditionState.text().trim().isBlank()) {
                    errorMessage = "Edition name is required when standard edition is No.";
                    return;
                }
                buildMoviePlan();
            }
            case TV_NEW_SERIES -> {
                if (newSeriesState.text().trim().isBlank()) {
                    errorMessage = "Series name is required.";
                    return;
                }
                screen = Screen.TV_SEASON;
            }
            case TV_SEASON -> {
                if (!isPositiveInteger(seasonState.text())) {
                    errorMessage = "Season number must be a positive integer.";
                    return;
                }
                screen = Screen.TV_EPISODE;
            }
            case TV_EPISODE -> {
                if (!isPositiveInteger(episodeState.text())) {
                    errorMessage = "Episode number must be a positive integer.";
                    return;
                }
                buildTvPlan();
            }
            default -> {
            }
        }
    }

    private void submitCurrentChoice() {
        clearError();
        switch (screen) {
            case CHOOSE_MEDIA_TYPE -> {
                selectedMediaType = mediaTypeCursor == 0 ? MediaType.MOVIE : MediaType.TV_SHOW;
                if (selectedMediaType == MediaType.MOVIE) {
                    screen = Screen.MOVIE_TITLE;
                } else {
                    screen = Screen.TV_SERIES;
                }
            }
            case MOVIE_STANDARD -> {
                if (movieStandardCursor == 0) {
                    buildMoviePlan();
                } else {
                    screen = Screen.MOVIE_EDITION;
                }
            }
            case TV_SERIES -> {
                if (seriesCursor == 0) {
                    screen = Screen.TV_NEW_SERIES;
                } else {
                    screen = Screen.TV_SEASON;
                }
            }
            case CONFIRM_DESTINATION -> {
                if (confirmCursor == 0) {
                    if (Files.exists(currentPlan.destinationFile())) {
                        conflictCursor = 0;
                        screen = Screen.CONFLICT;
                    } else {
                        executeCurrentPlan(false);
                    }
                } else {
                    resetCurrentVideoWorkflow();
                }
            }
            case CONFLICT -> executeCurrentPlan(conflictCursor == 1);
            default -> {
            }
        }
    }

    private void buildMoviePlan() {
        MovieMetadata metadata = new MovieMetadata(
                movieTitleState.text().trim(),
                Integer.parseInt(movieYearState.text().trim()),
                movieStandardCursor == 0,
                movieEditionState.text().trim());
        currentPlan = pathBuilder.buildMoviePlan(currentVideo, metadata);
        confirmCursor = 0;
        screen = Screen.CONFIRM_DESTINATION;
    }

    private void buildTvPlan() {
        String seriesName = seriesCursor == 0
                ? newSeriesState.text().trim()
                : existingSeries.get(seriesCursor - 1);
        TvMetadata metadata = new TvMetadata(
                seriesName,
                Integer.parseInt(seasonState.text().trim()),
                Integer.parseInt(episodeState.text().trim()));
        currentPlan = pathBuilder.buildTvPlan(currentVideo, metadata);
        confirmCursor = 0;
        screen = Screen.CONFIRM_DESTINATION;
    }

    private void executeCurrentPlan(boolean overwrite) {
        currentImportProgress = new ImportProgress(currentVideo.file(), currentPlan.destinationFile(), 0L, 0L);
        screen = Screen.IMPORTING;
        Thread thread = new Thread(() -> {
            ImportResult result = executor.execute(currentPlan, overwrite, progress ->
                    onUiThread(() -> currentImportProgress = progress));
            onUiThread(() -> {
                currentImportProgress = null;
                results.add(result);
                moveToNextVideo();
            });
        }, "jpi-import-move");
        thread.setDaemon(true);
        thread.start();
    }

    private void moveToNextVideo() {
        currentVideoIndex++;
        if (currentVideoIndex >= queuedVideos.size()) {
            screen = Screen.RESULTS;
            return;
        }
        currentVideo = queuedVideos.get(currentVideoIndex);
        resetCurrentVideoWorkflow();
    }

    private void resetCurrentVideoWorkflow() {
        selectedMediaType = MediaType.MOVIE;
        mediaTypeCursor = 0;
        movieTitleState.clear();
        movieYearState.clear();
        movieStandardCursor = 0;
        movieEditionState.clear();
        directorySearchActive = false;
        seriesCursor = 0;
        newSeriesState.clear();
        seasonState.clear();
        episodeState.clear();
        currentPlan = null;
        conflictCursor = 0;
        confirmCursor = 0;
        clearError();
        screen = Screen.CHOOSE_MEDIA_TYPE;
    }

    private void queueSelectedVideos() {
        queuedVideos.clear();
        results.clear();
        currentVideoIndex = -1;
        queuedImportCount = 0;
        selectedDirectoryIndexes.stream()
                .sorted()
                .map(candidateDirectories::get)
                .forEach(candidate -> queuedVideos.addAll(candidate.videos()));
        queuedImportCount = queuedVideos.size();
    }

    private List<String> seriesOptions() {
        List<String> options = new ArrayList<>();
        options.add(NEW_SERIES);
        options.addAll(existingSeries);
        return options;
    }

    private boolean isPositiveInteger(String value) {
        try {
            return Integer.parseInt(value.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isMovieYear(String value) {
        try {
            return Integer.parseInt(value.trim()) > 1900;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void clearError() {
        errorMessage = "";
    }

    private boolean isUpKey(KeyEvent event) {
        return event.code() == KeyCode.UP
                || (event.code() == KeyCode.CHAR
                && !event.hasCtrl()
                && !event.hasAlt()
                && event.isCharIgnoreCase('k'));
    }

    private boolean isDownKey(KeyEvent event) {
        return event.code() == KeyCode.DOWN
                || (event.code() == KeyCode.CHAR
                && !event.hasCtrl()
                && !event.hasAlt()
                && event.isCharIgnoreCase('j'));
    }

    private void restartWorkflow() {
        beginLibraryScan();
    }

    private boolean isInputScreen() {
        return switch (screen) {
            case MOVIE_TITLE, MOVIE_YEAR, MOVIE_EDITION, TV_NEW_SERIES, TV_SEASON, TV_EPISODE -> true;
            default -> false;
        };
    }

    private TextInputState activeInputState() {
        return switch (screen) {
            case MOVIE_TITLE -> movieTitleState;
            case MOVIE_YEAR -> movieYearState;
            case MOVIE_EDITION -> movieEditionState;
            case TV_NEW_SERIES -> newSeriesState;
            case TV_SEASON -> seasonState;
            case TV_EPISODE -> episodeState;
            default -> null;
        };
    }

    private boolean handleReadlineKey(TextInputState state, KeyEvent event) {
        if (event.code() == KeyCode.BACKSPACE) {
            state.deleteBackward();
            return true;
        }
        if (event.code() == KeyCode.DELETE) {
            state.deleteForward();
            return true;
        }
        if (event.code() == KeyCode.LEFT) {
            state.moveCursorLeft();
            return true;
        }
        if (event.code() == KeyCode.RIGHT) {
            state.moveCursorRight();
            return true;
        }
        if (event.code() == KeyCode.HOME) {
            state.moveCursorToStart();
            return true;
        }
        if (event.code() == KeyCode.END) {
            state.moveCursorToEnd();
            return true;
        }
        if (event.hasCtrl() && event.isCharIgnoreCase('a')) {
            state.moveCursorToStart();
            return true;
        }
        if (event.hasCtrl() && event.isCharIgnoreCase('e')) {
            state.moveCursorToEnd();
            return true;
        }
        if (event.hasCtrl() && event.isCharIgnoreCase('b')) {
            state.moveCursorLeft();
            return true;
        }
        if (event.hasCtrl() && event.isCharIgnoreCase('f')) {
            state.moveCursorRight();
            return true;
        }
        if (event.hasCtrl() && event.isCharIgnoreCase('d')) {
            state.deleteForward();
            return true;
        }
        if (event.hasCtrl() && event.isCharIgnoreCase('h')) {
            state.deleteBackward();
            return true;
        }
        if (event.hasCtrl() && event.isCharIgnoreCase('k')) {
            killToEndOfLine(state);
            return true;
        }
        if (event.hasCtrl() && event.isCharIgnoreCase('y')) {
            if (!killBuffer.isEmpty()) {
                state.insert(killBuffer);
            }
            return true;
        }
        if (event.code() == KeyCode.CHAR && !event.hasCtrl() && !event.hasAlt()) {
            char c = event.character();
            if (c >= 32 && c < 127) {
                state.insert(c);
                return true;
            }
        }
        return false;
    }

    private void killToEndOfLine(TextInputState state) {
        String text = state.text();
        int cursor = state.cursorPosition();
        if (cursor >= text.length()) {
            killBuffer = "";
            return;
        }
        killBuffer = text.substring(cursor);
        state.setText(text.substring(0, cursor));
    }

    private int currentOptionsSize() {
        return switch (screen) {
            case CHOOSE_MEDIA_TYPE -> 2;
            case MOVIE_STANDARD -> 2;
            case TV_SERIES -> seriesOptions().size();
            case CONFIRM_DESTINATION -> 2;
            case CONFLICT -> 2;
            default -> 0;
        };
    }

    private int currentCursor() {
        return switch (screen) {
            case CHOOSE_MEDIA_TYPE -> mediaTypeCursor;
            case MOVIE_STANDARD -> movieStandardCursor;
            case TV_SERIES -> seriesCursor;
            case CONFIRM_DESTINATION -> confirmCursor;
            case CONFLICT -> conflictCursor;
            default -> 0;
        };
    }

    private void setCurrentCursor(int value) {
        switch (screen) {
            case CHOOSE_MEDIA_TYPE -> mediaTypeCursor = value;
            case MOVIE_STANDARD -> movieStandardCursor = value;
            case TV_SERIES -> seriesCursor = value;
            case CONFIRM_DESTINATION -> confirmCursor = value;
            case CONFLICT -> conflictCursor = value;
            default -> {
            }
        }
    }

    private void setDirectoryCursor(int value) {
        directoryCursor = value;
    }

    private List<Integer> filteredCandidateIndexes() {
        List<Integer> indexes = new ArrayList<>();
        String query = directorySearchState.text().trim().toLowerCase();
        for (int i = 0; i < candidateDirectories.size(); i++) {
            CandidateDirectory candidate = candidateDirectories.get(i);
            String displayName = candidate.displayName(config.sourceRoot()).toLowerCase();
            if (query.isEmpty() || displayName.contains(query)) {
                indexes.add(i);
            }
        }
        if (directoryCursor >= indexes.size()) {
            directoryCursor = Math.max(0, indexes.size() - 1);
        }
        return indexes;
    }

    private void clampDirectoryCursor() {
        int size = filteredCandidateIndexes().size();
        if (directoryCursor >= size) {
            directoryCursor = Math.max(0, size - 1);
        }
    }

    private void resetDirectorySearch() {
        directorySearchState.clear();
        directorySearchActive = false;
        directoryCursor = 0;
    }

    private boolean handleTopBottomMotion(KeyEvent event, int size, Consumer<Integer> setter) {
        if (size <= 0) {
            pendingSecondG = false;
            return false;
        }
        if (event.code() == KeyCode.CHAR && !event.hasCtrl() && !event.hasAlt()) {
            if (event.character() == 'G') {
                pendingSecondG = false;
                setter.accept(size - 1);
                return true;
            }
            if (event.character() == 'g') {
                if (pendingSecondG) {
                    pendingSecondG = false;
                    setter.accept(0);
                    return true;
                }
                pendingSecondG = true;
                return true;
            }
        }
        pendingSecondG = false;
        return false;
    }

    private WindowedRows windowedRows(List<String> rows, int selectedIndex) {
        if (rows.isEmpty()) {
            return new WindowedRows(List.of(), 0);
        }
        int clampedSelected = Math.max(0, Math.min(selectedIndex, rows.size() - 1));
        int visibleCount = Math.min(MAX_VISIBLE_ROWS, rows.size());
        int start = Math.max(0, clampedSelected - (visibleCount / 2));
        if (start + visibleCount > rows.size()) {
            start = rows.size() - visibleCount;
        }
        List<String> visibleRows = rows.subList(start, start + visibleCount);
        return new WindowedRows(visibleRows, clampedSelected - start);
    }

    private String spinnerFrame() {
        return SPINNER_FRAMES.get(spinnerFrameIndex);
    }

    private String progressBar(long copied, long total) {
        int width = 30;
        if (total <= 0L) {
            return "[" + "=".repeat(width / 3) + " ".repeat(width - (width / 3)) + "]";
        }
        int filled = (int) Math.round((Math.min(copied, total) / (double) total) * width);
        filled = Math.max(0, Math.min(width, filled));
        return "[" + "=".repeat(filled) + " ".repeat(width - filled) + "]";
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024.0) {
            return String.format("%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024.0) {
            return String.format("%.1f MB", mb);
        }
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }

    private record WindowedRows(List<String> rows, int selectedIndex) {
    }

    private enum Screen {
        LOADING,
        EMPTY,
        ERROR,
        SELECT_DIRECTORIES,
        CHOOSE_MEDIA_TYPE,
        MOVIE_TITLE,
        MOVIE_YEAR,
        MOVIE_STANDARD,
        MOVIE_EDITION,
        TV_SERIES,
        TV_NEW_SERIES,
        TV_SEASON,
        TV_EPISODE,
        CONFIRM_DESTINATION,
        CONFLICT,
        IMPORTING,
        RESULTS
    }
}
