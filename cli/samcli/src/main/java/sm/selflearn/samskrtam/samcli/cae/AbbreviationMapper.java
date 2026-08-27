package sm.selflearn.samskrtam.samcli.cae;

import sm.selflearn.samskrtam.dictionary.mw.DerivationType;
import sm.selflearn.samskrtam.morphology.FormType;
import sm.selflearn.samskrtam.morphology.Gender;
import sm.selflearn.samskrtam.morphology.GrammaticalCase;
import sm.selflearn.samskrtam.morphology.Mood;
import sm.selflearn.samskrtam.morphology.NumberType;
import sm.selflearn.samskrtam.morphology.PartOfSpeech;
import sm.selflearn.samskrtam.morphology.Person;
import sm.selflearn.samskrtam.morphology.Tense;
import sm.selflearn.samskrtam.morphology.Voice;

import java.util.HashMap;
import java.util.Map;

/**
 * Статическая таблица соответствия сокращений словаря Каппеллера
 * грамматическим константам. Основано на списке аббревиатур,
 * реально встречающихся в <ab>...</ab> и <lex>...</lex> внутри cae.txt.
 *
 * <p>Важно: словарные аббревиатуры XIX века не являются
 * взаимно-однозначной грамматической разметкой (одно и то же "C."
 * значит "causative", "N." — "имя собственное" (Name), а не число/падеж).
 * Поэтому таблица заведомо неполна и расширяема — всё, что не найдено,
 * попадает в {@link CaeGrammarInfo#getUnmappedAbbreviations()}.</p>
 *
 * <p>Все enum-константы — из shared:samskrtam-dtos
 * (morphology / dictionary.mw), как и у mw-импортёра.</p>
 */
public final class AbbreviationMapper {

    private AbbreviationMapper() {}

    // ---- <lex> — часть речи + род (для существительных) --------------
    private static final Map<String, PartOfSpeech> LEX_POS = new HashMap<>();
    private static final Map<String, Gender> LEX_GENDER = new HashMap<>();
    static {
        LEX_POS.put("m.", PartOfSpeech.NOUN);
        LEX_POS.put("f.", PartOfSpeech.NOUN);
        LEX_POS.put("n.", PartOfSpeech.NOUN);
        LEX_POS.put("a.", PartOfSpeech.ADJECTIVE);

        LEX_GENDER.put("m.", Gender.MASCULINE);
        LEX_GENDER.put("f.", Gender.FEMININE);
        LEX_GENDER.put("n.", Gender.NEUTER);
    }

    // ---- <ab> — падеж --------------------------------------------------
    private static final Map<String, GrammaticalCase> CASE = new HashMap<>();
    static {
        CASE.put("nom.", GrammaticalCase.NOMINATIVE);
        CASE.put("acc.", GrammaticalCase.ACCUSATIVE);
        CASE.put("instr.", GrammaticalCase.INSTRUMENTAL);
        CASE.put("dat.", GrammaticalCase.DATIVE);
        CASE.put("abl.", GrammaticalCase.ABLATIVE);
        CASE.put("gen.", GrammaticalCase.GENITIVE);
        CASE.put("loc.", GrammaticalCase.LOCATIVE);
        CASE.put("voc.", GrammaticalCase.VOCATIVE);
    }

    // ---- <ab> — число ----------------------------------------------------
    private static final Map<String, NumberType> NUMBER = new HashMap<>();
    static {
        NUMBER.put("sg.", NumberType.SINGULAR);
        NUMBER.put("du.", NumberType.DUAL);
        NUMBER.put("pl.", NumberType.PLURAL);
    }

    // ---- <ab> — залог (пада: Parasmaipada / Atmanepada) -----------------
    private static final Map<String, Voice> VOICE = new HashMap<>();
    static {
        VOICE.put("P.", Voice.ACTIVE);      // Parasmaipada
        VOICE.put("A.", Voice.MIDDLE);      // Atmanepada
        VOICE.put("pass.", Voice.PASSIVE);
    }

    // ---- <ab> — наклонение ------------------------------------------------
    private static final Map<String, Mood> MOOD = new HashMap<>();
    static {
        MOOD.put("opt.", Mood.OPTATIVE);
        MOOD.put("impv.", Mood.IMPERATIVE);
        MOOD.put("cond.", Mood.CONDITIONAL);
        MOOD.put("inj.", Mood.INJUNCTIVE);
        MOOD.put("benot.", Mood.BENEDICTIVE);
    }

    // ---- <ab> — время -------------------------------------------------------
    private static final Map<String, Tense> TENSE = new HashMap<>();
    static {
        TENSE.put("pr.", Tense.PRESENT);
        TENSE.put("ipf.", Tense.IMPERFECT);
        TENSE.put("pf.", Tense.PERFECT);
        TENSE.put("aor.", Tense.AORIST);
        TENSE.put("fut.", Tense.FUTURE);
        TENSE.put("pft.", Tense.PERFECT);
    }

