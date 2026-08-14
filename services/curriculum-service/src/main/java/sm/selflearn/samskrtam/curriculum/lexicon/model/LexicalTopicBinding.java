package sm.selflearn.samskrtam.curriculum.lexicon.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Связь лексического урока (Topic.domain = VERSE) с лексемами пачки стиха
 * (lexicon-content-pipeline.md §7 шаг 3). Первичный ключ — (lexical_topic_id,
 * lexeme_id); оба хранятся id-ами (без JPA-отношения к Topic).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lexical_topic_binding", schema = "curriculum")
public class LexicalTopicBinding {

    @EmbeddedId
    private LexicalTopicBindingId id = new LexicalTopicBindingId();
}
