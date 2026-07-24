package sm.selflearn.samskrtam.sangraha.dto;

import sm.selflearn.samskrtam.sangraha.model.VerseStatus;

import java.util.UUID;

public record VerseTreeDto(
    UUID id,
    int orderIndex,
    String textIastPreview,
    String textIast,
    String textDevanagari,
    String translationRu,
    String translationEn,
    VerseStatus status
) {}