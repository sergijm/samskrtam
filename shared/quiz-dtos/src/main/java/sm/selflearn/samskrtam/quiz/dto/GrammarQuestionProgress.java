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
    private String caseEnding;
    private String caseType;
    private String caseRu;
    private String caseEn;
    private String numberType;
    private String numberRu;
    private String numberEn;
    private String gender;
    private String genderRu;
    private String genderEn;

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

    public String getCaseEnding() {
        return caseEnding;
    }

    public void setCaseEnding(String caseEnding) {
        this.caseEnding = caseEnding;
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getGenderRu() {
        return genderRu;
    }

    public void setGenderRu(String genderRu) {
        this.genderRu = genderRu;
    }

    public String getGenderEn() {
        return genderEn;
    }

    public void setGenderEn(String genderEn) {
        this.genderEn = genderEn;
    }
}