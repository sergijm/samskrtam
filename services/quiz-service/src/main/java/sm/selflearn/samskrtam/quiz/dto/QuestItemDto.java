package sm.selflearn.samskrtam.quiz.dto;

import com.fasterxml.jackson.databind.JsonNode;

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
 * @param correctAnswer эталонный ответ; {@code null} для MATCHING
 * @param distractors   неверные варианты для SINGLE_CHOICE; пусто для FREE_TEXT/MATCHING
 * @param payload       типоспецифичные данные, прокидываются без разбора
 */
public record QuestItemDto(
        UUID id,
        String itemType,
        String answerMode,
        String prompt,
        String correctAnswer,
        List<String> distractors,
        JsonNode payload
) {
}
