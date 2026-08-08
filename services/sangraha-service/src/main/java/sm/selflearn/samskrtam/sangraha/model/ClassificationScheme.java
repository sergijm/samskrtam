package sm.selflearn.samskrtam.sangraha.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Схема классификации (lemma-classification.md §1.2). CURRICULUM активна,
 * WORDNET выключена до наполнения справочника.
 */
@Entity
@Table(name = "classification_scheme", schema = "sangraha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassificationScheme {

    @Id
    @Column(name = "code")
    private String code;

    @Column(name = "title_ru", nullable = false)
    private String titleRu;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}