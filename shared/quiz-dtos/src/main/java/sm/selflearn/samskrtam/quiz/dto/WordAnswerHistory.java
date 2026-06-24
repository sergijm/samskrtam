package sm.selflearn.samskrtam.quiz.dto;

import java.util.List;
import java.util.UUID;

public class WordAnswerHistory {
    private UUID wordId;
    private String word;
    private UUID quizId;
    private List<AnswerHistoryEntry> entries;
    private int page;
    private int size;
    private int total;

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