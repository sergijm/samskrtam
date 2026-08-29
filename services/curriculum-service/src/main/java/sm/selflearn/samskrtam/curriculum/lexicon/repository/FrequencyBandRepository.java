package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.FrequencyBand;

import java.util.List;
import java.util.Optional;

@Repository
public interface FrequencyBandRepository extends JpaRepository<FrequencyBand, String> {
    List<FrequencyBand> findAllByOrderBySortOrderAsc();

    Optional<FrequencyBand> findByCode(String code);
}