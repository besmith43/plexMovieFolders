package com.example.pleximporter.dto;

public record ImportNotification(
        String status,
        String destinationPath,
        boolean sourceDirectoryDeleted,
        String message
) {
    public static ImportNotification from(ImportResponse response) {
        return new ImportNotification(
                response.status(),
                response.destinationPath(),
                response.sourceDirectoryDeleted(),
                response.message()
        );
    }
}
