package com.example.pleximporter.dto;

import com.example.pleximporter.model.ConflictAction;
import jakarta.validation.constraints.NotNull;

public record ImportRequest(
        @NotNull PreviewRequest preview,
        ConflictAction conflictAction
) {
}
