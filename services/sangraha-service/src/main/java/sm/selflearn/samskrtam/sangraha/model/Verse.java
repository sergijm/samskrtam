package sm.selflearn.samskrtam.sangraha.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "verses", schema = "sangraha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Verse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "text_devanagari", columnDefinition = "TEXT")
    private String textDevanagari;

    @Column(name = "text_iast", columnDefinition = "TEXT")
    private String textIast;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String  rawText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerseStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "vocabulary_quiz_slug")
    private String vocabularyQuizSlug;

    @Column(name = "vocabulary_quiz_id")
    private UUID vocabularyQuizId;

    @OneToMany(mappedBy = "verse", fetch = FetchType.LAZY)
    @Builder.Default
    private List<VerseWord> verseWords = new ArrayList<>();
}