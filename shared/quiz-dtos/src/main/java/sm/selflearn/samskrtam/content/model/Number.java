package sm.selflearn.samskrtam.content.model;

public enum Number {
    SINGULAR("Единственное", "Singular"),
    DUAL("Двойственное", "Dual"),
    PLURAL("Множественное", "Plural");

    private final String ruName;
    private final String enName;

    Number(String ruName, String enName) {
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
