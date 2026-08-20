package sm.selflearn.samskrtam.quiz.dto;

import com.fasterxml.jackson.databind.JsonNode;
import sm.selflearn.samskrtam.quest.AnswerMode;

import java.util.List;
import java.util.UUID;

/**
 * Клиентская модель QuestItem от curriculum-service (API v2), см.
 * curriculum-quest-items.md §5. Не путать с серверным DTO curriculum-service.
 * {@code payload} прокидывается дальше как есть (отображение на фронте) — quiz-service
 * не парсит его типизированно.
 *
 * @param id            id вопроса (curriculum.quest_item.id)
 * @param itemType      код QuestItemType (DECLENSION_FORM, ...)
 * @param answerMode    способ проверки ответа (FREE_TEXT, SINGLE_CHOICE, MATCHING, ...)
 * @param prompt        что показываем пользователю
 * @param promptRu      русский вариант текста вопроса (выбирает клиент по своему языку)
 * @param correctAnswer эталонный ответ; {@code null} для MATCHING
 * @param correctAnswerRu русский вариант эталонной метки (CASE_RECOGNITION; иначе {@code null})
 * @param distractors   неверные варианты для SINGLE_CHOICE; пусто для FREE_TEXT/MATCHING
 * @param distractorsRu русские варианты дистракторов (CASE_RECOGNITION; иначе {@code null})
 * @param payload       типоспецифичные данные, прокидываются без разбора
 * @param progressTag   тег прогресса (caseType|numberType|gender или lemmaSlp1)
 */
public record QuestItemDto(
        UUID id,
        String itemType,
        AnswerMode answerMode,
        String prompt,
        String promptRu,
        String correctAnswer,
        String correctAnswerRu,
        List<String> distractors,
        List<String> distractorsRu,
        JsonNode payload,
        String progressTag
) {
}
