package sm.selflearn.samskrtam.eamenau.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.eamenau.model.Phoneme;

@Repository
public interface PhonemeRepository extends JpaRepository<Phoneme, Integer> {
}
