package sm.selflearn.samskrtam.content.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyWordCategoryId implements Serializable {
    @Column(name = "vocabulary_word_id")
    private UUID vocabularyWordId;

    @Column(name = "category_id")
    private UUID categoryId;
}
