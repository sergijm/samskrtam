package sm.selflearn.samskrtam.sangraha.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "verse_words", schema = "sangraha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerseWord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "verse_id", nullable = false)
    private UUID verseId;

    @Column(nullable = false)
    private int position;

    @Column(name = "surface_iast", nullable = false)
    private String surfaceIast;

    @Column(name = "surface_devanagari", nullable = false)
    private String surfaceDevanagari;

    @Column(name = "lemma_iast", nullable = false)
    private String lemmaIast;

    @Column(nullable = false)
    private String stem;

    private String root;

    @Enumerated(EnumType.STRING)
    private PartOfSpeech pos;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    private GrammaticalCase caseType;

    @Enumerated(EnumType.STRING)
    private NumberType numberType;

    @Enumerated(EnumType.STRING)
    private Person person;

    @Enumerated(EnumType.STRING)
    private Tense tense;

    @Enumerated(EnumType.STRING)
    private Mood mood;

    @Enumerated(EnumType.STRING)
    private Voice voice;

    @Column(name = "gloss_ru", nullable = false)
    private String glossRu;

    @Column(name = "gloss_en", nullable = false)
    private String glossEn;

    /** JSON-массив целых чисел (внутренние правила 1–40), сохраняется как текст */
    @Column(name = "formation_rule_numbers", columnDefinition = "TEXT")
    private String formationRuleNumbers;
}