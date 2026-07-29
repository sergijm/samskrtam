package sm.selflearn.samskrtam.quiz.model;

import io.r2dbc.postgresql.codec.Json;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import sm.selflearn.samskrtam.content.dto.LessonType;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@Table(name = "quiz_session", schema = "quiz")
public class QuizSession {
    @Id
    private UUID id;
    private UUID userId;
    private UUID lessonId;
    private LessonType lessonType;
    private int totalQuestions;
    private int answeredQuestions;
    private int score;
    private SessionStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private String vocabularyWordsJson;

    // Filter columns for filtered quiz sessions (see docs/services/quiz-service/quiz-declension.md §3.4)
    private FilterScope filterScope;

    /**
     * JSONB array of caseType strings. Non-null only when filterScope = CASE_ONLY.
     * Example: ["NOMINATIVE","ACCUSATIVE"]
     */
    private Json filterCaseTypes;

    /**
     * JSONB array of numberType strings. Non-null only when filterScope = NUMBER_ONLY.
     * Example: ["SINGULAR","DUAL"]
     */
    private Json filterNumberTypes;

    /**
     * JSONB array of {caseType,numberType,gender} objects.
     * Non-null only when filterScope = CASE_NUMBER_GENDER.
     * Example: [{"caseType":"NOMINATIVE","numberType":"SINGULAR","gender":"MASCULINE"}]
     */
    private Json filterCombinations;

        /**
     * Status filter (NEW|LEARNING|REVIEW) for bucket-based quiz sessions.
     * Null when no status filter applied. Participates in IN_PROGRESS session
     * lookup for resume: a session with one statusFilter is not resumed
     * by a click on a badge with a different statusFilter (or without one).
     */
    private StatusFilter statusFilter;

    /**
     * JSONB array of vowelType strings. Non-null only when filterScope = ALL_STEMS.
     * Example: ["A_STEM","AA_STEM","I_STEM"]
     */
    private Json filterVowelTypes;

    /**
     * JSONB array of gender strings. Non-null only when filterScope = ALL_STEMS.
     * Example: ["MASCULINE","FEMININE"]
     */
    private Json filterGenders;
}

