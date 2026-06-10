package sm.selflearn.samskrtam.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizData;
import sm.selflearn.samskrtam.content.dto.QuestionLanguage;
import sm.selflearn.samskrtam.content.dto.QuizSummaryDto;
import sm.selflearn.samskrtam.content.dto.QuizType;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.events.AnswerData;
import sm.selflearn.samskrtam.quiz.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.event.QuizAnsweredEvent;
import sm.selflearn.samskrtam.quiz.event.QuizSessionStatusChangedEvent;
import sm.selflearn.samskrtam.quiz.model.*;
import sm.selflearn.samskrtam.quiz.outbox.OutboxEvent;
import sm.selflearn.samskrtam.quiz.outbox.OutboxEventType;
import sm.selflearn.samskrtam.quiz.outbox.OutboxStatus;
import sm.selflearn.samskrtam.quiz.repository.OutboxEventRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizSessionService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final ContentClient contentClient;
    private final DeclensionOptionGeneratorService declensionOptionGeneratorService;
    private final LexicalOptionGeneratorService lexicalOptionGeneratorService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    private static final Random random = new Random();

    // =================================================================================================================
    // Public API Methods
    // =================================================================================================================

    public Mono<StartOrResumeResponse> startSession(UUID quizId, UUID userId, String userLocale) {
        return createNewSessionAndBuildStartOrResumeResponse(quizId, userId, userLocale);
    }

    public Mono<StartOrResumeResponse> startOrResumeSession(UUID quizId, UUID userId, String userLocale) {
        return quizSessionRepository.findTopByUserIdAndQuizIdAndStatusOrderByStartedAtDesc(userId, quizId, SessionStatus.IN_PROGRESS)
                .flatMap(session -> resume(session.getId(), userId, userLocale))
                .switchIfEmpty(Mono.defer(() -> createNewSessionAndBuildStartOrResumeResponse(quizId, userId, userLocale)));
    }

    public Mono<StartOrResumeResponse> resumeSession(UUID sessionId, UUID userId, String userLocale) {
        return resume(sessionId, userId, userLocale);
    }

    public Mono<AnswerResponse> submitAnswer(UUID sessionId, UUID userId, AnswerRequest request, String userLocale) {
        return quizSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found: " + sessionId)))
                .filter(session -> session.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> quizAnswerRepository.existsBySessionIdAndQuestionId(sessionId, request.getQuestionId())
                        .flatMap(alreadyAnswered -> {
                            if (alreadyAnswered) {
                                return Mono.error(new SamskrtamException("ALREADY_ANSWERED", "Question already answered: " + request.getQuestionId()));
                            } else {
                                return contentClient.getGeneratedQuestion(request.getQuestionId())
                                        .flatMap(generatedQuestion -> processAndSaveAnswer(session, userId, request, generatedQuestion, userLocale));
                            }
                        })
                );
    }

    public Mono<CompleteSessionResponse> completeSession(UUID sessionId, UUID userId) {
        return quizSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found: " + sessionId)))
                .filter(session -> session.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> Mono.zip(
                                quizAnswerRepository.findBySessionId(sessionId).collectList(),
                                contentClient.getGeneratedQuizData(session.getGeneratedQuizDataId())
                        )
                        .flatMap(tuple -> {
                            List<QuizAnswer> quizAnswers = tuple.getT1();
                            GeneratedQuizData generatedQuizData = tuple.getT2();
                            return processAndCompleteSession(session, userId, quizAnswers, generatedQuizData);
                        }));
    }

    public Mono<StartOrResumeResponse> retakeSession(UUID sessionId, UUID userId, String userLocale) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> quizAnswerRepository.deleteBySessionId(sessionId)
                        .then(Mono.just(session)))
                .flatMap(session -> resetAndPublishSessionStatus(session, userId, userLocale));
    }

    public Mono<StartOrResumeResponse> startNewQuizFromExistingSession(UUID sessionId, UUID userId, String userLocale) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> completeAndPublishSessionStatus(session, userId))
                .flatMap(completedSession -> createNewSessionAndBuildStartOrResumeResponse(completedSession.getQuizId(), userId, userLocale));
    }

    // =================================================================================================================
    // Private Helper Methods for submitAnswer
    // =================================================================================================================

    private Mono<AnswerResponse> processAndSaveAnswer(QuizSession session, UUID userId, AnswerRequest request, GeneratedQuizQuestionDto generatedQuestion, String userLocale) {
        return getVocabularyWords(session)
                .flatMap(allVocabularyWords -> {
                    String selectedOptionIast = determineSelectedOptionIast(request, generatedQuestion, allVocabularyWords);
                    boolean isCorrect = generatedQuestion.getCorrectFormIast().equals(selectedOptionIast);

                    QuizAnswer newAnswer = QuizAnswer.builder()
                            .id(null)
                            .sessionId(session.getId())
                            .questionId(request.getQuestionId())
                            .selectedOptionId(request.getSelectedOptionId())
                            .selectedFormIast(selectedOptionIast)
                            .correctFormIast(generatedQuestion.getCorrectFormIast())
                            .isCorrect(isCorrect)
                            .responseTimeMs(request.getResponseTimeMs())
                            .answeredAt(Instant.now())
                            .build();

                    return quizAnswerRepository.save(newAnswer)
                            .then(quizSessionRepository.incrementAnsweredQuestionsAndScore(session.getId(), isCorrect))
                            .flatMap(updatedCount -> publishAnsweredEventAndReturnResponse(session, userId, request, generatedQuestion, selectedOptionIast, isCorrect));
                });
    }

    private Mono<List<VocabularyWordDto>> getVocabularyWords(QuizSession session) {
        if (session.getQuizType() == QuizType.VOCABULARY && session.getVocabularyWordsJson() != null) {
            try {
                return Mono.just(objectMapper.readValue(session.getVocabularyWordsJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, VocabularyWordDto.class)));
            } catch (JsonProcessingException e) {
                return Mono.error(new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to deserialize vocabulary words", e));
            }
        }
        return Mono.just(Collections.emptyList());
    }

    private String determineSelectedOptionIast(AnswerRequest request, GeneratedQuizQuestionDto generatedQuestion, List<VocabularyWordDto> allVocabularyWords) {
        if (generatedQuestion.getVocabularyWordId() != null) {
            VocabularyWordDto selectedWord = allVocabularyWords.stream()
                    .filter(w -> w.getId().equals(request.getSelectedOptionId()))
                    .findFirst()
                    .orElseThrow(() -> new SamskrtamException("VOCABULARY_WORD_NOT_FOUND", "Selected vocabulary word not found: " + request.getSelectedOptionId()));

            if (generatedQuestion.getQuestionTargetLanguage() == QuestionLanguage.SANSKRIT) {
                return selectedWord.getWordIast();
            } else if (generatedQuestion.getQuestionTargetLanguage() == QuestionLanguage.RUSSIAN) {
                return selectedWord.getTranslationRu();
            } else if (generatedQuestion.getQuestionTargetLanguage() == QuestionLanguage.ENGLISH) {
                return selectedWord.getTranslationEn();
            } else {
                return null;
            }
        } else {
            return request.getSelectedFormIast();
        }
    }

    private Mono<AnswerResponse> publishAnsweredEventAndReturnResponse(QuizSession session, UUID userId, AnswerRequest request, GeneratedQuizQuestionDto generatedQuestion, String selectedOptionIast, boolean isCorrect) {
        QuizAnsweredEvent event = new QuizAnsweredEvent(
                session.getId(),
                userId,
                session.getQuizId(),
                session.getQuizType(),
                request.getQuestionId(),
                selectedOptionIast,
                isCorrect,
                Instant.now()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(null)
                    .aggregateType("QuizSession")
                    .aggregateId(session.getId().toString())
                    .eventType(OutboxEventType.QUIZ_ANSWERED)
                    .payload(payload)
                    .createdAt(Instant.now())
                    .status(OutboxStatus.NEW)
                    .errorMessage(null)
                    .retryCount(0)
                    .processedAt(null)
                    .build();

            AnswerResponse answerResponse = AnswerResponse.builder()
                    .isCorrect(isCorrect)
                    .correctOptionId(request.getSelectedOptionId())
                    .explanationRu(generatedQuestion.getExplanationRu())
                    .explanationEn(generatedQuestion.getExplanationEn())
                    .questionNumber(session.getAnsweredQuestions() + 1)
                    .totalQuestions(session.getTotalQuestions())
                    .build();

            return outboxEventRepository.save(outboxEvent)
                    .then(Mono.just(answerResponse));
        } catch (JsonProcessingException e) {
            return Mono.error(new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to serialize QuizAnsweredEvent", e));
        }
    }

    // =================================================================================================================
    // Private Helper Methods for completeSession
    // =================================================================================================================

    private Mono<CompleteSessionResponse> processAndCompleteSession(QuizSession session, UUID userId, List<QuizAnswer> quizAnswers, GeneratedQuizData generatedQuizData) {
        List<GeneratedQuizQuestionDto> generatedQuestions = generatedQuizData.getGeneratedQuestions();
        Map<UUID, GeneratedQuizQuestionDto> generatedQuestionMap = generatedQuestions.stream()
                .collect(Collectors.toMap(GeneratedQuizQuestionDto::getId, gq -> gq));

        return Flux.fromIterable(generatedQuestions)
                .flatMap(gq -> {
                    QuizAnswer answer = quizAnswers.stream()
                            .filter(qa -> qa.getQuestionId().equals(gq.getId()))
                            .findFirst().orElse(null);

                    return Mono.just(AnswerData.builder()
                            .questionId(gq.getId())
                            .questionText(gq.getText())
                            .selectedOptionId(Optional.ofNullable(answer)
                                    .map(QuizAnswer::getSelectedOptionId).orElse(null))
                            .correctFormIast(gq.getCorrectFormIast())
                            .isCorrect(Optional.ofNullable(answer)
                                    .map(QuizAnswer::getIsCorrect).orElse(null))
                            .responseTimeMs(Optional.ofNullable(answer)
                                    .map(QuizAnswer::getResponseTimeMs).orElse(null))
                            .answeredAt(Optional.ofNullable(answer)
                                    .map(QuizAnswer::getAnsweredAt).orElse(null))
                            .explanationRu(gq.getExplanationRu())
                            .explanationEn(gq.getExplanationEn())
                            .build());
                })
                .collectList()
                .flatMap(answerDataList -> saveAndPublishCompletedSession(session, userId));
    }

    private Mono<CompleteSessionResponse> saveAndPublishCompletedSession(QuizSession session, UUID userId) {
        SessionStatus oldStatus = session.getStatus();
        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(Instant.now());

        return quizSessionRepository.save(session)
                .flatMap(savedSession -> {
                    QuizSessionStatusChangedEvent event = new QuizSessionStatusChangedEvent(
                            savedSession.getId(),
                            savedSession.getUserId(),
                            savedSession.getQuizId(),
                            savedSession.getQuizType(),
                            oldStatus.name(),
                            savedSession.getStatus().name(),
                            Instant.now()
                    );
                    try {
                        String payload = objectMapper.writeValueAsString(event);
                        OutboxEvent outboxEvent = OutboxEvent.builder()
                                .id(null)
                                .aggregateType("QuizSession")
                                .aggregateId(savedSession.getId().toString())
                                .eventType(OutboxEventType.QUIZ_SESSION_STATUS_CHANGED)
                                .payload(payload)
                                .createdAt(Instant.now())
                                .status(OutboxStatus.NEW)
                                .errorMessage(null)
                                .retryCount(0)
                                .processedAt(null)
                                .build();

                        CompleteSessionResponse completeSessionResponse = CompleteSessionResponse.builder()
                                .sessionId(savedSession.getId())
                                .score(savedSession.getScore())
                                .totalQuestions(savedSession.getTotalQuestions())
                                .durationMs(Duration.between(savedSession.getStartedAt(), savedSession.getCompletedAt()).toMillis())
                                .build();

                        return outboxEventRepository.save(outboxEvent)
                                .then(Mono.just(completeSessionResponse));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to serialize QuizSessionStatusChangedEvent", e));
                    }
                });
    }

    // =================================================================================================================
    // Private Helper Methods for createNewSessionAndBuildStartOrResumeResponse
    // =================================================================================================================

    private Mono<StartOrResumeResponse> createNewSessionAndBuildStartOrResumeResponse(UUID quizId, UUID userId, String userLocale) {
        return contentClient.generateQuizData(quizId)
                .flatMap(generatedQuizData -> {
                    List<GeneratedQuizQuestionDto> generatedQuestions = generatedQuizData.getGeneratedQuestions();
                    return getVocabularyWordsForNewSession(generatedQuizData, quizId)
                            .flatMap(allVocabularyWords -> {
                                String vocabularyWordsJson = serializeVocabularyWords(allVocabularyWords);
                                QuizSession newSession = buildNewQuizSession(quizId, userId, generatedQuizData, vocabularyWordsJson);
                                return quizSessionRepository.save(newSession)
                                        .flatMap(savedSession -> publishSessionStartedEventAndBuildResponse(savedSession, generatedQuestions, allVocabularyWords, userLocale));
                            });
                });
    }

    private Mono<List<VocabularyWordDto>> getVocabularyWordsForNewSession(GeneratedQuizData generatedQuizData, UUID quizId) {
        if (generatedQuizData.getQuizType() == QuizType.VOCABULARY) {
            if (generatedQuizData.getVocabularyWords() == null || generatedQuizData.getVocabularyWords().isEmpty()) {
                return Mono.error(new SamskrtamException("NO_VOCABULARY_WORDS", "No vocabulary words found for quiz: " + quizId));
            }
            return Mono.just(generatedQuizData.getVocabularyWords());
        }
        return Mono.just(Collections.emptyList());
    }

    private String serializeVocabularyWords(List<VocabularyWordDto> allVocabularyWords) {
        if (allVocabularyWords.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(allVocabularyWords);
        } catch (JsonProcessingException e) {
            throw new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to serialize vocabulary words", e);
        }
    }

    private QuizSession buildNewQuizSession(UUID quizId, UUID userId, GeneratedQuizData generatedQuizData, String vocabularyWordsJson) {
        return QuizSession.builder()
                .id(null)
                .userId(userId)
                .quizId(quizId)
                .quizType(generatedQuizData.getQuizType())
                .totalQuestions(generatedQuizData.getQuestionsPerSession())
                .answeredQuestions(0)
                .score(0)
                .status(SessionStatus.IN_PROGRESS)
                .startedAt(Instant.now())
                .vocabularyWordsJson(vocabularyWordsJson)
                .generatedQuizDataId(generatedQuizData.getGeneratedQuizDataId())
                .build();
    }

    private Mono<StartOrResumeResponse> publishSessionStartedEventAndBuildResponse(QuizSession savedSession, List<GeneratedQuizQuestionDto> generatedQuestions, List<VocabularyWordDto> allVocabularyWords, String userLocale) {
        QuizSessionStatusChangedEvent event = new QuizSessionStatusChangedEvent(
                savedSession.getId(),
                savedSession.getUserId(),
                savedSession.getQuizId(),
                savedSession.getQuizType(),
                null,
                savedSession.getStatus().name(),
                Instant.now()
        );
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(null)
                    .aggregateType("QuizSession")
                    .aggregateId(savedSession.getId().toString())
                    .eventType(OutboxEventType.QUIZ_SESSION_STATUS_CHANGED)
                    .payload(payload)
                    .createdAt(Instant.now())
                    .status(OutboxStatus.NEW)
                    .errorMessage(null)
                    .retryCount(0)
                    .processedAt(null)
                    .build();
            return outboxEventRepository.save(outboxEvent)
                    .then(buildStartOrResumeResponse(savedSession, generatedQuestions, allVocabularyWords, userLocale));
        } catch (JsonProcessingException e) {
            return Mono.error(new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to serialize QuizSessionStatusChangedEvent", e));
        }
    }

    // =================================================================================================================
    // Private Helper Methods for resume
    // =================================================================================================================

    private Mono<StartOrResumeResponse> resume(UUID sessionId, UUID userId, String userLocale) {
        return quizSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found: " + sessionId)))
                .filter(session -> session.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> {
                    if (session.getStatus() != SessionStatus.IN_PROGRESS) {
                        return updateAndPublishSessionStatusToInProgress(session, userId, userLocale);
                    } else {
                        return fetchGeneratedQuizDataAndBuildResponse(session, userLocale);
                    }
                });
    }

    private Mono<StartOrResumeResponse> updateAndPublishSessionStatusToInProgress(QuizSession session, UUID userId, String userLocale) {
        SessionStatus oldStatus = session.getStatus();
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setCompletedAt(null);
        return quizSessionRepository.save(session)
                .flatMap(savedSession -> {
                    QuizSessionStatusChangedEvent event = new QuizSessionStatusChangedEvent(
                            savedSession.getId(),
                            savedSession.getUserId(),
                            savedSession.getQuizId(),
                            savedSession.getQuizType(),
                            oldStatus.name(),
                            savedSession.getStatus().name(),
                            Instant.now()
                    );
                    try {
                        String payload = objectMapper.writeValueAsString(event);
                        OutboxEvent outboxEvent = OutboxEvent.builder()
                                .id(null)
                                .aggregateType("QuizSession")
                                .aggregateId(savedSession.getId().toString())
                                .eventType(OutboxEventType.QUIZ_SESSION_STATUS_CHANGED)
                                .payload(payload)
                                .createdAt(Instant.now())
                                .status(OutboxStatus.NEW)
                                .errorMessage(null)
                                .retryCount(0)
                                .processedAt(null)
                                .build();
                        return outboxEventRepository.save(outboxEvent)
                                .then(fetchGeneratedQuizDataAndBuildResponse(savedSession, userLocale));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to serialize QuizSessionStatusChangedEvent", e));
                    }
                });
    }

    private Mono<StartOrResumeResponse> fetchGeneratedQuizDataAndBuildResponse(QuizSession session, String userLocale) {
        return contentClient.getGeneratedQuizData(session.getGeneratedQuizDataId())
                .flatMap(generatedQuizData -> getVocabularyWords(session)
                        .flatMap(allVocabularyWords -> buildStartOrResumeResponse(session, generatedQuizData.getGeneratedQuestions(), allVocabularyWords, userLocale)));
    }

    // =================================================================================================================
    // Private Helper Methods for retakeSession
    // =================================================================================================================

    private Mono<StartOrResumeResponse> resetAndPublishSessionStatus(QuizSession session, UUID userId, String userLocale) {
        SessionStatus oldStatus = session.getStatus();
        session.setScore(0);
        session.setAnsweredQuestions(0);
        session.setStartedAt(Instant.now());
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setCompletedAt(null);
        return quizSessionRepository.save(session)
                .flatMap(updatedSession -> {
                    QuizSessionStatusChangedEvent event = new QuizSessionStatusChangedEvent(
                            updatedSession.getId(),
                            updatedSession.getUserId(),
                            updatedSession.getQuizId(),
                            updatedSession.getQuizType(),
                            oldStatus.name(),
                            updatedSession.getStatus().name(),
                            Instant.now()
                    );
                    try {
                        String payload = objectMapper.writeValueAsString(event);
                        OutboxEvent outboxEvent = OutboxEvent.builder()
                                .id(null)
                                .aggregateType("QuizSession")
                                .aggregateId(updatedSession.getId().toString())
                                .eventType(OutboxEventType.QUIZ_SESSION_STATUS_CHANGED)
                                .payload(payload)
                                .createdAt(Instant.now())
                                .status(OutboxStatus.NEW)
                                .errorMessage(null)
                                .retryCount(0)
                                .processedAt(null)
                                .build();
                        return outboxEventRepository.save(outboxEvent)
                                .then(contentClient.getGeneratedQuizData(updatedSession.getGeneratedQuizDataId())
                                        .flatMap(generatedQuizData -> getVocabularyWords(updatedSession)
                                                .flatMap(allVocabularyWords -> buildStartOrResumeResponse(updatedSession, generatedQuizData.getGeneratedQuestions(), allVocabularyWords, userLocale))));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to serialize QuizSessionStatusChangedEvent", e));
                    }
                });
    }

    // =================================================================================================================
    // Private Helper Methods for startNewQuizFromExistingSession
    // =================================================================================================================

    private Mono<QuizSession> completeAndPublishSessionStatus(QuizSession session, UUID userId) {
        SessionStatus oldStatus = session.getStatus();
        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(Instant.now());
        return quizSessionRepository.save(session)
                .flatMap(completedSession -> {
                    QuizSessionStatusChangedEvent event = new QuizSessionStatusChangedEvent(
                            completedSession.getId(),
                            completedSession.getUserId(),
                            completedSession.getQuizId(),
                            completedSession.getQuizType(),
                            oldStatus.name(),
                            completedSession.getStatus().name(),
                            Instant.now()
                    );
                    try {
                        String payload = objectMapper.writeValueAsString(event);
                        OutboxEvent outboxEvent = OutboxEvent.builder()
                                .id(null)
                                .aggregateType("QuizSession")
                                .aggregateId(completedSession.getId().toString())
                                .eventType(OutboxEventType.QUIZ_SESSION_STATUS_CHANGED)
                                .payload(payload)
                                .createdAt(Instant.now())
                                .status(OutboxStatus.NEW)
                                .errorMessage(null)
                                .retryCount(0)
                                .processedAt(null)
                                .build();
                        return outboxEventRepository.save(outboxEvent)
                                .thenReturn(completedSession);
                    } catch (JsonProcessingException e) {
                        return Mono.error(new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to serialize QuizSessionStatusChangedEvent", e));
                    }
                });
    }

    // =================================================================================================================
    // General Private Helper Methods
    // =================================================================================================================

    private Mono<StartOrResumeResponse> buildStartOrResumeResponse(QuizSession session, List<GeneratedQuizQuestionDto> generatedQuestions, List<VocabularyWordDto> allVocabularyWords, String userLocale) {
        return contentClient.getQuizSummary(session.getQuizId())
                .flatMap(quizSummary -> quizAnswerRepository.findBySessionId(session.getId())
                        .collectList()
                        .flatMap(answeredQuestions -> {
                            Set<UUID> answeredQuestionIds = answeredQuestions.stream()
                                    .map(QuizAnswer::getQuestionId)
                                    .collect(Collectors.toSet());

                            List<GeneratedQuizQuestionDto> sortedQuestions = generatedQuestions.stream()
                                    .sorted(Comparator.comparing(GeneratedQuizQuestionDto::getQuestionNumber))
                                    .collect(Collectors.toList());

                            return Flux.fromIterable(sortedQuestions)
                                    .flatMap(generatedQuestion -> generateQuestionOptions(session, generatedQuestion, allVocabularyWords, userLocale))
                                    .collectList()
                                    .map(questions -> StartOrResumeResponse.builder()
                                            .sessionId(session.getId())
                                            .quizId(session.getQuizId())
                                            .quizType(session.getQuizType())
                                            .questions(questions)
                                            .totalQuestions(session.getTotalQuestions())
                                            .answeredQuestions(answeredQuestionIds.size())
                                            .score(session.getScore())
                                            .currentQuestionIndex(answeredQuestionIds.size())
                                            .currentQuestionNumber(answeredQuestionIds.size() > 0 ? sortedQuestions.get(answeredQuestionIds.size()).getQuestionNumber() : 1)
                                            .quizTitleRu(quizSummary.getTitleRu())
                                            .quizTitleEn(quizSummary.getTitleEn())
                                            .quizDescriptionRu(quizSummary.getDescriptionRu())
                                            .quizDescriptionEn(quizSummary.getDescriptionEn())
                                            .slug(quizSummary.getSlug())
                                            .build());
                        }));
    }

    private Mono<QuestionDto> generateQuestionOptions(QuizSession session, GeneratedQuizQuestionDto generatedQuestion, List<VocabularyWordDto> allVocabularyWords, String userLocale) {
        if (session.getQuizType() == QuizType.VOCABULARY) {
            VocabularyWordDto correctWord = allVocabularyWords.stream()
                    .filter(w -> w.getId().equals(generatedQuestion.getVocabularyWordId()))
                    .findFirst()
                    .orElseThrow(() -> new SamskrtamException("VOCABULARY_WORD_NOT_FOUND", "Vocabulary word not found for ID: " + generatedQuestion.getVocabularyWordId()));

            return lexicalOptionGeneratorService.generateOptions(
                    correctWord,
                    allVocabularyWords,
                    generatedQuestion.getQuestionSourceLanguage(),
                    generatedQuestion.getQuestionTargetLanguage(),
                    userLocale
            ).map(options -> QuestionDto.builder()
                    .id(generatedQuestion.getId())
                    .questionNumber(generatedQuestion.getQuestionNumber())
                    .text(generatedQuestion.getText())
                    .options(options)
                    .stem(generatedQuestion.getStem())
                    .caseType(generatedQuestion.getCaseType())
                    .numberType(generatedQuestion.getNumberType())
                    .build());
        } else {
            return declensionOptionGeneratorService.generateOptions(
                    generatedQuestion.getDeclensionStemId(),
                    generatedQuestion.getTargetCase(),
                    generatedQuestion.getTargetNumber(),
                    generatedQuestion.getCorrectFormIast()
            ).map(options -> QuestionDto.builder()
                    .id(generatedQuestion.getId())
                    .questionNumber(generatedQuestion.getQuestionNumber())
                    .text(generatedQuestion.getText())
                    .options(options)
                    .stem(generatedQuestion.getStem())
                    .caseType(generatedQuestion.getCaseType())
                    .numberType(generatedQuestion.getNumberType())
                    .build());
        }
    }
}