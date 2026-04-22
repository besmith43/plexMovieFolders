package com.example.pleximporter.controller;

import com.example.pleximporter.dto.ImportRequest;
import com.example.pleximporter.dto.ImportResponse;
import com.example.pleximporter.dto.PreviewRequest;
import com.example.pleximporter.dto.PreviewResponse;
import com.example.pleximporter.dto.SourceDirectoryDto;
import com.example.pleximporter.service.FileSystemImportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ImportController {

    private final FileSystemImportService importService;

    public ImportController(FileSystemImportService importService) {
        this.importService = importService;
    }

    @GetMapping("/directories")
    public List<SourceDirectoryDto> directories() {
        return importService.listSourceDirectories();
    }

    @GetMapping("/tv-series")
    public List<String> tvSeries() {
        return importService.listExistingSeries();
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
