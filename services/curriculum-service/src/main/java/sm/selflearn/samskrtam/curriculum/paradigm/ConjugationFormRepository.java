package sm.selflearn.samskrtam.curriculum.paradigm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.morphology.Pada;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConjugationFormRepository extends JpaRepository<ConjugationForm, UUID> {

    List<ConjugationForm> findByTopicCodeOrderByLemmaIastAscVoiceAscPersonDescNumberTypeAsc(
            String topicCode);

    List<ConjugationForm> findByTopicCodeAndVoiceOrderByLemmaIastAscPersonDescNumberTypeAsc(
            String topicCode, Pada voice);
}