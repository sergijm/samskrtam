package sm.selflearn.samskrtam.quest;

/**
 * A word to highlight inside a bilingual question prompt. {@code text} matches
 * the English prompt, {@code textRu} the Russian one (both usually equal for
 * Sanskrit words). The frontend splits the prompt on these tokens and renders
 * matches as bold.
 */
public record HighlightToken(
        String text,
        String textRu
) {
}
