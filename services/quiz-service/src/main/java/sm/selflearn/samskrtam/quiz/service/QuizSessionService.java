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
import sm.selflearn.samskrtam.content.dto.QuizSummaryDto; // Import QuizSummaryDto
import sm.selflearn.samskrtam.content.dto.QuizType;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.quiz.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.events.AnswerData;
import sm.selflearn.samskrtam.events.SessionCompleted;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.event.OutboxEvent;
import sm.selflearn.samskrtam.quiz.event.OutboxEventType;
import sm.selflearn.samskrtam.quiz.event.OutboxStatus;
import sm.selflearn.samskrtam.quiz.model.*;
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
public class QuizSessionService { // Renamed from GrammarSessionService

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

    /**
     * Starts a brand new quiz session, regardless of any existing in-progress sessions.
     */
    public Mono<StartSessionResponse> startSession(UUID quizId, UUID userId, String userLocale) {
        return createNewSessionAndBuildStartOrResumeResponse(quizId, userId, userLocale)
                .map(startOrResumeResponse -> StartSessionResponse.builder()
                        .sessionId(startOrResumeResponse.getSessionId())
                        .quizId(startOrResumeResponse.getQuizId())
                        .quizType(startOrResumeResponse.getQuizType())
                        .questions(startOrResumeResponse.getQuestions())
                        .totalQuestions(startOrResumeResponse.getTotalQuestions())
                        .build());
    }

    /**
     * Starts a new quiz session or resumes the latest in-progress session for a given quiz and user.
     */
    public Mono<StartOrResumeResponse> startOrResumeSession(UUID quizId, UUID userId, String userLocale) {
        return quizSessionRepository.findTopByUserIdAndQuizIdAndStatusOrderByStartedAtDesc(userId, quizId, SessionStatus.IN_PROGRESS)
                .flatMap(session -> resume(session.getId(), userId, userLocale)) // Found in-progress, resume it
                .switchIfEmpty(Mono.defer(() -> createNewSessionAndBuildStartOrResumeResponse(quizId, userId, userLocale))); // No in-progress, create new
    }

    /**
     * Resumes a specific quiz session by its ID.
     */
    public Mono<ResumeSessionResponse> resumeSession(UUID sessionId, UUID userId, String userLocale) {
        return resume(sessionId, userId, userLocale)
                .map(startOrResumeResponse -> ResumeSessionResponse.builder()
                        .sessionId(startOrResumeResponse.getSessionId())
                        .quizId(startOrResumeResponse.getQuizId())
                        .quizType(startOrResumeResponse.getQuizType())
                        .questions(startOrResumeResponse.getQuestions())
                        .totalQuestions(startOrResumeResponse.getTotalQuestions())
                        .answeredQuestions(startOrResumeResponse.getAnsweredQuestions())
                        .score(startOrResumeResponse.getScore())
                        .currentQuestionIndex(startOrResumeResponse.getCurrentQuestionIndex())
                        .build());
    }

