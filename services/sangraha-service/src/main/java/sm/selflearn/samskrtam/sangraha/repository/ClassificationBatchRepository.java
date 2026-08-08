package sm.selflearn.samskrtam.sangraha.repository;

import sm.selflearn.samskrtam.sangraha.model.ClassificationBatch;
import sm.selflearn.samskrtam.sangraha.model.ClassificationBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassificationBatchRepository extends JpaRepository<ClassificationBatch, UUID> {

    List<ClassificationBatch> findByRunId(UUID runId);

    long countByRunIdAndStatus(UUID runId, ClassificationBatchStatus status);
}