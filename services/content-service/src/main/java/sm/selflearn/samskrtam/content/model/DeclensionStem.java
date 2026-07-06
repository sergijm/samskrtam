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

    @Column(name = "stem_iast", nullable = false, unique = true)
    private String stemIast;
    @Column(name = "stem_devanagari")
    private String stemDevanagari;

    @Column(name = "translation_ru")
    private String translationRu;

    @Column(name = "translation_en")
    private String translationEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "vowel_type", nullable = false)
    private VowelType vowelType;
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;
}

