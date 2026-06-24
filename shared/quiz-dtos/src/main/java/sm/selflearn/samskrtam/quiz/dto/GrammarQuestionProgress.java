package sm.selflearn.samskrtam.quiz.dto;

import java.util.UUID;

public class GrammarQuestionProgress {
    private UUID questionId;
    private String textRu;
    private String textEn;
    private String correctAnswerRu;
    private String correctAnswerEn;
    private int nSuccess;
    private int nAll;
    private float successRate;
    private WordStatus status;

    // Getters and setters
    public UUID getQuestionId() {
        return questionId;
    }

    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
    }

    public String getTextRu() {
        return textRu;
    }

    public void setTextRu(String textRu) {
        this.textRu = textRu;
    }

    public String getTextEn() {
        return textEn;
    }

    public void setTextEn(String textEn) {
        this.textEn = textEn;
    }

    public String getCorrectAnswerRu() {
        return correctAnswerRu;
    }

    public void setCorrectAnswerRu(String correctAnswerRu) {
        this.correctAnswerRu = correctAnswerRu;
    }

    public String getCorrectAnswerEn() {
        return correctAnswerEn;
    }

    public void setCorrectAnswerEn(String correctAnswerEn) {
        this.correctAnswerEn = correctAnswerEn;
    }

    public int getNSuccess() {
        return nSuccess;
    }

    public void setNSuccess(int nSuccess) {
        this.nSuccess = nSuccess;
    }

    public int getNAll() {
        return nAll;
    }

    public void setNAll(int nAll) {
        this.nAll = nAll;
    }

    public float getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(float successRate) {
        this.successRate = successRate;
    }

    public WordStatus getStatus() {
        return status;
    }

    public void setStatus(WordStatus status) {
        this.status = status;
    }
}