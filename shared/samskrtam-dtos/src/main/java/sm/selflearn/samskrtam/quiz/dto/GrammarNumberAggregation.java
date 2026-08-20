package sm.selflearn.samskrtam.quiz.dto;

/**
 * Агрегация прогресса по числу для вкладки «Прогресс» грамматического урока.
 * Вычисляется на бэкенде (ранее агрегировалось на фронте по lesson.items).
 */
public class GrammarNumberAggregation {
    private String numberType;
    private String numberRu;
    private String numberEn;
    private int aggregatedProgress;
    private int totalCombinations;
    private int learnedCombinations;
    private WordStatus status;

    public GrammarNumberAggregation() {
    }

    public GrammarNumberAggregation(String numberType, String numberRu, String numberEn,
                                    int aggregatedProgress, int totalCombinations,
                                    int learnedCombinations, WordStatus status) {
        this.numberType = numberType;
        this.numberRu = numberRu;
        this.numberEn = numberEn;
        this.aggregatedProgress = aggregatedProgress;
        this.totalCombinations = totalCombinations;
        this.learnedCombinations = learnedCombinations;
        this.status = status;
    }

    public String getNumberType() {
        return numberType;
    }

    public void setNumberType(String numberType) {
        this.numberType = numberType;
    }

    public String getNumberRu() {
        return numberRu;
    }

    public void setNumberRu(String numberRu) {
        this.numberRu = numberRu;
    }

    public String getNumberEn() {
        return numberEn;
    }

    public void setNumberEn(String numberEn) {
        this.numberEn = numberEn;
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