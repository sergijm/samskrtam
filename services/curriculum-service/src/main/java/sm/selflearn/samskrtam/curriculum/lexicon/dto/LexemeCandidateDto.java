package sm.selflearn.samskrtam.curriculum.lexicon.dto;

import java.util.List;
import java.util.UUID;

/**
 * Одна лексема в ответе pool/resolve (task-curriculum-15 §7): лемма, глоссы,
 * pos, gender и до 3 wordForms, если они есть.
 */
public record LexemeCandidateDto(
        UUID id,
        String lemmaIast,
        String lemmaDevanagari,
        String lemmaSlp1,
        String glossRu,
        String glossEn,
        String gender,
        String posCode,
        List<String> morphologyClassCodes,
        List<WordFormDto> wordForms
) {
    public record WordFormDto(
            String formIast,
            String formDevanagari,
            String grammaticalNote
    ) {
    }
}
