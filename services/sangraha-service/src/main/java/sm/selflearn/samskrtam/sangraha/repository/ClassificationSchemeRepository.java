package sm.selflearn.samskrtam.sangraha.repository;

import sm.selflearn.samskrtam.sangraha.model.ClassificationScheme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassificationSchemeRepository extends JpaRepository<ClassificationScheme, String> {

    List<ClassificationScheme> findByActiveTrue();
}