package sm.selflearn.samskrtam.curriculum.questsession.dto;

import java.util.List;
import java.util.UUID;

/**
 * One topic in a session composition request.
 *
 * <p>Exactly one of the two selection modes must be used:
 * <ul>
 *   <li>{@code byCount} — request {@code count} random materialized questions from the topic
 *       pool (random sample, used for ad-hoc / mixed sessions).</li>
 *   <li>{@code byIds} — request the exact question ids listed in {@code itemIds} (used when
 *       quiz-service has already picked questions by learning progress and asks
 *       curriculum-service to render exactly those).</li>
 * </ul>
 *
 * @param topicCode stable unique identifier of the topic (curriculum.topic.code)
 * @param count     number of questions requested from this topic; ignored when {@code itemIds} is present
 * @param itemIds   exact quest-item ids to render; when present, supersedes {@code count}
 */
public record TopicItemSpec(
        String topicCode,
        int count,
        List<UUID> itemIds
) {
    public TopicItemSpec {
        itemIds = itemIds == null ? List.of() : List.copyOf(itemIds);
    }

    /** Random-sample request mode. */
    public static TopicItemSpec byCount(String topicCode, int count) {
        return new TopicItemSpec(topicCode, Math.max(count, 0), List.of());
    }

    /** Exact-ids request mode (progress-selected by quiz-service). */
    public static TopicItemSpec byIds(String topicCode, List<UUID> itemIds) {
        return new TopicItemSpec(topicCode, itemIds == null ? 0 : itemIds.size(), itemIds);
    }

    public boolean hasExplicitIds() {
        return !itemIds.isEmpty();
    }
}