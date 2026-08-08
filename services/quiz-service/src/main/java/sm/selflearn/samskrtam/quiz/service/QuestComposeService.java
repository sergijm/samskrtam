package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.quiz.dto.ComposedQuestionDto;
import sm.selflearn.samskrtam.quiz.dto.ComposedSessionResponseDto;
import sm.selflearn.samskrtam.quiz.dto.ComposeQuizResponse;
import sm.selflearn.samskrtam.quiz.dto.QuestComposeRequest;
import sm.selflearn.samskrtam.quiz.dto.QuestPoolItemDto;
import sm.selflearn.samskrtam.quiz.dto.QuestionDto;
import sm.selflearn.samskrtam.quiz.dto.QuestSessionTopicDto;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.model.QuizItem;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionQuestion;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;
import sm.selflearn.samskrtam.quiz.repository.SessionQuestionRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Composes a curriculum-driven quiz session (universal engine).
 *
 * <p>Division of labour (2026-08 architecture decision): curriculum-service renders the
 * questions (prompt + correctAnswer + distractors + payload are materialized offline);
 * quiz-service runs the progress selection on the topic pool ({@link QuizGenerator},
 * due/new/reserve on {@code quiz_item_score} keyed by quest_item.id), then asks
 * curriculum-service to compose exactly the selected items, persists the session with
 * the rendered options fixed at start, publishes the STARTED event and serves the response.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestComposeService {

    private final CurriculumClient curriculumClient;
    private final SessionFactory sessionFactory;
    private final QuizSessionRepository quizSessionRepository;
    private final SessionQuestionRepository sessionQuestionRepository;
    private final SessionPublisher sessionPublisher;
    private final ComposedQuestionMapper composedQuestionMapper;
    private final QuizGenerator quizGenerator;

    @Transactional
    public Mono<ComposeQuizResponse> compose(UUID userId, QuestComposeRequest request) {
        if (request.topics() == null || request.topics().isEmpty()) {
            return Mono.error(new SamskrtamException("COMPOSE_TOPICS_EMPTY",
                    "Session composition requires at least one topic"));
        }
        return selectPerTopic(userId, request)
                .flatMap(selectedTopics -> {
                    if (selectedTopics.isEmpty()) {
                        return Mono.error(new SamskrtamException("COMPOSE_SELECTION_EMPTY",
                                "No questions selected for the requested topics"));
                    }
                    return curriculumClient.composeSession(selectedTopics, request.userLocale())
                            .flatMap(composed -> {
                                if (composed == null || composed.items() == null || composed.items().isEmpty()) {
                                    return Mono.error(new SamskrtamException("COMPOSE_EMPTY",
                                            "Curriculum-service returned no questions for the requested topics"));
                                }
                                List<ComposedQuestionDto> items = composed.items();
                                QuizSession newSession = sessionFactory.createComposedSession(userId, items.size());
                                return quizSessionRepository.save(newSession)
                                        .flatMap(savedSession -> {
                                            List<SessionQuestion> sessionQuestions = new ArrayList<>();
                                            for (ComposedQuestionDto item : items) {
                                                sessionQuestions.add(
                                                        composedQuestionMapper.toSessionQuestion(savedSession.getId(), item));
                                            }
                                            return sessionQuestionRepository.saveAll(sessionQuestions)
                                                    .then(sessionPublisher.publishStarted(savedSession))
                                                    .then(Mono.fromCallable(() -> assembleResponse(savedSession, sessionQuestions)));
                                        });
                            });
                });
    }

    /**
     * Progress-based selection per requested topic: fetches the topic pool, groups it by the
     * progress {@link ItemType}, runs {@link QuizGenerator} (due/new/reserve) per group and
     * interleaves the results up to the requested {@code count}. Produces the topics with
     * exact {@code itemIds} for curriculum compose.
     */
    private Mono<List<QuestSessionTopicDto>> selectPerTopic(UUID userId, QuestComposeRequest request) {
        return Flux.fromIterable(request.topics())
                .concatMap(spec -> selectTopic(userId, spec))
                .collectList();
    }

    private Mono<QuestSessionTopicDto> selectTopic(UUID userId, QuestSessionTopicDto spec) {
        return curriculumClient.fetchTopicPool(spec.topicCode())
                .flatMap(pool -> {
                    if (pool.isEmpty()) {
                        return Mono.error(new SamskrtamException("TOPIC_POOL_EMPTY",
                                "No materialized quest items for topic: " + spec.topicCode()));
                    }
                    Map<ItemType, List<QuestPoolItemDto>> byType = pool.stream()
                            .collect(Collectors.groupingBy(
                                    p -> QuestProgressTypes.resolve(p.itemType()),
                                    LinkedHashMap::new,
                                    Collectors.toList()));
                    return selectByProgress(userId, byType, spec.count())
                            .map(selected -> {
                                if (selected.isEmpty()) {
                                    throw new SamskrtamException("TOPIC_SELECTION_EMPTY",
                                            "No questions selected by progress for topic: " + spec.topicCode());
                                }
                                return QuestSessionTopicDto.byIds(spec.topicCode(), selected);
                            });
                });
    }

    private Mono<List<UUID>> selectByProgress(UUID userId, Map<ItemType, List<QuestPoolItemDto>> byType, int count) {
        List<Mono<List<QuizItem>>> perType = byType.entrySet().stream()
                .map(e -> quizGenerator.generate(userId, e.getKey(), e.getValue()))
                .toList();
        return Flux.mergeSequential(perType)
                .collectList()
                .map(groups -> QuestSelectionPlanner.takeRoundRobin(groups, count)
                        .stream()
                        .map(QuizItem::externalRefId)
                        .toList());
    }

    private ComposeQuizResponse assembleResponse(QuizSession session, List<SessionQuestion> questions) {
        List<QuestionDto> questionDtos = questions.stream()
                .map(composedQuestionMapper::toQuestionDto)
                .toList();
        return ComposeQuizResponse.builder()
                .sessionId(session.getId())
                .totalQuestions(session.getTotalQuestions())
                .answeredQuestions(0)
                .score(0)
                .currentQuestionIndex(0)
                .currentQuestionNumber(1)
                .questions(questionDtos)
                .build();
    }
}
