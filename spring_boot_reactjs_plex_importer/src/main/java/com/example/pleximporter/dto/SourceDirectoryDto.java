package com.example.pleximporter.dto;

import java.util.List;

public record SourceDirectoryDto(
        String name,
        String absolutePath,
        List<VideoFileDto> videoFiles
) {
}
