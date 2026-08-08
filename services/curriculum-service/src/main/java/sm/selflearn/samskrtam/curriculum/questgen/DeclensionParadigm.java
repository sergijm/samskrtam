package sm.selflearn.samskrtam.curriculum.questgen;

import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.questgen.morphology.CaseType;
import sm.selflearn.samskrtam.curriculum.questgen.morphology.NumberType;
import sm.selflearn.samskrtam.curriculum.questgen.morphology.VowelType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static declension paradigm (case endings), ported 1:1 from content-service's
 * {@code content.case_endings} table (see architecture.md §3.3). Every row is
 * the canonical {@code (vowel_type, gender, case_type, number_type) -> ending}
 * mapping present verbatim in the live database; no new rules are invented.
 *
 * <p>Gender-specific endings are resolved per lexeme: {@code a-stem-masc} uses
 * {@code A_STEM/MASCULINE}, the ā-stem {@code a-stem-fem} uses
 * {@code A_STEM/FEMININE} (mapped by the generator to the {@code AA_STEM}
 * vowel type), and i-/u-/ṛ-stems resolve by the lexeme's own gender.
 */
final class DeclensionParadigm {

    record Ending(String endingIast, String endingDevanagari) {
    }

    private static final Map<String, Ending> BY_KEY = new LinkedHashMap<>();

    private static String key(VowelType v, LexemeGender g, CaseType c, NumberType n) {
        return v.name() + "|" + g.name() + "|" + c.name() + "|" + n.name();
    }

    static Ending ending(VowelType vowel, LexemeGender gender, CaseType caseType, NumberType numberType) {
        return BY_KEY.get(key(vowel, gender, caseType, numberType));
    }

