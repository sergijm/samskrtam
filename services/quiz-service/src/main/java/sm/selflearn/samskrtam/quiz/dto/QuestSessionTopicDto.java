package sm.selflearn.samskrtam.quiz.dto;

import java.util.List;
import java.util.UUID;

/**
 * One topic in a quiz compose request (universal engine, curriculum-driven).
 *
 * @param topicCode curriculum.topic.code (e.g. a-stem-masc)
 * @param count     number of questions requested from this topic
 * @param itemIds   exact quest-item ids to compose from (set by quiz-service after
 *                  progress selection); empty/null when the caller wants a random sample
 */
public record QuestSessionTopicDto(
        String topicCode,
        int count,
        List<UUID> itemIds
) {
    public QuestSessionTopicDto {
        itemIds = itemIds == null ? List.of() : List.copyOf(itemIds);
    }

    public static QuestSessionTopicDto byCount(String topicCode, int count) {
        return new QuestSessionTopicDto(topicCode, Math.max(count, 0), List.of());
    }

    public static QuestSessionTopicDto byIds(String topicCode, List<UUID> itemIds) {
        return new QuestSessionTopicDto(topicCode, itemIds == null ? 0 : itemIds.size(), itemIds);
    }
}