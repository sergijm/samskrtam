package sm.selflearn.samskrtam.quiz.dto;

import java.time.Instant;

/**
 * DTO для статистики по одному слову от R2DBC-запроса.
 * Имена геттеров/сеттеров в camelCase соответствуют snake_case алиасам SQL.
 */
public class WordScoreDto {

    private long totalAttempts;
    private long correctAnswers;
    private Instant lastSeenAt;
    private float successRate;

    public WordScoreDto() {
    }

    public WordScoreDto(long totalAttempts, long correctAnswers, Instant lastSeenAt, float successRate) {
        this.totalAttempts = totalAttempts;
        this.correctAnswers = correctAnswers;
        this.lastSeenAt = lastSeenAt;
        this.successRate = successRate;
    }

    public long getTotalAttempts() {
        return totalAttempts;
    }

    public void setTotalAttempts(long totalAttempts) {
        this.totalAttempts = totalAttempts;
    }

    public long getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(long correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public float getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(float successRate) {
        this.successRate = successRate;
    }
}