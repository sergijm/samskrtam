package sm.selflearn.samskrtam.curriculum.lexicon.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Связь лексического (в т.ч. VERSE) урока с леммой по её написанию
 * (lemma_iast), без обращения к сущности Lexeme. Аналог lexeme_lexical_topic,
 * но ключ — (topic_code, lemma_iast).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lemma_lexical_topic", schema = "curriculum")
public class LemmaLexicalTopic {

    @EmbeddedId
    private LemmaLexicalTopicId id = new LemmaLexicalTopicId();
}
