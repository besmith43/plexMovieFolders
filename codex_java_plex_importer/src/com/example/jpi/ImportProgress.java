package com.example.jpi;

import java.nio.file.Path;

public record ImportProgress(
        Path sourceFile,
        Path destinationFile,
        long bytesCopied,
        long totalBytes) {
}
