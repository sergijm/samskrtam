package sm.selflearn.samskrtam.curriculum.paradigm;

import sm.selflearn.samskrtam.content.model.VowelType;

import java.util.Collections;
import java.util.List;

/**
 * Maps a v2 curriculum topic code (== lesson slug) to the suppletive pronoun
 * vowel-types whose paradigms are served by {@code ParadigmService}. Grammar
 * topics without a suppletive paradigm (regular nouns, verbs, …) resolve to an
 * empty list — their paradigms are composed on the fly from the quest_item pool
 * (see DeclensionQuestItemBatchGenerator), not stored here.
 *
 * <p>Closed under the existing {@code slug} contract: whoever calls the v2
 * declension-paradigm page passes the topic code as the lesson slug.
 */
public final class ParadigmTopicCodeToVowelMapper {

    private ParadigmTopicCodeToVowelMapper() {
    }

    public static List<VowelType> mapTopicCodeToVowelTypes(String topicCode) {
        if (topicCode == null) {
            return Collections.emptyList();
        }
        return switch (topicCode) {
            case "personal-pronouns" ->
                    List.of(VowelType.PRON_AHAM, VowelType.PRON_TVAM);
            case "pronoun-stems-declension", "demonstrative-pronouns" ->
                    List.of(VowelType.PRON_TAD, VowelType.PRON_ETAD, VowelType.PRON_IDAM);
            case "interrogative-pronouns" -> List.of(VowelType.PRON_KIM);
            case "relative-pronouns" -> List.of(VowelType.PRON_YAD);
            default -> Collections.emptyList();
        };
    }
}