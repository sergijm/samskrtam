package sm.selflearn.samskrtam.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.QuestionResponse;
import sm.selflearn.samskrtam.content.dto.SessionDataResponse;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.events.AnswerData;
import sm.selflearn.samskrtam.events.AnswerSubmitted;
import sm.selflearn.samskrtam.events.SessionCompleted;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.model.*;
import sm.selflearn.samskrtam.quiz.repository.OutboxEventRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;
import sm.selflearn.samskrtam.content.dto.QuizType;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrammarSessionService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final SessionCacheService sessionCacheService;
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

                    if (sessionData.getQuizType() == QuizType.VOCABULARY) {
                        if (sessionData.getVocabularyWords() == null || sessionData.getVocabularyWords().isEmpty()) {
                            return Mono.error(new SamskrtamException("NO_VOCABULARY_WORDS", "No vocabulary words found for quiz: " + quizId));
                        }
                        allVocabularyWords.addAll(sessionData.getVocabularyWords());
                        cachedQuestions = generateVocabularyQuestions(sessionData, userLocale);
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
                            .build();

                    return quizSessionRepository.save(newSession)
                            .flatMap(savedSession -> {
                                SessionCache sessionCache = SessionCache.builder()
                                        .sessionId(savedSession.getId())
                                        .userId(userId)
                                        .quizId(quizId)
                                        .quizType(sessionData.getQuizType())
                                        .questions(cachedQuestions)
                                        .answeredQuestionIds(new HashSet<>())
                                        .score(0)
                                        .allVocabularyWords(allVocabularyWords)
                                        .build();
                                return sessionCacheService.put(savedSession.getId(), sessionCache)
                                        .then(buildStartSessionResponse(sessionCache, userLocale));
                            });
                });
    }

    public Mono<ResumeSessionResponse> resumeSession(UUID sessionId, UUID userId, String userLocale) {
        return sessionCacheService.get(sessionId)
                .filter(cache -> cache.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(cache -> buildResumeSessionResponse(cache, userLocale));
    }

    public Mono<AnswerResponse> submitAnswer(UUID sessionId, UUID userId, AnswerRequest request, String userLocale) {
        return sessionCacheService.get(sessionId)
                .filter(cache -> cache.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .filter(cache -> !cache.getAnsweredQuestionIds().contains(request.getQuestionId()))
                .switchIfEmpty(Mono.error(new SamskrtamException("ALREADY_ANSWERED", "Question already answered: " + request.getQuestionId())))
                .flatMap(cache -> {
                    CachedQuestion cachedQuestion = cache.findQuestion(request.getQuestionId());
                    boolean isCorrect = cachedQuestion.getCorrectFormIast().equals(getOptionIast(request.getSelectedOptionId(), cachedQuestion, userLocale, cache.getAllVocabularyWords(), cache.getQuizType()));

                    QuizAnswer newAnswer = QuizAnswer.builder()
                            .id(null)
                            .sessionId(sessionId)
                            .questionId(request.getQuestionId())
                            .selectedOptionId(request.getSelectedOptionId())
                            .correctFormIast(cachedQuestion.getCorrectFormIast())
                            .correct(isCorrect)
                            .responseTimeMs(request.getResponseTimeMs())
                            .answeredAt(Instant.now())
                            .build();

                    return quizAnswerRepository.save(newAnswer)
                            .then(sessionCacheService.put(sessionId, cache))
                            .thenReturn(AnswerResponse.builder()
                                    .isCorrect(isCorrect)
                                    .correctOptionId(request.getSelectedOptionId())
                                    .explanationRu(cachedQuestion.getExplanationRu())
                                    .explanationEn(cachedQuestion.getExplanationEn())
                                    .questionNumber(cache.getAnsweredQuestionIds().size())
                                    .totalQuestions(cache.getQuestions().size())
                                    .build());
                });
    }

    public Mono<CompleteSessionResponse> completeSession(UUID sessionId, UUID userId) {
        return sessionCacheService.get(sessionId)
                .filter(cache -> cache.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(cache -> quizSessionRepository.findById(sessionId)
                        .flatMap(session ->
                                quizAnswerRepository.findBySessionId(sessionId)
                                        .collectList()
                                        .flatMap(quizAnswers -> {
                                            List<AnswerData> answerDataList = quizAnswers.stream()
                                                    .map(qa -> AnswerData.builder()
                                                            .questionId(qa.getQuestionId())
                                                            .selectedOptionId(qa.getSelectedOptionId())
                                                            .correctFormIast(qa.getCorrectFormIast())
                                                            .correct(qa.isCorrect())
                                                            .responseTimeMs(qa.getResponseTimeMs())
                                                            .answeredAt(qa.getAnsweredAt())
                                                            .build())
                                                    .collect(Collectors.toList());

                                            session.setStatus(SessionStatus.COMPLETED);
                                            session.setCompletedAt(Instant.now());
                                            session.setScore(cache.getScore());
                                            session.setAnsweredQuestions(cache.getAnsweredQuestionIds().size());

                                            SessionCompleted event = SessionCompleted.builder()
                                                    .userId(userId)
                                                    .quizType(cache.getQuizType())
                                                    .quizId(cache.getQuizId())
                                                    .score(cache.getScore())
                                                    .totalQuestions(cache.getQuestions().size())
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
                                                    .then(sessionCacheService.evict(sessionId))
                                                    .then(outboxMono)
                                                    .thenReturn(CompleteSessionResponse.builder()
                                                            .sessionId(sessionId)
                                                            .score(cache.getScore())
                                                            .totalQuestions(cache.getQuestions().size())
                                                            .durationMs(Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis())
                                                            .build());
                                        })
                        ));
    }

    private Mono<StartSessionResponse> buildStartSessionResponse(SessionCache cache, String userLocale) {
        return Flux.fromIterable(cache.getQuestions())
                .flatMap(cachedQuestion -> {
                    if (cache.getQuizType() == QuizType.VOCABULARY) {
                        VocabularyWordDto correctWord = cache.getAllVocabularyWords().stream()
                                .filter(w -> w.getId().equals(cachedQuestion.getVocabularyWordId()))
                                .findFirst()
                                .orElseThrow(() -> new SamskrtamException("VOCABULARY_WORD_NOT_FOUND", "Vocabulary word not found in cache for ID: " + cachedQuestion.getVocabularyWordId()));

                        return lexicalOptionGeneratorService.generateOptions(
                                correctWord,
                                cache.getAllVocabularyWords(),
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
                        .sessionId(cache.getSessionId())
                        .quizId(cache.getQuizId())
                        .quizType(cache.getQuizType())
                        .questions(questions)
                        .totalQuestions(cache.getQuestions().size())
                        .build());
    }

    private Mono<ResumeSessionResponse> buildResumeSessionResponse(SessionCache cache, String userLocale) {
        return Flux.fromIterable(cache.getQuestions())
                .flatMap(cachedQuestion -> {
                    if (cache.getQuizType() == QuizType.VOCABULARY) {
                        VocabularyWordDto correctWord = cache.getAllVocabularyWords().stream()
                                .filter(w -> w.getId().equals(cachedQuestion.getVocabularyWordId()))
                                .findFirst()
                                .orElseThrow(() -> new SamskrtamException("VOCABULARY_WORD_NOT_FOUND", "Vocabulary word not found in cache for ID: " + cachedQuestion.getVocabularyWordId()));

                        return lexicalOptionGeneratorService.generateOptions(
                                correctWord,
                                cache.getAllVocabularyWords(),
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
                        .sessionId(cache.getSessionId())
                        .quizId(cache.getQuizId())
                        .quizType(cache.getQuizType())
                        .questions(questions)
                        .totalQuestions(cache.getQuestions().size())
                        .answeredQuestions(cache.getAnsweredQuestionIds().size())
                        .score(cache.getScore())
                        .currentQuestionIndex(cache.getAnsweredQuestionIds().size())
                        .build());
    }

    private List<CachedQuestion> generateVocabularyQuestions(SessionDataResponse sessionData, String userLocale) {
        List<VocabularyWordDto> availableWords = new ArrayList<>(sessionData.getVocabularyWords());
        Collections.shuffle(availableWords);

        int questionsToGenerate = Math.min(sessionData.getQuestionsPerSession(), availableWords.size());
        List<CachedQuestion> generatedQuestions = new ArrayList<>();

        for (int i = 0; i < questionsToGenerate; i++) {
            VocabularyWordDto word = availableWords.get(i);
            UUID questionId = UUID.randomUUID();
            QuestionLanguage sourceLang;
            QuestionLanguage targetLang;
            String questionText;
            String correctFormIast;
            String correctFormDevanagari = null;

            if (random.nextBoolean()) { // Sanskrit -> Translation
                sourceLang = QuestionLanguage.SANSKRIT;
                targetLang = userLocale.equals("ru") ? QuestionLanguage.RUSSIAN : QuestionLanguage.ENGLISH;
                questionText = String.format(
                        userLocale.equals("ru") ? "Как переводится слово '%s'?" : "How is the word '%s' translated?",
                        userLocale.equals("ru") ? word.getWordDevanagari() : word.getWordIast()
                );
                correctFormIast = targetLang == QuestionLanguage.RUSSIAN ? word.getTranslationRu() : word.getTranslationEn();
            } else { // Translation -> Sanskrit
                sourceLang = userLocale.equals("ru") ? QuestionLanguage.RUSSIAN : QuestionLanguage.ENGLISH;
                targetLang = QuestionLanguage.SANSKRIT;
                questionText = String.format(
                        userLocale.equals("ru") ? "Как будет '%s' на санскрите?" : "How is '%s' in Sanskrit?",
                        sourceLang == QuestionLanguage.RUSSIAN ? word.getTranslationRu() : word.getTranslationEn()
                );
                correctFormIast = word.getWordIast();
                correctFormDevanagari = word.getWordDevanagari();
            }

            generatedQuestions.add(CachedQuestion.builder()
                    .questionId(questionId)
                    .text(questionText)
                    .explanationRu(word.getDictionaryEntry())
                    .explanationEn(word.getDictionaryEntry())
                    .vocabularyWordId(word.getId())
                    .questionSourceLanguage(sourceLang)
                    .questionTargetLanguage(targetLang)
                    .correctTranslationRu(word.getTranslationRu())
                    .correctTranslationEn(word.getTranslationEn())
                    .correctFormIast(correctFormIast)
                    .correctFormDevanagari(correctFormDevanagari)
                    .build());
        }
        return generatedQuestions;
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
