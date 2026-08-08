package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.quiz.dto.AnswerRequest;
import sm.selflearn.samskrtam.quiz.dto.AnswerResponse;
import sm.selflearn.samskrtam.quiz.dto.CompleteSessionResponse;
import sm.selflearn.samskrtam.quiz.dto.MatchSubmissionDto;
import sm.selflearn.samskrtam.quiz.dto.QuestionDto;
import sm.selflearn.samskrtam.quiz.dto.StartOrResumeResponse;
import sm.selflearn.samskrtam.quiz.event.QuizAnsweredEvent;
import sm.selflearn.samskrtam.quiz.event.QuizSessionStatusChangedEvent;
import sm.selflearn.samskrtam.quiz.model.QuizAnswer;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionQuestion;
import sm.selflearn.samskrtam.quiz.model.SessionStatus;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;
import sm.selflearn.samskrtam.quiz.repository.SessionQuestionRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lifecycle operations for curriculum-driven (composed) quiz sessions.
 *
 * <p>These sessions have {@code lessonId == null} and their questions carry curriculum
 * materialized data (answerMode/correctAnswer/options/payload). The legacy
 * {@link SessionOperationsService} assumes content-generated questions
 * (targetCase/targetNumber/correctFormIast/vocabulary), which do not apply here — so the
 * composed path is isolated rather than branch-injected.
 *
 * <p>Progress writing to {@code quiz_item_score} is intentionally deferred (decision
 * 2026-08: curriculum itemType → progress key not touched yet); answer correctness and
 * session state are fully handled.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComposedSessionService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final SessionQuestionRepository sessionQuestionRepository;
    private final OutboxEventCreator outboxEventCreator;
    private final ComposedQuestionMapper composedQuestionMapper;
    private final QuizItemScoreService quizItemScoreService;

    @Transactional
    public Mono<StartOrResumeResponse> resume(QuizSession session, String userLocale) {
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            return setInProgress(session);
        }
        return buildResumeResponse(session);
    }

    @Transactional
    public Mono<AnswerResponse> submitAnswer(QuizSession session, UUID userId, AnswerRequest request, String userLocale) {
        return quizAnswerRepository.existsBySessionIdAndQuestionId(session.getId(), request.getQuestionId())
                .flatMap(alreadyAnswered -> {
                    if (alreadyAnswered) {
                        return Mono.error(new SamskrtamException("ALREADY_ANSWERED",
                                "Question already answered: " + request.getQuestionId()));
                    }
                    return sessionQuestionRepository.findByQuestionId(request.getQuestionId())
                            .switchIfEmpty(Mono.error(new SamskrtamException("QUESTION_NOT_FOUND",
                                    "Question not found: " + request.getQuestionId())))
                            .flatMap(stored -> processAnswer(session, userId, request, stored));
                });
    }

    @Transactional
    public Mono<CompleteSessionResponse> complete(QuizSession session) {
        SessionStatus oldStatus = session.getStatus();
        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(Instant.now());
        return quizSessionRepository.save(session)
                .flatMap(saved -> outboxEventCreator.createAndSaveSessionStatusChangedEvent(
                        new QuizSessionStatusChangedEvent(saved.getId(), saved.getUserId(),
                                saved.getLessonId(), saved.getLessonType(),
                                oldStatus.name(), saved.getStatus().name(), Instant.now()))
                        .thenReturn(CompleteSessionResponse.builder()
                                .sessionId(saved.getId())
                                .score(saved.getScore())
                                .totalQuestions(saved.getTotalQuestions())
                                .durationMs(Duration.between(saved.getStartedAt(), saved.getCompletedAt()).toMillis())
                                .build()));
    }

    @Transactional
    public Mono<StartOrResumeResponse> retake(QuizSession session, String userLocale) {
        SessionStatus oldStatus = session.getStatus();
        session.setScore(0);
        session.setAnsweredQuestions(0);
        session.setStartedAt(Instant.now());
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setCompletedAt(null);
        return quizAnswerRepository.deleteBySessionId(session.getId())
                .then(quizSessionRepository.save(session))
                .flatMap(saved -> outboxEventCreator.createAndSaveSessionStatusChangedEvent(
                        new QuizSessionStatusChangedEvent(saved.getId(), saved.getUserId(),
                                saved.getLessonId(), saved.getLessonType(),
                                oldStatus.name(), saved.getStatus().name(), Instant.now()))
                        .then(buildResumeResponse(saved)));
    }

    // ===================== private =====================

    private Mono<AnswerResponse> processAnswer(QuizSession session, UUID userId, AnswerRequest request,
                                               SessionQuestion stored) {
        if ("MATCHING".equals(stored.getQuestionType())) {
            return processMatchingAnswer(session, userId, request, stored);
        }
        String correctAnswer = stored.getCorrectAnswer();
        String selectedText = resolveSelectedText(stored, request);
        boolean isCorrect = selectedText != null && selectedText.equals(correctAnswer);
        UUID correctOptionId = composedQuestionMapper.findCorrectOptionId(stored.getOptions(), correctAnswer);

        QuizAnswer newAnswer = QuizAnswer.builder()
                .id(null)
                .sessionId(session.getId())
                .questionId(request.getQuestionId())
                .selectedOptionId(request.getSelectedOptionId())
                .selectedFormIast(selectedText)
                .correctFormIast(correctAnswer)
                .isCorrect(isCorrect)
                .responseTimeMs(request.getResponseTimeMs())
                .answeredAt(Instant.now())
                .build();

        int questionNumber = session.getAnsweredQuestions() + 1;
        return quizAnswerRepository.save(newAnswer)
                .then(quizSessionRepository.incrementAnsweredQuestionsAndScore(session.getId(), isCorrect))
                .then(quizSessionScoreRepositoryUpdater(userId, stored, isCorrect))
                .then(outboxEventCreator.createAndSaveQuizAnsweredEvent(
                        new QuizAnsweredEvent(session.getId(), userId, session.getLessonId(),
                                session.getLessonType(), request.getQuestionId(),
                                selectedText, isCorrect, Instant.now())))
                .thenReturn(AnswerResponse.builder()
                        .correct(isCorrect)
                        .correctOptionId(correctOptionId)
                        .correctOptionIds(correctOptionId == null ? null : List.of(correctOptionId))
                        .correctAnswerText(correctAnswer)
                        .explanationRu(stored.getExplanationRu())
                        .explanationEn(stored.getExplanationEn())
                        .questionNumber(questionNumber)
                        .totalQuestions(session.getTotalQuestions())
                        .build());
    }

    /**
     * MATCHING verification: the client pairs each word-form row with a case+number label.
     * Every submitted pair must equal the reference pair from the payload and all rows must
     * be answered, otherwise the question counts as incorrect.
     */
    private Mono<AnswerResponse> processMatchingAnswer(QuizSession session, UUID userId, AnswerRequest request,
                                                       SessionQuestion stored) {
        Map<UUID, String[]> referencePairs = composedQuestionMapper.parseMatchPairMap(stored.getPayload());
        Map<UUID, String[]> labelMap = composedQuestionMapper.parseMatchLabelMap(stored.getOptions());
        List<MatchSubmissionDto> submissions = request.getMatchSubmissions() == null
                ? List.of() : request.getMatchSubmissions();

        boolean isCorrect = false;
        int correctCount = 0;
        if (!submissions.isEmpty() && submissions.size() == referencePairs.size()) {
            boolean allMatched = true;
            for (MatchSubmissionDto sub : submissions) {
                String[] expected = referencePairs.get(sub.rowId());
                String[] actual = labelMap.get(sub.optionId());
                if (expected == null || actual == null
                        || !expected[0].equals(actual[0]) || !expected[1].equals(actual[1])) {
                    allMatched = false;
                    break;
                }
                correctCount++;
            }
            isCorrect = allMatched && correctCount == referencePairs.size();
        }

        String summary = correctCount + "/" + referencePairs.size();
        QuizAnswer newAnswer = QuizAnswer.builder()
                .id(null)
                .sessionId(session.getId())
                .questionId(request.getQuestionId())
                .selectedOptionId(null)
                .selectedFormIast(summary)
                .correctFormIast(null)
                .isCorrect(isCorrect)
                .responseTimeMs(request.getResponseTimeMs())
                .answeredAt(Instant.now())
                .build();

        int questionNumber = session.getAnsweredQuestions() + 1;
        return quizAnswerRepository.save(newAnswer)
                .then(quizSessionRepository.incrementAnsweredQuestionsAndScore(session.getId(), isCorrect))
                .then(quizSessionScoreRepositoryUpdater(userId, stored, isCorrect))
                .then(outboxEventCreator.createAndSaveQuizAnsweredEvent(
                        new QuizAnsweredEvent(session.getId(), userId, session.getLessonId(),
                                session.getLessonType(), request.getQuestionId(),
                                summary, isCorrect, Instant.now())))
                .thenReturn(AnswerResponse.builder()
                        .correct(isCorrect)
                        .correctAnswerText(null)
                        .explanationRu(stored.getExplanationRu())
                        .explanationEn(stored.getExplanationEn())
                        .questionNumber(questionNumber)
                        .totalQuestions(session.getTotalQuestions())
                        .build());
    }

    /**
     * Writes progress for an answered quest item: keyed as ({@link QuestProgressTypes
     * resolved item type}, progress_tag) in {@code quiz_item_score}. The progress tag
     * groups all quest items sharing the same morphology attributes (case+number+gender)
     * or vocabulary lemma into a single progress row.
     */
    private Mono<sm.selflearn.samskrtam.quiz.model.QuizItemScore> quizSessionScoreRepositoryUpdater(
            UUID userId, SessionQuestion stored, boolean isCorrect) {
        String progressTag = stored.getProgressTag();
        if (progressTag == null || progressTag.isBlank()) {
            log.warn("No progressTag on session_question {}, skipping progress update", stored.getQuestionId());
            return Mono.empty();
        }
        return quizItemScoreService.upsertScore(
                userId, QuestProgressTypes.resolve(stored.getItemType()), progressTag, isCorrect);
    }

    /**
     * The selected answer text: by option id (choice questions) or the raw submitted text
     * (FREE_TEXT fallback). Null when the client sent no usable selection → counted incorrect.
     */
    private String resolveSelectedText(SessionQuestion stored, AnswerRequest request) {
        if (request.getSelectedOptionId() != null) {
            return composedQuestionMapper.resolveOptionText(stored.getOptions(), request.getSelectedOptionId());
        }
        if (request.getSelectedFormIast() != null && !request.getSelectedFormIast().isBlank()) {
            return request.getSelectedFormIast();
        }
        return null;
    }

    private Mono<StartOrResumeResponse> buildResumeResponse(QuizSession session) {
        return sessionQuestionRepository.findBySessionId(session.getId())
                .sort(Comparator.comparingInt(SessionQuestion::getQuestionNumber))
                .map(composedQuestionMapper::toQuestionDto)
                .collectList()
                .map(questions -> toStartOrResumeResponse(session, questions));
    }

    private StartOrResumeResponse toStartOrResumeResponse(QuizSession session, List<QuestionDto> questions) {
        int answered = session.getAnsweredQuestions();
        return StartOrResumeResponse.builder()
                .sessionId(session.getId())
                .lessonId(null)
                .lessonType(null)
                .questions(questions)
                .totalQuestions(session.getTotalQuestions())
                .answeredQuestions(answered)
                .score(session.getScore())
                .currentQuestionIndex(Math.min(answered, session.getTotalQuestions()))
                .currentQuestionNumber(Math.min(answered + 1, session.getTotalQuestions()))
                .build();
    }

    private Mono<StartOrResumeResponse> setInProgress(QuizSession session) {
        SessionStatus oldStatus = session.getStatus();
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setCompletedAt(null);
        return quizSessionRepository.save(session)
                .flatMap(saved -> outboxEventCreator.createAndSaveSessionStatusChangedEvent(
                        new QuizSessionStatusChangedEvent(saved.getId(), saved.getUserId(),
                                saved.getLessonId(), saved.getLessonType(),
                                oldStatus.name(), saved.getStatus().name(), Instant.now()))
                        .then(buildResumeResponse(saved)));
    }
}