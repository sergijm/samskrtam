package sm.selflearn.samskrtam.sangraha.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Учебная иерархия тем (lemma-classification.md §1.3). Код формата kebab-case;
 * корни имеют parent == null, листья ссылаются на корень.
 */
@Entity
@Table(name = "curriculum_semantic_class", schema = "sangraha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurriculumSemanticClass {

    @Id
    @Column(name = "code")
    private String code;

    @Column(name = "label_ru", nullable = false)
    private String labelRu;

    @Column(name = "label_en", nullable = false)
    private String labelEn;

    @Column(name = "description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "parent_code", referencedColumnName = "code")
    private CurriculumSemanticClass parent;
}