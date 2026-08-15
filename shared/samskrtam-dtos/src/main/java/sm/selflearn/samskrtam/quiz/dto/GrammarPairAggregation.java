package sm.selflearn.samskrtam.quiz.dto;

/**
 * Агрегация прогресса по семантической паре падежей (progress-tag set slice,
 * напр. GEN_LOC = Genitive ↔ Locative) для вкладки «Прогресс» грамматического урока.
 * setId совпадает с {@code ProgressTagSetId} (пары падежей) —
 * по нему фронт запускает квиз по срезу progressTagSetId.
 * Вычисляется на бэкенде (ранее агрегировалось на фронте по lesson.items).
 */
public class GrammarPairAggregation {
    private String setId;
    private String caseTypeA;
    private String caseTypeB;
    private String caseRuA;
    private String caseRuB;
    private String caseEnA;
    private String caseEnB;
    private int aggregatedProgress;
    private int totalCombinations;
    private int learnedCombinations;
    private WordStatus status;

    public GrammarPairAggregation() {
    }

    public GrammarPairAggregation(String setId, String caseTypeA, String caseTypeB,
                                  String caseRuA, String caseRuB, String caseEnA, String caseEnB,
                                  int aggregatedProgress, int totalCombinations,
                                  int learnedCombinations, WordStatus status) {
        this.setId = setId;
        this.caseTypeA = caseTypeA;
        this.caseTypeB = caseTypeB;
        this.caseRuA = caseRuA;
        this.caseRuB = caseRuB;
        this.caseEnA = caseEnA;
        this.caseEnB = caseEnB;
        this.aggregatedProgress = aggregatedProgress;
        this.totalCombinations = totalCombinations;
        this.learnedCombinations = learnedCombinations;
        this.status = status;
    }

    public String getSetId() {
        return setId;
    }

    public void setSetId(String setId) {
        this.setId = setId;
    }

    public String getCaseTypeA() {
        return caseTypeA;
    }

    public void setCaseTypeA(String caseTypeA) {
        this.caseTypeA = caseTypeA;
    }

    public String getCaseTypeB() {
        return caseTypeB;
    }

    public void setCaseTypeB(String caseTypeB) {
        this.caseTypeB = caseTypeB;
    }

    public String getCaseRuA() {
        return caseRuA;
    }

    public void setCaseRuA(String caseRuA) {
        this.caseRuA = caseRuA;
    }

    public String getCaseRuB() {
        return caseRuB;
    }

    public void setCaseRuB(String caseRuB) {
        this.caseRuB = caseRuB;
    }

    public String getCaseEnA() {
        return caseEnA;
    }

    public void setCaseEnA(String caseEnA) {
        this.caseEnA = caseEnA;
    }

    public String getCaseEnB() {
        return caseEnB;
    }

    public void setCaseEnB(String caseEnB) {
        this.caseEnB = caseEnB;
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