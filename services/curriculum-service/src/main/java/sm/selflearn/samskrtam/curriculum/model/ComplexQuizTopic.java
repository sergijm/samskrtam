package sm.selflearn.samskrtam.curriculum.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "complex_quiz_topic", schema = "curriculum")
public class ComplexQuizTopic {
    @EmbeddedId
    private ComplexQuizTopicId id;
}
