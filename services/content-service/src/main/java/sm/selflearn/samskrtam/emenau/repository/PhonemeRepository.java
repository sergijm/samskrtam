package sm.selflearn.samskrtam.emenau.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.emenau.model.Phoneme;

@Repository
public interface PhonemeRepository extends JpaRepository<Phoneme, Integer> {
}
