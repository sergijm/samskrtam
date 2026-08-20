package sm.selflearn.samskrtam.curriculum.mapper;

import sm.selflearn.samskrtam.curriculum.dto.TopicTypeGroup;

import java.util.Locale;

/**
 * Helpers deriving UI attributes of a learning-map card from a topic code.
 * Kept out of the MapStruct mapper so its methods are not auto-detected as
 * implicit property conversions.
 */
final class LearnTopicDeriver {

    private LearnTopicDeriver() {
    }

    static TopicTypeGroup classifyTypeGroup(String code) {
        if (code == null) {
            return TopicTypeGroup.OTHER;
        }
        String c = code.toLowerCase(Locale.ROOT);
        if (c.contains("sandhi")) {
            return TopicTypeGroup.SANDHI;
        }
        if (c.contains("vocabulary") || c.contains("lexicon") || c.contains("function-word")
                || c.contains("word") && (c.contains("basic") || c.contains("svo"))) {
            return TopicTypeGroup.VOCABULARY;
        }
        if (containsAny(c, "stem", "declension", "case", "pronoun", "numeral", "agreement")) {
            return TopicTypeGroup.DECLENSION;
        }
        if (containsAny(c, "verb", "present", "indicativus", "imperfect", "future", "perfect",
                "aorist", "imperative", "imperativus", "optative", "optativus", "participle", "absolutive",
                "root-class", "conjugation")) {
            return TopicTypeGroup.CONJUGATION;
        }
        if (containsAny(c, "sentence", "construction", "clause", "compound", "karaka", "relative",
                "correlative", "reported", "conditional", "word-order", "ellipsis", "syntactic", "analysis")) {
            return TopicTypeGroup.SYNTAX;
        }
        return TopicTypeGroup.OTHER;
    }

    static String resolveRoute(String code) {
        return code == null ? null : "/lessons/grammar/" + code;
    }

    private static boolean containsAny(String code, String... keywords) {
        for (String keyword : keywords) {
            if (code.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}