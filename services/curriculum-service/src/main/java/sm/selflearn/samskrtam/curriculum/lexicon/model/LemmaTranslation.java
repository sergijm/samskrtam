package sm.selflearn.samskrtam.curriculum.lexicon.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lemma_translation", schema = "curriculum")
public class LemmaTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lemma_iast", nullable = false, length = 120)
    private String lemmaIast;

    @Column(name = "language", nullable = false, length = 10)
    private String language;

    @Column(name = "gloss", nullable = false, length = 300)
    private String gloss;

    @Column(name = "pos", length = 40)
    private String pos;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private LexemeGender gender;

    @Column(name = "is_main", nullable = false)
    private boolean isMain;

    @Column(name = "freq_order")
    private Integer freqOrder;
}
