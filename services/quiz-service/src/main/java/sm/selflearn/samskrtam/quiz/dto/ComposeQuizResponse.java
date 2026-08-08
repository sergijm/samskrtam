package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/**
 * Response of composing a curriculum-driven quiz session.
 *
 * @param sessionId            created session
 * @param totalQuestions       number of questions in the session
 * @param answeredQuestions    always 0 for a freshly composed session
 * @param score                always 0
 * @param currentQuestionIndex 0-based position of the first unanswered question
 * @param currentQuestionNumber 1-based number of the first unanswered question
 * @param questions            rendered questions (options from materialized distractors)
 */
@Value
@Builder
public class ComposeQuizResponse {
    UUID sessionId;
    int totalQuestions;
    int answeredQuestions;
    int score;
    int currentQuestionIndex;
    int currentQuestionNumber;
    List<QuestionDto> questions;
}