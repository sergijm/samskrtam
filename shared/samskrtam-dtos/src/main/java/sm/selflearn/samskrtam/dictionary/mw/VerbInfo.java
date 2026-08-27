package sm.selflearn.samskrtam.dictionary.mw;

import sm.selflearn.samskrtam.morphology.FormType;
import sm.selflearn.samskrtam.morphology.GrammaticalCase;
import sm.selflearn.samskrtam.morphology.Mood;
import sm.selflearn.samskrtam.morphology.NumberType;
import sm.selflearn.samskrtam.morphology.Person;
import sm.selflearn.samskrtam.morphology.Tense;
import sm.selflearn.samskrtam.morphology.Voice;

/**
 * Информация о глагольной статье, извлечённая из
 * {@code <info verb="X" cp="Y" parse="P+R"/>}.
 *
 * kind:
 *   genuineroot - "подлинный" корень (~750 корней, напечатанных деванагари крупным шрифтом)
 *   root        - обычный (не "подлинный") корень
 *   pre         - приставочный глагол (parse содержит разбор "приставка+корень")
 *   gati        - глагол с иным (не стандартным упасарга-) префиксом
 *   nom         - деноминативный (отымённый) глагол ("Nom.")
 */
public class VerbInfo {

    private String kind;

    /** Сводка класс-пада, напр. "1P", "10Ā", "1,10P", "0Ā,0P" */
    private String classPada;

    /**
     * Разбор на приставку(и) и корень для kind=pre/gati,
     * напр. "A+kamp" или "urarI+kf". Может быть null.
     */
    private String parse;

    /** Тип деривации, выведенный из {@link #kind}. */
    private DerivationType derivationType;

    /** Тип формы (инфинитив, абсолютив, причастие, герундив...), выведен из parse. */
    private FormType formType;

    /** Время, выведенное из parse (pres/impf/perf/aor/fut...). */
    private Tense tense;

    /** Наклонение, выведенное из parse (ind/opt/imp/cond/ben/inj). */
    private Mood mood;

    /** Залог, выведенное из parse (act/mid/pass). */
    private Voice voice;

    /** Лицо, выведенное из parse (v1s/v2d/v3p...). */
    private Person person;

    /** Число, выведенное из parse (v1s/v2d/v3p...). */
    private NumberType number;

    /** Падеж (для отглагольных форм), выведен из parse (nom/acc/...). */
    private GrammaticalCase grammaticalCase;

    public VerbInfo() {
    }

    public VerbInfo(String kind, String classPada, String parse) {
        this.kind = kind;
        this.classPada = classPada;
        this.parse = parse;
        recomputeEnums();
    }

