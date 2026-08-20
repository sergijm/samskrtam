package sm.selflearn.samskrtam.curriculum.lexicon.dto;

/**
 * Headline progress for the lexicon home page — mastered words out of the total
 * lexeme count in the schema. Continent for the `LexiconHero` card.
 */
public record LexiconSummaryDto(
        int totalWords,
        int masteredCount
) {
}