package sm.selflearn.samskrtam.samcli.model;

public class MwEntry {

    private final String entryId;
    private final String pageCol;
    private final String key1;
    private final String key2;
    private final String homonym;
    private final String entryNo;
    private String body;
    private String grammarJson;
    private String cleanText;

    public MwEntry(String entryId, String pageCol, String key1, String key2,
                  String homonym, String entryNo) {
        this.entryId = entryId;
        this.pageCol = pageCol;
        this.key1 = key1;
        this.key2 = key2;
        this.homonym = homonym;
        this.entryNo = entryNo;
    }

    public String getEntryId() {
        return entryId;
    }

    public String getPageCol() {
        return pageCol;
    }

    public String getKey1() {
        return key1;
    }

    public String getKey2() {
        return key2;
    }

    public String getHomonym() {
        return homonym;
    }

    public String getEntryNo() {
        return entryNo;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getGrammarJson() {
        return grammarJson;
    }

    public void setGrammarJson(String grammarJson) {
        this.grammarJson = grammarJson;
    }

    public String getCleanText() {
        return cleanText;
    }

    public void setCleanText(String cleanText) {
        this.cleanText = cleanText;
    }
}
