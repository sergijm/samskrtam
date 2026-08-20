package sm.selflearn.samskrtam.curriculum.lexicon.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

/**
 * Композитный ключ {@link LexemeLexicalTopic}: пара (лексический урок, лексема).
 */
@Data
@Embeddable
public class LexemeLexicalTopicId implements Serializable {
    @Column(name = "lexical_topic_id", nullable = false)
    private UUID lexicalTopicId;

    @Column(name = "lexeme_id", nullable = false)
    private UUID lexemeId;
}