    public Mono<AnswerResponse> submitAnswer(UUID sessionId, UUID userId, AnswerRequest request, String userLocale) {
        return quizSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found: " + sessionId)))
                .filter(session -> session.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> quizAnswerRepository.existsBySessionIdAndQuestionId(sessionId, request.getQuestionId()) // Changed from SessionQuestionId
                        .flatMap(alreadyAnswered -> {
                            if (alreadyAnswered) {
                                return Mono.error(new SamskrtamException("ALREADY_ANSWERED", "Question already answered: " + request.getQuestionId()));
                            } else {
                                return contentClient.getGeneratedQuestion(request.getQuestionId()) // Fetch question details
                                        .flatMap(generatedQuestion -> {
                                            List<VocabularyWordDto> allVocabularyWords = new ArrayList<>();
                                            if (session.getQuizType() == QuizType.VOCABULARY && session.getVocabularyWordsJson() != null) {
                                                try {
                                                    allVocabularyWords.addAll(objectMapper.readValue(session.getVocabularyWordsJson(),
                                                            objectMapper.getTypeFactory().constructCollectionType(List.class, VocabularyWordDto.class)));
                                                } catch (JsonProcessingException e) {
                                                    return Mono.error(new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to deserialize vocabulary words", e));
                                                }
                                            }

                                            String selectedOptionIast;
                                            if (generatedQuestion.getVocabularyWordId() != null) { // Check if it's a vocabulary question
                                                VocabularyWordDto selectedWord = allVocabularyWords.stream()
                                                        .filter(w -> w.getId().equals(request.getSelectedOptionId()))
                                                        .findFirst()
                                                        .orElseThrow(() -> new SamskrtamException("VOCABULARY_WORD_NOT_FOUND", "Selected vocabulary word not found: " + request.getSelectedOptionId()));

                                                if (generatedQuestion.getQuestionTargetLanguage() == QuestionLanguage.SANSKRIT) {
                                                    selectedOptionIast = selectedWord.getWordIast();
                                                } else if (generatedQuestion.getQuestionTargetLanguage() == QuestionLanguage.RUSSIAN) {
                                                    selectedOptionIast = selectedWord.getTranslationRu();
                                                } else if (generatedQuestion.getQuestionTargetLanguage() == QuestionLanguage.ENGLISH) {
                                                    selectedOptionIast = selectedWord.getTranslationEn();
                                                } else {
                                                    selectedOptionIast = null;
                                                }
                                            } else {
                                                selectedOptionIast = request.getSelectedFormIast();
                                            }

                                            boolean isCorrect = generatedQuestion.getCorrectFormIast().equals(selectedOptionIast);

                                            QuizAnswer newAnswer = QuizAnswer.builder()
                                                    .id(null)
                                                    .sessionId(sessionId)
                                                    .questionId(request.getQuestionId()) // Changed from SessionQuestionId
                                                    .selectedOptionId(request.getSelectedOptionId())
                                                    .selectedFormIast(selectedOptionIast)
                                                    .correctFormIast(generatedQuestion.getCorrectFormIast())
                                                    .isCorrect(isCorrect)
                                                    .responseTimeMs(request.getResponseTimeMs())
                                                    .answeredAt(Instant.now())
                                                    .build();

                                            return quizAnswerRepository.save(newAnswer)
                                                    .then(quizSessionRepository.incrementAnsweredQuestionsAndScore(sessionId, isCorrect))
                                                    .thenReturn(AnswerResponse.builder()
                                                            .isCorrect(isCorrect)
                                                            .correctOptionId(request.getSelectedOptionId())
                                                            .explanationRu(generatedQuestion.getExplanationRu())
                                                            .explanationEn(generatedQuestion.getExplanationEn())
                                                            .questionNumber(session.getAnsweredQuestions() + 1)
                                                            .totalQuestions(session.getTotalQuestions())
                                                            .build());
                                        });
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
                                contentClient.getGeneratedQuizData(session.getGeneratedQuizDataId()) // Use generatedQuizDataId
                        )
                        .flatMap(tuple -> {
                            List<QuizAnswer> quizAnswers = tuple.getT1();
                            GeneratedQuizData generatedQuizData = tuple.getT2(); // Get GeneratedQuizData
                            List<GeneratedQuizQuestionDto> generatedQuestions = generatedQuizData.getGeneratedQuestions(); // Extract questions
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
                                    .flatMap(answerDataList -> {
                                        session.setStatus(SessionStatus.COMPLETED);
                                        session.setCompletedAt(Instant.now());

                                        // Removed outbox event publishing logic

                                        return quizSessionRepository.save(session)
                                                .thenReturn(CompleteSessionResponse.builder()
                                                        .sessionId(sessionId)
                                                        .score(session.getScore())
                                                        .totalQuestions(session.getTotalQuestions())
                                                        .durationMs(Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis())
                                                        .build());
                                    });
                        }));
    }

    // =================================================================================================================
    // Private Helper Methods
    // =================================================================================================================

    /**
     * Core logic for creating a new quiz session and building the initial response.
     * Returns StartOrResumeResponse as it's the most comprehensive DTO for session data.
     */
    private Mono<StartOrResumeResponse> createNewSessionAndBuildStartOrResumeResponse(UUID quizId, UUID userId, String userLocale) {
        return contentClient.generateQuizData(quizId) // Changed method call
                .flatMap(generatedQuizData -> { // Changed variable name
                    List<GeneratedQuizQuestionDto> generatedQuestions = generatedQuizData.getGeneratedQuestions();
                    final List<VocabularyWordDto> allVocabularyWords = new ArrayList<>();
                    String vocabularyWordsJson = null;

                    if (generatedQuizData.getQuizType() == QuizType.VOCABULARY) {
                        if (generatedQuizData.getVocabularyWords() == null || generatedQuizData.getVocabularyWords().isEmpty()) {
                            return Mono.error(new SamskrtamException("NO_VOCABULARY_WORDS", "No vocabulary words found for quiz: " + quizId));
                        }
                        allVocabularyWords.addAll(generatedQuizData.getVocabularyWords());
                        try {
                            vocabularyWordsJson = objectMapper.writeValueAsString(allVocabularyWords);
                        } catch (JsonProcessingException e) {
                            return Mono.error(new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to deserialize vocabulary words", e));
                        }
                    }

                    Collections.shuffle(generatedQuestions);

                    QuizSession newSession = QuizSession.builder()
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
                            .generatedQuizDataId(generatedQuizData.getGeneratedQuizDataId()) // Save the generatedQuizDataId
                            .build();

                    return quizSessionRepository.save(newSession)
                            .flatMap(savedSession -> buildStartOrResumeResponse(savedSession, generatedQuestions, allVocabularyWords, userLocale));
                });
    }

    /**
     * Core logic for resuming an existing quiz session and building the response.
     * Returns StartOrResumeResponse as it's the most comprehensive DTO for session data.
     */
    private Mono<StartOrResumeResponse> resume(UUID sessionId, UUID userId, String userLocale) {
        return quizSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found: " + sessionId)))
                .filter(session -> session.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> contentClient.getGeneratedQuizData(session.getGeneratedQuizDataId()) // Changed method call
                                .flatMap(generatedQuizData -> { // Changed variable name
                                    List<GeneratedQuizQuestionDto> generatedQuestions = generatedQuizData.getGeneratedQuestions(); // Extract questions
                                    List<VocabularyWordDto> allVocabularyWords = new ArrayList<>();
                                    if (session.getQuizType() == QuizType.VOCABULARY && session.getVocabularyWordsJson() != null) {
                                        try {
                                            allVocabularyWords.addAll(objectMapper.readValue(session.getVocabularyWordsJson(),
                                                    objectMapper.getTypeFactory().constructCollectionType(List.class, VocabularyWordDto.class)));
                                        } catch (JsonProcessingException e) {
                                            return Mono.error(new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to deserialize vocabulary words", e));
                                        }
                                    }
                                    return buildStartOrResumeResponse(session, generatedQuestions, allVocabularyWords, userLocale);
                                }));
    }

    /**
     * Builds a StartOrResumeResponse from a QuizSession and its associated questions/vocabulary.
     * This is a common helper for both starting and resuming sessions.
     */
    private Mono<StartOrResumeResponse> buildStartOrResumeResponse(QuizSession session, List<GeneratedQuizQuestionDto> generatedQuestions, List<VocabularyWordDto> allVocabularyWords, String userLocale) {
        return contentClient.getQuizSummary(session.getQuizId()) // Fetch QuizSummaryDto
                .flatMap(quizSummary -> quizAnswerRepository.findBySessionId(session.getId())
                        .collectList()
                        .flatMap(answeredQuestions -> {
                            Set<UUID> answeredQuestionIds = answeredQuestions.stream()
                                    .map(QuizAnswer::getQuestionId)
                                    .collect(Collectors.toSet());

                            return Flux.fromIterable(generatedQuestions)
                                    .flatMap(generatedQuestion -> {
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
                                                    .text(generatedQuestion.getText())
                                                    .options(options)
                                                    .build());
                                        } else {
                                            return declensionOptionGeneratorService.generateOptions(
                                                    generatedQuestion.getDeclensionStemId(),
                                                    generatedQuestion.getTargetCase(),
                                                    generatedQuestion.getTargetNumber(),
                                                    generatedQuestion.getCorrectFormIast()
                                            ).map(options -> QuestionDto.builder()
                                                    .id(generatedQuestion.getId())
                                                    .text(generatedQuestion.getText())
                                                    .options(options)
                                                    .build());
                                        }
                                    })
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
                                            .quizTitleRu(quizSummary.getTitleRu()) // Populate new fields
                                            .quizTitleEn(quizSummary.getTitleEn()) // Populate new fields
                                            .quizDescriptionRu(quizSummary.getDescriptionRu()) // Populate new fields
                                            .quizDescriptionEn(quizSummary.getDescriptionEn()) // Populate new fields
                                            .slug(quizSummary.getSlug()) // Populate new fields
                                            .build());
                        }));
    }
}
