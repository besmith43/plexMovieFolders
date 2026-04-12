package com.example.pleximporter.service;

import java.nio.file.Path;

public record ImportPreview(
        Path sourceDirectory,
        Path sourceFile,
        Path destinationPath,
        boolean collision,
        String message
) {
}
