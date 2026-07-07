package sm.selflearn.samskrtam.quiz.dto;

import java.util.List;
import java.util.UUID;

public class GrammarLesson {
    private UUID lessonId;
    private String type;
    private String titleRu;
    private String titleEn;
    private String difficulty;
    private int totalQuestions;
    private int learnedQuestions;
    private float progressPercent;
    private LessonStatusSummary statusSummary;
    private List<GrammarQuestionProgress> questions;

    // Getters and setters
    public UUID getLessonId() {
        return lessonId;
    }

    public void setLessonId(UUID lessonId) {
        this.lessonId = lessonId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getLearnedQuestions() {
        return learnedQuestions;
    }

    public void setLearnedQuestions(int learnedQuestions) {
        this.learnedQuestions = learnedQuestions;
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

    public List<GrammarQuestionProgress> getQuestions() {
        return questions;
    }

    public void setQuestions(List<GrammarQuestionProgress> questions) {
        this.questions = questions;
    }
}