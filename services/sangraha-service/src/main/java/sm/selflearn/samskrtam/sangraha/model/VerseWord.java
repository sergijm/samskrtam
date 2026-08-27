package sm.selflearn.samskrtam.sangraha.model;

import sm.selflearn.samskrtam.morphology.FormType;
import sm.selflearn.samskrtam.morphology.PartOfSpeech;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verse_id", nullable = false)
    private Verse verse;

    @Column(nullable = false)
    private int position;

    @Column(name = "surface_iast", nullable = false)
    private String surfaceIast;

    @Column(name = "surface_devanagari", nullable = false)
    private String surfaceDevanagari;

    @Column(name = "lemma_iast", nullable = false)
    private String lemmaIast;

    @Column(nullable = true)
    private String stem;

    @Column(nullable = true)
    private String root;

    @Enumerated(EnumType.STRING)
    private PartOfSpeech pos;

    @Enumerated(EnumType.STRING)
    @Column(name = "form_type")
    private FormType formType;

    @Column(name = "is_finite")
    private Boolean isFinite;

    @Column(name = "lemma_gloss_ru")
    private String lemmaGlossRu;

    @Column(name = "lemma_gloss_en")
    private String lemmaGlossEn;

    @Column(name = "context_gloss_ru", nullable = false)
    private String contextGlossRu;

    @Column(name = "context_gloss_en", nullable = false)
    private String contextGlossEn;
        @Column(name = "formation_rule_numbers", columnDefinition = "TEXT")
        private String formationRuleNumbers;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_confidence")
    private AnalysisConfidence analysisConfidence;

    @Column(name = "ambiguity_notes")
    private String ambiguityNotes;

    @Column(name = "vocabulary_word_id")
        private UUID vocabularyWordId;

    @OneToOne(mappedBy = "verseWord", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private VerseWordMorphology morphology;

    @OneToOne(mappedBy = "verseWord", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private VerseWordDerivation derivation;
}