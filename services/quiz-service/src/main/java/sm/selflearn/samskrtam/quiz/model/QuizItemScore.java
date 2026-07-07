package sm.selflearn.samskrtam.quiz.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Единая модель прогресса для всех типов квизов (ADR-007).
 *
 * <p>Заменяет {@link WordScore} и {@link GrammarFormScore}.
 * Абстракция QuizItem = (itemType, externalRefId).
 * Нет строки = NEW (score не хранится, статус вычисляется лениво из score).
 *
 * <p>Физические FK на content-service отсутствуют — целостность эвентуальная (§2.2 спеки).
 *
 * @see ItemType
 * @see sm.selflearn.samskrtam.quiz.service.ScoreCalculator
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "quiz_item_score", schema = "quiz")
public class QuizItemScore {

    @Id
    private UUID id;

    private UUID userId;

    private ItemType itemType;

    private UUID externalRefId;

    /** Текущее значение 0-100. Расчёт по формуле §2.5. */
    private int score;

    /** Устойчивость к ошибке. Растёт при успехах, падает при ошибках. min=1. */
    private int stability;

    private Instant lastAnsweredAt;

    /** null, если ошибок не было */
    private Instant lastMistakeAt;

    /** Сбрасывается при успехе */
    private int consecutiveMistakes;

    /** Время следующего показа. Временная заглушка — фиксированный интервал. */
    private Instant nextReviewAt;

    private Instant updatedAt;
}