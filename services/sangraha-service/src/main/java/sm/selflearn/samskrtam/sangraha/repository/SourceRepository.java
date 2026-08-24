package sm.selflearn.samskrtam.sangraha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.sangraha.model.Source;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SourceRepository extends JpaRepository<Source, UUID> {

    Optional<Source> findByCode(String code);

    java.util.List<Source> findAllByOrderByCodeAsc();
}