    /**
     * Пересчитывает типизированные грамматические поля ({@link #derivationType},
     * {@link #tense}, {@link #mood}, {@link #voice}, {@link #person},
     * {@link #number}, {@link #grammaticalCase}, {@link #formType}) на основе
     * {@link #kind} и {@link #parse}. Вызывается после изменения этих полей.
     */
    public void recomputeEnums() {
        this.derivationType = deriveKind(kind);
        this.tense = null;
        this.mood = null;
        this.voice = null;
        this.person = null;
        this.number = null;
        this.grammaticalCase = null;
        this.formType = null;
        if (parse == null) {
            return;
        }
        for (String tok : parse.trim().split("\\s+")) {
            if (tok.isEmpty()) {
                continue;
            }
            switch (tok) {
                case "pres" -> tense = Tense.PRESENT;
                case "impf" -> tense = Tense.IMPERFECT;
                case "perf" -> tense = Tense.PERFECT;
                case "aor" -> tense = Tense.AORIST;
                case "fut" -> tense = Tense.FUTURE;
                case "pfut", "perip" -> tense = Tense.PERIPHRASTIC_FUTURE;
                case "cond" -> mood = Mood.CONDITIONAL;
                case "ben" -> mood = Mood.BENEDICTIVE;
                case "ind" -> mood = Mood.INDICATIVE;
                case "opt" -> mood = Mood.OPTATIVE;
                case "imp" -> mood = Mood.IMPERATIVE;
                case "inj" -> mood = Mood.INJUNCTIVE;
                case "act" -> voice = Voice.ACTIVE;
                case "mid" -> voice = Voice.MIDDLE;
                case "pass" -> voice = Voice.PASSIVE;
                case "inf" -> formType = FormType.INFINITIVE;
                case "abs" -> formType = FormType.ABSOLUTIVE;
                case "ger" -> formType = FormType.GERUNDIVE;
                case "pp", "pt" -> formType = FormType.PARTICIPLE;
                case "nom" -> grammaticalCase = GrammaticalCase.NOMINATIVE;
                case "acc" -> grammaticalCase = GrammaticalCase.ACCUSATIVE;
                case "instr" -> grammaticalCase = GrammaticalCase.INSTRUMENTAL;
                case "dat" -> grammaticalCase = GrammaticalCase.DATIVE;
                case "abl" -> grammaticalCase = GrammaticalCase.ABLATIVE;
                case "gen" -> grammaticalCase = GrammaticalCase.GENITIVE;
                case "loc" -> grammaticalCase = GrammaticalCase.LOCATIVE;
                case "voc" -> grammaticalCase = GrammaticalCase.VOCATIVE;
                default -> {
                    if (tok.matches("^(v?)(\\d)([sdp])$")) {
                        int p = tok.charAt(tok.length() - 2) - '0';
                        char n = tok.charAt(tok.length() - 1);
                        person = switch (p) {
                            case 1 -> Person.FIRST;
                            case 2 -> Person.SECOND;
                            default -> Person.THIRD;
                        };
                        number = switch (n) {
                            case 's' -> NumberType.SINGULAR;
                            case 'd' -> NumberType.DUAL;
                            default -> NumberType.PLURAL;
                        };
                    }
                }
            }
        }
    }

    private static DerivationType deriveKind(String k) {
        if (k == null) {
            return null;
        }
        return switch (k) {
            case "genuineroot", "root" -> DerivationType.SIMPLE_INFLECTION;
            case "nom" -> DerivationType.DENOMINATIVE;
            case "pre", "gati" -> DerivationType.COMPOUND_VERB;
            default -> DerivationType.SIMPLE_INFLECTION;
        };
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
        recomputeEnums();
    }

    public String getClassPada() {
        return classPada;
    }

    public void setClassPada(String classPada) {
        this.classPada = classPada;
    }

    public String getParse() {
        return parse;
    }

    public void setParse(String parse) {
        this.parse = parse;
        recomputeEnums();
    }

    public DerivationType getDerivationType() {
        return derivationType;
    }

    public void setDerivationType(DerivationType derivationType) {
        this.derivationType = derivationType;
    }

    public FormType getFormType() {
        return formType;
    }

    public void setFormType(FormType formType) {
        this.formType = formType;
    }

    public Tense getTense() {
        return tense;
    }

    public void setTense(Tense tense) {
        this.tense = tense;
    }

    public Mood getMood() {
        return mood;
    }

    public void setMood(Mood mood) {
        this.mood = mood;
    }

    public Voice getVoice() {
        return voice;
    }

    public void setVoice(Voice voice) {
        this.voice = voice;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public NumberType getNumber() {
        return number;
    }

    public void setNumber(NumberType number) {
        this.number = number;
    }

    public GrammaticalCase getGrammaticalCase() {
        return grammaticalCase;
    }

    public void setGrammaticalCase(GrammaticalCase grammaticalCase) {
        this.grammaticalCase = grammaticalCase;
    }

    @Override
    public String toString() {
        return "VerbInfo{kind=" + kind + ", cp=" + classPada + ", parse=" + parse + "}";
    }
}
