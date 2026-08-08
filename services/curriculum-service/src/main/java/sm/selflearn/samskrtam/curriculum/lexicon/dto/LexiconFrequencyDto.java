package sm.selflearn.samskrtam.curriculum.lexicon.dto;

/**
 * A frequency band on the lexicon home page. {code} is the stable id (CORE,
 * ESSENTIAL, ...); from/to are the inclusive frequency ranks. wordCount is the
 * size of the curated band slice; masteredCount is per-user progress (random).
 */
public record LexiconFrequencyDto(
        String id,
        int from,
        int to,
        int wordCount,
        int masteredCount
) {
}