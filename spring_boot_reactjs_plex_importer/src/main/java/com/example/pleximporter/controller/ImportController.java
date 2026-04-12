package com.example.pleximporter.controller;

import com.example.pleximporter.dto.ImportRequest;
import com.example.pleximporter.dto.ImportResponse;
import com.example.pleximporter.dto.PreviewRequest;
import com.example.pleximporter.dto.PreviewResponse;
import com.example.pleximporter.dto.SourceDirectoryDto;
import com.example.pleximporter.service.ImportEventStreamService;
import com.example.pleximporter.service.FileSystemImportService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ImportController {

    private final FileSystemImportService importService;
    private final ImportEventStreamService importEventStreamService;

    public ImportController(FileSystemImportService importService, ImportEventStreamService importEventStreamService) {
        this.importService = importService;
        this.importEventStreamService = importEventStreamService;
    }

    @GetMapping("/directories")
    public List<SourceDirectoryDto> directories() {
        return importService.listSourceDirectories();
    }

    @GetMapping("/tv-series")
    public List<String> tvSeries() {
        return importService.listExistingSeries();
    }

    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events() {
        return importEventStreamService.subscribe();
    }

    @PostMapping("/preview")
    public PreviewResponse preview(@Valid @RequestBody PreviewRequest request) {
        return importService.preview(request);
    }

    @PostMapping("/import")
    public ImportResponse executeImport(@Valid @RequestBody ImportRequest request) {
        return importService.executeImport(request);
    }
}
