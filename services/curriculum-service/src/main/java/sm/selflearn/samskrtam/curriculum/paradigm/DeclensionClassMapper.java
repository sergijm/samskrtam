package sm.selflearn.samskrtam.curriculum.paradigm;

import sm.selflearn.samskrtam.content.model.VowelType;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Maps curriculum topics to the declension classes ({@link VowelType}) their
 * paradigms are stored under in {@code curriculum.declension_form}.
 *
 * <p>A topic (lesson slug) may cover <b>several</b> declension classes — the
 * merged {@code a-stem} lesson covers {@code a-stem-masc}/{@code a-stem-neut},
 * {@code i-u-stems} covers {@code i-stem}/{@code u-stem} — so the relation
 * {@code topic → classCodes} is 1:N while {@code classCode → VowelType} is 1:1.
 * Noun/adjective topics are expressed through their {@code morphology_class}
 * codes; pronoun topics map directly to their {@code PRON_*} vowel types since
 * their paradigms live in {@code declension_form} keyed by
 * {@code (lemma_iast, vowel_type)} with no separate morphology-class binding.
 *
 * <p>Shared by {@link ParadigmService}, the batch generator and the
 * {@code DeclensionDataImporter}.
 */
public final class DeclensionClassMapper {

    /** Topic slug → the morphology classes the topic's declension is built from. */
    private static final Map<String, List<String>> TOPIC_CLASS_CODES = Map.ofEntries(
            Map.entry("a-stem", List.of("a-stem-masc", "a-stem-neut")), // merged lesson (V10)
            Map.entry("a-stem-fem", List.of("a-stem-fem")),
            Map.entry("i-u-stems", List.of("i-stem", "u-stem")),
            Map.entry("r-stems", List.of("r-stem")),
            // consonant finals and one-syllable stems, grouped into lessons on shared
            // behaviour (see docs/services/curriculum.md §2):
            Map.entry("n-stems", List.of("in-stem", "an-stem")),
            Map.entry("s-stems", List.of("as-stem", "is-stem", "us-stem")),
            Map.entry("at-stems", List.of("ant-stem", "vat-stem")),
            Map.entry("root-stems", List.of("root-stem", "o-stem", "au-stem")),
            // long-vowel feminine stems (-ī/-ū), parallel to i/u-stems:
            Map.entry("ii-uu-stems", List.of("ii-stem", "uu-stem"))
    );

    /**
     * Pronoun topics map straight to their {@code PRON_*} vowel types (no
     * morphology-class codes), per ADR-008. Bases are split by gender
     * (e.g. {@code PRON_TAD_MASC/NEUT/FEM}); the lemma's stored paradigm cells
     * select which of these the paradigm page serves.
     */
    private static final Map<String, List<VowelType>> TOPIC_VOWEL_TYPES = Map.ofEntries(
            // personal-pronouns: 1st/2nd person (asmad/yuṣmad) are epicene — a single,
            // non-gender-split category each, like PRON_AN / PRON_KATI.
            Map.entry("personal-pronouns", List.of(VowelType.PRON_ASMAD, VowelType.PRON_YUSMAD)),
            Map.entry("demonstrative-pronouns", List.of(
                    VowelType.PRON_TAD_MASC, VowelType.PRON_TAD_NEUT, VowelType.PRON_TAD_FEM,
                    VowelType.PRON_IDAM_MASC, VowelType.PRON_IDAM_NEUT, VowelType.PRON_IDAM_FEM,
                    VowelType.PRON_ADAS_MASC, VowelType.PRON_ADAS_NEUT, VowelType.PRON_ADAS_FEM)),
            Map.entry("interrogative-pronouns", List.of(
                    VowelType.PRON_TAD_MASC, VowelType.PRON_TAD_NEUT, VowelType.PRON_TAD_FEM)),
            Map.entry("relative-pronouns", List.of(
                    VowelType.PRON_TAD_MASC, VowelType.PRON_TAD_NEUT, VowelType.PRON_TAD_FEM)),
            Map.entry("indefinite-pronouns", List.of(
                    VowelType.PRON_TAD_MASC, VowelType.PRON_TAD_NEUT, VowelType.PRON_TAD_FEM)),
            Map.entry("reflexive-possessive-pronouns", List.of(
                    VowelType.PRON_AN, VowelType.PRON_VAT_MASC, VowelType.PRON_VAT_FEM,
                    VowelType.A_STEM, VowelType.AA_STEM)),
            Map.entry("pronominal-adjectives", List.of(
                    VowelType.PRON_SARVA_MASC, VowelType.PRON_SARVA_NEUT, VowelType.PRON_SARVA_FEM,
                    VowelType.PRON_PURVA_MASC, VowelType.PRON_PURVA_NEUT, VowelType.PRON_PURVA_FEM)),
            Map.entry("quantifier-pronouns", List.of(VowelType.PRON_UBHA_MASC, VowelType.PRON_UBHA_FN))
    );

    private DeclensionClassMapper() {
    }

    /**
     * The morphology classes a topic's declension lesson is built from. Falls
     * back to the topic code itself when it already is a morphology class code
     * (e.g. {@code a-stem-masc}, {@code i-stem}).
     */
    public static List<String> topicToClassCodes(String topicCode) {
        if (topicCode == null) {
            return List.of();
        }
        List<String> classCodes = TOPIC_CLASS_CODES.get(topicCode);
        if (classCodes != null) {
            return classCodes;
        }
        return toVowelType(topicCode) != null ? List.of(topicCode) : List.of();
    }

    /** The declension classes (vowel types) a topic's paradigm page serves. */
    public static List<VowelType> topicToVowelTypes(String topicCode) {
        if (topicCode == null) {
            return List.of();
        }
        List<VowelType> direct = TOPIC_VOWEL_TYPES.get(topicCode);
        if (direct != null) {
            return direct;
        }
        return topicToClassCodes(topicCode).stream()
                .map(DeclensionClassMapper::toVowelType)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /** Whether the topic serves stored paradigms of regular noun classes. */
    public static boolean isRegularDeclensionTopic(String topicCode) {
        return !topicToVowelTypes(topicCode).isEmpty();
    }

    /** {@code null} when the code is not a regular noun declension class. */
    public static VowelType toVowelType(String classCode) {
        return switch (classCode) {
            case "a-stem-masc", "a-stem-neut", "a-stem" -> VowelType.A_STEM;
            case "a-stem-fem" -> VowelType.AA_STEM;
            case "i-stem" -> VowelType.I_STEM;
            case "u-stem" -> VowelType.U_STEM;
            case "r-stem" -> VowelType.R_STEM;
            case "in-stem" -> VowelType.IN_STEM;
            case "an-stem" -> VowelType.AN_STEM;
            case "as-stem" -> VowelType.AS_STEM;
            case "ant-stem" -> VowelType.ANT_STEM;
            case "vat-stem" -> VowelType.VAT_STEM;
            case "root-stem" -> VowelType.ROOT_STEM;
            case "o-stem" -> VowelType.O_STEM;
            case "au-stem" -> VowelType.AU_STEM;
            case "is-stem" -> VowelType.IS_STEM;
            case "us-stem" -> VowelType.US_STEM;
            case "ii-stem" -> VowelType.II_STEM;
            case "uu-stem" -> VowelType.UU_STEM;
            default -> null;
        };
    }

    public static boolean isRegularDeclensionClass(String classCode) {
        return toVowelType(classCode) != null;
    }
}
