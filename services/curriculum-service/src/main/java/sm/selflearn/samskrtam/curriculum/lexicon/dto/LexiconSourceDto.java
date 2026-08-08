package sm.selflearn.samskrtam.curriculum.lexicon.dto;

/**
 * A "vocabulary by text" card — unique words across a Source. titleRu/titleEn
 * are the source titles; devanagari is an optional native-script short title.
 * wordCount is the source's unique-lemma count; masteredCount is per-user
 * progress (random).
 */
public record LexiconSourceDto(
        String id,
        String titleEn,
        String titleRu,
        String devanagari,
        int wordCount,
        int masteredCount
) {
}