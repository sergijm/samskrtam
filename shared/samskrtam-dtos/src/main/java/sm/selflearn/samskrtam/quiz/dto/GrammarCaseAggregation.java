package sm.selflearn.samskrtam.quiz.dto;

/**
 * Агрегация прогресса по падежу для вкладки «Прогресс» грамматического урока.
 * Вычисляется на бэкенде (ранее агрегировалось на фронте по lesson.items).
 */
public class GrammarCaseAggregation {
    private String caseType;
    private String caseRu;
    private String caseEn;
    private int aggregatedProgress;
    private int totalCombinations;
    private int learnedCombinations;
    private WordStatus status;

    public GrammarCaseAggregation() {
    }

    public GrammarCaseAggregation(String caseType, String caseRu, String caseEn,
                                  int aggregatedProgress, int totalCombinations,
                                  int learnedCombinations, WordStatus status) {
        this.caseType = caseType;
        this.caseRu = caseRu;
        this.caseEn = caseEn;
        this.aggregatedProgress = aggregatedProgress;
        this.totalCombinations = totalCombinations;
        this.learnedCombinations = learnedCombinations;
        this.status = status;
    }

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public String getCaseRu() {
        return caseRu;
    }

    public void setCaseRu(String caseRu) {
        this.caseRu = caseRu;
    }

    public String getCaseEn() {
        return caseEn;
    }

    public void setCaseEn(String caseEn) {
        this.caseEn = caseEn;
    }

    public int getAggregatedProgress() {
        return aggregatedProgress;
    }

    public void setAggregatedProgress(int aggregatedProgress) {
        this.aggregatedProgress = aggregatedProgress;
    }

    public int getTotalCombinations() {
        return totalCombinations;
    }

    public void setTotalCombinations(int totalCombinations) {
        this.totalCombinations = totalCombinations;
    }

    public int getLearnedCombinations() {
        return learnedCombinations;
    }

    public void setLearnedCombinations(int learnedCombinations) {
        this.learnedCombinations = learnedCombinations;
    }

    public WordStatus getStatus() {
        return status;
    }

    public void setStatus(WordStatus status) {
        this.status = status;
    }
}