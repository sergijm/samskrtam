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
 * <p>Единая стратегия обновления score для всех типов квизов.
 * Не ветвится по itemType — использует объединённую логику через {@link QuizItemScoreService}.
 * Отображение {@link LessonType} → {@link ItemType} выполняется здесь.
 * Progress key — progressTag (String), вычисляется из полей DTO.
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
        String progressTag = resolveProgressTag(generatedQuestion);
        if (progressTag == null) {
            return Mono.empty();
        }

        ItemType itemType = resolveItemType(generatedQuestion);
        return quizItemScoreService.upsertScore(userId, itemType, progressTag, isCorrect).then();
    }

    /**
     * Определяет progressTag по itemType + соответствующим полям DTO.
     * Для declension: caseType|numberType|gender
     * Для vocabulary: correctFormIast (lemma)
     */
    private String resolveProgressTag(GeneratedQuizQuestionDto question) {
        ItemType itemType = resolveItemType(question);
        return switch (itemType) {
            case DECLENSION_FORM -> {
                String caseType = question.getCaseType() != null ? question.getCaseType()
                        : (question.getTargetCase() != null ? question.getTargetCase().name() : null);
                String numberType = question.getNumberType() != null ? question.getNumberType()
                        : (question.getTargetNumber() != null ? question.getTargetNumber().name() : null);
                String gender = question.getGender() != null ? question.getGender() : "UNSPECIFIED";
                yield caseType != null && numberType != null
                        ? caseType + "|" + numberType + "|" + gender
                        : null;
            }
            case CONJUGATION_FORM -> {
                // conjugation progress tags are not used in legacy flow
                yield null;
            }
            case VOCABULARY_WORD -> question.getCorrectFormIast();
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
        // Fallback для обратной совместимости
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