package sm.selflearn.samskrtam.quiz.model;

/**
 * Тип элемента квиза.
 * Инвариант: генератор generate() не ветвится по itemType — рендеринг вопроса делегируется
 * внешнему модулю после того, как генератор отобрал список QuizItem для сессии.
 *
 * <p>При добавлении нового типа:
 * <ol>
 *   <li>Добавить значение сюда</li>
 *   <li>Реализовать модуль рендеринга по (itemType, externalRefId)</li>
 *   <li>Обеспечить content-side выдачу списка externalRefId под scope (§2.3 спеки)</li>
 * </ol>
 *
 * @see sm.selflearn.samskrtam.quiz.model.QuizItemScore
 */
public enum ItemType {

    /**
     * Лексические слова: externalRefId → content.vocabulary_words.id
     */
    VOCABULARY_WORD,

    /**
     * Грамматические формы (склонения/спряжения):
     * externalRefId → content.case_endings.id (эталонная связка vowel_type+gender+case_type+number_type).
     * Прогресс общий для всех основ с одинаковым сочетанием.
     */
    DECLENSION_FORM
    // CONJUGATION_FORM, PRONOUN_FORM — для будущих типов
}