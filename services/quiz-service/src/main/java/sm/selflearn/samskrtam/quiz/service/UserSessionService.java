package sm.selflearn.samskrtam.quiz.service;

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
import sm.selflearn.samskrtam.quiz.model.SessionStatus;
import sm.selflearn.samskrtam.quiz.model.SessionQuestion; // Import SessionQuestion
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;
import sm.selflearn.samskrtam.quiz.repository.SessionQuestionRepository; // Import SessionQuestionRepository
import com.fasterxml.jackson.databind.ObjectMapper; // Import ObjectMapper
import com.fasterxml.jackson.core.JsonProcessingException; // Import JsonProcessingException

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
    private final SessionQuestionRepository sessionQuestionRepository; // Inject SessionQuestionRepository
    private final ContentClient contentClient;
    private final ObjectMapper objectMapper; // Inject ObjectMapper

    private static final Random random = new Random(); // Needed for generateVocabularyQuestions

    public Mono<Page<QuizSessionSummaryDto>> getUserQuizSessions(
            UUID userId,
            QuizType quizType,
            SessionStatus status,
            Pageable pageable) {

        // 1. Fetch sessions with pagination and sorting using new repository methods
        Mono<Long> totalElementsMono = quizSessionRepository.countUserSessions(userId, quizType, status);
        Flux<QuizSession> sessionsFlux = quizSessionRepository.findUserSessions(userId, quizType, status, pageable);

        // 2. Fetch quiz titles from content-service
        Mono<Map<UUID, QuizSummaryDto>> quizSummariesMapMono = sessionsFlux
                .map(QuizSession::getQuizId)
                .collect(Collectors.toSet()) // Collect unique quiz IDs
                .flatMap(quizIds -> {
                    if (quizIds.isEmpty()) {
                        return Mono.just(Map.of());
                    }
                    return Flux.fromIterable(quizIds)
                            .flatMap(contentClient::getQuizSummary)
                            .collect(Collectors.toMap(QuizSummaryDto::getId, Function.identity()));
                });

        // 3. Combine sessions with quiz titles and map to DTOs
        return Mono.zip(sessionsFlux.collectList(), quizSummariesMapMono, totalElementsMono)
                .map(tuple -> {
                    List<QuizSession> sessions = tuple.getT1();
                    Map<UUID, QuizSummaryDto> quizSummariesMap = tuple.getT2();
                    Long totalElements = tuple.getT3();

                    List<QuizSessionSummaryDto> dtoList = sessions.stream()
                            .map(session -> {
                                String quizTitle = quizSummariesMap.getOrDefault(session.getQuizId(), QuizSummaryDto.builder().titleEn("Unknown Quiz").build()).getTitleEn(); // Default to English title
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

    public Mono<Page<AnswerHistoryDto>> getSessionAnswerHistory(
            UUID sessionId,
            UUID userId,
            Pageable pageable,
            Locale locale) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> sessionQuestionRepository.findBySessionId(sessionId).collectList() // Fetch session questions
                        .flatMap(sessionQuestions -> {
                            Map<UUID, CachedQuestion> cachedQuestionMap = sessionQuestions.stream()
                                    .map(sq -> CachedQuestion.builder()
                                            .questionId(sq.getQuestionId())
                                            .text(sq.getText())
                                            .explanationRu(sq.getExplanationRu())
                                            .explanationEn(sq.getExplanationEn())
                                            .declensionStemId(sq.getDeclensionStemId())
                                            .targetCase(sq.getTargetCase())
                                            .targetNumber(sq.getTargetNumber())
                                            .correctFormIast(sq.getCorrectFormIast())
                                            .correctFormDevanagari(sq.getCorrectFormDevanagari())
                                            .vocabularyWordId(sq.getVocabularyWordId())
                                            .questionSourceLanguage(sq.getQuestionSourceLanguage())
                                            .questionTargetLanguage(sq.getQuestionTargetLanguage())
                                            .correctTranslationRu(sq.getCorrectTranslationRu())
                                            .correctTranslationEn(sq.getCorrectTranslationEn())
                                            .build())
                                    .collect(Collectors.toMap(CachedQuestion::getQuestionId, Function.identity()));

                            List<VocabularyWordDto> allVocabularyWords = new ArrayList<>();
                            if (session.getQuizType() == QuizType.VOCABULARY && session.getVocabularyWordsJson() != null) {
                                try {
                                    allVocabularyWords.addAll(objectMapper.readValue(session.getVocabularyWordsJson(),
                                            objectMapper.getTypeFactory().constructCollectionType(List.class, VocabularyWordDto.class)));
                                } catch (JsonProcessingException e) {
                                    return Mono.error(new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to deserialize vocabulary words", e));
                                }
                            }

                            return quizAnswerRepository.findSessionAnswers(sessionId, pageable)
                                    .collectList()
                                    .flatMap(answers -> {
                                        List<AnswerHistoryDto> history = answers.stream()
                                                .map(answer -> {
                                                    CachedQuestion cachedQuestion = cachedQuestionMap.get(answer.getSessionQuestionId());
                                                    if (cachedQuestion == null) {
                                                        throw new SamskrtamException("QUESTION_NOT_FOUND", "Question not found in session questions for ID: " + answer.getSessionQuestionId());
                                                    }
                                                    String explanation = locale.getLanguage().equals("ru") ? cachedQuestion.getExplanationRu() : cachedQuestion.getExplanationEn();

                                                    return AnswerHistoryDto.builder()
                                                            .questionId(answer.getSessionQuestionId())
                                                            .questionText(cachedQuestion.getText())
                                                            .selectedAnswerIast(answer.getSelectedFormIast())
                                                            .correctOptionIast(cachedQuestion.getCorrectFormIast())
                                                            .isCorrect(answer.isCorrect())
                                                            .responseTimeMs(answer.getResponseTimeMs())
                                                            .answeredAt(answer.getAnsweredAt())
                                                            .explanation(explanation)
                                                            .build();
                                                })
                                                .collect(Collectors.toList());

                                        return quizAnswerRepository.countBySessionId(sessionId)
                                                .map(total -> new PageImpl<>(history, pageable, total));
                                    });
                        })
                );
    }

    public Mono<QuizProgressDto> getLatestUnfinishedQuizProgress(UUID userId, UUID quizId) { // Added quizId parameter
        return quizSessionRepository.findTopByUserIdAndQuizIdAndStatusOrderByStartedAtDesc(userId, quizId, SessionStatus.IN_PROGRESS) // Use new repository method
                .map(session -> new QuizProgressDto(session.getId(), session.getAnsweredQuestions(), session.getTotalQuestions(), true))
                .defaultIfEmpty(new QuizProgressDto(null, 0, 0, false));
    }

    // Helper methods copied from GrammarSessionService
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

            // Generate Sanskrit -> Translation question
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

            // Generate Translation -> Sanskrit question
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
                    .questionSourceLanguage(userLocale.equals("ru") ? QuestionLanguage.RUSSIAN : QuestionLanguage.ENGLISH)
                    .questionTargetLanguage(QuestionLanguage.SANSKRIT)
                    .correctTranslationRu(translationRu)
                    .correctTranslationEn(translationEn)
                    .correctFormIast(correctFormIastTranslationToSanskrit)
                    .correctFormDevanagari(wordDevanagari)
                    .build());
        }

        Collections.shuffle(allPossibleQuestions);
        // Select the required number of questions
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
