package sm.selflearn.samskrtam.curriculum.questsession.dto;

import java.util.List;

/**
 * Request for session composition: a sequence of questions is built from the
 * requested topics, each topic contributing {@code count} questions.
 *
 * <p>Contract-first: see docs/services/curriculum-service/curriculum-session-composition.md.
 *
 * @param topics list of (topicCode, count) specs; empty list is rejected by the caller
 * @param userLocale optional user locale for localization hints in the payload
 */
public record QuizSessionComposeRequest(
        List<TopicItemSpec> topics,
        String userLocale
) {
    public QuizSessionComposeRequest {
        topics = topics == null ? List.of() : List.copyOf(topics);
        userLocale = userLocale == null ? null : userLocale;
    }
}