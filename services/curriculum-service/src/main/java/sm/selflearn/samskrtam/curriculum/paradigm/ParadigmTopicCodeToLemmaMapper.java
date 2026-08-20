package sm.selflearn.samskrtam.curriculum.paradigm;

import sm.selflearn.samskrtam.content.model.VowelType;

import java.util.Collections;
import java.util.List;

/**
 * Maps a v2 curriculum topic code (== lesson slug) to the {@link ParadigmRef}s of
 * the suppletive pronoun paradigms served by {@link ParadigmService}. Pronouns are
 * lexemes like any other; their paradigms live in {@code curriculum.declension_form},
 * keyed by {@code (lemma_iast, vowel_type)} with the {@code PRON_*} class.
 *
 * <p>The correspondence is the fixed {@code vowelType → lemmaIast} mapping used for
 * {@code PRON_*} classes (see sangraha-service.md): {@code PRON_AHAM → asmad},
 * {@code PRON_TVAM → yuṣmad}, {@code PRON_TAD → tad}, etc.
 */
public final class ParadigmTopicCodeToLemmaMapper {

    /** One suppletive paradigm: the lemma and its {@code PRON_*} declension class. */
    public record ParadigmRef(String lemmaIast, VowelType vowelType) {
    }

    private ParadigmTopicCodeToLemmaMapper() {
    }

    public static List<ParadigmRef> mapTopicCodeToParadigms(String topicCode) {
        if (topicCode == null) {
            return Collections.emptyList();
        }
        return switch (topicCode) {
            case "personal-pronouns" -> List.of(
                    new ParadigmRef("asmad", VowelType.PRON_AHAM),
                    new ParadigmRef("yuṣmad", VowelType.PRON_TVAM));
            case "pronoun-stems-declension", "demonstrative-pronouns" -> List.of(
                    new ParadigmRef("tad", VowelType.PRON_TAD),
                    new ParadigmRef("etad", VowelType.PRON_ETAD),
                    new ParadigmRef("idam", VowelType.PRON_IDAM));
            case "interrogative-pronouns" -> List.of(
                    new ParadigmRef("kim", VowelType.PRON_KIM));
            case "relative-pronouns" -> List.of(
                    new ParadigmRef("yad", VowelType.PRON_YAD));
            default -> Collections.emptyList();
        };
    }
}
