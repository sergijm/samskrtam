package sm.selflearn.samskrtam.quiz.model;

/**
 * Ручной фильтр по бакету прогресса для старта квиза с LessonPage.
 * Передаётся опциональным параметром в {@code startSession}/{@code startOrResumeSession}.
 *
 * <p>Соответствие бейджам LessonStatsBadges (frontend):
 * <ul>
 *   <li>«Изучено» ({mastered}/{total}) → {@link #REVIEW}</li>
 *   <li>«Не изучено»/«Новые» ({newCount}) → {@link #NEW}</li>
 *   <li>«В процессе» ({learning}) → {@link #LEARNING}</li>
 * </ul>
 *
 * <p>Применим к любому {@link ItemType} (VOCABULARY_WORD, DECLENSION_FORM).
 * Независим от {@link FilterScope}, который фильтрует по грамматическому признаку.
 *
 * @see <a href="docs/services/quiz-service/quiz-generator-spec.md#3-quizgeneratorconfig--параметры-отбора">Спецификация §3</a>
 */
public enum StatusFilter {

    /**
     * Все единицы без строки в quiz_item_score.
     * Пул = все externalRefId урока минус уже существующие строки score.
     */
    NEW,

    /**
     * Единицы с существующей строкой quiz_item_score, чей бакет LEARNING или DIFFICULT.
     * nextReviewAt не учитывается.
     */
    LEARNING,

    /**
     * Единицы бакета MASTERED с nextReviewAt ≤ текущее время.
     * Эквивалент существующего findDueItems.
     */
    REVIEW
}