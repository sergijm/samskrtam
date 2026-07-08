package sm.selflearn.samskrtam.quiz.service.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.service.QuizItemScoreService;

import java.util.UUID;

/**
 * Единая стратегия обновления score для всех типов квизов.
 *
 * <p>Заменяет {@link VocabularyScoreUpdateStrategy} и {@link GrammarFormScoreUpdateStrategy}.
 * Не ветвится по itemType — использует объединённую логику через {@link QuizItemScoreService}.
 * Отображение {@link LessonType} → {@link ItemType} выполняется здесь.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuizItemScoreUpdateStrategy implements ScoreUpdateStrategy {

    private final QuizItemScoreService quizItemScoreService;

    @Override
    public boolean supports(LessonType lessonType) {
        // Единая стратегия для всех типов
        return true;
    }

    @Override
    public Mono<Void> updateScore(UUID userId, UUID lessonId, GeneratedQuizQuestionDto generatedQuestion, boolean isCorrect) {
        UUID externalRefId = resolveExternalRefId(generatedQuestion);
        if (externalRefId == null) {
            return Mono.empty();
        }

        ItemType itemType = resolveItemType(generatedQuestion);
        return quizItemScoreService.upsertScore(userId, itemType, externalRefId, isCorrect).then();
    }

    /**
     * Определяет externalRefId по itemType + соответствующему полю DTO.
     *
     * <p>При добавлении нового ItemType: добавить case в switch и
     * соответствующее *Id поле в {@link GeneratedQuizQuestionDto}.
     */
    private UUID resolveExternalRefId(GeneratedQuizQuestionDto question) {
        ItemType itemType = resolveItemType(question);
        return switch (itemType) {
            case VOCABULARY_WORD -> question.getVocabularyWordId();
            case DECLENSION_FORM -> question.getCaseEndingId();
        };
    }

    /**
     * Определяет ItemType по явному полю itemType в DTO.
     * Если поле отсутствует (обратная совместимость) — угадывает по *Id полям.
     */
    private ItemType resolveItemType(GeneratedQuizQuestionDto question) {
        if (question.getItemType() != null) {
            try {
                return ItemType.valueOf(question.getItemType());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown itemType from content-service: {}, falling back to heuristics",
                        question.getItemType());
            }
        }
        // Fallback для обратной совместимости (пока content-service не обновлён)
        if (question.getVocabularyWordId() != null) {
            return ItemType.VOCABULARY_WORD;
        }
        if (question.getCaseEndingId() != null) {
            return ItemType.DECLENSION_FORM;
        }
        log.error("Cannot resolve ItemType: no itemType field and no known Id field in question {}",
                question.getId());
        throw new IllegalStateException(
                "Cannot resolve ItemType for question " + question.getId()
                + ": no itemType field and no known Id field");
    }
}

