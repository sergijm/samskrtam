package sm.selflearn.samskrtam.curriculum.lexicon.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lemma_semantic_class", schema = "curriculum")
public class LemmaSemanticClass {

    @EmbeddedId
    private LemmaSemanticClassId id = new LemmaSemanticClassId();
}
