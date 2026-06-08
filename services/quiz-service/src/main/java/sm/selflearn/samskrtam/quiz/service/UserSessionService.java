package sm.selflearn.samskrtam.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.QuizSummaryDto;
import sm.selflearn.samskrtam.content.dto.QuizType;
import sm.selflearn.samskrtam.content.dto.SessionDataResponse;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.quiz.dto.AnswerHistoryDto;
import sm.selflearn.samskrtam.quiz.dto.QuizProgressDto;
import sm.selflearn.samskrtam.quiz.dto.QuizSessionSummaryDto;
import sm.selflearn.samskrtam.quiz.model.CachedQuestion;
import sm.selflearn.samskrtam.quiz.model.QuestionLanguage;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionQuestion;
import sm.selflearn.samskrtam.quiz.model.SessionStatus;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;
import sm.selflearn.samskrtam.quiz.repository.SessionQuestionRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final SessionQuestionRepository sessionQuestionRepository;
    private final ContentClient contentClient;
    private final ObjectMapper objectMapper;

    private static final Random random = new Random();

    public Mono<Page<QuizSessionSummaryDto>> getUserQuizSessions(
            UUID userId,
            QuizType quizType,
            SessionStatus status,
            Pageable pageable) {

        Mono<Long> totalElementsMono = quizSessionRepository.countUserSessions(userId, quizType, status);
        Flux<QuizSession> sessionsFlux = quizSessionRepository.findUserSessions(userId, quizType, status, pageable);

        Mono<Map<UUID, QuizSummaryDto>> quizSummariesMapMono = sessionsFlux
                .map(QuizSession::getQuizId)
                .collect(Collectors.toSet())
                .flatMap(quizIds -> {
                    if (quizIds.isEmpty()) {
                        return Mono.just(Map.of());
                    }
                    return Flux.fromIterable(quizIds)
                            .flatMap(contentClient::getQuizSummary)
                            .collect(Collectors.toMap(QuizSummaryDto::getId, Function.identity()));
                });

        return Mono.zip(sessionsFlux.collectList(), quizSummariesMapMono, totalElementsMono)
                .map(tuple -> {
                    List<QuizSession> sessions = tuple.getT1();
                    Map<UUID, QuizSummaryDto> quizSummariesMap = tuple.getT2();
                    Long totalElements = tuple.getT3();

                    List<QuizSessionSummaryDto> dtoList = sessions.stream()
                            .map(session -> {
                                String quizTitle = quizSummariesMap.getOrDefault(session.getQuizId(), QuizSummaryDto.builder().titleEn("Unknown Quiz").build()).getTitleEn();
                                Long durationMs = null;
                                if (session.getStartedAt() != null && session.getCompletedAt() != null) {
                                    durationMs = Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis();
                                }

                                return QuizSessionSummaryDto.builder()
                                        .sessionId(session.getId())
                                        .quizId(session.getQuizId())
                                        .quizTitle(quizTitle)
                                        .quizType(session.getQuizType())
                                        .score(session.getScore())
                                        .totalQuestions(session.getTotalQuestions())
                                        .status(session.getStatus())
                                        .startedAt(session.getStartedAt())
                                        .completedAt(session.getCompletedAt())
                                        .durationMs(durationMs)
                                        .build();
                            })
                            .collect(Collectors.toList());

                    return new PageImpl<>(dtoList, pageable, totalElements);
                });
    }

    public Mono<List<AnswerHistoryDto>> getSessionAnswerHistory( // Changed return type to List
            UUID sessionId,
            UUID userId,
            Locale locale) { // Removed Pageable
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> Mono.zip(
                                sessionQuestionRepository.findBySessionId(sessionId).collectList(),
                                quizAnswerRepository.findBySessionId(sessionId).collectList()
                        )
                        .flatMap(tuple -> {
                            List<SessionQuestion> sessionQuestions = tuple.getT1();
                            List<sm.selflearn.samskrtam.quiz.model.QuizAnswer> quizAnswers = tuple.getT2();

                            Map<UUID, sm.selflearn.samskrtam.quiz.model.QuizAnswer> answersMap = quizAnswers.stream()
                                    .collect(Collectors.toMap(sm.selflearn.samskrtam.quiz.model.QuizAnswer::getSessionQuestionId, Function.identity()));

                            List<AnswerHistoryDto> fullHistory = sessionQuestions.stream()
                                    .sorted(Comparator.comparing(SessionQuestion::getQuestionId)) // Ensure consistent order
                                    .map(sq -> {
                                        sm.selflearn.samskrtam.quiz.model.QuizAnswer answer = answersMap.get(sq.getQuestionId());
                                        String explanation = locale.getLanguage().equals("ru") ? sq.getExplanationRu() : sq.getExplanationEn();

                                        return AnswerHistoryDto.builder()
                                                .questionId(sq.getQuestionId())
                                                .questionText(sq.getText())
                                                .selectedAnswerIast(answer != null ? answer.getSelectedFormIast() : null)
                                                .correctOptionIast(sq.getCorrectFormIast()) // Always from SessionQuestion
                                                .isCorrect(answer != null ? answer.isCorrect() : null) // Use null for unanswered
                                                .responseTimeMs(answer != null ? answer.getResponseTimeMs() : null)
                                                .answeredAt(answer != null ? answer.getAnsweredAt() : null)
                                                .explanation(explanation)
                                                .build();
                                    })
                                    .collect(Collectors.toList());

                            return Mono.just(fullHistory); // Return the full list
                        })

                );
    }

    public Mono<QuizProgressDto> getLatestUnfinishedQuizProgress(UUID userId, UUID quizId) {
        return quizSessionRepository.findTopByUserIdAndQuizIdAndStatusOrderByStartedAtDesc(userId, quizId, SessionStatus.IN_PROGRESS)
                .map(session -> new QuizProgressDto(session.getId(), session.getAnsweredQuestions(), session.getTotalQuestions(), true))
                .defaultIfEmpty(new QuizProgressDto(null, 0, 0, false));
    }

    private List<CachedQuestion> generateVocabularyQuestions(SessionDataResponse sessionData, String userLocale) {
        List<CachedQuestion> allPossibleQuestions = new ArrayList<>();

        if (sessionData.getVocabularyWords() == null) {
            return Collections.emptyList();
        }

        for (VocabularyWordDto word : sessionData.getVocabularyWords()) {
            if (word == null) {
                log.warn("Skipping null vocabulary word in session data.");
                continue;
            }

            String wordDevanagari = Optional.ofNullable(word.getWordDevanagari()).orElse("");
            String wordIast = Optional.ofNullable(word.getWordIast()).orElse("");
            String translationRu = Optional.ofNullable(word.getTranslationRu()).orElse("");
            String translationEn = Optional.ofNullable(word.getTranslationEn()).orElse("");
            String dictionaryEntry = Optional.ofNullable(word.getDictionaryEntry()).orElse("");

            String questionTextSanskritToTranslation = String.format(
                    userLocale.equals("ru") ? "Как переводится слово '%s'?" : "How is the word '%s' translated?",
                    userLocale.equals("ru") ? wordDevanagari : wordIast
            );
            String correctFormIastSanskritToTranslation = userLocale.equals("ru") ? translationRu : translationEn;
            UUID questionIdSanskritToTranslation = UUID.nameUUIDFromBytes((Optional.ofNullable(word.getId()).map(UUID::toString).orElse("") + "SANSKRIT_TO_TRANSLATION").getBytes());

            allPossibleQuestions.add(CachedQuestion.builder()
                    .questionId(questionIdSanskritToTranslation)
                    .text(questionTextSanskritToTranslation)
                    .explanationRu(dictionaryEntry)
                    .explanationEn(dictionaryEntry)
                    .vocabularyWordId(word.getId())
                    .questionSourceLanguage(QuestionLanguage.SANSKRIT)
                    .questionTargetLanguage(userLocale.equals("ru") ? QuestionLanguage.RUSSIAN : QuestionLanguage.ENGLISH)
                    .correctTranslationRu(translationRu)
                    .correctTranslationEn(translationEn)
                    .correctFormIast(correctFormIastSanskritToTranslation)
                    .correctFormDevanagari(wordDevanagari)
                    .build());

            String questionTextTranslationToSanskrit = String.format(
                    userLocale.equals("ru") ? "Как будет '%s' на санскрите?" : "How is '%s' in Sanskrit?",
                    userLocale.equals("ru") ? translationRu : translationEn
            );
            String correctFormIastTranslationToSanskrit = wordIast;
            UUID questionIdTranslationToSanskrit = UUID.nameUUIDFromBytes((Optional.ofNullable(word.getId()).map(UUID::toString).orElse("") + "TRANSLATION_TO_SANSKRIT").getBytes());

            allPossibleQuestions.add(CachedQuestion.builder()
                    .questionId(questionIdTranslationToSanskrit)
                    .text(questionTextTranslationToSanskrit)
                    .explanationRu(dictionaryEntry)
                    .explanationEn(dictionaryEntry)
                    .vocabularyWordId(word.getId())
                    .questionSourceLanguage(QuestionLanguage.RUSSIAN)
                    .questionTargetLanguage(QuestionLanguage.SANSKRIT)
                    .correctTranslationRu(translationRu)
                    .correctTranslationEn(translationEn)
                    .correctFormIast(correctFormIastTranslationToSanskrit)
                    .correctFormDevanagari(wordDevanagari)
                    .build());
        }

        Collections.shuffle(allPossibleQuestions);
        int questionsToSelect = Math.min(sessionData.getQuestionsPerSession(), allPossibleQuestions.size());
        return allPossibleQuestions.subList(0, questionsToSelect);
    }

    private CachedQuestion findQuestion(UUID questionId, List<CachedQuestion> cachedQuestions) {
        return cachedQuestions.stream()
                .filter(q -> q.getQuestionId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new SamskrtamException("QUESTION_NOT_FOUND", "Question not found in session: " + questionId));
    }
}