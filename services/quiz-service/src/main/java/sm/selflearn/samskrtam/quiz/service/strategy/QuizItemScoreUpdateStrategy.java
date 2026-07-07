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
     * Определяет externalRefId в зависимости от типа вопроса.
     * Для лексики — vocabularyWordId; для грамматики — целевая caseEnding (vowel_type+gender+case_type+number_type).
     * TODO: При добавлении новых itemType добавить ветку сюда.
     */
    private UUID resolveExternalRefId(GeneratedQuizQuestionDto question) {
        if (question.getVocabularyWordId() != null) {
            return question.getVocabularyWordId();
        }
        if (question.getCaseEndingId() != null) {
            return question.getCaseEndingId();
        }
        if (question.getTargetCase() != null && question.getTargetNumber() != null) {
            // Fallback: генерируем детерминированный UUID из компонентов
            // TODO: В будущем получать от content-service caseEndingId
            return UUID.nameUUIDFromBytes((question.getTargetCase().name()
                    + "_" + (question.getGender() != null ? question.getGender() : "UNSPECIFIED")
                    + "_" + question.getTargetNumber().name()).getBytes());
        }
        return null;
    }

    /**
     * Определяет ItemType по типу вопроса.
     */
    private ItemType resolveItemType(GeneratedQuizQuestionDto question) {
        if (question.getVocabularyWordId() != null) {
            return ItemType.VOCABULARY_WORD;
        }
        return ItemType.DECLENSION_FORM;
    }
}