package sm.selflearn.samskrtam.quiz.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.content.dto.QuizSummaryDto;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.service.LessonService;
import sm.selflearn.samskrtam.quiz.service.ContentClient;
import sm.selflearn.samskrtam.quiz.service.QuizDataAssembler;
import sm.selflearn.samskrtam.quiz.service.UserSessionService;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonServiceImpl implements LessonService {

    private final ContentClient contentClient;
    private final QuizDataAssembler quizDataAssembler;
    private final UserSessionService userSessionService;

    @Override
    public Mono<VocabularyLesson> getVocabularyLesson(String slug, UUID userId) {
        // Get the quiz summary to get basic information
        return contentClient.getQuizSummaryBySlug(slug)
                .flatMap(quizSummary -> {
                    return contentClient.getVocabularyWordsForQuiz(quizSummary.getId(), 100000 )
                            .flatMap(vocabularyWords -> {
                                // Get user progress for the quiz
                                QuizProgressDto progress = userSessionService.getUserQuizProgress(userId, quizSummary.getId());
                                return createVocabularyLesson(quizSummary, vocabularyWords, progress);
                            });
                });
    }

    @Override
    public Mono<GrammarLesson> getGrammarLesson(LessonType type, UUID userId) {
        // For now, we'll return a generic empty lesson until we get full functionality
        return Mono.empty();
    }

    @Override
    public Mono<WordAnswerHistory> getWordAnswerHistory(String slug, UUID wordId, UUID userId, Pageable pageable, Locale locale) {
        // Get quiz summary to get quiz id from slug
        return contentClient.getQuizSummaryBySlug(slug)
                .flatMap(quizSummary -> {
                    // Get word statistics for the specific word and user
                    return userSessionService.getWordStatistics(userId, quizSummary.getId(), wordId)
                            .flatMap(stats -> {
                                // Create history based on stats and pageable criteria
                                return createWordAnswerHistory(stats, wordId, pageable, locale);
                            });
                })
                .switchIfEmpty(Mono.empty()); // Return empty if no quiz found
    }

    @Override
    public Mono<QuestionAnswerHistory> getQuestionAnswerHistory(String type, UUID questionId, UUID userId, Pageable pageable, Locale locale) {
        // Placeholder implementation - this would need to be implemented based on actual requirements
        return Mono.empty();
    }

    private Mono<VocabularyLesson> createVocabularyLesson(QuizSummaryDto quizSummary,
                                                          java.util.List<VocabularyWordDto> vocabularyWords,
                                                          QuizProgressDto progress) {
        VocabularyLesson lesson = new VocabularyLesson();
        lesson.setQuizId(quizSummary.getId());
        lesson.setSlug(quizSummary.getSlug());
        lesson.setTitleRu(quizSummary.getTitleRu());
        lesson.setTitleEn(quizSummary.getTitleEn());
        lesson.setDifficulty(quizSummary.getDifficulty().toString());

        // Calculate progress statistics
        lesson.setTotalWords(vocabularyWords.size());
        //lesson.setLearnedWords(progress.getLearnedWords());
        if (lesson.getTotalWords() > 0) {
            lesson.setProgressPercent((float) lesson.getLearnedWords() / lesson.getTotalWords() * 100);
        } else {
            lesson.setProgressPercent(0);
        }

        // Create vocabulary word progress items
// In your LessonServiceImpl.java or appropriate service file

// Replace the vocabulary word creation section with this implementation:
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

    private Mono<WordAnswerHistory> createWordAnswerHistory(Object stats, UUID wordId, Pageable pageable, Locale locale) {
        // This is a placeholder implementation. In a real system, this would involve
        // creating a proper history based on the statistics and pagination criteria
        WordAnswerHistory history = new WordAnswerHistory();

        return Mono.just(history);
    }
}