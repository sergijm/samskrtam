package sm.selflearn.samskrtam.curriculum.lexicon.dto;

/**
 * A ready-made "Quick start" workout preset shown at the top of the page.
 * titleRu/titleEn and metaRu/metaEn (duration/hint) are localised so the UI
 * renders them directly.
 */
public record LexiconQuickStartDto(
        String id,
        String titleRu,
        String titleEn,
        String metaRu,
        String metaEn
) {
}