package sm.selflearn.samskrtam.quiz.dto;

import java.util.List;
import java.util.UUID;

public class VocabularyLessonDto {
    private UUID lessonId;
    private String slug;
    private String titleRu;
    private String titleEn;
    private String difficulty;
    private int totalWords;
    private int learnedWords;
    private float progressPercent;
    private LessonStatusSummary statusSummary;
    private List<VocabularyWordProgress> words;

    // Getters and setters
    public UUID getLessonId() {
        return lessonId;
    }

    public void setLessonId(UUID lessonId) {
        this.lessonId = lessonId;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitleRu() {
        return titleRu;
    }

    public void setTitleRu(String titleRu) {
        this.titleRu = titleRu;
    }

    public String getTitleEn() {
        return titleEn;
    }

    public void setTitleEn(String titleEn) {
        this.titleEn = titleEn;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public int getTotalWords() {
        return totalWords;
    }

    public void setTotalWords(int totalWords) {
        this.totalWords = totalWords;
    }

    public int getLearnedWords() {
        return learnedWords;
    }

    public void setLearnedWords(int learnedWords) {
        this.learnedWords = learnedWords;
    }

        public float getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(float progressPercent) {
        this.progressPercent = progressPercent;
    }

    public LessonStatusSummary getStatusSummary() {
        return statusSummary;
    }

    public void setStatusSummary(LessonStatusSummary statusSummary) {
        this.statusSummary = statusSummary;
    }

    public List<VocabularyWordProgress> getWords() {
        return words;
    }

    public void setWords(List<VocabularyWordProgress> words) {
        this.words = words;
    }
}