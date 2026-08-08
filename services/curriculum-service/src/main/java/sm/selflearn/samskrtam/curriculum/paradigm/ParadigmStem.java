package sm.selflearn.samskrtam.curriculum.paradigm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sm.selflearn.samskrtam.content.model.Gender;
import sm.selflearn.samskrtam.content.model.VowelType;

import java.util.UUID;

/**
 * Suppletive declension stem served by the v2 paradigm page (mirror of
 * {@code content.declension_stems} for PRON_* paradigms, populated by V9).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "declension_stem", schema = "curriculum")
public class ParadigmStem {

    @Id
    private UUID id;

    @Column(name = "stem_iast", nullable = false, unique = true, length = 120)
    private String stemIast;

    @Column(name = "stem_devanagari", length = 120)
    private String stemDevanagari;

    @Column(name = "translation_ru", length = 200)
    private String translationRu;

    @Column(name = "translation_en", length = 200)
    private String translationEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "vowel_type", nullable = false, length = 40)
    private VowelType vowelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 40)
    private Gender gender;
}
