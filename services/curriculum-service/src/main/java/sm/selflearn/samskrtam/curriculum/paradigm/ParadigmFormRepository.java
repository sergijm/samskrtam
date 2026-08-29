package sm.selflearn.samskrtam.curriculum.paradigm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.content.model.VowelType;

import java.util.Collection;
import java.util.List;

@Repository
public interface ParadigmFormRepository extends JpaRepository<ParadigmForm, ParadigmFormId> {

    List<ParadigmForm> findByLemmaIastAndVowelType(String lemmaIast, VowelType vowelType);

    /**
     * Distinct {@code (lemmaIast, vowelType)} pairs that have at least one stored
     * paradigm cell in the given declension classes. Used to enumerate the lemmas a
     * topic's paradigm page serves, directly from {@code curriculum.declension_form}.
     */
    @Query("select distinct f.lemmaIast as lemmaIast, f.vowelType as vowelType "
            + "from ParadigmForm f where f.vowelType in :vowelTypes")
    List<LemmaVowelType> findDistinctLemmaVowelTypeByVowelTypeIn(
            @Param("vowelTypes") Collection<VowelType> vowelTypes);

    /** Projection of a distinct {@code (lemmaIast, vowelType)} pair. */
    interface LemmaVowelType {
        String getLemmaIast();

        VowelType getVowelType();
    }
}
