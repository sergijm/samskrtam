package sm.selflearn.samskrtam.quiz.constants;

/**
 * Единый источник констант для прогресса освоенности.
 * Все пороговые значения — только здесь.
 */
public final class ProgressConstants {

    private ProgressConstants() {
        // утилитный класс
    }

    /**
     * Минимальный процент успешных ответов для статуса MASTERED.
     */
    public static final float MASTERY_THRESHOLD = 80f;

    /**
     * Минимальный процент для статуса LEARNING (грамматика).
     */
    public static final float GRAMMAR_LEARNING_THRESHOLD = 50f;
}