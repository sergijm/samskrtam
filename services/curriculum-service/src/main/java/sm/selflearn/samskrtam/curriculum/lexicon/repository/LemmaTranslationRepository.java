package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LemmaTranslation;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface LemmaTranslationRepository extends JpaRepository<LemmaTranslation, UUID> {

    List<LemmaTranslation> findByLemma_LemmaIast(String lemmaIast);

    List<LemmaTranslation> findByLemma_LemmaIastAndLanguage(String lemmaIast, String language);

    Optional<LemmaTranslation> findByLemma_LemmaIastAndLanguageAndIsMainTrue(String lemmaIast, String language);

    List<LemmaTranslation> findByLanguage(String language);

    List<LemmaTranslation> findByLemma_LemmaIastIn(Set<String> lemmaIasts);
}