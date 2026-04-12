package com.example.pleximporter.dto;

public record ImportResponse(
        String status,
        String destinationPath,
        boolean sourceDirectoryDeleted,
        String message
) {
}
