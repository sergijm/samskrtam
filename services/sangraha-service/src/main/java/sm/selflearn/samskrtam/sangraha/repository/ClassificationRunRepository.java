package sm.selflearn.samskrtam.sangraha.repository;

import sm.selflearn.samskrtam.sangraha.model.ClassificationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClassificationRunRepository extends JpaRepository<ClassificationRun, UUID> {
}