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

    /** Distinct lemma IASTs that have at least one stored paradigm cell in the given classes. */
    @Query("select distinct f.lemmaIast from ParadigmForm f where f.vowelType in :vowelTypes")
    List<String> findDistinctLemmaIastsByVowelTypeIn(@Param("vowelTypes") Collection<VowelType> vowelTypes);
}
