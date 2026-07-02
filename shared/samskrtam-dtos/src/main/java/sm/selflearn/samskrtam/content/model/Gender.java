package sm.selflearn.samskrtam.content.model;

public enum Gender {
    MASCULINE("Мужской", "Masculine"),
    FEMININE("Женский", "Feminine"),
    NEUTER("Средний", "Neuter"),
    UNKNOWN("Неизвестный", "Unknown"),
    UNSPECIFIED("Не указан", "Unspecified");

    private final String ruName;
    private final String enName;

    Gender(String ruName, String enName) {
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

