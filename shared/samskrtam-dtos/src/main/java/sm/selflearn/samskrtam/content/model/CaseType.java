package sm.selflearn.samskrtam.content.model;

public enum CaseType {
    NOMINATIVE("Именительный", "Nominative"),
    ACCUSATIVE("Винительный", "Accusative"),
    INSTRUMENTAL("Творительный", "Instrumental"),
    DATIVE("Дательный", "Dative"),
    ABLATIVE("Отложительный", "Ablative"),
    GENITIVE("Родительный", "Genitive"),
    LOCATIVE("Местный", "Locative"),
    VOCATIVE("Звательный", "Vocative");

    private final String ruName;
    private final String enName;

    CaseType(String ruName, String enName) {
        this.ruName = ruName;
        this.enName = enName;
    }

    public String getRuName() {
        return ruName;
    }

    public String getEnName() {
        return enName;
    }
}
