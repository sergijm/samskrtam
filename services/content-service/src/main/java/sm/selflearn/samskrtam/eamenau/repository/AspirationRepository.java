package sm.selflearn.samskrtam.eamenau.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.eamenau.model.Aspiration;

@Repository
public interface AspirationRepository extends JpaRepository<Aspiration, Integer> {
}
