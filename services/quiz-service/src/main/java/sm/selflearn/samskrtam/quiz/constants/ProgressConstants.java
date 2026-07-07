package sm.selflearn.samskrtam.quiz.constants;

/**
 * Единый источник констант для прогресса освоенности.
 * Все пороговые значения — только здесь.
 *
 * <p>Значения обновлены в рамках перехода на единую таблицу quiz_item_score (ADR-007).
 * Заменяет старые MASTERY_THRESHOLD (80) и GRAMMAR_LEARNING_THRESHOLD (50) одним порогом.
 *
 * @see sm.selflearn.samskrtam.quiz.model.QuizItemScore
 */
public final class ProgressConstants {

    private ProgressConstants() {
        // утилитный класс
    }

    /**
     * Единый порог MASTERED для всех типов элементов (VOCABULARY_WORD и DECLENSION_FORM).
     * score >= 90 считается освоенным (MASTERED).
     */
    public static final int MASTERED_LOWER_THRESHOLD = 90;
}