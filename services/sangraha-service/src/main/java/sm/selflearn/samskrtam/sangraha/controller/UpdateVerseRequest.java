package sm.selflearn.samskrtam.sangraha.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO для PUT /verses/{verseId} — обновление orderIndex и rawText.
 */
public record UpdateVerseRequest(
    @NotNull
    @Min(0)
    int orderIndex,

    String rawText
) {}
