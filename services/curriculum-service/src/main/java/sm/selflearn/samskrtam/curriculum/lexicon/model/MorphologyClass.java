package sm.selflearn.samskrtam.curriculum.lexicon.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "morphology_class", schema = "curriculum")
public class MorphologyClass {
    @Id
    @Column(name = "code", length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "applies_to", nullable = false, length = 10)
    private MorphologyAppliesTo appliesTo;

    @Column(name = "name_ru", nullable = false, length = 60)
    private String nameRu;

    @Column(name = "name_en", nullable = false, length = 60)
    private String nameEn;
}