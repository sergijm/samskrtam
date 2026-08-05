package sm.selflearn.samskrtam.curriculum.lexicon.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sm.selflearn.samskrtam.curriculum.model.Topic;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lexical_topic_binding", schema = "curriculum")
public class LexicalTopicBinding {
    @EmbeddedId
    private LexicalTopicBindingId id = new LexicalTopicBindingId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("lexicalTopicId")
    @JoinColumn(name = "lexical_topic_id", nullable = false)
    private Topic lexicalTopic;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("lexemeId")
    @JoinColumn(name = "lexeme_id", nullable = false)
    private Lexeme lexeme;
}