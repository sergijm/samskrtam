package sm.selflearn.samskrtam.quiz.dto;

/**
 * Сводка прогресса пользователя по области обучения (scope).
 *
 * <p>«Реальное» значение: считается только по таблице {@code quiz.quiz_item_score}
 * (система записи прогресса — quiz-service). {@code totalProgressTags} — число
 * различных progress_tag, по которым у пользователя есть записи в рамках
 * области; {@code percent} — средний процент освоенности по этим тегам.
 */
public record ProgressSummaryDto(
        String scope,
        int totalProgressTags,
        int masteredProgressTags,
        int learnedProgressTags,
        int percent) {
}
