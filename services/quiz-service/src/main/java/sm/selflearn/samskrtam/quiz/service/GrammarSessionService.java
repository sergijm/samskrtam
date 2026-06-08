package sm.selflearn.samskrtam.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.SessionDataResponse;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.events.AnswerData;
import sm.selflearn.samskrtam.events.SessionCompleted;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.model.*;
import sm.selflearn.samskrtam.quiz.repository.OutboxEventRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;
import sm.selflearn.samskrtam.quiz.repository.SessionQuestionRepository; // Import SessionQuestionRepository
import sm.selflearn.samskrtam.content.dto.QuizType;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrammarSessionService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final SessionQuestionRepository sessionQuestionRepository; // Inject SessionQuestionRepository
    private final ContentClient contentClient;
    private final DeclensionOptionGeneratorService declensionOptionGeneratorService;
    private final LexicalOptionGeneratorService lexicalOptionGeneratorService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    private static final Random random = new Random();

    public Mono<StartSessionResponse> startSession(UUID quizId, UUID userId, String userLocale) {
        return contentClient.getSessionData(quizId)
                .flatMap(sessionData -> {
                    List<CachedQuestion> cachedQuestions;
                    final List<VocabularyWordDto> allVocabularyWords = new ArrayList<>();
                    String vocabularyWordsJson = null;

                    if (sessionData.getQuizType() == QuizType.VOCABULARY) {
                        if (sessionData.getVocabularyWords() == null || sessionData.getVocabularyWords().isEmpty()) {
                            return Mono.error(new SamskrtamException("NO_VOCABULARY_WORDS", "No vocabulary words found for quiz: " + quizId));
                        }
                        allVocabularyWords.addAll(sessionData.getVocabularyWords());
                        cachedQuestions = generateVocabularyQuestions(sessionData, userLocale);
                        try {
                            vocabularyWordsJson = objectMapper.writeValueAsString(allVocabularyWords);
                        } catch (JsonProcessingException e) {
                            return Mono.error(new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to serialize vocabulary words", e));
                        }
                    } else {
                        cachedQuestions = sessionData.getQuestions().stream()
                                .map(qr -> CachedQuestion.builder()
                                        .questionId(qr.getId())
                                        .text(qr.getText())
                                        .explanationRu(qr.getExplanationRu())
                                        .explanationEn(qr.getExplanationEn())
                                        .declensionStemId(qr.getDeclensionStemId())
                                        .targetCase(qr.getTargetCase())
                                        .targetNumber(qr.getTargetNumber())
                                        .correctFormIast(qr.getCorrectFormIast())
                                        .correctFormDevanagari(qr.getCorrectFormDevanagari())
                                        .build())
                                .collect(Collectors.toList());
                    }

                    Collections.shuffle(cachedQuestions);

                    QuizSession newSession = QuizSession.builder()
                            .id(null)
                            .userId(userId)
                            .quizId(quizId)
                            .quizType(sessionData.getQuizType())
                            .totalQuestions(sessionData.getQuestionsPerSession())
                            .answeredQuestions(0)
                            .score(0)
                            .status(SessionStatus.IN_PROGRESS)
                            .startedAt(Instant.now())
                            .vocabularyWordsJson(vocabularyWordsJson) // Save vocabulary words JSON
                            .build();

                    return quizSessionRepository.save(newSession)
                            .flatMap(savedSession -> {
                                List<SessionQuestion> sessionQuestions = cachedQuestions.stream()
                                        .map(cq -> SessionQuestion.builder()
                                                .sessionId(savedSession.getId())
                                                .questionId(cq.getQuestionId())
                                                .text(cq.getText())
                                                .explanationRu(cq.getExplanationRu())
                                                .explanationEn(cq.getExplanationEn())
                                                .declensionStemId(cq.getDeclensionStemId())
                                                .targetCase(cq.getTargetCase())
                                                .targetNumber(cq.getTargetNumber())
                                                .correctFormIast(cq.getCorrectFormIast())
                                                .correctFormDevanagari(cq.getCorrectFormDevanagari())
                                                .vocabularyWordId(cq.getVocabularyWordId())
                                                .questionSourceLanguage(cq.getQuestionSourceLanguage())
                                                .questionTargetLanguage(cq.getQuestionTargetLanguage())
                                                .correctTranslationRu(cq.getCorrectTranslationRu())
                                                .correctTranslationEn(cq.getCorrectTranslationEn())
                                                .build())
                                        .collect(Collectors.toList());
                                return sessionQuestionRepository.saveAll(sessionQuestions)
                                        .then(buildStartSessionResponse(savedSession, cachedQuestions, allVocabularyWords, userLocale));
                            });
                });
    }

    public Mono<ResumeSessionResponse> resumeSession(UUID sessionId, UUID userId, String userLocale) {
        return quizSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found: " + sessionId)))
                .filter(session -> session.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> sessionQuestionRepository.findBySessionId(sessionId).collectList() // Fetch session questions
                        .flatMap(sessionQuestions -> {
                            List<CachedQuestion> cachedQuestions = sessionQuestions.stream()
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
                                    .collect(Collectors.toList());

                            List<VocabularyWordDto> allVocabularyWords = new ArrayList<>();
                            if (session.getQuizType() == QuizType.VOCABULARY && session.getVocabularyWordsJson() != null) {
                                try {
                                    allVocabularyWords.addAll(objectMapper.readValue(session.getVocabularyWordsJson(),
                                            objectMapper.getTypeFactory().constructCollectionType(List.class, VocabularyWordDto.class)));
                                } catch (JsonProcessingException e) {
                                    return Mono.error(new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to deserialize vocabulary words", e));
                                }
                            }
                            return buildResumeSessionResponse(session, cachedQuestions, allVocabularyWords, userLocale);
                        }));
    }

    public Mono<AnswerResponse> submitAnswer(UUID sessionId, UUID userId, AnswerRequest request, String userLocale) {
        return quizSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found: " + sessionId)))
                .filter(session -> session.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> quizAnswerRepository.existsBySessionIdAndSessionQuestionId(sessionId, request.getQuestionId())
                        .flatMap(alreadyAnswered -> {
                            if (alreadyAnswered) {
                                return Mono.error(new SamskrtamException("ALREADY_ANSWERED", "Question already answered: " + request.getQuestionId()));
                            } else {
                                return sessionQuestionRepository.findBySessionIdAndQuestionId(sessionId, request.getQuestionId()) // Fetch specific session question
                                        .switchIfEmpty(Mono.error(new SamskrtamException("QUESTION_NOT_FOUND", "Question not found in session: " + request.getQuestionId())))
                                        .flatMap(sessionQuestion -> {
                                            List<VocabularyWordDto> allVocabularyWords = new ArrayList<>();
                                            if (session.getQuizType() == QuizType.VOCABULARY && session.getVocabularyWordsJson() != null) {
                                                try {
                                                    allVocabularyWords.addAll(objectMapper.readValue(session.getVocabularyWordsJson(),
                                                            objectMapper.getTypeFactory().constructCollectionType(List.class, VocabularyWordDto.class)));
                                                } catch (JsonProcessingException e) {
                                                    return Mono.error(new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to deserialize vocabulary words", e));
                                                }
                                            }

                                            // Convert SessionQuestion to CachedQuestion for existing logic compatibility
                                            CachedQuestion cachedQuestion = CachedQuestion.builder()
                                                    .questionId(sessionQuestion.getQuestionId())
                                                    .text(sessionQuestion.getText())
                                                    .explanationRu(sessionQuestion.getExplanationRu())
                                                    .explanationEn(sessionQuestion.getExplanationEn())
                                                    .declensionStemId(sessionQuestion.getDeclensionStemId())
                                                    .targetCase(sessionQuestion.getTargetCase())
                                                    .targetNumber(sessionQuestion.getTargetNumber())
                                                    .correctFormIast(sessionQuestion.getCorrectFormIast())
                                                    .correctFormDevanagari(sessionQuestion.getCorrectFormDevanagari())
                                                    .vocabularyWordId(sessionQuestion.getVocabularyWordId())
                                                    .questionSourceLanguage(sessionQuestion.getQuestionSourceLanguage())
                                                    .questionTargetLanguage(sessionQuestion.getQuestionTargetLanguage())
                                                    .correctTranslationRu(sessionQuestion.getCorrectTranslationRu())
                                                    .correctTranslationEn(sessionQuestion.getCorrectTranslationEn())
                                                    .build();

                                            String selectedOptionIast = getOptionIast(request.getSelectedOptionId(), cachedQuestion, userLocale, allVocabularyWords, session.getQuizType());
                                            boolean isCorrect = cachedQuestion.getCorrectFormIast().equals(selectedOptionIast);

                                            QuizAnswer newAnswer = QuizAnswer.builder()
                                                    .id(null)
                                                    .sessionId(sessionId)
                                                    .sessionQuestionId(request.getQuestionId())
                                                    .selectedOptionId(request.getSelectedOptionId())
                                                    .selectedFormIast(selectedOptionIast)
                                                    .correctFormIast(cachedQuestion.getCorrectFormIast())
                                                    .isCorrect(isCorrect) // Changed to isCorrect
                                                    .responseTimeMs(request.getResponseTimeMs())
                                                    .answeredAt(Instant.now())
                                                    .build();

                                            return quizAnswerRepository.save(newAnswer)
                                                    .then(quizSessionRepository.incrementAnsweredQuestionsAndScore(sessionId, isCorrect))
                                                    .thenReturn(AnswerResponse.builder()
                                                            .isCorrect(isCorrect)
                                                            .correctOptionId(request.getSelectedOptionId())
                                                            .explanationRu(cachedQuestion.getExplanationRu())
                                                            .explanationEn(cachedQuestion.getExplanationEn())
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
                                sessionQuestionRepository.findBySessionId(sessionId).collectList() // Fetch all session questions
                        )
                        .flatMap(tuple -> {
                            List<QuizAnswer> quizAnswers = tuple.getT1();
                            List<SessionQuestion> sessionQuestions = tuple.getT2();
                            Map<UUID, SessionQuestion> sessionQuestionMap = sessionQuestions.stream()
                                    .collect(Collectors.toMap(SessionQuestion::getQuestionId, sq -> sq));

                            List<AnswerData> answerDataList = quizAnswers.stream()
                                    .map(qa -> {
                                        SessionQuestion sq = sessionQuestionMap.get(qa.getSessionQuestionId());
                                        String explanationRu = (sq != null) ? sq.getExplanationRu() : null;
                                        String explanationEn = (sq != null) ? sq.getExplanationEn() : null;

                                        return AnswerData.builder()
                                                .questionId(qa.getSessionQuestionId())
                                                .questionText((sq != null) ? sq.getText() : null) // Populate questionText
                                                .selectedOptionId(qa.getSelectedOptionId())
                                                .correctFormIast(qa.getCorrectFormIast())
                                                .isCorrect(qa.isCorrect()) // Changed to isCorrect
                                                .responseTimeMs(qa.getResponseTimeMs())
                                                .answeredAt(qa.getAnsweredAt())
                                                .explanationRu(explanationRu) // Add explanations to AnswerData
                                                .explanationEn(explanationEn)
                                                .build();
                                    })
                                    .collect(Collectors.toList());

                            session.setStatus(SessionStatus.COMPLETED);
                            session.setCompletedAt(Instant.now());

                            SessionCompleted event = SessionCompleted.builder()
                                    .userId(userId)
                                    .quizType(session.getQuizType())
                                    .quizId(session.getQuizId())
                                    .score(session.getScore())
                                    .totalQuestions(session.getTotalQuestions())
                                    .durationMs(Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis())
                                    .answers(answerDataList)
                                    .build();

                            Mono<Void> outboxMono = Mono.fromCallable(() -> {
                                try {
                                    return objectMapper.writeValueAsString(event);
                                } catch (JsonProcessingException e) {
                                    log.error("Error serializing SessionCompleted event: {}", event, e);
                                    throw new SamskrtamException("JSON_SERIALIZATION_ERROR", "Failed to serialize SessionCompleted event", e);
                                }
                            })
                                    .flatMap(payload -> outboxEventRepository.save(OutboxEvent.builder()
                                            .id(null)
                                            .aggregateId(userId.toString())
                                            .topic("quiz.session.completed")
                                            .payload(payload)
                                            .status(OutboxStatus.PENDING)
                                            .eventType(OutboxEventType.SESSION_COMPLETED)
                                            .build()))
                                    .then();

                            return quizSessionRepository.save(session)
                                    .then(outboxMono)
                                    .thenReturn(CompleteSessionResponse.builder()
                                            .sessionId(sessionId)
                                            .score(session.getScore())
                                            .totalQuestions(session.getTotalQuestions())
                                            .durationMs(Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis())
                                            .build());
                        }));
    }

    private Mono<StartSessionResponse> buildStartSessionResponse(QuizSession session, List<CachedQuestion> cachedQuestions, List<VocabularyWordDto> allVocabularyWords, String userLocale) {
        return Flux.fromIterable(cachedQuestions)
                .flatMap(cachedQuestion -> {
                    if (session.getQuizType() == QuizType.VOCABULARY) {
                        VocabularyWordDto correctWord = allVocabularyWords.stream()
                                .filter(w -> w.getId().equals(cachedQuestion.getVocabularyWordId()))
                                .findFirst()
                                .orElseThrow(() -> new SamskrtamException("VOCABULARY_WORD_NOT_FOUND", "Vocabulary word not found in cache for ID: " + cachedQuestion.getVocabularyWordId()));

                        return lexicalOptionGeneratorService.generateOptions(
                                correctWord,
                                allVocabularyWords,
                                cachedQuestion.getQuestionSourceLanguage(),
                                cachedQuestion.getQuestionTargetLanguage(),
                                userLocale
                        ).map(options -> QuestionDto.builder()
                                .id(cachedQuestion.getQuestionId())
                                .text(cachedQuestion.getText())
                                .options(options)
                                .build());
                    } else {
                        return declensionOptionGeneratorService.generateOptions(
                                cachedQuestion.getDeclensionStemId(),
                                cachedQuestion.getTargetCase(),
                                cachedQuestion.getTargetNumber(),
                                cachedQuestion.getCorrectFormIast()
                        ).map(options -> QuestionDto.builder()
                                .id(cachedQuestion.getQuestionId())
                                .text(cachedQuestion.getText())
                                .options(options)
                                .build());
                    }
                })
                .collectList()
                .map(questions -> StartSessionResponse.builder()
                        .sessionId(session.getId())
                        .quizId(session.getQuizId())
                        .quizType(session.getQuizType())
                        .questions(questions)
                        .totalQuestions(session.getTotalQuestions())
                        .build());
    }

    private Mono<ResumeSessionResponse> buildResumeSessionResponse(QuizSession session, List<CachedQuestion> cachedQuestions, List<VocabularyWordDto> allVocabularyWords, String userLocale) {
        return quizAnswerRepository.findBySessionId(session.getId())
                .collectList()
                .flatMap(answeredQuestions -> {
                    Set<UUID> answeredQuestionIds = answeredQuestions.stream()
                            .map(QuizAnswer::getSessionQuestionId)
                            .collect(Collectors.toSet());

                    return Flux.fromIterable(cachedQuestions)
                            .flatMap(cachedQuestion -> {
                                if (session.getQuizType() == QuizType.VOCABULARY) {
                                    VocabularyWordDto correctWord = allVocabularyWords.stream()
                                            .filter(w -> w.getId().equals(cachedQuestion.getVocabularyWordId()))
                                            .findFirst()
                                            .orElseThrow(() -> new SamskrtamException("VOCABULARY_WORD_NOT_FOUND", "Vocabulary word not found for ID: " + cachedQuestion.getVocabularyWordId()));

                                    return lexicalOptionGeneratorService.generateOptions(
                                            correctWord,
                                            allVocabularyWords,
                                            cachedQuestion.getQuestionSourceLanguage(),
                                            cachedQuestion.getQuestionTargetLanguage(),
                                            userLocale
                                    ).map(options -> QuestionDto.builder()
                                            .id(cachedQuestion.getQuestionId())
                                            .text(cachedQuestion.getText())
                                            .options(options)
                                            .build());
                                } else {
                                    return declensionOptionGeneratorService.generateOptions(
                                            cachedQuestion.getDeclensionStemId(),
                                            cachedQuestion.getTargetCase(),
                                            cachedQuestion.getTargetNumber(),
                                            cachedQuestion.getCorrectFormIast()
                                    ).map(options -> QuestionDto.builder()
                                            .id(cachedQuestion.getQuestionId())
                                            .text(cachedQuestion.getText())
                                            .options(options)
                                            .build());
                                }
                            })
                            .collectList()
                            .map(questions -> ResumeSessionResponse.builder()
                                    .sessionId(session.getId())
                                    .quizId(session.getQuizId())
                                    .quizType(session.getQuizType())
                                    .questions(questions)
                                    .totalQuestions(session.getTotalQuestions())
                                    .answeredQuestions(answeredQuestionIds.size())
                                    .score(session.getScore())
                                    .currentQuestionIndex(answeredQuestionIds.size())
                                    .build());
                });
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

    private String getOptionIast(UUID selectedOptionId, CachedQuestion cachedQuestion, String userLocale, List<VocabularyWordDto> allVocabularyWords, QuizType quizType) {
        if (quizType == QuizType.VOCABULARY) {
            VocabularyWordDto selectedWord = allVocabularyWords.stream()
                    .filter(w -> w.getId().equals(selectedOptionId))
                    .findFirst()
                    .orElseThrow(() -> new SamskrtamException("VOCABULARY_WORD_NOT_FOUND", "Selected vocabulary word not found: " + selectedOptionId));

            if (cachedQuestion.getQuestionTargetLanguage() == QuestionLanguage.SANSKRIT) {
                return selectedWord.getWordIast();
            } else if (cachedQuestion.getQuestionTargetLanguage() == QuestionLanguage.RUSSIAN) {
                return selectedWord.getTranslationRu();
            } else if (cachedQuestion.getQuestionTargetLanguage() == QuestionLanguage.ENGLISH) {
                return selectedWord.getTranslationEn();
            }
            return null;
        } else {
            return cachedQuestion.getCorrectFormIast();
        }
    }
}
