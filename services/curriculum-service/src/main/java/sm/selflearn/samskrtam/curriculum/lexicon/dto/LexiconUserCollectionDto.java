package sm.selflearn.samskrtam.curriculum.lexicon.dto;

/**
 * A user collection in "My lists". name is the user-supplied (not localised)
 * collection title; wordCount is the number of lexemes it holds.
 */
public record LexiconUserCollectionDto(
        String id,
        String name,
        int wordCount
) {
}