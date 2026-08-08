package sm.selflearn.samskrtam.curriculum.paradigm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.content.model.VowelType;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ParadigmStemRepository extends JpaRepository<ParadigmStem, UUID> {

    List<ParadigmStem> findByVowelTypeIn(Collection<VowelType> vowelTypes);
}