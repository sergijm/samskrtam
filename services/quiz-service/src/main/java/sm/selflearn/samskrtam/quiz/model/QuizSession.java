package sm.selflearn.samskrtam.quiz.model;

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

    /**
     * Стабильный идентификатор прогресс-сета, по которому запущена/резюмируется сессия
     * (NEW/LEARNING/MASTERED/DIFFICULT/SINGULAR/DUAL/PLURAL/ACC_LOC/INS_ABL/GEN_LOC/DAT_ACC).
     * Null — сессия по всему уроку без среза. Участвует в поиске IN_PROGRESS-сессии
     * для резюма по равенству (см. quest-engine.md §2.4, quiz-declension.md §3.4).
     */
    private String progressTagSetId;
}