    // ---- <ab> — лицо -----------------------------------------------------
    private static final Map<String, Person> PERSON = new HashMap<>();
    static {
        PERSON.put("1.", Person.FIRST);
        PERSON.put("2.", Person.SECOND);
        PERSON.put("3.", Person.THIRD);
        PERSON.put("pers.", null); // общее упоминание "лица", не конкретное -> игнор
    }

    // ---- <ab> — часть речи / прочие грамматич. пометы --------------------
    private static final Map<String, PartOfSpeech> AB_POS = new HashMap<>();
    static {
        AB_POS.put("adv.", PartOfSpeech.ADVERB);
        AB_POS.put("pron.", PartOfSpeech.PRONOUN);
        AB_POS.put("part.", PartOfSpeech.PARTICLE);
        AB_POS.put("prep.", PartOfSpeech.PARTICLE);
        AB_POS.put("conj.", PartOfSpeech.CONJUNCTION);
        AB_POS.put("interj.", PartOfSpeech.INTERJECTION);
        AB_POS.put("num.", PartOfSpeech.NUMERAL);
        AB_POS.put("indecl.", PartOfSpeech.INDECLINABLE);
        AB_POS.put("adj.", PartOfSpeech.ADJECTIVE);
    }

    // ---- <ab> — тип деривации / форма -------------------------------------
    private static final Map<String, DerivationType> DERIVATION = new HashMap<>();
    private static final Map<String, FormType> FORM = new HashMap<>();
    static {
        DERIVATION.put("C.", DerivationType.CAUSATIVE);
        DERIVATION.put("caus.", DerivationType.CAUSATIVE);
        DERIVATION.put("desid.", DerivationType.DESIDERATIVE);
        DERIVATION.put("denom.", DerivationType.DENOMINATIVE);
        DERIVATION.put("ger.", DerivationType.ABSOLUTIVE);
        DERIVATION.put("pp.", DerivationType.PARTICIPLE);
        DERIVATION.put("ppr.", DerivationType.PARTICIPLE);
        DERIVATION.put("fpp.", DerivationType.GERUNDIVE);   // grdv./fpp. = future pass. part.
        DERIVATION.put("grdv.", DerivationType.GERUNDIVE);
        DERIVATION.put("inf.", DerivationType.INFINITIVE);
        DERIVATION.put("infin.", DerivationType.INFINITIVE);

        FORM.put("ger.", FormType.ABSOLUTIVE);
        FORM.put("pp.", FormType.PARTICIPLE);
        FORM.put("ppr.", FormType.PARTICIPLE);
        FORM.put("grdv.", FormType.GERUNDIVE);
        FORM.put("fpp.", FormType.GERUNDIVE);
        FORM.put("inf.", FormType.INFINITIVE);
        FORM.put("infin.", FormType.INFINITIVE);
        FORM.put("indecl.", FormType.INDECLINABLE);
    }

    /**
     * Пытается сопоставить одно сокращение (значение тега <ab> или <lex>)
     * со всеми применимыми константами и записывает их в info.
     * Если сокращение не распознано ни в одной категории — добавляет
     * его как есть в unmappedAbbreviations.
     */
    public static void apply(String rawAbbrev, CaeGrammarInfo info) {
        if (rawAbbrev == null || rawAbbrev.isBlank()) return;
        String v = rawAbbrev.trim();
        boolean matched = false;

        if (LEX_POS.containsKey(v))    { info.getPartsOfSpeech().add(LEX_POS.get(v)); matched = true; }
        if (LEX_GENDER.containsKey(v)) { info.getGenders().add(LEX_GENDER.get(v)); matched = true; }
        if (CASE.containsKey(v))       { info.getCases().add(CASE.get(v)); matched = true; }
        if (NUMBER.containsKey(v))     { info.getNumbers().add(NUMBER.get(v)); matched = true; }
        if (VOICE.containsKey(v))      { info.getVoices().add(VOICE.get(v)); matched = true; }
        if (MOOD.containsKey(v))       { info.getMoods().add(MOOD.get(v)); matched = true; }
        if (TENSE.containsKey(v))      { info.getTenses().add(TENSE.get(v)); matched = true; }
        if (PERSON.containsKey(v)) {
            Person p = PERSON.get(v);
            if (p != null) info.getPersons().add(p);
            matched = true;
        }
        if (AB_POS.containsKey(v))     { info.getPartsOfSpeech().add(AB_POS.get(v)); matched = true; }
        if (DERIVATION.containsKey(v)) { info.getDerivationTypes().add(DERIVATION.get(v)); matched = true; }
        if (FORM.containsKey(v))       { info.getFormTypes().add(FORM.get(v)); matched = true; }

        if (!matched) {
            info.getUnmappedAbbreviations().add(v);
        }
    }
}
