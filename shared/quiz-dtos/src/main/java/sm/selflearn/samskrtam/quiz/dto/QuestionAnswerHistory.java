package sm.selflearn.samskrtam.quiz.dto;

import java.util.List;
import java.util.UUID;

public class QuestionAnswerHistory {
    private UUID questionId;
    private String textRu;
    private UUID quizId;
    private List<AnswerHistoryEntry> entries;
    private int page;
    private int size;
    private int total;

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

    public UUID getQuizId() {
        return quizId;
    }

    public void setQuizId(UUID quizId) {
        this.quizId = quizId;
    }

    public List<AnswerHistoryEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<AnswerHistoryEntry> entries) {
        this.entries = entries;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}