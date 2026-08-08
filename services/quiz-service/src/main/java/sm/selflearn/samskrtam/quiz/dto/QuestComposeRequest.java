package sm.selflearn.samskrtam.quiz.dto;

import java.util.List;

/**
 * Request to compose a quiz session from curriculum topics (universal engine).
 *
 * <p>Contract-first: see docs/services/curriculum-session-composition.md §2.
 * Questions are materialized by curriculum-service (prompt + options/distractors
 * fixed at session start); quiz-service stores and serves them.
 *
 * @param topics     non-empty list of (topicCode, count); grammar + lexical topics may be mixed
 * @param userLocale optional user locale hint
 */
public record QuestComposeRequest(
        List<QuestSessionTopicDto> topics,
        String userLocale
) {
}