package sm.selflearn.samskrtam.quiz.dto;

public class TagSetProgress {
    private String setId;
    private String labelRu;
    private String labelEn;
    private int aggregatedProgress;
    private int totalCombinations;
    private int learnedCombinations;
    private String status;

    public String getSetId() {
        return setId;
    }

    public void setSetId(String setId) {
        this.setId = setId;
    }

    public String getLabelRu() {
        return labelRu;
    }

    public void setLabelRu(String labelRu) {
        this.labelRu = labelRu;
    }

    public String getLabelEn() {
        return labelEn;
    }

    public void setLabelEn(String labelEn) {
        this.labelEn = labelEn;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}