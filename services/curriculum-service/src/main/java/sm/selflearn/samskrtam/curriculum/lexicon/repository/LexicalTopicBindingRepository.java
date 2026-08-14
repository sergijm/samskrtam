package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexicalTopicBinding;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexicalTopicBindingId;

import java.util.List;
import java.util.UUID;

@Repository
public interface LexicalTopicBindingRepository
        extends JpaRepository<LexicalTopicBinding, LexicalTopicBindingId> {

    List<LexicalTopicBinding> findByIdLexicalTopicId(UUID lexicalTopicId);
}
