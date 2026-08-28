package sm.selflearn.samskrtam.sangraha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.sangraha.model.Chapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, UUID> {

    List<Chapter> findAllByWorkIdAndDeletedAtIsNullOrderByOrderIndexAsc(UUID workId);

    long countByWorkIdAndDeletedAtIsNull(UUID workId);

    Optional<Chapter> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByWorkIdAndSlug(UUID workId, String slug);
}