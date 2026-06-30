package sm.selflearn.samskrtam.emenau.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.emenau.model.MannerOfArticulation;

@Repository
public interface MannerOfArticulationRepository extends JpaRepository<MannerOfArticulation, Integer> {
}
