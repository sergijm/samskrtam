package sm.selflearn.samskrtam.sangraha.dto;

import java.util.UUID;
import java.util.List;

public record VerseWordDto(
    UUID id,
    int position,
    String surfaceIast,
    String surfaceDevanagari,
    String lemmaIast,
    String stem,
    String root,
    String pos,
    String formType,
    Boolean isFinite,
    MorphologyDto morphology,
    DerivationDto derivation,
    String lemmaGlossRu,
    String lemmaGlossEn,
    String contextGlossRu,
    String contextGlossEn,
    List<Integer> formationRuleNumbers,
    String analysisConfidence,
    String ambiguityNotes,
    UUID vocabularyWordId
) {
    public record MorphologyDto(
        String caseType,
        String gender,
        String numberType,
        String person,
        String tense,
        String mood,
        String voice
    ) {}

    public record DerivationDto(
        String derivationType,
        String derivationalSuffix,
        String derivationalBase,
        String description
    ) {}
}
