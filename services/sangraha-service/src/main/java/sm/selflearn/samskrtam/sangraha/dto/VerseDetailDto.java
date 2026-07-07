package sm.selflearn.samskrtam.sangraha.dto;

import sm.selflearn.samskrtam.sangraha.model.VerseStatus;

import java.util.List;
import java.util.UUID;

public record VerseDetailDto(
    UUID id,
    UUID chapterId,
    int orderIndex,
    String textDevanagari,
    String textIast,
    String rawText,
    VerseStatus status,
    VerseAnalysisDto analysis,
    List<VerseWordDto> words
) {}