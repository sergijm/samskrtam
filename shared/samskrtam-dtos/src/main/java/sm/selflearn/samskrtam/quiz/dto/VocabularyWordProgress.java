package sm.selflearn.samskrtam.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class VocabularyWordProgress {
    private UUID wordId;
    private String word;
    private String wordDevanagari;
    private String translationRu;
    private String translationEn;
    @JsonProperty("nSuccess")
    private int nSuccess;

    @JsonProperty("nAll")
    private int nAll;    private float successRate;
    private WordStatus status;

    // Getters and setters
    public UUID getWordId() {
        return wordId;
    }

    public void setWordId(UUID wordId) {
        this.wordId = wordId;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getWordDevanagari() {
        return wordDevanagari;
    }

    public void setWordDevanagari(String wordDevanagari) {
        this.wordDevanagari = wordDevanagari;
    }

    public String getTranslationRu() {
        return translationRu;
    }

    public void setTranslationRu(String translationRu) {
        this.translationRu = translationRu;
    }

    public String getTranslationEn() {
        return translationEn;
    }

    public void setTranslationEn(String translationEn) {
        this.translationEn = translationEn;
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