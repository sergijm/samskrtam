package sm.selflearn.samskrtam.emenau.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.emenau.model.Aspiration;

@Repository
public interface AspirationRepository extends JpaRepository<Aspiration, Integer> {
}
