package sm.selflearn.samskrtam.sangraha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.sangraha.model.Verse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerseRepository extends JpaRepository<Verse, UUID> {

    List<Verse> findAllByChapterIdAndDeletedAtIsNullOrderByOrderIndexAsc(UUID chapterId);

    Optional<Verse> findByIdAndDeletedAtIsNull(UUID id);

    int countByChapterIdAndDeletedAtIsNull(UUID chapterId);
}