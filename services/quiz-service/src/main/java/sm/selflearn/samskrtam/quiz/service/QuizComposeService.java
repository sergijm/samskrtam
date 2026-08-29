package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.quiz.config.ConjugationSessionProperties;
import sm.selflearn.samskrtam.quiz.config.DeclensionSessionProperties;
import sm.selflearn.samskrtam.quiz.config.QuizGeneratorConfig;
import sm.selflearn.samskrtam.quiz.dto.ComposedQuestionDto;
import sm.selflearn.samskrtam.quiz.dto.ComposeQuizResponse;
import sm.selflearn.samskrtam.quiz.dto.QuestItemDto;
import sm.selflearn.samskrtam.quiz.dto.QuestionDto;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.model.ProgressTagSetId;
import sm.selflearn.samskrtam.quiz.model.QuizItemScore;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionQuestion;
import sm.selflearn.samskrtam.quiz.repository.QuizItemScoreRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;
import sm.selflearn.samskrtam.quiz.repository.SessionQuestionRepository;
import sm.selflearn.samskrtam.quest.AnswerMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Creates a curriculum-driven quiz session using the window-select endpoint.
 * Replaces the old QuestComposeService / QuizGenerator / QuizProgressTagSetGenerator
 * / QuizStatusFilteredGenerator / QuestSelectionPlanner pipeline.
 *
 * <p>Supports single-topic sessions with optional progressTagSetId, itemType, answerMode.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizComposeService {

    private final CurriculumClient curriculumClient;
    private final SessionFactory sessionFactory;
    private final QuizSessionRepository quizSessionRepository;
    private final SessionQuestionRepository sessionQuestionRepository;
    private final SessionPublisher sessionPublisher;
    private final ComposedQuestionMapper composedQuestionMapper;
    private final QuizItemScoreRepository quizItemScoreRepository;
    private final QuizGeneratorConfig config;
    private final DeclensionSessionProperties declensionSessionProperties;
    private final ConjugationSessionProperties conjugationSessionProperties;

    @Transactional
    public Mono<ComposeQuizResponse> compose(
            UUID userId,
            String topicCode,
            ProgressTagSetId progressTagSetId,
            String itemType,
            String answerMode,
            int limit) {

        return selectItems(userId, topicCode, progressTagSetId, itemType, answerMode, limit)
                .flatMap(items -> {
                    if (items == null || items.isEmpty()) {
                        return Mono.error(new SamskrtamException("SELECT_EMPTY",
                                "No quest items selected for topic: " + topicCode));
                    }
                    QuizSession newSession = sessionFactory.createComposedSession(userId, items.size());
                    return quizSessionRepository.save(newSession)
                            .flatMap(savedSession -> {
List<SessionQuestion> questions = new ArrayList<>();
                                int seq = 1;
                                for (QuestItemDto item : items) {
                                    ComposedQuestionDto composed = new ComposedQuestionDto(
                                            seq, topicCode, item, item.progressTag());
                                    questions.add(composedQuestionMapper.toSessionQuestion(
                                            savedSession.getId(), composed));
                                    seq++;
                                }
                                return sessionQuestionRepository.saveAll(questions)
                                        .then(sessionPublisher.publishStarted(savedSession))
                                        .then(Mono.fromCallable(() -> {
                                            List<QuestionDto> dtos = questions.stream()
                                                    .map(composedQuestionMapper::toQuestionDto)
                                                    .toList();
                                            return ComposeQuizResponse.builder()
                                                    .sessionId(savedSession.getId())
                                                    .totalQuestions(savedSession.getTotalQuestions())
                                                    .answeredQuestions(0)
                                                    .score(0)
                                                    .currentQuestionIndex(0)
                                                    .currentQuestionNumber(1)
                                                    .questions(dtos)
                                                    .build();
                                        }));
                            });
                });
    }

    /**
     * Variant of {@link #compose(UUID, String, ProgressTagSetId, String, String, int)}
     * that identifies the topic by its {@code topicId} (UUID) rather than its code.
     * The legacy lesson-based flow passes the lessonId, which is in fact the
     * curriculum topicId, so the topic code is resolved first and then the normal
     * compose pipeline runs.
     */
    @Transactional
    public Mono<ComposeQuizResponse> composeByTopicId(
            UUID userId,
            UUID topicId,
            ProgressTagSetId progressTagSetId,
            String itemType,
            String answerMode,
            int limit) {
        return curriculumClient.fetchTopicById(topicId)
                .flatMap(topic -> compose(userId, topic.code(), progressTagSetId, itemType, answerMode, limit));
    }

    private Mono<List<QuestItemDto>> selectItems(
            UUID userId, String topicCode, ProgressTagSetId setId,
            String itemType, String answerMode, int limit) {
        int sessionSize = limit > 0 ? limit : config.getSessionSize().getSessionSize();

        if (setId == null) {
            return selectFullLesson(topicCode, itemType, answerMode, sessionSize);
        }

        return switch (setId) {
            case NEW -> selectNewItems(userId, topicCode, itemType, answerMode, sessionSize);
            case LEARNING -> selectStatusItems(userId, topicCode, itemType, answerMode, true);
            case MASTERED -> selectStatusItems(userId, topicCode, itemType, answerMode, false);
            case DIFFICULT -> selectDifficultItems(userId, topicCode, itemType, answerMode);
            case SINGULAR, DUAL, PLURAL, NOMINATIVE, ACCUSATIVE, INSTRUMENTAL, DATIVE, ABLATIVE,
                    GENITIVE, LOCATIVE, VOCATIVE, ACC_LOC, INS_ABL, GEN_LOC, DAT_ACC, GEN_ABL,
                 INS_LOC, DAT_GEN, ABL_LOC, NOM_ACC ->
                    selectGrammarItems(topicCode, setId, itemType, answerMode, sessionSize);
        };
    }

    /**
     * Full-lesson selection. Declension topics (curriculum domain
     * {@code NOMINAL_MORPHOLOGY}) get a fixed {@code answer_mode} mix
     * (SINGLE_CHOICE → MATCHING → FREE_TEXT); everything else keeps the
     * generic one-per-tag/type/mode window selection.
     */
    private Mono<List<QuestItemDto>> selectFullLesson(
            String topicCode, String itemType, String answerMode, int sessionSize) {
        return curriculumClient.fetchTopicByCode(topicCode)
                .flatMap(topic -> {
                    String domain = topic.domain();
                    if (isDeclensionTopic(domain)) {
                        return selectDeclensionMix(topicCode, null);
                    }
                    if (isConjugationTopic(domain)) {
                        return selectConjugationMix(topicCode, null);
                    }
                    return selectGeneric(topicCode, null, itemType, answerMode, sessionSize);
                })
                .switchIfEmpty(Mono.defer(() -> selectGeneric(topicCode, null, itemType, answerMode, sessionSize)));
    }

    private Mono<List<QuestItemDto>> selectDeclensionMix(String topicCode, List<String> progressTags) {
        return curriculumClient.selectQuestItems(topicCode, progressTags, null, null, 0)
                .map(all -> {
                    DeclensionSessionProperties props = declensionSessionProperties;
                    int singleChoiceTotal = props.getSingleChoiceCount() + props.getCaseRecognitionCount();
                    List<QuestItemDto> singleChoice = filterByAnswerMode(all, AnswerMode.SINGLE_CHOICE,
                            singleChoiceTotal);
                    List<QuestItemDto> matching = filterByAnswerMode(all, AnswerMode.MATCHING,
                            props.getMatchCount());
                    List<QuestItemDto> freeText = filterByAnswerMode(all, AnswerMode.FREE_TEXT,
                            props.getFreeTextCount());
                    List<QuestItemDto> ordered = new ArrayList<>();
                    ordered.addAll(singleChoice); // SINGLE_CHOICE first
                    ordered.addAll(matching);     // MATCHING
                    ordered.addAll(freeText);     // FREE_TEXT last
                    return ordered;
                });
    }

    private Mono<List<QuestItemDto>> selectConjugationMix(String topicCode, List<String> progressTags) {
        return curriculumClient.selectQuestItems(topicCode, progressTags, null, null, 0)
                .map(all -> {
                    ConjugationSessionProperties props = conjugationSessionProperties;
                    int singleChoiceTotal = props.getSingleChoiceCount() + props.getAnalysisCount();
                    List<QuestItemDto> singleChoice = filterByAnswerMode(all, AnswerMode.SINGLE_CHOICE,
                            singleChoiceTotal);
                    List<QuestItemDto> matching = filterByAnswerMode(all, AnswerMode.MATCHING,
                            props.getMatchCount());
                    List<QuestItemDto> freeText = filterByAnswerMode(all, AnswerMode.FREE_TEXT,
                            props.getFreeTextCount());
                    List<QuestItemDto> ordered = new ArrayList<>();
                    ordered.addAll(singleChoice);
                    ordered.addAll(matching);
                    ordered.addAll(freeText);
                    return ordered;
                });
    }

    private static List<QuestItemDto> filterByAnswerMode(List<QuestItemDto> items,
                                                         AnswerMode answerMode, int count) {
        return items.stream()
                .filter(i -> answerMode == i.answerMode())
                .limit(count)
                .toList();
    }

    private Mono<List<QuestItemDto>> selectGeneric(
            String topicCode, List<String> progressTags, String itemType, String answerMode, int sessionSize) {
        return curriculumClient.selectQuestItems(topicCode, progressTags, itemType, answerMode, 0)
                .map(list -> cap(list, sessionSize));
    }

    private static boolean isDeclensionTopic(String domain) {
        return "NOMINAL_MORPHOLOGY".equals(domain);
    }

    private static boolean isConjugationTopic(String domain) {
        return "VERBAL_MORPHOLOGY".equals(domain);
    }

    private static boolean isCaseSyntaxTopic(String domain) {
        return "CASE_SYNTAX".equals(domain);
    }

    /** NEW: fetch full lesson, then remove items whose progress_tag already has a score. */
    private Mono<List<QuestItemDto>> selectNewItems(
            UUID userId, String topicCode, String itemType, String answerMode, int sessionSize) {
        return curriculumClient.selectQuestItems(topicCode, null, itemType, answerMode, 0)
                .flatMap(allItems -> {
                    if (allItems.isEmpty()) return Mono.just(List.of());
                    ItemType resolvedType = resolveItemType(itemType);
                    return quizItemScoreRepository
                            .findByUserIdAndItemType(userId, resolvedType)
                            .map(QuizItemScore::getProgressTag)
                            .collectList()
                            .map(scoredTags -> {
                                Set<String> scored = Set.copyOf(scoredTags);
                                List<QuestItemDto> newItems = allItems.stream()
                                        .filter(i -> i.progressTag() == null
                                                || i.progressTag().isBlank()
                                                || !scored.contains(i.progressTag()))
                                        .collect(Collectors.toList());
                                Collections.shuffle(newItems);
                                return cap(newItems, sessionSize);
                            });
                });
    }

    /** LEARNING or MASTERED: query scores, extract matching tags, fetch items. */
    private Mono<List<QuestItemDto>> selectStatusItems(
            UUID userId, String topicCode, String itemType, String answerMode,
            boolean isLearning) {
        ItemType resolvedType = resolveItemType(itemType);
        return quizItemScoreRepository.findByUserIdAndItemType(userId, resolvedType)
                .collectList()
                .flatMap(scores -> {
                    int threshold = config.getBuckets().getMasteredLowerThreshold();
                    List<String> tags = scores.stream()
                            .filter(s -> isLearning ? s.getScore() < threshold : s.getScore() >= threshold)
                            .map(QuizItemScore::getProgressTag)
                            .distinct()
                            .collect(Collectors.toList());
                    if (tags.isEmpty()) return Mono.just(List.of());
                    return curriculumClient.selectQuestItems(topicCode, tags, itemType, answerMode, 0);
                });
    }

    /** DIFFICULT: score < difficultUpperThreshold. */
    private Mono<List<QuestItemDto>> selectDifficultItems(
            UUID userId, String topicCode, String itemType, String answerMode) {
        ItemType resolvedType = resolveItemType(itemType);
        return quizItemScoreRepository.findByUserIdAndItemType(userId, resolvedType)
                .collectList()
                .flatMap(scores -> {
                    int threshold = config.getBuckets().getDifficultUpperThreshold();
                    List<String> tags = scores.stream()
                            .filter(s -> s.getScore() < threshold)
                            .map(QuizItemScore::getProgressTag)
                            .distinct()
                            .collect(Collectors.toList());
                    if (tags.isEmpty()) return Mono.just(List.of());
                    return curriculumClient.selectQuestItems(topicCode, tags, itemType, answerMode, 0);
                });
    }

    /** Grammar sets: hardcode the tag collection, fetch one per tag. */
    private Mono<List<QuestItemDto>> selectGrammarItems(
            String topicCode, ProgressTagSetId setId, String itemType,
            String answerMode, int sessionSize) {
        return curriculumClient.fetchTopicByCode(topicCode)
                .flatMap(topic -> {
                    if (isCaseSyntaxTopic(topic.domain())) {
                        return curriculumClient.selectQuestItems(
                                topicCode, List.of(setId.name()), itemType, answerMode, 0)
                                .map(list -> cap(list, sessionSize));
                    }
                    List<String> tags = hardcodeTags(setId);
                    return curriculumClient.selectQuestItems(topicCode, tags, itemType, answerMode, 0)
                            .map(list -> cap(list, sessionSize));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    List<String> tags = hardcodeTags(setId);
                    return curriculumClient.selectQuestItems(topicCode, tags, itemType, answerMode, 0)
                            .map(list -> cap(list, sessionSize));
                }));
    }

    // ---- helpers ----

    private static List<QuestItemDto> cap(List<QuestItemDto> items, int max) {
        return items.size() <= max ? items : items.subList(0, max);
    }

    private static ItemType resolveItemType(String itemTypeCode) {
        if (itemTypeCode == null) return ItemType.DECLENSION_FORM;
        if (itemTypeCode.startsWith("DECLENSION_") || itemTypeCode.startsWith("CASE_")) {
            return ItemType.DECLENSION_FORM;
        }
        if (itemTypeCode.startsWith("CONJUGATION_")) {
            return ItemType.CONJUGATION_FORM;
        }
        return ItemType.VOCABULARY_WORD;
    }

    /** Generates all possible progress_tag values for a grammar set. */
    static List<String> hardcodeTags(ProgressTagSetId setId) {
        String[] allCases = {"NOMINATIVE", "ACCUSATIVE", "INSTRUMENTAL", "DATIVE",
                "ABLATIVE", "GENITIVE", "LOCATIVE", "VOCATIVE"};
        String[] allNumbers = {"SINGULAR", "DUAL", "PLURAL"};
        String[] allGenders = {"MASCULINE", "FEMININE", "NEUTER", "UNSPECIFIED"};

        List<String> cases = switch (setId) {
            case ACC_LOC -> List.of("ACCUSATIVE", "LOCATIVE");
            case INS_ABL -> List.of("INSTRUMENTAL", "ABLATIVE");
            case GEN_LOC -> List.of("GENITIVE", "LOCATIVE");
            case DAT_ACC -> List.of("DATIVE", "ACCUSATIVE");
            case GEN_ABL -> List.of("GENITIVE", "ABLATIVE");
            case INS_LOC -> List.of("INSTRUMENTAL", "LOCATIVE");
            case DAT_GEN -> List.of("DATIVE", "GENITIVE");
            case ABL_LOC -> List.of("ABLATIVE", "LOCATIVE");
            case NOM_ACC -> List.of("NOMINATIVE", "ACCUSATIVE");
            case NOMINATIVE -> List.of("NOMINATIVE");
            case ACCUSATIVE -> List.of("ACCUSATIVE");
            case INSTRUMENTAL -> List.of("INSTRUMENTAL");
            case DATIVE -> List.of("DATIVE");
            case ABLATIVE -> List.of("ABLATIVE");
            case GENITIVE -> List.of("GENITIVE");
            case LOCATIVE -> List.of("LOCATIVE");
            case VOCATIVE -> List.of("VOCATIVE");
            default -> List.of(allCases);
        };
        List<String> numbers = switch (setId) {
            case SINGULAR -> List.of("SINGULAR");
            case DUAL -> List.of("DUAL");
            case PLURAL -> List.of("PLURAL");
            default -> List.of(allNumbers);
        };
        List<String> result = new ArrayList<>();
        for (String c : cases) {
            for (String n : numbers) {
                for (String g : allGenders) {
                    result.add(c + "|" + n + "|" + g);
                }
            }
        }
        // Also add the bare case tag so it matches items with progress_tag = case
        // (e.g. CASE_MEANING items where tag is just "NOMINATIVE").
        result.addAll(cases);
        return result;
    }
}