package sm.selflearn.samskrtam.morphology;

public enum NumberType {
    SINGULAR("Единственное", "Singular"),
    DUAL("Двойственное", "Dual"),
    PLURAL("Множественное", "Plural");

    private final String ruName;
    private final String enName;

    NumberType(String ruName, String enName) {
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
