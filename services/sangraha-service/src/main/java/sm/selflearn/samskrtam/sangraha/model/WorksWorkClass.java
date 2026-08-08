package sm.selflearn.samskrtam.sangraha.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * Связь произведение ↔ категория классификатора (many-to-many).
 * Таблица works_work_class, композитный ключ (work_id, class_id).
 */
@Entity
@Table(name = "works_work_class", schema = "sangraha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorksWorkClass {

    @EmbeddedId
    private Key key;

    @Column(name = "work_id", insertable = false, updatable = false)
    private UUID workId;

    @Column(name = "class_id", insertable = false, updatable = false)
    private UUID classId;

    public WorksWorkClass(UUID workId, UUID classId) {
        this.key = new Key(workId, classId);
        this.workId = workId;
        this.classId = classId;
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {

        @Column(name = "work_id")
        private UUID workId;

        @Column(name = "class_id")
        private UUID classId;
    }
}