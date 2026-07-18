package sm.selflearn.samskrtam.quiz.model;

/**
 * Immutable triple for filter combinations: (caseType, numberType, gender).
 * Extracted from QuizSessionService for reuse and to keep files compact.
 */
public record FilterCombination(String caseType, String numberType, String gender)
        implements Comparable<FilterCombination> {

    @Override
    public int compareTo(FilterCombination o) {
        int c = caseType.compareTo(o.caseType);
        if (c != 0) return c;
        c = numberType.compareTo(o.numberType);
        if (c != 0) return c;
        return gender.compareTo(o.gender);
    }
}
