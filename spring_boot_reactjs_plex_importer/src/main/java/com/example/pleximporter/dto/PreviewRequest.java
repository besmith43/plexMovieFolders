package com.example.pleximporter.dto;

import com.example.pleximporter.model.ContentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PreviewRequest(
        @NotBlank String sourceDirectory,
        @NotBlank String sourceFile,
        @NotNull ContentType contentType,
        String title,
        Integer year,
        Boolean standardEdition,
        String edition,
        String existingSeries,
        String newSeriesName,
        @Min(1) Integer seasonNumber,
        @Min(1) Integer episodeNumber
) {
}
