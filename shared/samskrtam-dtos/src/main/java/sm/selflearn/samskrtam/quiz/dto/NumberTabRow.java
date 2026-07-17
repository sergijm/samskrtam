package sm.selflearn.samskrtam.quiz.dto;

/**
 * Row in the "По числам" (By Number) tab of the declension quiz start page.
 * Each row aggregates progress across all caseType and gender for a single numberType.
 *
 * <p>"Изучено" (learned) = count of (caseType, numberType, gender) combinations
 * with status MASTERED or REVIEW (score >= 90).
 */
public class NumberTabRow {
    private String numberType;
    private String numberRu;
    private String numberEn;
    private int learned;
    private int total;

    public NumberTabRow() {
    }

    public NumberTabRow(String numberType, String numberRu, String numberEn, int learned, int total) {
        this.numberType = numberType;
        this.numberRu = numberRu;
        this.numberEn = numberEn;
        this.learned = learned;
        this.total = total;
    }

    public String getNumberType() { return numberType; }
    public void setNumberType(String numberType) { this.numberType = numberType; }
    public String getNumberRu() { return numberRu; }
    public void setNumberRu(String numberRu) { this.numberRu = numberRu; }
    public String getNumberEn() { return numberEn; }
    public void setNumberEn(String numberEn) { this.numberEn = numberEn; }
    public int getLearned() { return learned; }
    public void setLearned(int learned) { this.learned = learned; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
}
