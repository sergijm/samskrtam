package sm.selflearn.samskrtam.morphology;

public enum Pada {
    PARASMAIPADA("активный залог", "active voice"),
    ATMANEPADA("средний залог", "middle voice");

    private final String ruName;
    private final String enName;

    Pada(String ruName, String enName) {
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
