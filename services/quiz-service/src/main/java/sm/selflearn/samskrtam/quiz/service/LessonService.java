package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.*;
import sm.selflearn.samskrtam.quiz.dto.*;

import java.util.Locale;
import java.util.UUID;




import sm.selflearn.samskrtam.quiz.dto.AnswerHistoryEntry;
import sm.selflearn.samskrtam.quiz.model.QuizAnswer;


@Service
@RequiredArgsConstructor
@Slf4j
public class LessonService {

    private final ContentClient contentClient;
    private final QuizDataAssembler quizDataAssembler;
    private final UserSessionService userSessionService;

    public Mono<VocabularyLessonDto> getVocabularyLesson(String slug, UUID userId) {
        // Get the quiz summary to get basic information
        return contentClient.getLessonItemBySlug(slug)
                        .flatMap(lessonSummary -> {
                            return contentClient.getVocabularyWordsForLesson(lessonSummary.getId(), 100000 )
                                    .flatMap(vocabularyWords -> {
                                        // Get user progress for the lesson
                                        QuizProgressDto progress = userSessionService.getUserQuizProgress(userId, lessonSummary.getId());
                                        return createVocabularyLesson(lessonSummary, vocabularyWords, progress);
                            });
                });
    }

    public Mono<GrammarLesson> getGrammarLesson(LessonType type, UUID userId) {
        // For now, we'll return a generic empty lesson until we get full functionality
        return Mono.empty();
    }

    public Mono<WordAnswerHistory> getWordAnswerHistory(String slug, UUID wordId, UUID userId, Pageable pageable, Locale locale) {
        // Get quiz summary to get quiz id from slug
        return contentClient.getLessonItemBySlug(slug)
                        .flatMap(lessonSummary -> {
                            return contentClient.getVocabularyWordById(wordId)
                                    .flatMap(vocabularyWord -> createWordAnswerHistory(
                                            wordId, lessonSummary.getId(),
                                            vocabularyWord.getWordIast(),
                                            userId, pageable, locale));
                        })
                        .switchIfEmpty(Mono.empty());
    }

    public Mono<QuestionAnswerHistory> getQuestionAnswerHistory(String type, UUID questionId, UUID userId, Pageable pageable, Locale locale) {
        // This is a placeholder implementation. In a real system, we would need to get
        // question details and answer history from a data source or service
        // For now, we'll return a minimal implementation without real question data
        QuestionAnswerHistory history = new QuestionAnswerHistory();
        history.setQuestionId(questionId);
        history.setTextRu("Question details not available");
        history.setLessonId(null);
        history.setEntries(java.util.Collections.emptyList());
        history.setPage(pageable.getPageNumber());
        history.setSize(pageable.getPageSize());
        history.setTotal(0); // Total would come from data source in a real implementation
        
        return Mono.just(history);
    }

    private Mono<VocabularyLessonDto> createVocabularyLesson(LessonItemResponse lessonItem,
                                                             java.util.List<VocabularyWordDto> vocabularyWords,
                                                             QuizProgressDto progress) {
            VocabularyLessonDto lesson = new VocabularyLessonDto();
            lesson.setLessonId(lessonItem.getId());
            lesson.setSlug(lessonItem.getSlug());
            lesson.setTitleRu(lessonItem.getTitleRu());
            lesson.setTitleEn(lessonItem.getTitleEn());
            lesson.setDifficulty(lessonItem.getDifficulty().toString());

        // Calculate progress statistics
        lesson.setTotalWords(vocabularyWords.size());
        //lesson.setLearnedWords(progress.getLearnedWords());
        if (lesson.getTotalWords() > 0) {
            lesson.setProgressPercent((float) lesson.getLearnedWords() / lesson.getTotalWords() * 100);
        } else {
            lesson.setProgressPercent(0);
        }

        // Create vocabulary word progress items
        java.util.List<VocabularyWordProgress> wordProgressList = vocabularyWords.stream()
                .map(word -> {
                    VocabularyWordProgress progressItem = new VocabularyWordProgress();
                    progressItem.setWordId(word.getId());
                    progressItem.setWord(word.getWordIast());
                    progressItem.setWordDevanagari(word.getWordDevanagari());
                    progressItem.setTranslationRu(word.getTranslationRu());
                    progressItem.setTranslationEn(word.getTranslationEn());

                    // Реальный расчет рейтинга на основе данных из quiz-service
                    try {
                        // Здесь будет логика получения статистики по каждому слову
                        // Это должно быть реализовано через вызов статистического сервиса или БД

                        // Пример реальных значений (настоящая логика):
                        int nAll = 0;  // Общее количество попыток
                        int nSuccess = 0;  // Количество успешных попыток

                        // В реальной реализации вызов к сервису статистики будет выглядеть так:
                        // WordStatisticsDto stats = userSessionService.getWordStatistics(userId, quizId, word.getId());
                        // nAll = stats.getNAll();
                        // nSuccess = stats.getNSuccess();


                        progressItem.setNSuccess(nSuccess);
                        progressItem.setNAll(nAll);
                        //progressItem.setSuccessRate(successRate);

                        // Определение статуса по рейтингу


                    } catch (Exception e) {
                        // Обработка ошибок: возвращаем значения по умолчанию
                        progressItem.setNSuccess(0);
                        progressItem.setNAll(0);
                        progressItem.setSuccessRate(0);
                        progressItem.setStatus(WordStatus.NEW);
                    }

                    return progressItem;
                })
                .collect(java.util.stream.Collectors.toList());
        lesson.setWords(wordProgressList);
        return Mono.just(lesson);
    }

                    private Mono<WordAnswerHistory> createWordAnswerHistory(
            UUID wordId, UUID lessonId,
            String wordIast, UUID userId, Pageable pageable, Locale locale) {

        Mono<java.util.List<QuizAnswer>> answersMono =
                userSessionService.getWordAnswers(userId, wordId, lessonId);
        Mono<Long> totalMono =
                userSessionService.countWordAnswers(userId, wordId, lessonId);

        return Mono.zip(answersMono, totalMono)
                .map(tuple -> {
                    java.util.List<QuizAnswer> answers = tuple.getT1();
                    long total = tuple.getT2();

                    java.util.List<AnswerHistoryEntry> entries = answers.stream()
                            .map(qa -> AnswerHistoryEntry.builder()
                                    .answeredAt(qa.getAnsweredAt() != null
                                            ? java.time.LocalDateTime.ofInstant(qa.getAnsweredAt(), java.time.ZoneOffset.UTC)
                                            : null)
                                    .correctAnswer(qa.getCorrectFormIast())
                                    .userAnswer(qa.getSelectedFormIast())
                                    .isCorrect(Boolean.TRUE.equals(qa.getIsCorrect()))
                                    .build())
                            .collect(java.util.stream.Collectors.toList());

                                        return WordAnswerHistory.builder()
                            .wordId(wordId)
                            .lessonId(lessonId)
                            .word(wordIast)
                            .entries(entries)
                            .page(pageable.getPageNumber())
                            .size(pageable.getPageSize())
                            .total((int) total)
                            .build();
                });
    }
}