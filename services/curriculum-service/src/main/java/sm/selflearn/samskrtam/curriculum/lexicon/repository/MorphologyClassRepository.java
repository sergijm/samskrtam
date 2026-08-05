package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.MorphologyClass;

import java.util.Optional;

@Repository
public interface MorphologyClassRepository extends JpaRepository<MorphologyClass, String> {
    Optional<MorphologyClass> findByCode(String code);
}