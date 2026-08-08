package sm.selflearn.samskrtam.sangraha.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Классификатор произведений (works_class): иерархия категорий с id→parent_id.
 * {@code classification} — имя классификатора (например GENRE, ERA), внутри
 * которого строится дерево через {@code parentId}. Связь с {@link Work} —
 * many-to-many через таблицу works_work_class.
 */
@Entity
@Table(name = "works_class", schema = "sangraha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorksClass {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false)
    private String classification;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "title_sa_iast", nullable = false)
    private String titleSaIast;

    @Column(name = "title_sa_deva")
    private String titleSaDeva;

    @Column(name = "title_ru", nullable = false)
    private String titleRu;

    @Column(name = "title_en", nullable = false)
    private String titleEn;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}