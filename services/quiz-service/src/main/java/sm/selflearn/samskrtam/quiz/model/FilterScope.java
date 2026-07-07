package sm.selflearn.samskrtam.quiz.model;

/**
 * Scope of filtering applied to a quiz session.
 * See: docs/quizzes/quiz-declension.md §3.4
 */
public enum FilterScope {
    /**
     * Filter only by case type (from the "By Case" tab).
     * filterNumberType and filterGender are null.
     */
    CASE_ONLY,

    /**
     * Detailed filter by case type, number type, and gender (from the "Details" tab).
     * filterNumberType and filterGender are both set.
     */
    CASE_NUMBER_GENDER
}