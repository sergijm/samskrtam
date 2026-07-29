package sm.selflearn.samskrtam.quiz.model;

/**
 * Scope of filtering applied to a quiz session.
 * See: docs/services/quiz-service/quiz-declension.md §3.4
 */
public enum FilterScope {
    /**
     * Filter only by case type (from the "By Case" tab).
     * filterCaseTypes is non-empty, filterNumberTypes/filterCombinations are null.
     */
    CASE_ONLY,

    /**
     * Filter only by number type (from the "By Number" tab).
     * filterNumberTypes is non-empty, filterCaseTypes/filterCombinations are null.
     */
    NUMBER_ONLY,

        /**
     * Detailed filter by combinations of case type, number type, and gender (from the "Details" tab).
     * filterCombinations is non-empty, filterCaseTypes/filterNumberTypes are null.
     */
    CASE_NUMBER_GENDER,

    /**
     * Cross-lesson filter for the virtual "declensions-all" lesson (see quiz-declension.md §5).
     * Uses filterVowelTypes, filterGenders, and filterNumberTypes together.
     * filterCaseTypes and filterCombinations are not used with this scope.
     */
    ALL_STEMS
}