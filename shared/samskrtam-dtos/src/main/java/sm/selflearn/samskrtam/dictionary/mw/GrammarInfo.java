package sm.selflearn.samskrtam.dictionary.mw;

import sm.selflearn.samskrtam.morphology.PartOfSpeech;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Вся грамматическая и справочная информация, извлечённая парсером из
 * XML-разметки внутри тела статьи (поле {@code body}), но не относящаяся
 * к "читаемому" тексту (не идёт в {@link MwBodyParser#parse(String)} вторым
 * значением результата).
 */
public class GrammarInfo {

    /** Текст тега <hom>N.</hom>, если тело начинается с номера омонима, напр. "2." */
    private String homonymMarker;

    /** Родовые/лексические характеристики из <lex>...</lex> внутри тела статьи */
    private List<LexGender> lexTags = new ArrayList<>();

    /** Разобранное значение <info lex="m:f#ikA:n"/> (сводная родовая информация) */
    private List<LexGender> lexSummary = new ArrayList<>();

    /** Значение <info lexcat="LEXID=...,STEM=...,..."/>, разобранное в пары ключ=значение */
    private Map<String, String> lexCategory = new LinkedHashMap<>();

    /** Глагольная информация из <info verb=.../>, null для неглагольных статей */
    private VerbInfo verbInfo;

    /** Часть речи статьи (выводится парсером, напр. VERB для глагольных статей). */
    private PartOfSpeech partOfSpeech;

    /** Ссылки на Westergaard Dhatupatha: <info westergaard="X,Y,Z"/> -> [X,Y,Z] */
    private List<String> westergaardRef = new ArrayList<>();

    /** Ссылка на Whitney Roots: <info whitneyroots="R,p"/> -> [R,p] */
    private List<String> whitneyRootsRef = new ArrayList<>();

    /**
     * Прочие атрибуты тега <info .../>, не разобранные в отдельные поля:
     * ключ = имя атрибута (or, and, orsl, orwr, phwchild, phwparent, n, hui, ...),
     * значение = его "сырое" содержимое.
     */
    private Map<String, String> otherInfoAttributes = new LinkedHashMap<>();

    /** true, если статья из приложения (supplement): <info n="sup"/> */
    private boolean supplement;

    /** true, если это правка существующей статьи: <info n="rev"/> */
    private boolean revision;

    /** Санскритские слова из <s>...</s> (slp1, "очищенные" от <srs/>/<shortlong/>) */
    private List<String> sanskritWords = new ArrayList<>();

    /** Санскритские имена собственные из <s1 slp1="...">Text</s1>: IAST-текст -> slp1 */
    private Map<String, String> sanskritProperNames = new LinkedHashMap<>();

    /** Литературные источники <ls>...</ls> */
    private List<String> literarySources = new ArrayList<>();

    /** Общие сокращения <ab>...</ab> (текст сокращения, как напечатано) */
    private List<String> abbreviations = new ArrayList<>();

    /** Локальные сокращения <ab n="Y">X</ab>: X -> развёрнутое значение Y */
    private Map<String, String> localAbbreviations = new LinkedHashMap<>();

    /** Ботанические названия <bot>...</bot> */
    private List<String> botanicalNames = new ArrayList<>();

    /** Зоологические названия <bio>...</bio> */
    private List<String> biologicalNames = new ArrayList<>();

    /** Родственные (когнатные) слова из других языков: <lang>/<etym>/<gk> */
    private List<CognateWord> cognates = new ArrayList<>();

    /** Внутренние ссылки на другую страницу/колонку <pcol>...</pcol> */
    private List<String> pageColRefs = new ArrayList<>();

    /** Логические разделы <div n="X"/> (to, vp, p, ...) */
    private List<String> divMarkers = new ArrayList<>();

    /**
     * Если тело статьи является просто ссылкой на другую запись
     * ({@code {{Lbody=NN}}}), здесь хранится NN (entry_no/entry_id родителя).
     * В этом случае читаемый текст, как правило, пуст.
     */
    private String crossReferenceToBody;

    // ---- getters / setters ----

    public String getHomonymMarker() {
        return homonymMarker;
    }

    public void setHomonymMarker(String homonymMarker) {
        this.homonymMarker = homonymMarker;
    }

    public List<LexGender> getLexTags() {
        return lexTags;
    }

    public void setLexTags(List<LexGender> lexTags) {
        this.lexTags = lexTags;
    }

    public List<LexGender> getLexSummary() {
        return lexSummary;
    }

    public void setLexSummary(List<LexGender> lexSummary) {
        this.lexSummary = lexSummary;
    }

    public Map<String, String> getLexCategory() {
        return lexCategory;
    }

    public void setLexCategory(Map<String, String> lexCategory) {
        this.lexCategory = lexCategory;
    }

    public VerbInfo getVerbInfo() {
        return verbInfo;
    }

    public void setVerbInfo(VerbInfo verbInfo) {
        this.verbInfo = verbInfo;
    }

    public PartOfSpeech getPartOfSpeech() {
        return partOfSpeech;
    }

    public void setPartOfSpeech(PartOfSpeech partOfSpeech) {
        this.partOfSpeech = partOfSpeech;
    }

    public List<String> getWestergaardRef() {
        return westergaardRef;
    }

    public void setWestergaardRef(List<String> westergaardRef) {
        this.westergaardRef = westergaardRef;
    }

    public List<String> getWhitneyRootsRef() {
        return whitneyRootsRef;
    }

    public void setWhitneyRootsRef(List<String> whitneyRootsRef) {
        this.whitneyRootsRef = whitneyRootsRef;
    }

    public Map<String, String> getOtherInfoAttributes() {
        return otherInfoAttributes;
    }

    public void setOtherInfoAttributes(Map<String, String> otherInfoAttributes) {
        this.otherInfoAttributes = otherInfoAttributes;
    }

    public boolean isSupplement() {
        return supplement;
    }

    public void setSupplement(boolean supplement) {
        this.supplement = supplement;
    }

    public boolean isRevision() {
        return revision;
    }

    public void setRevision(boolean revision) {
        this.revision = revision;
    }

    public List<String> getSanskritWords() {
        return sanskritWords;
    }

    public void setSanskritWords(List<String> sanskritWords) {
        this.sanskritWords = sanskritWords;
    }

    public Map<String, String> getSanskritProperNames() {
        return sanskritProperNames;
    }

    public void setSanskritProperNames(Map<String, String> sanskritProperNames) {
        this.sanskritProperNames = sanskritProperNames;
    }

    public List<String> getLiterarySources() {
        return literarySources;
    }

    public void setLiterarySources(List<String> literarySources) {
        this.literarySources = literarySources;
    }

    public List<String> getAbbreviations() {
        return abbreviations;
    }

    public void setAbbreviations(List<String> abbreviations) {
        this.abbreviations = abbreviations;
    }

    public Map<String, String> getLocalAbbreviations() {
        return localAbbreviations;
    }

    public void setLocalAbbreviations(Map<String, String> localAbbreviations) {
        this.localAbbreviations = localAbbreviations;
    }

    public List<String> getBotanicalNames() {
        return botanicalNames;
    }

    public void setBotanicalNames(List<String> botanicalNames) {
        this.botanicalNames = botanicalNames;
    }

    public List<String> getBiologicalNames() {
        return biologicalNames;
    }

    public void setBiologicalNames(List<String> biologicalNames) {
        this.biologicalNames = biologicalNames;
    }

    public List<CognateWord> getCognates() {
        return cognates;
    }

    public void setCognates(List<CognateWord> cognates) {
        this.cognates = cognates;
    }

    public List<String> getPageColRefs() {
        return pageColRefs;
    }

    public void setPageColRefs(List<String> pageColRefs) {
        this.pageColRefs = pageColRefs;
    }

    public List<String> getDivMarkers() {
        return divMarkers;
    }

    public void setDivMarkers(List<String> divMarkers) {
        this.divMarkers = divMarkers;
    }

    public String getCrossReferenceToBody() {
        return crossReferenceToBody;
    }

    public void setCrossReferenceToBody(String crossReferenceToBody) {
        this.crossReferenceToBody = crossReferenceToBody;
    }
}
