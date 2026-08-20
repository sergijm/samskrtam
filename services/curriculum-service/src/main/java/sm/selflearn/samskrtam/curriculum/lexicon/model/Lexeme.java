package sm.selflearn.samskrtam.curriculum.lexicon.model;

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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lexeme", schema = "curriculum")
public class Lexeme {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lemma_iast", nullable = false, length = 100)
    private String lemmaIast;

    @Column(name = "lemma_devanagari", nullable = false, length = 100)
    private String lemmaDevanagari;

    @Column(name = "lemma_slp1", nullable = false, length = 100)
    private String lemmaSlp1;

    @Column(name = "gloss_ru", nullable = false, length = 300)
    private String glossRu;

    @Column(name = "gloss_en", nullable = false, length = 300)
    private String glossEn;

    @Column(name = "long_definition_ru")
    private String longDefinitionRu;

    @Column(name = "long_definition_en")
    private String longDefinitionEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private LexemeGender gender;

    /** Порядковый номер значения леммы внутри написания (lexicon.md §1); первичный импорт = 1. */
    @Column(name = "meaning_number", nullable = false)
    private int meaningNumber = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "lexeme", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WordForm> wordForms = new ArrayList<>();

    @OneToMany(mappedBy = "lexeme", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LexemeFrequency> frequencies = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "lexeme_semantic_class",
        schema = "curriculum",
        joinColumns = @JoinColumn(name = "lexeme_id"),
        inverseJoinColumns = @JoinColumn(name = "semantic_class_id")
    )
    private Set<SemanticClass> semanticClasses = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "lexeme_pos",
        schema = "curriculum",
        joinColumns = @JoinColumn(name = "lexeme_id"),
        inverseJoinColumns = @JoinColumn(name = "pos_code")
    )
    private Set<PartOfSpeech> partsOfSpeech = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "lexeme_morphology",
        schema = "curriculum",
        joinColumns = @JoinColumn(name = "lexeme_id"),
        inverseJoinColumns = @JoinColumn(name = "morphology_class_code")
    )
    private Set<MorphologyClass> morphologyClasses = new HashSet<>();

    @OneToMany(mappedBy = "lexeme", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserCollectionItem> collectionItems = new ArrayList<>();

    @OneToMany(mappedBy = "lexeme", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserLexemeProgress> userProgress = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}