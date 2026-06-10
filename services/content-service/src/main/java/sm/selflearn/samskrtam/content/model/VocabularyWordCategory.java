package sm.selflearn.samskrtam.content.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "vocabulary_word_categories", schema = "content")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VocabularyWordCategory {
    @EmbeddedId
    private VocabularyWordCategoryId id;

    @ManyToOne
    @MapsId("vocabularyWordId")
    @JoinColumn(name = "vocabulary_word_id")
    private VocabularyWord vocabularyWord;

    @ManyToOne
    @MapsId("categoryId")
    @JoinColumn(name = "category_id")
    private VocabularyCategory category;

    @Column(name = "created_at")
    private Instant createdAt;
}
