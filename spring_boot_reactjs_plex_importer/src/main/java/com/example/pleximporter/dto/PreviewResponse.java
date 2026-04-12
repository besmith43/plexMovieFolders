package com.example.pleximporter.dto;

public record PreviewResponse(
        String sourceFile,
        String destinationPath,
        boolean collision,
        String message
) {
}
