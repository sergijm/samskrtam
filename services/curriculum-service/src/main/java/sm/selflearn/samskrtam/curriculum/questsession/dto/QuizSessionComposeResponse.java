package sm.selflearn.samskrtam.curriculum.questsession.dto;

import java.util.List;

/**
 * Response of session composition: a randomly ordered sequence of ready-made
 * questions spanning all requested topics.
 *
 * @param items the composed sequence (questionNumber is 1-based, assigned after
 *              the cross-topic shuffle)
 */
public record QuizSessionComposeResponse(
        List<ComposedQuizItemDto> items
) {
}