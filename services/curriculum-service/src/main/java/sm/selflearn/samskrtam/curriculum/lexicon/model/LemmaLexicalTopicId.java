package sm.selflearn.samskrtam.curriculum.lexicon.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;

@Data
@Embeddable
public class LemmaLexicalTopicId implements Serializable {
    @Column(name = "topic_code", nullable = false, length = 60)
    private String topicCode;

    @Column(name = "lemma_iast", nullable = false, length = 120)
    private String lemmaIast;
}
