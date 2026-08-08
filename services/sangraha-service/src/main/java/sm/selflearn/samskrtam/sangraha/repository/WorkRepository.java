package sm.selflearn.samskrtam.sangraha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.sangraha.model.Work;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkRepository extends JpaRepository<Work, UUID> {

    Optional<Work> findBySlugAndDeletedAtIsNull(String slug);

    List<Work> findAllByDeletedAtIsNullOrderByCreatedAtAsc();

    /**
     * Произведения по набору id, не удалённые, в порядке создания.
     * Используется фильтрацией по классификатору (works_work_class).
     */
    List<Work> findAllByIdInAndDeletedAtIsNullOrderByCreatedAtAsc(Collection<UUID> ids);

    boolean existsBySlug(String slug);
}