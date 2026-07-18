package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.LessonItemResponse;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.quiz.constants.ProgressConstants;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.repository.QuizItemScoreRepository;

import java.util.List;
import java.util.UUID;
/**
 * Фасад для работы с прогрессом по словарным урокам (vocabulary).
 * Делегирует построение {@link VocabularyLessonDto} в {@link VocabularyLessonBuilder},
 * историю ответов — в {@link WordAnswerHistoryBuilder}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VocabularyProgressService {

    private final QuizItemScoreRepository quizItemScoreRepository;
    private final VocabularyLessonBuilder vocabularyLessonBuilder;
    private final WordAnswerHistoryBuilder wordAnswerHistoryBuilder;

    /**
     * Обогащает список уроков прогрессом для vocabulary-уроков.
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

                if (userId != null && LessonType.isVocabulary(lesson.getLessonType())) {
            return quizItemScoreRepository.countLearnedItems(
                            userId, ItemType.VOCABULARY_WORD, ProgressConstants.MASTERED_LOWER_THRESHOLD)
                    .map(learnedCount -> builder
                            .learnedWords(learnedCount.intValue())
                            .build());
        }

        return Mono.just(builder
                .totalWordsOwn(0)
                .learnedWords(0)
                .build());
    }

        /**
     * Создаёт {@link VocabularyLessonDto} с прогрессом по каждому слову.
     */
    public Mono<VocabularyLessonDto> createVocabularyLesson(
            LessonItemResponse lessonItem,
            List<VocabularyWordDto> vocabularyWords,
            UUID userId) {
        return vocabularyLessonBuilder.build(lessonItem, vocabularyWords, userId);
    }

    /**
     * Создаёт {@link WordAnswerHistory} для конкретного слова.
     */
    public Mono<WordAnswerHistory> createWordAnswerHistory(
            UUID wordId, UUID lessonId, String wordIast,
            UUID userId, org.springframework.data.domain.Pageable pageable) {
        return wordAnswerHistoryBuilder.build(wordId, lessonId, wordIast, userId, pageable);
    }
}
