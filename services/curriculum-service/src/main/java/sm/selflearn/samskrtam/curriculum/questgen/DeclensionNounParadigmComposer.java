package sm.selflearn.samskrtam.curriculum.questgen;

import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.questgen.morphology.CaseType;
import sm.selflearn.samskrtam.curriculum.questgen.morphology.NumberType;
import sm.selflearn.samskrtam.curriculum.questgen.morphology.VowelType;

import java.util.ArrayList;
import java.util.List;

/**
 * Composes the full declension paradigm of a regular noun lexeme for a given
 * declension class, sharing the exact morphology-class binding and gender
 * resolution used by {@link DeclensionQuestItemBatchGenerator}. Kept in the
 * same package so it can reach the package-private {@link WordFormBuilder} and
 * {@link DeclensionParadigm}. Used by the v2 paradigm page for regular (non
 * suppletive) noun classes, mirroring one composed paradigm per lexeme.
 *
 * <p>A &#39;regular&#39; topic is one whose {@code Topic.code} doubles as a
 * {@code morphology_class.code} (e.g. {@code a-stem-masc}) — the very contract
 * the batch generator lives on (see curriculum-quest-items.md §4).
 */
public final class DeclensionNounParadigmComposer {

    private DeclensionNounParadigmComposer() {
    }

    /** A composed, script-localized word form (IAST + Devanagari). */
    public record Form(String iast, String devanagari) {
    }

    /** One composed paradigm cell: (case, number) plus the word form. */
    public record Cell(CaseType caseType, NumberType numberType, Form form) {
    }

    /**
     * Returns the full paradigm of a noun of the given declension class, or an
     * empty list when the class code is unknown. {@code lexemeGender} is the
     * lexeme's stored gender; it matters for the i/u/ṛ-stems which resolve their
     * endings by gender.
     */
    public static List<Cell> compose(String classCode, String lemmaIast, String lemmaDevanagari,
                                     LexemeGender lexemeGender) {
        VowelType vowel = vowelTypeOf(classCode);
        if (vowel == null) {
            return List.of();
        }
        LexemeGender gender = resolveGender(classCode, vowel, lexemeGender);

        List<Cell> cells = new ArrayList<>();
        for (CaseType caseType : CaseType.values()) {
            for (NumberType numberType : NumberType.values()) {
                DeclensionParadigm.Ending ending =
                        DeclensionParadigm.ending(vowel, gender, caseType, numberType);
                if (ending == null) {
                    continue;
                }
                WordFormBuilder.Form form = WordFormBuilder.compose(
                        lemmaIast, lemmaDevanagari, ending.endingIast(), ending.endingDevanagari());
                cells.add(new Cell(caseType, numberType, new Form(form.iast(), form.devanagari())));
            }
        }
        return cells;
    }

    /** True if {@code classCode} is a regular noun class served by composition (not stored). */
    public static boolean isRegularDecensionClass(String classCode) {
        return classCode != null && vowelTypeOf(classCode) != null;
    }

    private static VowelType vowelTypeOf(String code) {
        return switch (code) {
            case "a-stem-masc", "a-stem-neut", "a-stem" -> VowelType.A_STEM;
            case "a-stem-fem" -> VowelType.AA_STEM;
            case "i-stem" -> VowelType.I_STEM;
            case "u-stem" -> VowelType.U_STEM;
            case "r-stem" -> VowelType.R_STEM;
            default -> null;
        };
    }

    private static LexemeGender resolveGender(String code, VowelType vowel, LexemeGender lexemeGender) {
        return switch (code) {
            case "a-stem-masc" -> LexemeGender.MASCULINE;
            case "a-stem-neut" -> LexemeGender.NEUTER;
            case "a-stem-fem" -> LexemeGender.FEMININE;
            case "a-stem" -> lexemeGender; // merged: use lexeme's own gender
            case "i-stem", "u-stem" ->
                    lexemeGender == LexemeGender.NEUTER ? LexemeGender.NEUTER : LexemeGender.MASCULINE;
            case "r-stem" ->
                    lexemeGender == LexemeGender.FEMININE ? LexemeGender.FEMININE : LexemeGender.MASCULINE;
            default -> LexemeGender.UNSPECIFIED;
        };
    }
}