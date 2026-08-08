package sm.selflearn.samskrtam.quiz.dto;

import java.util.List;

/**
 * Client-side mirror of curriculum-service {@code QuizSessionComposeResponse}: a randomly
 * ordered sequence of ready-made questions spanning the requested topics.
 *
 * @param items composed questions (questionNumber 1-based, assigned after cross-topic shuffle)
 */
public record ComposedSessionResponseDto(
        List<ComposedQuestionDto> items
) {
}