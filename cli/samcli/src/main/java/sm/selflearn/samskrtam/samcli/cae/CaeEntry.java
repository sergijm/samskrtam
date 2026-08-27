package sm.selflearn.samskrtam.samcli.cae;

/**
 * Плоское представление одной записи cae.txt, готовое к вставке
 * в целевую таблицу импорта. Аналог {@code model.MwEntry} для cae.
 */
public class CaeEntry {

    private long id;
    private int page;
    private Integer homonymNum;
    private String entryVariant;
    private String headwordPlain;
    private String headwordAccented;
    private String rawText;
    private String cleanText;
    private String gloss;
    private CaeGrammarInfo grammar;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public Integer getHomonymNum() {
        return homonymNum;
    }

    public void setHomonymNum(Integer homonymNum) {
        this.homonymNum = homonymNum;
    }

    public String getEntryVariant() {
        return entryVariant;
    }

    public void setEntryVariant(String entryVariant) {
        this.entryVariant = entryVariant;
    }

    public String getHeadwordPlain() {
        return headwordPlain;
    }

    public void setHeadwordPlain(String headwordPlain) {
        this.headwordPlain = headwordPlain;
    }

    public String getHeadwordAccented() {
        return headwordAccented;
    }

    public void setHeadwordAccented(String headwordAccented) {
        this.headwordAccented = headwordAccented;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getCleanText() {
        return cleanText;
    }

    public void setCleanText(String cleanText) {
        this.cleanText = cleanText;
    }

    public String getGloss() {
        return gloss;
    }

    public void setGloss(String gloss) {
        this.gloss = gloss;
    }

    public CaeGrammarInfo getGrammar() {
        return grammar;
    }

    public void setGrammar(CaeGrammarInfo grammar) {
        this.grammar = grammar;
    }
}
