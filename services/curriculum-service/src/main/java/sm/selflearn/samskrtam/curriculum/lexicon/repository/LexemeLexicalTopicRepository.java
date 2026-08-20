package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeLexicalTopic;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeLexicalTopicId;

import java.util.List;
import java.util.UUID;

@Repository
public interface LexemeLexicalTopicRepository
        extends JpaRepository<LexemeLexicalTopic, LexemeLexicalTopicId> {

    List<LexemeLexicalTopic> findByIdLexicalTopicId(UUID lexicalTopicId);

    /** Clears all bindings of one lexical lesson (used to (re)populate frequency lessons). */
    long deleteByIdLexicalTopicId(UUID lexicalTopicId);
}
