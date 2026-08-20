package sm.selflearn.samskrtam.curriculum.lexicon.dto;

import sm.selflearn.samskrtam.curriculum.lexicon.model.VocabularyQuizKind;

import java.util.UUID;

/**
 * Создание/обновление определения вок. викторины (task-curriculum-16 §10).
 * Ровно одно из {topicId, complexQuizId, frequencyRankMax}
 * должно быть заполнено.
 */
public record VocabularyQuizDefinitionUpsertRequest(
        VocabularyQuizKind kind,
        String titleRu,
        String titleEn,
        UUID topicId,
        UUID complexQuizId,
        Integer frequencyRankMax
) {
}