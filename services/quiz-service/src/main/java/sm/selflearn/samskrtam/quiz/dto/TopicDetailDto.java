package sm.selflearn.samskrtam.quiz.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Client mirror of curriculum-service {@code TopicDetailDto}
 * (GET /api/v2/curriculum/topics/{id}): a topic wrapped with its prerequisites.
 */
public record TopicDetailDto(TopicDto topic) {
    @JsonCreator
    public TopicDetailDto(@JsonProperty("topic") TopicDto topic) {
        this.topic = topic;
    }
}
