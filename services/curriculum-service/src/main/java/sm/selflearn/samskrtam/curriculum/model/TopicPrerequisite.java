package sm.selflearn.samskrtam.curriculum.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "topic_prerequisite", schema = "curriculum")
public class TopicPrerequisite {
    @EmbeddedId
    private TopicPrerequisiteId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "strength", nullable = false, length = 20)
    private PrerequisiteStrength strength;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
