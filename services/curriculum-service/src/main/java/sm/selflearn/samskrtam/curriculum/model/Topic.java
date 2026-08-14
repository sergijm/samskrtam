package sm.selflearn.samskrtam.curriculum.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "topic", schema = "curriculum")
public class Topic {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 80)
    private String code;

    @Column(name = "title_ru", nullable = false, length = 200)
    private String titleRu;

    @Column(name = "title_en", nullable = false, length = 200)
    private String titleEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "learning_level", nullable = false, length = 2)
    private LearningLevel learningLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "domain", nullable = false, length = 25)
    private TopicDomain domain = TopicDomain.GRAMMAR;

    /** Coarse top-level classifier: GRAMMAR or LEXICON. */
    @Enumerated(EnumType.STRING)
    @Column(name = "domain_type", nullable = false, length = 16)
    private TopicDomainType domainType = TopicDomainType.GRAMMAR;

    /** Semantic classifier node the lesson is built from (LEXICON lessons); null otherwise. */
    @Column(name = "semantic_topic_id")
    private UUID semanticTopicId;

    @Column(name = "is_evergreen", nullable = false)
    private boolean isEvergreen;

    @Column(name = "display_order")
    private Short displayOrder;

    @Column(name = "target_item_count", nullable = false)
    private int targetItemCount = 0;

    @Column(name = "hidden", nullable = false)
    private boolean hidden;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
