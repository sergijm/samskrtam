package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.LessonItemResponse;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.quiz.constants.ProgressConstants;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.repository.QuizItemScoreRepository;

import java.util.UUID;

/**
 * Сервис для работы с прогрессом по грамматическим урокам (declensions).
 * Делегирует построение GrammarLesson в {@link GrammarProgressBuilder}.
 *
 * <p>Статус вопросов вычисляется через единую таблицу quiz_item_score (ADR-007).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GrammarProgressService {

    private final QuizItemScoreRepository quizItemScoreRepository;
    private final GrammarProgressBuilder grammarProgressBuilder;

    /**
     * Обогащает список уроков прогрессом для грамматических уроков.
     */
    public Mono<LessonItemDto> enrichWithProgress(LessonItemResponse lesson, UUID userId) {
        LessonItemDto.LessonItemDtoBuilder builder = LessonItemDto.builder()
                .id(lesson.getId())
                .slug(lesson.getSlug())
                .titleRu(lesson.getTitleRu())
                .titleEn(lesson.getTitleEn())
                .descriptionRu(lesson.getDescriptionRu())
                .descriptionEn(lesson.getDescriptionEn())
                .lessonType(lesson.getLessonType())
                .difficulty(lesson.getDifficulty())
                .totalQuestions(lesson.getTotalQuestions())
                .totalWordsOwn(lesson.getWordCount());

        if (userId != null && !LessonType.isVocabulary(lesson.getLessonType())) {
            return quizItemScoreRepository.countLearnedItems(
                            userId, ItemType.DECLENSION_FORM, ProgressConstants.MASTERED_LOWER_THRESHOLD)
                    .map(count -> builder
                            .learnedWords(count.intValue())
                            .totalWordsOwn(count.intValue())
                            .build());
        }

        return Mono.just(builder
                .totalWordsOwn(0)
                .learnedWords(0)
                .build());
    }

    /**
     * Строит GrammarLesson с прогрессом по каждой уникальной комбинации
     * (gender, caseType, numberType) в рамках урока.
     * Делегирует в {@link GrammarProgressBuilder#build(String, UUID)}.
     */
    public Mono<GrammarLesson> getGrammarLesson(String slug, UUID userId) {
        return grammarProgressBuilder.build(slug, userId);
    }
}

