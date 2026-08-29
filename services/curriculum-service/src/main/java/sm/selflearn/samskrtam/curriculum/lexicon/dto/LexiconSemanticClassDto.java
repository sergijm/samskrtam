package sm.selflearn.samskrtam.curriculum.lexicon.dto;

/**
 * A semantic group card on the home page "Topics" section. id is the stable
 * code; localised labels are carried on the node (ru + en) so the UI renders
 * them directly by current locale instead of hard-coded i18n keys — the
 * taxonomy is too large to enumerate into lexicon.json. wordCount includes the
 * whole subtree; masteredCount is the per-user mastered subset.
 */
public record LexiconSemanticClassDto(
        String id,
        String nameRu,
        String nameEn,
        int wordCount,
        int masteredCount
) {
}