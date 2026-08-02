package sm.selflearn.samskrtam.sangraha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.sangraha.model.NominalLemma;

import java.util.Collection;
import java.util.List;

@Repository
public interface NominalLemmaRepository extends JpaRepository<NominalLemma, Long> {

    List<NominalLemma> findByLemmaIastIn(Collection<String> lemmaIasts);
}