    static {
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.ABLATIVE, NumberType.DUAL, "ābhyām", "आभ्याम्");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.ABLATIVE, NumberType.PLURAL, "ābhyaḥ", "आभ्यः");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.ABLATIVE, NumberType.SINGULAR, "āyāḥ", "आयाः");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.ACCUSATIVE, NumberType.DUAL, "e", "ए");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.ACCUSATIVE, NumberType.PLURAL, "āḥ", "आः");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.ACCUSATIVE, NumberType.SINGULAR, "ām", "आम्");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.DATIVE, NumberType.DUAL, "ābhyām", "आभ्याम्");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.DATIVE, NumberType.PLURAL, "ābhyaḥ", "आभ्यः");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.DATIVE, NumberType.SINGULAR, "āyai", "आयै");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.GENITIVE, NumberType.DUAL, "ayoḥ", "योः");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.GENITIVE, NumberType.PLURAL, "ānām", "आनाम्");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.GENITIVE, NumberType.SINGULAR, "āyāḥ", "आयाः");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.INSTRUMENTAL, NumberType.DUAL, "ābhyām", "आभ्याम्");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.INSTRUMENTAL, NumberType.PLURAL, "ābhiḥ", "आभिः");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.INSTRUMENTAL, NumberType.SINGULAR, "ayā", "या");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.LOCATIVE, NumberType.DUAL, "ayoḥ", "योः");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.LOCATIVE, NumberType.PLURAL, "āsu", "आसु");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.LOCATIVE, NumberType.SINGULAR, "āyām", "आयाम्");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.NOMINATIVE, NumberType.DUAL, "e", "ए");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.NOMINATIVE, NumberType.PLURAL, "āḥ", "आः");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.NOMINATIVE, NumberType.SINGULAR, "ā", "आ");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.VOCATIVE, NumberType.DUAL, "e", "ए");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.VOCATIVE, NumberType.PLURAL, "āḥ", "आः");
        r(VowelType.AA_STEM, LexemeGender.FEMININE, CaseType.VOCATIVE, NumberType.SINGULAR, "e", "ए");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.ABLATIVE, NumberType.DUAL, "ābhyām", "आभ्याम्");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.ABLATIVE, NumberType.PLURAL, "ebhyaḥ", "एभ्यः");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.ABLATIVE, NumberType.SINGULAR, "āt", "आत्");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.ACCUSATIVE, NumberType.DUAL, "au", "औ");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.ACCUSATIVE, NumberType.PLURAL, "ān", "आन्");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.ACCUSATIVE, NumberType.SINGULAR, "m", "म्");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.DATIVE, NumberType.DUAL, "ābhyām", "आभ्याम्");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.DATIVE, NumberType.PLURAL, "ebhyaḥ", "एभ्यः");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.DATIVE, NumberType.SINGULAR, "āya", "आय");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.GENITIVE, NumberType.DUAL, "ayoḥ", "योः");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.GENITIVE, NumberType.PLURAL, "ānām", "आनाम्");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.GENITIVE, NumberType.SINGULAR, "asya", "अस्य");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.INSTRUMENTAL, NumberType.DUAL, "ābhyām", "आभ्याम्");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.INSTRUMENTAL, NumberType.PLURAL, "aiḥ", "ऐः");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.INSTRUMENTAL, NumberType.SINGULAR, "ena", "एन");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.LOCATIVE, NumberType.DUAL, "ayoḥ", "योः");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.LOCATIVE, NumberType.PLURAL, "eṣu", "एषु");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.LOCATIVE, NumberType.SINGULAR, "e", "ए");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.NOMINATIVE, NumberType.DUAL, "au", "औ");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.NOMINATIVE, NumberType.PLURAL, "āḥ", "आः");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.NOMINATIVE, NumberType.SINGULAR, "ḥ", "ः");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.VOCATIVE, NumberType.DUAL, "au", "औ");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.VOCATIVE, NumberType.PLURAL, "āḥ", "आः");
        r(VowelType.A_STEM, LexemeGender.MASCULINE, CaseType.VOCATIVE, NumberType.SINGULAR, "a", "अ");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.ABLATIVE, NumberType.DUAL, "ābhyām", "आभ्याम्");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.ABLATIVE, NumberType.PLURAL, "ebhyaḥ", "एभ्यः");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.ABLATIVE, NumberType.SINGULAR, "āt", "आत्");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.ACCUSATIVE, NumberType.DUAL, "e", "ए");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.ACCUSATIVE, NumberType.PLURAL, "āni", "आनि");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.ACCUSATIVE, NumberType.SINGULAR, "m", "म्");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.DATIVE, NumberType.DUAL, "ābhyām", "आभ्याम्");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.DATIVE, NumberType.PLURAL, "ebhyaḥ", "एभ्यः");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.DATIVE, NumberType.SINGULAR, "āya", "आय");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.GENITIVE, NumberType.DUAL, "ayoḥ", "योः");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.GENITIVE, NumberType.PLURAL, "ānām", "आनाम्");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.GENITIVE, NumberType.SINGULAR, "asya", "अस्य");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.INSTRUMENTAL, NumberType.DUAL, "ābhyām", "आभ्याम्");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.INSTRUMENTAL, NumberType.PLURAL, "aiḥ", "ऐः");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.INSTRUMENTAL, NumberType.SINGULAR, "ena", "एन");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.LOCATIVE, NumberType.DUAL, "ayoḥ", "योः");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.LOCATIVE, NumberType.PLURAL, "eṣu", "एषु");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.LOCATIVE, NumberType.SINGULAR, "e", "ए");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.NOMINATIVE, NumberType.DUAL, "e", "ए");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.NOMINATIVE, NumberType.PLURAL, "āni", "आनि");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.NOMINATIVE, NumberType.SINGULAR, "m", "म्");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.VOCATIVE, NumberType.DUAL, "e", "ए");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.VOCATIVE, NumberType.PLURAL, "āni", "आनि");
        r(VowelType.A_STEM, LexemeGender.NEUTER, CaseType.VOCATIVE, NumberType.SINGULAR, "a", "अ");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.ABLATIVE, NumberType.DUAL, "ībhyām", "ईभ्याम्");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.ABLATIVE, NumberType.PLURAL, "ībhyaḥ", "ईभ्यः");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.ABLATIVE, NumberType.SINGULAR, "yāḥ", "याः");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.ACCUSATIVE, NumberType.DUAL, "yau", "यौ");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.ACCUSATIVE, NumberType.PLURAL, "īḥ", "ईः");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.ACCUSATIVE, NumberType.SINGULAR, "īm", "ईम्");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.DATIVE, NumberType.DUAL, "ībhyām", "ईभ्याम्");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.DATIVE, NumberType.PLURAL, "ībhyaḥ", "ईभ्यः");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.DATIVE, NumberType.SINGULAR, "yai", "यै");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.GENITIVE, NumberType.DUAL, "yoḥ", "योः");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.GENITIVE, NumberType.PLURAL, "īnām", "ईनाम्");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.GENITIVE, NumberType.SINGULAR, "yāḥ", "याः");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.INSTRUMENTAL, NumberType.DUAL, "ībhyām", "ईभ्याम्");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.INSTRUMENTAL, NumberType.PLURAL, "ībhiḥ", "ईभिः");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.INSTRUMENTAL, NumberType.SINGULAR, "yā", "या");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.LOCATIVE, NumberType.DUAL, "yoḥ", "योः");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.LOCATIVE, NumberType.PLURAL, "īṣu", "ईषु");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.LOCATIVE, NumberType.SINGULAR, "yām", "याम्");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.NOMINATIVE, NumberType.DUAL, "yau", "यौ");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.NOMINATIVE, NumberType.PLURAL, "yaḥ", "यः");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.NOMINATIVE, NumberType.SINGULAR, "ī", "ई");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.VOCATIVE, NumberType.DUAL, "yau", "यौ");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.VOCATIVE, NumberType.PLURAL, "yaḥ", "यः");
        r(VowelType.II_STEM, LexemeGender.FEMININE, CaseType.VOCATIVE, NumberType.SINGULAR, "i", "इ");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.ABLATIVE, NumberType.DUAL, "ibhyām", "इभ्याम्");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.ABLATIVE, NumberType.PLURAL, "ibhyaḥ", "इभ्यः");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.ABLATIVE, NumberType.SINGULAR, "eḥ", "एः");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.ACCUSATIVE, NumberType.DUAL, "ī", "ई");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.ACCUSATIVE, NumberType.PLURAL, "īn", "ईन्");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.ACCUSATIVE, NumberType.SINGULAR, "im", "इम्");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.DATIVE, NumberType.DUAL, "ibhyām", "इभ्याम्");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.DATIVE, NumberType.PLURAL, "ibhyaḥ", "इभ्यः");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.DATIVE, NumberType.SINGULAR, "aye", "अये");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.GENITIVE, NumberType.DUAL, "yoḥ", "योः");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.GENITIVE, NumberType.PLURAL, "īnām", "ईनाम्");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.GENITIVE, NumberType.SINGULAR, "eḥ", "एः");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.INSTRUMENTAL, NumberType.DUAL, "ibhyām", "इभ्याम्");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.INSTRUMENTAL, NumberType.PLURAL, "ibhiḥ", "इभिः");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.INSTRUMENTAL, NumberType.SINGULAR, "inā", "इना");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.LOCATIVE, NumberType.DUAL, "yoḥ", "योः");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.LOCATIVE, NumberType.PLURAL, "iṣu", "इषु");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.LOCATIVE, NumberType.SINGULAR, "au", "औ");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.NOMINATIVE, NumberType.DUAL, "ī", "ई");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.NOMINATIVE, NumberType.PLURAL, "ayaḥ", "अयः");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.NOMINATIVE, NumberType.SINGULAR, "iḥ", "इः");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.VOCATIVE, NumberType.DUAL, "ī", "ई");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.VOCATIVE, NumberType.PLURAL, "ayaḥ", "अयः");
        r(VowelType.I_STEM, LexemeGender.MASCULINE, CaseType.VOCATIVE, NumberType.SINGULAR, "e", "ए");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.ABLATIVE, NumberType.DUAL, "ibhyām", "इभ्याम्");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.ABLATIVE, NumberType.PLURAL, "ibhyaḥ", "इभ्यः");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.ABLATIVE, NumberType.SINGULAR, "inaḥ", "इनः");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.ACCUSATIVE, NumberType.DUAL, "inī", "इनी");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.ACCUSATIVE, NumberType.PLURAL, "īni", "ईनि");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.ACCUSATIVE, NumberType.SINGULAR, "i", "इ");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.DATIVE, NumberType.DUAL, "ibhyām", "इभ्याम्");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.DATIVE, NumberType.PLURAL, "ibhyaḥ", "इभ्यः");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.DATIVE, NumberType.SINGULAR, "ine", "इने");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.GENITIVE, NumberType.DUAL, "inoḥ", "इनोः");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.GENITIVE, NumberType.PLURAL, "īnām", "ईनाम्");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.GENITIVE, NumberType.SINGULAR, "inaḥ", "इनः");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.INSTRUMENTAL, NumberType.DUAL, "ibhyām", "इभ्याम्");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.INSTRUMENTAL, NumberType.PLURAL, "ibhiḥ", "इभिः");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.INSTRUMENTAL, NumberType.SINGULAR, "inā", "इना");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.LOCATIVE, NumberType.DUAL, "inoḥ", "इनोः");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.LOCATIVE, NumberType.PLURAL, "iṣu", "इषु");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.LOCATIVE, NumberType.SINGULAR, "ini", "इनि");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.NOMINATIVE, NumberType.DUAL, "inī", "इनी");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.NOMINATIVE, NumberType.PLURAL, "īni", "ईनि");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.NOMINATIVE, NumberType.SINGULAR, "i", "इ");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.VOCATIVE, NumberType.DUAL, "inī", "इनी");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.VOCATIVE, NumberType.PLURAL, "īni", "ईनि");
        r(VowelType.I_STEM, LexemeGender.NEUTER, CaseType.VOCATIVE, NumberType.SINGULAR, "e", "ए");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.ABLATIVE, NumberType.DUAL, "ṛbhyām", "ऋभ्याम्");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.ABLATIVE, NumberType.PLURAL, "ṛbhyaḥ", "ऋभ्यः");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.ABLATIVE, NumberType.SINGULAR, "uḥ", "उः");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.ACCUSATIVE, NumberType.DUAL, "ārau", "आरौ");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.ACCUSATIVE, NumberType.PLURAL, "ṝḥ", "ॄः");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.ACCUSATIVE, NumberType.SINGULAR, "āram", "आरम्");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.DATIVE, NumberType.DUAL, "ṛbhyām", "ऋभ्याम्");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.DATIVE, NumberType.PLURAL, "ṛbhyaḥ", "ऋभ्यः");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.DATIVE, NumberType.SINGULAR, "re", "रे");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.GENITIVE, NumberType.DUAL, "roḥ", "रोः");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.GENITIVE, NumberType.PLURAL, "ṝṇām", "ॄणाम्");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.GENITIVE, NumberType.SINGULAR, "uḥ", "उः");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.INSTRUMENTAL, NumberType.DUAL, "ṛbhyām", "ऋभ्याम्");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.INSTRUMENTAL, NumberType.PLURAL, "ṛbhiḥ", "ऋभिः");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.INSTRUMENTAL, NumberType.SINGULAR, "rā", "रा");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.LOCATIVE, NumberType.DUAL, "roḥ", "रोः");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.LOCATIVE, NumberType.PLURAL, "ṛṣu", "ऋषु");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.LOCATIVE, NumberType.SINGULAR, "ari", "अरि");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.NOMINATIVE, NumberType.DUAL, "ārau", "आरौ");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.NOMINATIVE, NumberType.PLURAL, "āraḥ", "आरः");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.NOMINATIVE, NumberType.SINGULAR, "ā", "आ");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.VOCATIVE, NumberType.DUAL, "ārau", "आरौ");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.VOCATIVE, NumberType.PLURAL, "āraḥ", "आरः");
        r(VowelType.R_STEM, LexemeGender.FEMININE, CaseType.VOCATIVE, NumberType.SINGULAR, "ar", "अर्");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.ABLATIVE, NumberType.DUAL, "ṛbhyām", "ऋभ्याम्");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.ABLATIVE, NumberType.PLURAL, "ṛbhyaḥ", "ऋभ्यः");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.ABLATIVE, NumberType.SINGULAR, "uḥ", "उः");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.ACCUSATIVE, NumberType.DUAL, "ārau", "आरौ");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.ACCUSATIVE, NumberType.PLURAL, "ṝn", "ॄन्");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.ACCUSATIVE, NumberType.SINGULAR, "āram", "आरम्");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.DATIVE, NumberType.DUAL, "ṛbhyām", "ऋभ्याम्");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.DATIVE, NumberType.PLURAL, "ṛbhyaḥ", "ऋभ्यः");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.DATIVE, NumberType.SINGULAR, "re", "रे");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.GENITIVE, NumberType.DUAL, "roḥ", "रोः");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.GENITIVE, NumberType.PLURAL, "ṝṇām", "ॄणाम्");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.GENITIVE, NumberType.SINGULAR, "uḥ", "उः");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.INSTRUMENTAL, NumberType.DUAL, "ṛbhyām", "ऋभ्याम्");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.INSTRUMENTAL, NumberType.PLURAL, "ṛbhiḥ", "ऋभिः");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.INSTRUMENTAL, NumberType.SINGULAR, "rā", "रा");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.LOCATIVE, NumberType.DUAL, "roḥ", "रोः");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.LOCATIVE, NumberType.PLURAL, "ṛṣu", "ऋषु");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.LOCATIVE, NumberType.SINGULAR, "ari", "अरि");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.NOMINATIVE, NumberType.DUAL, "ārau", "आरौ");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.NOMINATIVE, NumberType.PLURAL, "āraḥ", "आरः");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.NOMINATIVE, NumberType.SINGULAR, "ā", "आ");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.VOCATIVE, NumberType.DUAL, "ārau", "आरौ");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.VOCATIVE, NumberType.PLURAL, "āraḥ", "आरः");
        r(VowelType.R_STEM, LexemeGender.MASCULINE, CaseType.VOCATIVE, NumberType.SINGULAR, "ar", "अर्");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.ABLATIVE, NumberType.DUAL, "ubhyām", "उभ्याम्");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.ABLATIVE, NumberType.PLURAL, "ubhyaḥ", "उभ्यः");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.ABLATIVE, NumberType.SINGULAR, "oḥ", "ओः");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.ACCUSATIVE, NumberType.DUAL, "ū", "ऊ");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.ACCUSATIVE, NumberType.PLURAL, "ūn", "ऊन्");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.ACCUSATIVE, NumberType.SINGULAR, "um", "उम्");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.DATIVE, NumberType.DUAL, "ubhyām", "उभ्याम्");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.DATIVE, NumberType.PLURAL, "ubhyaḥ", "उभ्यः");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.DATIVE, NumberType.SINGULAR, "ave", "अवे");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.GENITIVE, NumberType.DUAL, "voḥ", "वोः");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.GENITIVE, NumberType.PLURAL, "ūnām", "ऊनाम्");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.GENITIVE, NumberType.SINGULAR, "oḥ", "ओः");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.INSTRUMENTAL, NumberType.DUAL, "ubhyām", "उभ्याम्");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.INSTRUMENTAL, NumberType.PLURAL, "ubhiḥ", "उभिः");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.INSTRUMENTAL, NumberType.SINGULAR, "unā", "उना");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.LOCATIVE, NumberType.DUAL, "voḥ", "वोः");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.LOCATIVE, NumberType.PLURAL, "uṣu", "उषु");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.LOCATIVE, NumberType.SINGULAR, "au", "औ");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.NOMINATIVE, NumberType.DUAL, "ū", "ऊ");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.NOMINATIVE, NumberType.PLURAL, "avaḥ", "अवः");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.NOMINATIVE, NumberType.SINGULAR, "uḥ", "उः");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.VOCATIVE, NumberType.DUAL, "ū", "ऊ");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.VOCATIVE, NumberType.PLURAL, "avaḥ", "अवः");
        r(VowelType.U_STEM, LexemeGender.MASCULINE, CaseType.VOCATIVE, NumberType.SINGULAR, "o", "ओ");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.ABLATIVE, NumberType.DUAL, "ubhyām", "उभ्याम्");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.ABLATIVE, NumberType.PLURAL, "ubhyaḥ", "उभ्यः");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.ABLATIVE, NumberType.SINGULAR, "unaḥ", "उनः");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.ACCUSATIVE, NumberType.DUAL, "unī", "उनी");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.ACCUSATIVE, NumberType.PLURAL, "ūni", "ऊनि");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.ACCUSATIVE, NumberType.SINGULAR, "u", "उ");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.DATIVE, NumberType.DUAL, "ubhyām", "उभ्याम्");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.DATIVE, NumberType.PLURAL, "ubhyaḥ", "उभ्यः");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.DATIVE, NumberType.SINGULAR, "une", "उने");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.GENITIVE, NumberType.DUAL, "unoḥ", "उनोः");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.GENITIVE, NumberType.PLURAL, "ūnām", "ऊनाम्");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.GENITIVE, NumberType.SINGULAR, "unaḥ", "उनः");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.INSTRUMENTAL, NumberType.DUAL, "ubhyām", "उभ्याम्");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.INSTRUMENTAL, NumberType.PLURAL, "ubhiḥ", "उभिः");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.INSTRUMENTAL, NumberType.SINGULAR, "unā", "उना");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.LOCATIVE, NumberType.DUAL, "unoḥ", "उनोः");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.LOCATIVE, NumberType.PLURAL, "uṣu", "उषु");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.LOCATIVE, NumberType.SINGULAR, "uni", "उनि");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.NOMINATIVE, NumberType.DUAL, "unī", "उनी");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.NOMINATIVE, NumberType.PLURAL, "ūni", "ऊनि");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.NOMINATIVE, NumberType.SINGULAR, "u", "उ");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.VOCATIVE, NumberType.DUAL, "unī", "उनी");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.VOCATIVE, NumberType.PLURAL, "ūni", "ऊनि");
        r(VowelType.U_STEM, LexemeGender.NEUTER, CaseType.VOCATIVE, NumberType.SINGULAR, "o", "ओ");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.ABLATIVE, NumberType.DUAL, "ūbhyām", "ऊभ्याम्");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.ABLATIVE, NumberType.PLURAL, "ūbhyaḥ", "ऊभ्यः");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.ABLATIVE, NumberType.SINGULAR, "vāḥ", "वाः");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.ACCUSATIVE, NumberType.DUAL, "vau", "वौ");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.ACCUSATIVE, NumberType.PLURAL, "ūḥ", "ऊः");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.ACCUSATIVE, NumberType.SINGULAR, "ūm", "ऊम्");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.DATIVE, NumberType.DUAL, "ūbhyām", "ऊभ्याम्");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.DATIVE, NumberType.PLURAL, "ūbhyaḥ", "ऊभ्यः");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.DATIVE, NumberType.SINGULAR, "vai", "वै");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.GENITIVE, NumberType.DUAL, "voḥ", "वोः");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.GENITIVE, NumberType.PLURAL, "ūnām", "ऊनाम्");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.GENITIVE, NumberType.SINGULAR, "vāḥ", "वाः");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.INSTRUMENTAL, NumberType.DUAL, "ūbhyām", "ऊभ्याम्");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.INSTRUMENTAL, NumberType.PLURAL, "ūbhiḥ", "ऊभिः");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.INSTRUMENTAL, NumberType.SINGULAR, "vā", "वा");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.LOCATIVE, NumberType.DUAL, "voḥ", "वोः");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.LOCATIVE, NumberType.PLURAL, "ūṣu", "ऊषु");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.LOCATIVE, NumberType.SINGULAR, "vām", "वाम्");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.NOMINATIVE, NumberType.DUAL, "vau", "वौ");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.NOMINATIVE, NumberType.PLURAL, "vaḥ", "वः");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.NOMINATIVE, NumberType.SINGULAR, "ūḥ", "ऊः");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.VOCATIVE, NumberType.DUAL, "vau", "वौ");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.VOCATIVE, NumberType.PLURAL, "vaḥ", "वः");
        r(VowelType.UU_STEM, LexemeGender.FEMININE, CaseType.VOCATIVE, NumberType.SINGULAR, "u", "उ");
    }

    private static void r(VowelType v, LexemeGender g, CaseType c, NumberType n, String i, String dev) {
        BY_KEY.put(key(v, g, c, n), new Ending(i, dev));
    }
}
