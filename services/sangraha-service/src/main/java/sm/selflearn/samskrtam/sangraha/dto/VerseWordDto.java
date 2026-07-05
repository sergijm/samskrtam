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
    String gender,
    String caseType,
    String numberType,
    String person,
    String tense,
    String mood,
    String voice,
    String glossRu,
    String glossEn,
    List<Integer> formationRuleNumbers
) {}