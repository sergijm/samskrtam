package sm.selflearn.samskrtam.content.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity
@Table(name = "declension_stems", schema = "content")
public class DeclensionStem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "stem_name_iast", nullable = false, unique = true)
    private String stemNameIast; // e.g., "deva", "agāra", "dhenu"

    @Column(name = "stem_name_devanagari")
    private String stemNameDevanagari;

    @Column(name = "translation_ru")
    private String translationRu;

    @Column(name = "translation_en")
    private String translationEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "vowel_type", nullable = false)
    private VowelType vowelType; // e.g., A_STEM, I_STEM, U_STEM, R_STEM

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender; // MASCULINE, FEMININE, NEUTER
}

