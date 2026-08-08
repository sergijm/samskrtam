package sm.selflearn.samskrtam.curriculum.lexicon.dto;

/**
 * A chip in the "Parts of speech" section. id is the stable part-of-speech
 * code; localised labels are returned on the node (ru + en).
 */
public record LexiconPosDto(
        String id,
        String nameRu,
        String nameEn,
        int wordCount
) {
}