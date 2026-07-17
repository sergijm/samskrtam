package sm.selflearn.samskrtam.quiz.dto;

/**
 * Row in the "По падежам" (By Case) tab of the declension quiz start page.
 * Each row aggregates progress across all numberType and gender for a single caseType.
 *
 * <p>"Изучено" (learned) = count of (caseType, numberType, gender) combinations
 * with status MASTERED or REVIEW (score >= 90).
 */
public class CaseTabRow {
    private String caseType;
    private String caseRu;
    private String caseEn;
    private int learned;
    private int total;

    public CaseTabRow() {
    }

    public CaseTabRow(String caseType, String caseRu, String caseEn, int learned, int total) {
        this.caseType = caseType;
        this.caseRu = caseRu;
        this.caseEn = caseEn;
        this.learned = learned;
        this.total = total;
    }

    public String getCaseType() { return caseType; }
    public void setCaseType(String caseType) { this.caseType = caseType; }
    public String getCaseRu() { return caseRu; }
    public void setCaseRu(String caseRu) { this.caseRu = caseRu; }
    public String getCaseEn() { return caseEn; }
    public void setCaseEn(String caseEn) { this.caseEn = caseEn; }
    public int getLearned() { return learned; }
    public void setLearned(int learned) { this.learned = learned; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
}
