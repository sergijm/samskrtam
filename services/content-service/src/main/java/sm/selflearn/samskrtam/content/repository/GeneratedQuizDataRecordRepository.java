package sm.selflearn.samskrtam.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.content.model.GeneratedQuizDataRecord;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeneratedQuizDataRecordRepository extends JpaRepository<GeneratedQuizDataRecord, UUID> {
    Optional<GeneratedQuizDataRecord> findById(UUID id);
}
