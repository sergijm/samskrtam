package sm.selflearn.samskrtam.quiz.model;

/**
 * Тип элемента квиза.
 * Инвариант: генератор generate() не ветвится по itemType — рендеринг вопроса делегируется
 * внешнему модулю после того, как генератор отобрал список QuizItem для сессии.
 *
 * <p>При добавлении нового типа:
 * <ol>
 *   <li>Добавить значение сюда</li>
 *   <li>Добавить соответствующее *Id поле и заполнение itemType в {@code GeneratedQuizQuestionDto}</li>
 *   <li>Добавить case в switch-выражения:
 *     <ul>
 *       <li>{@code QuizItemScoreUpdateStrategy.resolveExternalRefId()}</li>
 *       <li>{@code QuizItemScoreUpdateStrategy.resolveItemType()}</li>
 *       <li>{@code SessionCreationService.filterAndSaveQuestions()}</li>
 *     </ul>
 *   </li>
 *   <li>Реализовать модуль рендеринга по (itemType, externalRefId)</li>
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
     * Грамматические формы склонения:
     * externalRefId → content.case_endings.id.
     */
    DECLENSION_FORM,

    /**
     * Грамматические формы спряжения: progress_tag = voice|person|numberType.
     */
    CONJUGATION_FORM
}