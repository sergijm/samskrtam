package sm.selflearn.samskrtam.curriculum.paradigm;

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
import sm.selflearn.samskrtam.content.model.NumberType;
import sm.selflearn.samskrtam.content.model.Voice;

import java.util.UUID;

/**
 * One present-tense conjugation paradigm cell (example sentence) of a verb
 * lemma, keyed by {@code (topic_code, lemma_iast, voice, person, number_type)}.
 * Serves the v2 conjugation-paradigm carousel.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "conjugation_forms", schema = "curriculum")
public class ConjugationForm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "topic_code", nullable = false, length = 80)
    private String topicCode;

    @Column(name = "lemma_iast", nullable = false, length = 120)
    private String lemmaIast;

    @Column(name = "lemma_devanagari", length = 120)
    private String lemmaDevanagari;

    @Column(name = "meaning_ru", length = 200)
    private String meaningRu;

    @Enumerated(EnumType.STRING)
    @Column(name = "voice", nullable = false, length = 40)
    private Voice voice;

    @Column(name = "person", nullable = false)
    private int person;

    @Enumerated(EnumType.STRING)
    @Column(name = "number_type", nullable = false, length = 40)
    private NumberType numberType;

    @Column(name = "sentence_iast", nullable = false, length = 300)
    private String sentenceIast;

    @Column(name = "sentence_devanagari", nullable = false, length = 300)
    private String sentenceDevanagari;

    @Column(name = "translation_ru", nullable = false, length = 300)
    private String translationRu;
}