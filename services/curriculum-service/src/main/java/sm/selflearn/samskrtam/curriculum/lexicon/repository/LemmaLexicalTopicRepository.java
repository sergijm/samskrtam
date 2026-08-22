package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LemmaLexicalTopic;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LemmaLexicalTopicId;

import java.util.List;

@Repository
public interface LemmaLexicalTopicRepository extends JpaRepository<LemmaLexicalTopic, LemmaLexicalTopicId> {

    List<LemmaLexicalTopic> findByIdTopicCode(String topicCode);

    @Query("SELECT DISTINCT l.id.lemmaIast FROM LemmaLexicalTopic l WHERE l.id.topicCode = :code")
    List<String> findDistinctLemmaIastByTopicCode(@Param("code") String code);

    void deleteByIdTopicCode(String topicCode);

    boolean existsByIdTopicCodeAndIdLemmaIast(String topicCode, String lemmaIast);
}
