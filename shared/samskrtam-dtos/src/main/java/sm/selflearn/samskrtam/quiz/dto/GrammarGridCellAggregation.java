package sm.selflearn.samskrtam.quiz.dto;

/**
 * Агрегация прогресса по комбинации (caseType, numberType) — ячейка сетки
 * падеж × число вкладки «Прогресс» грамматического урока.
 * Вычисляется на бэкенде (ранее агрегировалось на фронте по lesson.items).
 */
public class GrammarGridCellAggregation {
    private String caseType;
    private String numberType;
    private int aggregatedProgress;
    private int totalCombinations;
    private int learnedCombinations;
    private WordStatus status;

    public GrammarGridCellAggregation() {
    }

    public GrammarGridCellAggregation(String caseType, String numberType,
                                      int aggregatedProgress, int totalCombinations,
                                      int learnedCombinations, WordStatus status) {
        this.caseType = caseType;
        this.numberType = numberType;
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

    public String getNumberType() {
        return numberType;
    }

    public void setNumberType(String numberType) {
        this.numberType = numberType;
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