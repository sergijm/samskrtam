package sm.selflearn.samskrtam.curriculum.lexicon.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@Embeddable
public class LemmaSemanticClassId implements Serializable {
    @Column(name = "lemma_id", nullable = false)
    private UUID lemmaId;

    @Column(name = "semantic_class_id", nullable = false)
    private UUID semanticClassId;
}
