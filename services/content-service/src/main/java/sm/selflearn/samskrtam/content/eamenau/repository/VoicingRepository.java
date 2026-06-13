package sm.selflearn.samskrtam.content.eamenau.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.content.eamenau.model.Voicing;

@Repository
public interface VoicingRepository extends JpaRepository<Voicing, Integer> {
}
