import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import sm.selflearn.samskrtam.content.dto.*;
import sm.selflearn.samskrtam.content.model.Case;
import sm.selflearn.samskrtam.content.model.Number;
import sm.selflearn.samskrtam.quiz.dto.GrammarLesson;
import sm.selflearn.samskrtam.quiz.dto.GrammarQuestionProgress;
import sm.selflearn.samskrtam.quiz.dto.WordStatus;
import sm.selflearn.samskrtam.quiz.model.GrammarFormScore;
import sm.selflearn.samskrtam.quiz.repository.GrammarFormScoreRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository;
import sm.selflearn.samskrtam.quiz.repository.WordScoreRepository;
import sm.selflearn.samskrtam.quiz.service.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock
    private ContentClient contentClient;
    @Mock
    private QuizDataAssembler quizDataAssembler;
    @Mock
    private UserSessionService userSessionService;
    @Mock
    private QuizAnswerRepository quizAnswerRepository;
    @Mock
    private WordScoreRepository wordScoreRepository;
    @Mock
    private GrammarFormScoreRepository grammarFormScoreRepository;
    @Mock
    private VocabularyProgressService vocabularyProgressService;
    @Mock
    private GrammarProgressService grammarProgressService;

    private LessonService lessonService;

    private final UUID lessonId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID stemId1 = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private final UUID stemId2 = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private final UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000100");
    private final String slug = "a-stem-declensions";

    private LessonItemResponse lessonItem;
    private DeclensionStemDto stem1;
    private DeclensionStemDto stem2;

    @BeforeEach
    void setUp() {
        lessonService = new LessonService(
                contentClient, userSessionService,
                quizAnswerRepository, wordScoreRepository, grammarFormScoreRepository,
                vocabularyProgressService, grammarProgressService);

        lessonItem = LessonItemResponse.builder()
                .id(lessonId)
                .slug(slug)
                .titleRu("А-основы (женский род)")
                .titleEn("A-stems (feminine)")
                .lessonType(LessonType.DECLENSIONS)
                .difficulty(Difficulty.BEGINNER)
                .totalQuestions(0)
                .wordCount(0)
                .build();

        stem1 = DeclensionStemDto.builder()
                .id(stemId1)
                .lessonId(lessonId)
                .slug("a-stem-fem")
                .build();

        stem2 = DeclensionStemDto.builder()
                .id(stemId2)
                .lessonId(lessonId)
                .slug("a-stem-fem-2")
                .build();
    }

    @Test
    @DisplayName("getGrammarLesson returns lesson with unique question pairs and deterministic IDs")
    void getGrammarLesson_happyPath_returnsLessonWithUniquePairs() {
        // Arrange
        when(contentClient.getLessonItemBySlug(slug)).thenReturn(Mono.just(lessonItem));
        when(contentClient.getDeclensionStemsForLesson(slug)).thenReturn(Mono.just(List.of(stem1, stem2)));

        // Stem 1 has 2 forms: Nominative Singular, Accusative Plural
        DeclensionFormDto stem1Form1 = DeclensionFormDto.builder()
                .declensionStemId(stemId1)
                .caseType(Case.NOMINATIVE)
                .numberType(Number.SINGULAR)
                .formIast("-a")
                .formDevanagari("-अ")
                .build();
        DeclensionFormDto stem1Form2 = DeclensionFormDto.builder()
                .declensionStemId(stemId1)
                .caseType(Case.ACCUSATIVE)
                .numberType(Number.PLURAL)
                .formIast("-a")
                .formDevanagari("-आ")
                .build();

        // Stem 2 has same pair (same case+number) to test deduplication
        DeclensionFormDto stem2Form1 = DeclensionFormDto.builder()
                .declensionStemId(stemId2)
                .caseType(Case.NOMINATIVE)
                .numberType(Number.SINGULAR)
                .formIast("-a")
                .formDevanagari("-अ")
                .build();

        when(contentClient.getDeclensionForms(stemId1)).thenReturn(Mono.just(List.of(stem1Form1, stem1Form2)));
        when(contentClient.getDeclensionForms(stemId2)).thenReturn(Mono.just(List.of(stem2Form1)));

        // Mock grammar form scores: stem1/nominative/singular has score 80, other pairs have no score
        GrammarFormScore scoreEntry = GrammarFormScore.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .lessonId(lessonId)
                .caseType("NOMINATIVE")
                .numberType("SINGULAR")
                .score(80)
                .updatedAt(Instant.now())
                .build();

        when(grammarFormScoreRepository.findByUserIdAndLessonIdAndCaseTypeAndNumberType(
                eq(userId), eq(lessonId), eq("NOMINATIVE"), eq("SINGULAR")))
                .thenReturn(Mono.just(scoreEntry));

        when(grammarFormScoreRepository.findByUserIdAndLessonIdAndCaseTypeAndNumberType(
                eq(userId), eq(lessonId), eq("ACCUSATIVE"), eq("PLURAL")))
                .thenReturn(Mono.empty());

        // Act
        Mono<GrammarLesson> result = lessonService.getGrammarLesson(slug, userId);

        // Assert
        StepVerifier.create(result)
                .assertNext(lesson -> {
                    assertThat(lesson.getLessonId()).isEqualTo(lessonId);
                    assertThat(lesson.getType()).isEqualTo("DECLENSIONS");
                    assertThat(lesson.getTitleRu()).isEqualTo("А-основы (женский род)");
                    assertThat(lesson.getTitleEn()).isEqualTo("A-stems (feminine)");
                    assertThat(lesson.getDifficulty()).isEqualTo("BEGINNER");

                    // Should have 2 unique pairs (NOMINATIVE+SINGULAR, ACCUSATIVE+PLURAL)
                    // despite 3 total form entries (2 from stem1 + 1 from stem2)
                    assertThat(lesson.getTotalQuestions()).isEqualTo(2);

                    // One pair mastered (score=80 → MASTERED)
                    assertThat(lesson.getLearnedQuestions()).isEqualTo(1);
                    assertThat(lesson.getProgressPercent()).isEqualTo(50.0f);

                    List<GrammarQuestionProgress> questions = lesson.getQuestions();
                    assertThat(questions).hasSize(2);

                    // Check deterministic IDs
                    UUID expectedNomSingId = UUID.nameUUIDFromBytes(
                            (stemId1.toString() + ":NOMINATIVE:SINGULAR").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    UUID expectedAccPlId = UUID.nameUUIDFromBytes(
                            (stemId1.toString() + ":ACCUSATIVE:PLURAL").getBytes(java.nio.charset.StandardCharsets.UTF_8));

                    for (GrammarQuestionProgress q : questions) {
                        if (q.getQuestionId().equals(expectedNomSingId)) {
                            assertThat(q.getTextRu()).isEqualTo("Именительный, Единственное");
                            assertThat(q.getTextEn()).isEqualTo("Nominative, Singular");
                            assertThat(q.getCorrectAnswerRu()).isEqualTo("-a");
                            assertThat(q.getSuccessRate()).isEqualTo(80.0f);
                            assertThat(q.getStatus()).isEqualTo(WordStatus.MASTERED);
                        } else if (q.getQuestionId().equals(expectedAccPlId)) {
                            assertThat(q.getTextRu()).isEqualTo("Винительный, Множественное");
                            assertThat(q.getTextEn()).isEqualTo("Accusative, Plural");
                            assertThat(q.getCorrectAnswerRu()).isEqualTo("-a");
                            assertThat(q.getSuccessRate()).isEqualTo(0f);
                            assertThat(q.getStatus()).isEqualTo(WordStatus.NEW);
                        }
                    }
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getGrammarLesson deduplicates by questionId keeping max score")
    void getGrammarLesson_differentScoresForSameQuestion_keepsMaxScore() {
        // Arrange
        when(contentClient.getLessonItemBySlug(slug)).thenReturn(Mono.just(lessonItem));
        when(contentClient.getDeclensionStemsForLesson(slug)).thenReturn(Mono.just(List.of(stem1, stem2)));

        // Both stems have NOMINATIVE+SINGULAR (same pair)
        DeclensionFormDto form1 = DeclensionFormDto.builder()
                .declensionStemId(stemId1)
                .caseType(Case.NOMINATIVE)
                .numberType(Number.SINGULAR)
                .formIast("-a")
                .formDevanagari("-अ")
                .build();
        DeclensionFormDto form2 = DeclensionFormDto.builder()
                .declensionStemId(stemId2)
                .caseType(Case.NOMINATIVE)
                .numberType(Number.SINGULAR)
                .formIast("-ā")
                .formDevanagari("-आ")
                .build();

        when(contentClient.getDeclensionForms(stemId1)).thenReturn(Mono.just(List.of(form1)));
        when(contentClient.getDeclensionForms(stemId2)).thenReturn(Mono.just(List.of(form2)));

        // Different scores for same question from different stems
        // Stem1: score 30 (REVIEW)
        when(grammarFormScoreRepository.findByUserIdAndLessonIdAndCaseTypeAndNumberType(
                eq(userId), eq(lessonId), eq("NOMINATIVE"), eq("SINGULAR")))
                .thenReturn(Mono.just(
                        GrammarFormScore.builder()
                                .id(UUID.randomUUID())
                                .userId(userId)
                                .lessonId(lessonId)
                                .caseType("NOMINATIVE")
                                .numberType("SINGULAR")
                                .score(30) // REVIEW
                                .updatedAt(Instant.now())
                                .build()))
                .thenReturn(Mono.just(
                        GrammarFormScore.builder()
                                .id(UUID.randomUUID())
                                .userId(userId)
                                .lessonId(lessonId)
                                .caseType("NOMINATIVE")
                                .numberType("SINGULAR")
                                .score(70) // LEARNING (higher)
                                .updatedAt(Instant.now())
                                .build()));

        // Act
        Mono<GrammarLesson> result = lessonService.getGrammarLesson(slug, userId);

        // Assert
        StepVerifier.create(result)
                .assertNext(lesson -> {
                    assertThat(lesson.getTotalQuestions()).isEqualTo(1);
                    assertThat(lesson.getLearnedQuestions()).isEqualTo(0); // 70 < 80, so not MASTERED
                    List<GrammarQuestionProgress> questions = lesson.getQuestions();
                    assertThat(questions).hasSize(1);

                    // Should keep the higher score (70 → LEARNING)
                    GrammarQuestionProgress q = questions.get(0);
                    assertThat(q.getSuccessRate()).isEqualTo(70.0f);
                    assertThat(q.getStatus()).isEqualTo(WordStatus.LEARNING);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getGrammarLesson with no forms returns empty lesson")
    void getGrammarLesson_noForms_returnsEmptyLesson() {
        // Arrange
        when(contentClient.getLessonItemBySlug(slug)).thenReturn(Mono.just(lessonItem));
        when(contentClient.getDeclensionStemsForLesson(slug)).thenReturn(Mono.just(List.of(stem1)));
        when(contentClient.getDeclensionForms(stemId1)).thenReturn(Mono.just(List.of()));

        // Act
        Mono<GrammarLesson> result = lessonService.getGrammarLesson(slug, userId);

        // Assert
        StepVerifier.create(result)
                .assertNext(lesson -> {
                    assertThat(lesson.getTotalQuestions()).isEqualTo(0);
                    assertThat(lesson.getLearnedQuestions()).isEqualTo(0);
                    assertThat(lesson.getProgressPercent()).isEqualTo(0f);
                    assertThat(lesson.getQuestions()).isEmpty();
                })
                .verifyComplete();
    }
}
