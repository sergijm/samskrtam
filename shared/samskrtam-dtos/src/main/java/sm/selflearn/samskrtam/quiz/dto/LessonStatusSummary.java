package sm.selflearn.samskrtam.quiz.dto;

/**
 * Агрегация статусов элементов урока (NEW, LEARNING, MASTERED, REVIEW, total).
 * Вычисляется за один проход через QuizItemScoreRepository.
 */
public class LessonStatusSummary {
    private int total;
    private int newCount;
    private int learning;
    private int mastered;
    private int reviewDue;

    public LessonStatusSummary() {
    }

    public LessonStatusSummary(int total, int newCount, int learning, int mastered, int reviewDue) {
        this.total = total;
        this.newCount = newCount;
        this.learning = learning;
        this.mastered = mastered;
        this.reviewDue = reviewDue;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getNewCount() {
        return newCount;
    }

    public void setNewCount(int newCount) {
        this.newCount = newCount;
    }

    public int getLearning() {
        return learning;
    }

    public void setLearning(int learning) {
        this.learning = learning;
    }

    public int getMastered() {
        return mastered;
    }

    public void setMastered(int mastered) {
        this.mastered = mastered;
    }

    public int getReviewDue() {
        return reviewDue;
    }

    public void setReviewDue(int reviewDue) {
        this.reviewDue = reviewDue;
    }
}