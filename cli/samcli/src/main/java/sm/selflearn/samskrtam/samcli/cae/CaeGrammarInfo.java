package sm.selflearn.samskrtam.samcli.cae;

import com.fasterxml.jackson.annotation.JsonIgnore;
import sm.selflearn.samskrtam.dictionary.mw.DerivationType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import sm.selflearn.samskrtam.morphology.FormType;
import sm.selflearn.samskrtam.morphology.Gender;
import sm.selflearn.samskrtam.morphology.GrammaticalCase;
import sm.selflearn.samskrtam.morphology.Mood;
import sm.selflearn.samskrtam.morphology.NumberType;
import sm.selflearn.samskrtam.morphology.PartOfSpeech;
import sm.selflearn.samskrtam.morphology.Person;
import sm.selflearn.samskrtam.morphology.Tense;
import sm.selflearn.samskrtam.morphology.Voice;

/**
 * DTO, сериализуемое в поле dict_entries.grammar (jsonb).
 * Одна словарная статья может нести несколько значений на каждый
 * признак (напр. <lex>m.</lex> ... <lex>f.</lex> в одной статье),
 * поэтому используются множества/списки, а не одиночные поля.
 *
 * <p>Грамматические enum-константы берутся из общей библиотеки
 * shared:samskrtam-dtos (пакеты morphology / dictionary.mw), чтобы
 * импортёр cae и импортёр mw оперировали одним и тем же набором
 * типов.</p>
 */
public class CaeGrammarInfo {

    private final EnumSet<PartOfSpeech>    partsOfSpeech    = EnumSet.noneOf(PartOfSpeech.class);
    private final EnumSet<Gender>          genders          = EnumSet.noneOf(Gender.class);
    private final EnumSet<GrammaticalCase> cases            = EnumSet.noneOf(GrammaticalCase.class);
    private final EnumSet<NumberType>      numbers          = EnumSet.noneOf(NumberType.class);
    private final EnumSet<Mood>            moods            = EnumSet.noneOf(Mood.class);
    private final EnumSet<Tense>           tenses           = EnumSet.noneOf(Tense.class);
    private final EnumSet<Voice>           voices           = EnumSet.noneOf(Voice.class);
    private final EnumSet<Person>          persons          = EnumSet.noneOf(Person.class);
    private final EnumSet<FormType>        formTypes        = EnumSet.noneOf(FormType.class);
    private final EnumSet<DerivationType>  derivationTypes  = EnumSet.noneOf(DerivationType.class);

    /** Аббревиатуры <ab>...</ab>, которые не удалось сопоставить ни с одной
     *  константой (ссылки типа cf., q.v., opp., etc., N. и т.п.) —
     *  сохраняются как есть для ручного/ML-разбора. */
    private final List<String> unmappedAbbreviations = new ArrayList<>();

    /** Иноязычные параллели <lang n="greek">...</lang>. */
    private final List<ForeignRef> foreignRefs = new ArrayList<>();

    /** Приставочные формы глагола <div n="p">—prefix ... gloss</div>. */
    private final List<CompoundForm> compoundForms = new ArrayList<>();

    public static class ForeignRef {
        public String lang;   // "greek", "latin", "germ." ...
        public String text;
        public ForeignRef() {}
        public ForeignRef(String lang, String text) { this.lang = lang; this.text = text; }
    }

    public static class CompoundForm {
        public String prefix; // "sam", "ni", "ud" ...
        public String gloss;
        public CompoundForm() {}
        public CompoundForm(String prefix, String gloss) { this.prefix = prefix; this.gloss = gloss; }
    }

    // --- getters (изменяемые коллекции достаточно для builder-стиля парсера) ---
    public EnumSet<PartOfSpeech>    getPartsOfSpeech()   { return partsOfSpeech; }
    public EnumSet<Gender>          getGenders()         { return genders; }
    public EnumSet<GrammaticalCase> getCases()           { return cases; }
    public EnumSet<NumberType>      getNumbers()         { return numbers; }
    public EnumSet<Mood>            getMoods()           { return moods; }
    public EnumSet<Tense>           getTenses()          { return tenses; }
    public EnumSet<Voice>           getVoices()          { return voices; }
    public EnumSet<Person>          getPersons()         { return persons; }
    public EnumSet<FormType>        getFormTypes()       { return formTypes; }
    public EnumSet<DerivationType>  getDerivationTypes() { return derivationTypes; }
    public List<String>             getUnmappedAbbreviations() { return unmappedAbbreviations; }

    // foreignRefs/compoundForms хранятся в отдельных колонках таблицы,
    // поэтому исключаем их из сериализации поля grammar (jsonb).
    @JsonIgnore
    public List<ForeignRef>   getForeignRefs()   { return foreignRefs; }
    @JsonIgnore
    public List<CompoundForm> getCompoundForms() { return compoundForms; }

    public boolean isEmpty() {
        return partsOfSpeech.isEmpty() && genders.isEmpty() && cases.isEmpty()
            && numbers.isEmpty() && moods.isEmpty() && tenses.isEmpty()
            && voices.isEmpty() && persons.isEmpty() && formTypes.isEmpty()
            && derivationTypes.isEmpty() && unmappedAbbreviations.isEmpty()
            && foreignRefs.isEmpty() && compoundForms.isEmpty();
    }
}
