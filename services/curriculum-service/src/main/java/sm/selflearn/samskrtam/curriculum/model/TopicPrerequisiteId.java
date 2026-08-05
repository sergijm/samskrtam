package sm.selflearn.samskrtam.curriculum.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@Embeddable
public class TopicPrerequisiteId implements Serializable {
    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(name = "prerequisite_topic_id", nullable = false)
    private UUID prerequisiteTopicId;
}
