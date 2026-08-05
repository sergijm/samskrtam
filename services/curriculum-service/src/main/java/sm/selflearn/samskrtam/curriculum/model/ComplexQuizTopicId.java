package sm.selflearn.samskrtam.curriculum.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@Embeddable
public class ComplexQuizTopicId implements Serializable {
    @Column(name = "complex_quiz_id", nullable = false)
    private UUID complexQuizId;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;
}
